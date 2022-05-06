package apr.aprlab.repair.adapt.patch.validate.strategy;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import apr.aprlab.repair.adapt.patch.validate.Defects4JValidator;
import apr.aprlab.repair.adapt.patch.validate.PatchTest;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.DirUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.PrintUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestPrioritization {

    public static final Logger logger = LogManager.getLogger(TestPrioritization.class);

    private String outputDir;

    private String compileDir;

    private int ntce;

    private boolean passCompilation;

    private boolean passFailed;

    private boolean passSelected;

    private boolean passAll;

    private boolean isPartialFix;

    private List<String> oriFailedMethods = new ArrayList<String>();

    private List<String> actualFailedMethods = new ArrayList<String>();

    private List<String> actualFailedSelectedMethods = new ArrayList<String>();

    private String coveredPosFilePath;

    private String diffFilePath;

    public TestPrioritization(boolean passCompilation, String outputDir, String compileDir, String diffFilePath, String coveredPosFilePath) {
        this.passCompilation = passCompilation;
        this.outputDir = outputDir;
        this.compileDir = compileDir;
        this.coveredPosFilePath = coveredPosFilePath;
        this.diffFilePath = diffFilePath;
    }

    public boolean isPassed() {
        return passFailed && passAll;
    }

    public void setOriFailedMethods() {
        if (Globals.isForD4j2) {
            oriFailedMethods = TestUtil.obtainOriFailedMethods(Globals.buggyDir);
        } else {
            oriFailedMethods = FileUtil.readFileToList(Globals.localizeInfoDir + "/expected_failed_test_replicate.txt");
        }
        ExceptionUtil.myAssert(!oriFailedMethods.isEmpty());
    }

    public void testForD4j2() {
        resetFields();
        setOriFailedMethods();
        actualFailedMethods = Defects4JValidator.runOriFailedMethods(oriFailedMethods);
        if (!actualFailedMethods.isEmpty()) {
            passFailed = false;
            if (actualFailedMethods.size() < oriFailedMethods.size()) {
                judgeIfPartialFixForD4j2();
            }
            return;
        }
        passFailed = true;
        List<String> actualFailedAllMethods = Defects4JValidator.runAll();
        if (actualFailedAllMethods != null && actualFailedAllMethods.isEmpty()) {
            logger.info("[run all test cases] (d4j2) A valid patch is obtained.");
            FileUtil.writeToFile(Globals.runInfoPath, String.format("[run all test cases] (d4j2) A valid patch is obtained.\n\n"));
            passSelected = true;
            passAll = true;
        } else {
            logger.info("[run all test cases] (d4j2) The patch passing selected test cases failed!");
        }
    }

    private void judgeIfPartialFixForD4j2() {
        List<String> actualFailedAllMethods = Defects4JValidator.runAll();
        if (actualFailedAllMethods != null) {
            actualFailedAllMethods.removeAll(oriFailedMethods);
            if (actualFailedAllMethods.isEmpty()) {
                savePartialFix();
                isPartialFix = true;
            }
        }
    }

    public void test() {
        resetFields();
        setOriFailedMethods();
        String extraFailedMethodPath = Paths.get(Globals.localizeInfoDir, "extra_failed_test_replicate.txt").toString();
        String outputFileName = "prioritize.txt";
        PatchTest pt = new PatchTest(outputDir, outputFileName, Globals.jvmPath, Globals.validateJarPath, Globals.binJavaDir, Globals.binTestDir, Globals.deps, extraFailedMethodPath);
        pt.configure(oriFailedMethods, true, false);
        actualFailedMethods = pt.runTests(compileDir);
        logger.info("run original failed test cases: {} [{}/{}]", actualFailedMethods.isEmpty(), actualFailedMethods.size(), oriFailedMethods.size());
        if (!actualFailedMethods.isEmpty()) {
            passFailed = false;
            if (actualFailedMethods.size() < oriFailedMethods.size()) {
                judgeIfPartialFix(pt);
            }
            return;
        }
        passFailed = true;
        List<String> coveredTestCases = FileUtil.readFileToList(coveredPosFilePath);
        if (coveredTestCases.size() >= oriFailedMethods.size()) {
            pt.configure(coveredPosFilePath, true, true);
            actualFailedSelectedMethods = pt.runTests(compileDir);
            ntce = coveredTestCases.size() + oriFailedMethods.size();
            logger.info("run selected positive test cases: {} ({})", actualFailedSelectedMethods.isEmpty(), coveredTestCases.size());
            if (actualFailedSelectedMethods.isEmpty()) {
                passSelected = true;
            } else {
                return;
            }
        } else {
            passSelected = passFailed;
        }
        String allTestFilePath = Globals.localizeInfoDir + "/positive_test_replicate.txt";
        pt.configure(allTestFilePath, true, true);
        logger.info("now we run all tests!");
        List<String> actualFailedAllMethods = pt.runTests(compileDir);
        if (actualFailedAllMethods.isEmpty()) {
            logger.info("[run all test cases] ({}) A valid patch is obtained.", Globals.allTestMethodCnt);
            FileUtil.writeToFile(Globals.runInfoPath, String.format("[run all test cases] (%s) A valid patch is obtained.\n\n", Globals.allTestMethodCnt));
            passAll = true;
        } else {
            logger.info("[run all test cases] ({}) The patch passing selected test cases failed!", Globals.allTestMethodCnt);
            passAll = false;
        }
    }

    public void resetFields() {
        passFailed = false;
        passSelected = false;
        passAll = false;
        isPartialFix = false;
    }

    private void judgeIfPartialFix(PatchTest pt) {
        if (actualFailedMethods.size() < oriFailedMethods.size()) {
            String allTestFilePath = Globals.localizeInfoDir + "/positive_test_replicate.txt";
            pt.configure(allTestFilePath, true, true);
            logger.info("now we run all tests!");
            List<String> actualFailedAllMethods = pt.runTests(compileDir);
            if (actualFailedAllMethods.isEmpty()) {
                savePartialFix();
                isPartialFix = true;
            }
        }
    }

    private void savePartialFix() {
        logger.info("[is a partial fix] ({})", Globals.allTestMethodCnt);
        FileUtil.writeToFile(Globals.runInfoPath, String.format("[is a partial fix] (%s)", Globals.allTestMethodCnt));
        String partialFixFilePath = DirUtil.createNumberedFile(Globals.partialFixDir, "partial_fix_", ".diff");
        DirUtil.copyFileToFile(diffFilePath, partialFixFilePath);
        FileUtil.writeToFile(partialFixFilePath, String.format("\n\npassed failed methods: %s", PrintUtil.listToString(CollectionUtil.getUniqueInSrc(oriFailedMethods, actualFailedMethods))), true);
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getCompileDir() {
        return compileDir;
    }

    public int getNtce() {
        return ntce;
    }

    public boolean isPassFailed() {
        return passFailed;
    }

    public boolean isPassAll() {
        return passAll;
    }

    public List<String> getOriFailedMethods() {
        return oriFailedMethods;
    }

    public List<String> getActualFailedMethods() {
        return actualFailedMethods;
    }

    public boolean isPartialFix() {
        return isPartialFix;
    }

    public List<String> getPassedFailedCases() {
        List<String> oriFailed = new ArrayList<String>(oriFailedMethods);
        oriFailed.removeAll(actualFailedMethods);
        return oriFailed;
    }

    public boolean isPassCompilation() {
        return passCompilation;
    }

    public boolean isPassSelected() {
        return passSelected;
    }

    public List<String> getActualFailedSelectedMethods() {
        return actualFailedSelectedMethods;
    }

    public String getCoveredPosFilePath() {
        return coveredPosFilePath;
    }

    public String getDiffFilePath() {
        return diffFilePath;
    }
}
