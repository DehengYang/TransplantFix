package apr.aprlab.repair.adapt.patch.validate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.utils.general.CmdUtil;
import apr.aprlab.utils.general.FileUtil;

public class PatchTest {

    String testFilePath;

    private List<String> testCases;

    private String jvmPath;

    private String classpath;

    private String externalProjPath;

    private String savePath;

    private String replicateLogPath;

    private String binJavaDir;

    private String binTestDir;

    private String extraFailedMethodPath;

    String flag;

    private boolean earlyExit;

    boolean runTestMethods = false;

    public PatchTest(String saveDir, String saveName, String jvmPath, String externalProjPath, String binJavaDir, String binTestDir, String classpath, String extraFailedMethodPath) {
        this.jvmPath = jvmPath;
        this.externalProjPath = externalProjPath;
        this.binJavaDir = binJavaDir;
        this.binTestDir = binTestDir;
        this.extraFailedMethodPath = extraFailedMethodPath;
        this.classpath = classpath;
        this.savePath = saveDir + "/" + saveName;
        replicateLogPath = this.savePath + ".log";
    }

    public void configure(String testFilePath, boolean runTestMethods, boolean earlyExit) {
        this.testFilePath = testFilePath;
        this.runTestMethods = runTestMethods;
        flag = "file";
        this.earlyExit = earlyExit;
    }

    public void configure(List<String> testCases, boolean runTestMethods, boolean earlyExit) {
        this.testCases = testCases;
        this.runTestMethods = runTestMethods;
        flag = "str";
        this.earlyExit = earlyExit;
    }

    public void runTests() {
        runTests(null);
    }

    public List<String> runTests(String compileDir) {
        String cmd = "";
        cmd += String.format("cd %s;\n", Globals.buggyDir);
        cmd += this.jvmPath + "/java" + " -cp ";
        cmd += this.externalProjPath + File.pathSeparator;
        if (compileDir != null) {
            cmd += compileDir + File.pathSeparator;
        }
        if (!this.classpath.contains(this.binJavaDir)) {
            cmd += this.binJavaDir + File.pathSeparator;
        }
        if (!this.classpath.contains(this.binTestDir)) {
            cmd += this.binTestDir + File.pathSeparator;
        }
        cmd += this.classpath;
        if (flag.equals("str")) {
            cmd += " apr.junit.PatchTest -testStr ";
            for (String test : testCases) {
                cmd += test.trim() + File.pathSeparator;
            }
        } else if (flag.equals("file")) {
            cmd += " apr.junit.PatchTest -testFile " + testFilePath;
        } else {
            System.out.format("unknown flag of PatchTest: %s\n", flag);
        }
        if (runTestMethods) {
            cmd += " -runTestMethods true";
        }
        if (extraFailedMethodPath != null) {
            cmd += " -extraFailedMethodPath " + extraFailedMethodPath;
        }
        cmd += String.format(" -earlyExit %s", earlyExit);
        cmd += " -savePath " + savePath + " > " + replicateLogPath + " 2>&1";
        CmdUtil.runCmd(cmd, true, true);
        FileUtil.writeToFile(replicateLogPath, "\n" + cmd);
        if (!new File(savePath).exists()) {
            return new ArrayList<>();
        }
        List<String> failedMethodsAfterTest = FileUtil.readFileToList(savePath);
        return failedMethodsAfterTest;
    }
}
