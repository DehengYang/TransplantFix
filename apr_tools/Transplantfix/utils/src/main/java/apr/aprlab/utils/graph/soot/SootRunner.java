package apr.aprlab.utils.graph.soot;

public class SootRunner {

    public static boolean failed = false;

    private static boolean isRun = false;

    public static void runTargetClass(String classPath, String targetClass, int jdkVersion) {
        String javaLibs = "/home/apr/env/jdk1.7.0_80/jre/lib/rt.jar";
        if (jdkVersion == 8) {
            javaLibs = "/home/apr/env/jdk1.8.0_202/jre/lib/rt.jar";
        }
        soot.options.Options.v().setPhaseOption("jb", "use-original-names:true");
        String[] sootArgs = new String[] { "-cp", classPath + java.io.File.pathSeparator + javaLibs, "-keep-line-number", "-no-writeout-body-releasing", "-p", "jb", "use-original-names:true", "jap.npc", "enabled:true", "-allow-phantom-refs", "-src-prec", "apk", "-f", "J", targetClass };
        if (isRun)
            soot.G.reset();
        failed = false;
        try {
            soot.Main.main(sootArgs);
        } catch (Exception e) {
            System.out.println("[ERROR] soot run failed");
            failed = true;
        } finally {
            isRun = true;
        }
    }
}
