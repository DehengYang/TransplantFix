package apr.aprlab.repair.adapt.patch.validate;

import apr.aprlab.utils.general.CmdUtil;

public class PatchCompile {

    public static boolean runCompile(String logFilePath, String dependencies, String jvmPath, String outputDirPath, String tmpPatchFile, String externalProjPath, boolean cleanCompileOutDir) {
        String cmd = "";
        cmd += jvmPath + "/java" + " -cp ";
        cmd += externalProjPath;
        cmd += String.format(" apr.junit.MainCompile --logFilePath %s " + "--dependencies %s --outputDirPath %s " + "--jvmPath %s --tmpPatchFile %s --cleanCompileOutDir %s", logFilePath, dependencies, outputDirPath, jvmPath, tmpPatchFile, cleanCompileOutDir);
        String output = CmdUtil.runCmd(cmd, true, false);
        boolean passed = false;
        for (String line : output.split("\n")) {
            if (line.startsWith("[compilation result] ")) {
                String str = line.trim().split(" ")[2];
                passed = Boolean.parseBoolean(str);
                break;
            }
        }
        return passed;
    }
}
