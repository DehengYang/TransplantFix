package apr.aprlab.repair.adapt.ged.patch;

import java.nio.file.Paths;
import java.util.List;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import apr.aprlab.repair.adapt.ged.action.DependentAction;
import apr.aprlab.repair.adapt.ged.action.PosRange;
import apr.aprlab.repair.adapt.patch.validate.PatchCompile;
import apr.aprlab.repair.adapt.patch.validate.strategy.TestPrioritization;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.general.CmdUtil;
import apr.aprlab.utils.general.DirUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SingleFilePatch {

    public static final Logger logger = LogManager.getLogger(SingleFilePatch.class);

    private MethodDeclaration methodDeclaration;

    private String oriFilePath;

    private String fileName;

    private String patchDirPath;

    private String patchFilePath;

    private String patchFileContent;

    private String diffFilePath;

    private String patchDiff;

    private boolean passCompilation;

    private String compileOutputDir;

    private List<DependentAction> depActions;

    private TestPrioritization testPrioritization;

    private String coveredPosFilePath;

    private boolean cleanCompileOutDir = false;

    public SingleFilePatch(List<DependentAction> depActions, String compileOutputDir, String patchDirPath, String diffFilePath, String coveredPosFilePath, boolean cleanCompileOutDir) {
        this.compileOutputDir = compileOutputDir;
        this.patchDirPath = patchDirPath;
        this.diffFilePath = diffFilePath;
        this.coveredPosFilePath = coveredPosFilePath;
        this.depActions = depActions;
        DependentAction action = depActions.get(0);
        MethodSnippet methodSnippet = action.getSrcMethodSnippet();
        methodDeclaration = methodSnippet.getMethodDeclaration();
        oriFilePath = methodSnippet.getFilePath();
        String className = methodSnippet.getClassName();
        fileName = StringUtil.getShortName(className);
        setPatchFilePath();
        testPrioritization = new TestPrioritization(passCompilation, patchDirPath, compileOutputDir, diffFilePath, coveredPosFilePath);
        this.cleanCompileOutDir = cleanCompileOutDir;
    }

    private void setPatchFilePath() {
        patchFilePath = Paths.get(patchDirPath, fileName + ".java").toString();
    }

    public boolean compile() {
        String logPath = Paths.get(patchDirPath, "compile.txt").toString();
        passCompilation = PatchCompile.runCompile(logPath, Globals.deps, Globals.jvmPath, compileOutputDir, patchFilePath, Globals.validateJarPath, cleanCompileOutDir);
        return passCompilation;
    }

    public boolean test() {
        ExceptionUtil.assertTrue(passCompilation);
        if (Globals.isForD4j2) {
            DirUtil.restoreDir(Globals.binJavaDir);
            DirUtil.copyDirectory(compileOutputDir, Globals.binJavaDir);
            testPrioritization.testForD4j2();
        } else {
            testPrioritization.test();
        }
        logger.info("test result: {}", testPrioritization.isPassed());
        return testPrioritization.isPassed();
    }

    public void setPatchDiff() {
        String cmd = String.format("diff -Naur %s %s > %s", oriFilePath, patchFilePath, diffFilePath);
        CmdUtil.runCmd(cmd);
        patchDiff = FileUtil.readFileToStr(diffFilePath);
    }

    public void savePatchFile() {
        FileUtil.writeToFile(patchFilePath, patchFileContent, false);
    }

    public void createPatch() {
        if (depActions.get(0).getSrcPosRange().isEqual(new PosRange(3105, 3205))) {
            logger.debug("");
        }
        String fileString = depActions.get(0).getInitAction().getSrcMethodSnippet().getFileString();
        int startPos = 0;
        int endPos = fileString.length();
        String newString = "";
        if (hasSameDelAreas(depActions)) {
            int cutStartPos = depActions.get(0).getStartPos();
            int cutEndPos = depActions.get(0).getEndPos();
            newString += StringUtil.substring(fileString, startPos, cutStartPos);
            for (DependentAction action : depActions) {
                newString += action.getCurPatternNewString();
            }
            startPos = cutEndPos;
        } else {
            for (DependentAction action : depActions) {
                int cutStartPos = action.getSrcPosRange().getStartPos();
                int cutEndPos = action.getSrcPosRange().getEndPos();
                newString += StringUtil.substring(fileString, startPos, cutStartPos);
                newString += action.getCurPatternNewString();
                startPos = cutEndPos;
            }
        }
        newString += StringUtil.substring(fileString, startPos, endPos);
        patchFileContent = newString;
    }

    private boolean hasSameDelAreas(List<DependentAction> depActions) {
        if (depActions.size() <= 1) {
            return false;
        }
        int startPos = depActions.get(0).getStartPos();
        int endPos = depActions.get(0).getEndPos();
        for (DependentAction depAction : depActions) {
            if (startPos != depAction.getStartPos() || endPos != depAction.getEndPos()) {
                return false;
            }
        }
        return true;
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public String getOriFilePath() {
        return oriFilePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPatchDirPath() {
        return patchDirPath;
    }

    public String getPatchFilePath() {
        return patchFilePath;
    }

    public String getPatchFileContent() {
        return patchFileContent;
    }

    public String getDiffFilePath() {
        return diffFilePath;
    }

    public String getPatchDiff() {
        return patchDiff;
    }

    public boolean isPassCompilation() {
        return passCompilation;
    }

    public String getCompileOutputDir() {
        return compileOutputDir;
    }

    public void setPatternIndex(int patternIndex) {
        for (DependentAction depAction : depActions) {
            depAction.setPatternIndex(patternIndex);
        }
    }

    public boolean isPassFailed() {
        if (testPrioritization == null) {
            return false;
        }
        return testPrioritization.isPassFailed();
    }

    public boolean isPassAll() {
        if (testPrioritization == null) {
            return false;
        }
        return testPrioritization.isPassAll();
    }

    public boolean isPassSelected() {
        if (testPrioritization == null) {
            return false;
        }
        return testPrioritization.isPassSelected();
    }

    public List<DependentAction> getDepActions() {
        return depActions;
    }

    public TestPrioritization getTestPrioritization() {
        return testPrioritization;
    }

    public String getCoveredPosFilePath() {
        return coveredPosFilePath;
    }
}
