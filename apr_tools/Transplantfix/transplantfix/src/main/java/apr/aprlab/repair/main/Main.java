package apr.aprlab.repair.main;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import apr.aprlab.repair.adapt.MethodAdaption;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.fl.PerfectFL;
import apr.aprlab.repair.fl.StandardFL;
import apr.aprlab.repair.fl.StandardTxtFL;
import apr.aprlab.repair.fl.SuspiciousMethod;
import apr.aprlab.repair.search.MethodSearchEngine;
import apr.aprlab.repair.search.SearchTypeHierarchy;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.repair.snippet.MethodSnippetBuilder;
import apr.aprlab.utils.general.DirUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.TimeUtil;

public class Main {

    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        long mainStartTime = System.currentTimeMillis();
        parseCommandLine(args);
        if (Globals.isForD4j2) {
            DirUtil.restoreDir(Globals.binJavaDir);
            DirUtil.backupDir(Globals.binJavaDir);
        }
        long startTime = System.currentTimeMillis();
        SearchTypeHierarchy.traverseProject(Globals.srcJavaDir, Globals.depList);
        logger.info("time cost of traverseProject: {}", TimeUtil.countTime(startTime));
        startTime = System.currentTimeMillis();
        List<SuspiciousMethod> standSmList = null;
        if (Globals.isForD4j2) {
            StandardTxtFL standardFL = new StandardTxtFL(Globals.flTxtPath);
            standardFL.localize();
            standSmList = standardFL.getSuspiciousMethods();
        } else {
            StandardFL standardFL = new StandardFL(Globals.localizeInfoDir);
            standardFL.localize();
            standSmList = standardFL.getSuspiciousMethods();
        }
        logger.info("time cost of get standard fl: {}", TimeUtil.countTime(startTime));
        startTime = System.currentTimeMillis();
        PerfectFL perfectFL = new PerfectFL(Globals.patchInfoDir);
        perfectFL.localize();
        perfectFL.toMethodRankingList(standSmList);
        List<SuspiciousMethod> perfectSmList = perfectFL.getSuspiciousMethods();
        logger.info("time cost of get perfect fl: {}", TimeUtil.countTime(startTime));
        logger.info("perfect sm list size: {}", perfectSmList.size());
        logger.info("standard sm list size: {}", standSmList.size());
        FileUtil.writeToFile(Globals.infoPath, PrintUtil.listToString(perfectSmList, "perfectSmList"));
        Globals.gedGraphStartIndex = 0;
        List<MethodSnippet> visitedMsList = new ArrayList<>();
        boolean getValidPatch = false;
        int cnt = 0;
        List<SuspiciousMethod> finalSuspMethods = new ArrayList<SuspiciousMethod>();
        if (Globals.isPerfectFLMode) {
            finalSuspMethods.addAll(perfectSmList);
        } else {
            finalSuspMethods.addAll(standSmList);
        }
        logger.info("finalSuspMethods list size: {}", finalSuspMethods.size());
        FileUtil.writeToFile(Globals.runInfoPath, String.format("finalSuspMethods list size: %s\n\n", finalSuspMethods.size()));
        for (SuspiciousMethod sm : finalSuspMethods) {
            if (Globals.debugMode && !Globals.debugSuspMethodNumer.isEmpty()) {
                if (Globals.debugSuspMethodNumer.get(0) == cnt) {
                    logger.debug("start to repair sm.");
                    Globals.debugSuspMethodNumer.remove(0);
                } else {
                    logger.debug("skip sm.");
                    cnt++;
                    continue;
                }
            }
            cnt++;
            Globals.suspPatchDir = DirUtil.createNumberedDir(Globals.initPatchDir, "susp_", "");
            MethodSnippetBuilder msb = new MethodSnippetBuilder(sm);
            MethodSnippet ms = msb.create();
            visitedMsList.add(ms);
            FileUtil.writeToFile(Globals.runInfoPath, String.format("current suspicious method: (%s) %s\n\n", Globals.suspMCnt++, ms.getMethodSignature()));
            Globals.donorMCnt = 0;
            if (Globals.debugMode) {
                if (!ms.getMethodSignature().toString().contains(Globals.debugSuspSnippet)) {
                    continue;
                }
            }
            MethodSearchEngine mse = new MethodSearchEngine(ms);
            List<MethodSnippet> candMsList = mse.search();
            MethodAdaption ma = new MethodAdaption(ms, candMsList);
            getValidPatch = ma.adapt();
            if (getValidPatch) {
                if (!new File(Globals.finalPatchFilePath).exists()) {
                    continue;
                } else {
                    logger.info("a valid patch is found.");
                    break;
                }
            }
        }
        logger.debug("Main time cost: {}", TimeUtil.countTime(mainStartTime));
        ExceptionUtil.programExit();
    }

    private static void parseCommandLine(String[] args) {
        Options options = new Options();
        options.addRequiredOption("patchInfoDir", "patchInfoDir", true, "Directory that contains buggy file, fixed file and patch diff.");
        options.addRequiredOption("buggyDir", "buggyDir", true, "Directory of buggy program.");
        options.addRequiredOption("dataset", "dataset", true, "dataset name");
        options.addRequiredOption("srcJavaDir", "srcJavaDir", true, "");
        options.addRequiredOption("binJavaDir", "binJavaDir", true, "");
        options.addRequiredOption("binTestDir", "binTestDir", true, "");
        options.addRequiredOption("failedTestsStr", "failedTestsStr", true, "Failed test methods.");
        options.addRequiredOption("d4jDir", "d4jDir", true, "Path of defects4j root dir.");
        options.addRequiredOption("outputDir", "outputDir", true, "Path of output dir.");
        options.addRequiredOption("dependencies", "dependencies", true, "dependencies (connected with :)");
        options.addRequiredOption("isPerfectFLMode", "isPerfectFLMode", true, "true or not");
        options.addRequiredOption("validateJarPath", "validateJarPath", true, "");
        options.addRequiredOption("jvmPath", "jvmPath", true, "");
        Option option = new Option("flTxtPath", "flTxtPath", true, "flTxtPath when the gzoltar does not work (for d4j2)");
        option.setRequired(false);
        options.addOption(option);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage() + "\n");
            formatter.printHelp(">>>>>>>>>> Help: \n\n", options);
            System.exit(0);
        }
        if (cmd.hasOption("flTxtPath") && !cmd.getOptionValue("flTxtPath").equals("None")) {
            Globals.flTxtPath = FileUtil.getCanonicalPath(cmd.getOptionValue("flTxtPath"));
            Globals.isForD4j2 = true;
        }
        Globals.isPerfectFLMode = Boolean.parseBoolean(cmd.getOptionValue("isPerfectFLMode"));
        Globals.patchInfoDir = FileUtil.getCanonicalPath(cmd.getOptionValue("patchInfoDir"));
        Globals.buggyDir = FileUtil.getCanonicalPath(cmd.getOptionValue("buggyDir"));
        Globals.srcJavaDir = FileUtil.getCanonicalPath(cmd.getOptionValue("srcJavaDir"));
        Globals.binJavaDir = FileUtil.getCanonicalPath(cmd.getOptionValue("binJavaDir"));
        Globals.binTestDir = FileUtil.getCanonicalPath(cmd.getOptionValue("binTestDir"));
        Globals.d4jDir = FileUtil.getCanonicalPath(cmd.getOptionValue("d4jDir"));
        Globals.outputDir = FileUtil.getCanonicalPath(cmd.getOptionValue("outputDir"));
        Globals.validateJarPath = FileUtil.getCanonicalPath(cmd.getOptionValue("validateJarPath"));
        Globals.jvmPath = FileUtil.getCanonicalPath(cmd.getOptionValue("jvmPath"));
        Globals.deps = cmd.getOptionValue("dependencies");
        Globals.depList = Arrays.asList(Globals.deps.split(":"));
        String failedTestsStr = cmd.getOptionValue("failedTestsStr");
        Globals.oriFailedMethods = Arrays.asList(failedTestsStr.split(":"));
        Globals.infoPath = Paths.get(Globals.outputDir, "info.txt").toAbsolutePath().toString();
        FileUtil.writeToFile(Globals.infoPath, "", false);
        Globals.runInfoPath = Paths.get(Globals.outputDir, "runInfo.txt").toAbsolutePath().toString();
        FileUtil.writeToFile(Globals.runInfoPath, "", false);
        Globals.siblingDir = Paths.get(Globals.outputDir, "siblings").toAbsolutePath().toString();
        DirUtil.delAndMkdir(Globals.siblingDir);
        Globals.initGraphDir = Paths.get(Globals.outputDir, "graph").toAbsolutePath().toString();
        DirUtil.delAndMkdir(Globals.initGraphDir);
        Globals.initPatchDir = Paths.get(Globals.outputDir, "patch").toAbsolutePath().toString();
        DirUtil.delAndMkdir(Globals.initPatchDir);
        Globals.allCandPatchPath = Paths.get(Globals.initPatchDir, "candidatePatches.diff").toString();
        Globals.localizeInfoDir = FileUtil.getCanonicalPath(Paths.get(Globals.outputDir, "..", "gzoltar"));
        ExceptionUtil.assertFileExists(Globals.localizeInfoDir);
        if (!Globals.isForD4j2) {
            String testMethodFile = Paths.get(Globals.localizeInfoDir, "test_methods.txt").toString();
            Globals.allTestMethodCnt = FileUtil.readFileToList(testMethodFile).size();
        }
        Globals.finalPatchFilePath = Paths.get(Globals.outputDir, "finalPatch.diff").toString();
        DirUtil.removeFile(Globals.finalPatchFilePath);
        Globals.allValidPatchFilePath = Paths.get(Globals.outputDir, "allValidPatches.diff").toString();
        DirUtil.removeFile(Globals.allValidPatchFilePath);
        Globals.partialFixDir = Paths.get(Globals.outputDir, "partialFixes").toString();
        DirUtil.delAndMkdir(Globals.partialFixDir);
        Globals.passedFailedCases.clear();
        Globals.partialFixes.clear();
        Globals.sootClassMap.clear();
        Globals.patchDiffs.clear();
        Globals.validPatches.clear();
        Globals.suspMCnt = 0;
        Globals.donorMCnt = 0;
    }
}
