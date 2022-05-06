package apr.module.fl.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.gzoltar.core.GZoltar;
import apr.module.fl.execute.Replicate;
import apr.module.fl.global.Globals;
import apr.module.fl.localization.FaultLocalizer;
import apr.module.fl.utils.ClassFinder;
import apr.module.fl.utils.FileUtil;
import apr.module.fl.utils.YamlUtil;

public class Main {

    final static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        long mainStartTime = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        parseCommandLine(args);
        ClassFinder cf = new ClassFinder();
        Set<String> testClasses = cf.getTestClasses(Globals.binTestDir, Globals.binJavaDir, Globals.depList);
        Set<String> srcClasses = cf.getJavaClasses(Globals.binJavaDir, Globals.depList);
        String testClassesPath = new File(Globals.outputDir).getAbsolutePath() + "/test_classes.txt";
        String srcClassesPath = new File(Globals.outputDir).getAbsolutePath() + "/src_classes.txt";
        FileUtil.writeLinesToFile(srcClassesPath, srcClasses);
        FileUtil.writeLinesToFile(testClassesPath, testClasses);
        Globals.outputData.put("time_cost_classes_collection_1", FileUtil.countTime(startTime));
        faultLocalize(testClasses, srcClasses);
        startTime = System.currentTimeMillis();
        Replicate.replicateTests(Globals.testListPath);
        Globals.outputData.put("time_cost_in_replication", FileUtil.countTime(startTime));
        Globals.outputData.put("time_cost_in_total", FileUtil.countTime(mainStartTime));
        YamlUtil.writeYaml(Globals.outputData, Globals.outputDataPath);
    }

    private static void faultLocalize(Set<String> testClasses, Set<String> srcClasses) {
        long startTime = System.currentTimeMillis();
        FaultLocalizer fl = new FaultLocalizer(Globals.rankListPath, testClasses, srcClasses);
        GZoltar gz = fl.runGzoltar();
        Globals.outputData.put("time_cost_run_fl_2", FileUtil.countTime(startTime));
        startTime = System.currentTimeMillis();
        fl.calculateSusp(gz);
        Globals.outputData.put("time_cost_calculate_susp_3", FileUtil.countTime(startTime));
    }

    private static void parseCommandLine(String[] args) {
        Options options = new Options();
        options.addRequiredOption("sjd", "srcJavaDir", true, "src folder of the buggy program (e.g., /mnt/benchmarks/repairDir/Defects4J_Mockito_10/src)");
        options.addRequiredOption("bjd", "binJavaDir", true, "bin folder of the buggy program (e.g., /mnt/benchmarks/repairDir/Defects4J_Mockito_10/build/classes/main/)");
        options.addRequiredOption("btd", "binTestDir", true, "bin test folder of the buggy program (e.g., /mnt/benchmarks/repairDir/Defects4J_Mockito_10/build/classes/test/ )");
        options.addRequiredOption("dep", "dependencies", true, "all dependencies (i.e., classpath)");
        options.addRequiredOption("wd", "workingDir", true, "path of the buggy program (e.g., /mnt/benchmarks/repairDir/Defects4J_Mockito_10/)");
        options.addRequiredOption("od", "outputDir", true, "directory to save the fl results.");
        options.addRequiredOption("jp", "jvmPath", true, "java path to run junit tests (e.g.,  /home/apr/env/jdk1.7.0_80/jre/bin/java)");
        options.addRequiredOption("ft", "failedTests", true, "expected bug triggering test(s) of the buggy program (e.g., com.google.javascript.jscomp.CollapseVariableDeclarationsTest)");
        options.addRequiredOption("epp", "externalProjPath", true, "test case executor.");
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage() + "\n");
            formatter.printHelp(">>>>>>>>>> fault localization: \n\n", options);
            System.exit(1);
        }
        Globals.srcJavaDir = cmd.getOptionValue("srcJavaDir");
        Globals.binJavaDir = cmd.getOptionValue("binJavaDir");
        Globals.binTestDir = cmd.getOptionValue("binTestDir");
        Globals.dependencies = cmd.getOptionValue("dependencies");
        Globals.jvmPath = cmd.getOptionValue("jvmPath");
        Globals.failedTests = cmd.getOptionValue("failedTests");
        Globals.workingDir = cmd.getOptionValue("workingDir");
        Globals.outputDir = cmd.getOptionValue("outputDir");
        Globals.externalProjPath = cmd.getOptionValue("externalProjPath");
        Globals.depList.addAll(Arrays.asList(Globals.dependencies.split(":")));
        Globals.oriFailedTestList = Arrays.asList(Globals.failedTests.split(":"));
        String toolOutputDir = new File(Globals.outputDir).getAbsolutePath();
        Globals.flLogPath = Paths.get(toolOutputDir, "fl.log").toString();
        Globals.rankListPath = Paths.get(toolOutputDir, "ranking_list.txt").toString();
        FileUtil.writeToFile(Globals.flLogPath, "", false);
        Globals.coveragePath = Paths.get(toolOutputDir, "coverage.txt").toString();
        Globals.testListPath = Paths.get(toolOutputDir, "test_method_list.txt").toString();
        Globals.stmtListPath = Paths.get(toolOutputDir, "stmt_list.txt").toString();
        Globals.matrixPathAgain = Paths.get(toolOutputDir, "matrix_again.txt").toString();
        Globals.testListPathAgain = Paths.get(toolOutputDir, "test_method_list_again.txt").toString();
        Globals.rankListPathAgain = Paths.get(toolOutputDir, "rank_list_again.txt").toString();
        Globals.outputDataPath = Paths.get(toolOutputDir, "output_data.yaml").toString();
        logger.info("clear outputdir: {}", Globals.outputDir);
        try {
            FileUtils.cleanDirectory(new File(Globals.outputDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
