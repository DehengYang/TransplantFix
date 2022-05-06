package apr.junit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import apr.junit.utils.TimeOut;

public class PatchTest {

    private static boolean printTrace = true;

    private static boolean earlyExit = false;

    private static List<String> testsToRun;

    private static List<String> failedTestMethods = new ArrayList<>();

    private static List<String> extraFailedTestMethods = new ArrayList<>();

    private static Result result;

    private static int testCnt = 0;

    private static String savePath = "";

    private static Map<String, String> parameters = new HashMap<>();

    public static void main(final String[] args) {
        int timeout = 30;
        final long startTime = System.currentTimeMillis();
        try {
            TimeOut.runWithTimeout(new Runnable() {

                @Override
                public void run() {
                    mainTask(args);
                }
            }, timeout, TimeUnit.MINUTES);
        } catch (Exception e1) {
            failedTestMethods.add(String.format("test execution timeout! (%s min)", timeout));
            e1.printStackTrace();
        } catch (Error er) {
            failedTestMethods.add(String.format("test execution timeout! (%s min)", timeout));
            er.printStackTrace();
        } finally {
            System.out.format("total test size: %s, executed test size: %s\n", testsToRun.size(), testCnt);
            if (parameters.containsKey("savePath")) {
                saveFailedMethods(savePath);
            }
        }
        System.out.format("time used: %s\n", countTime(startTime));
        System.exit(0);
    }

    public static List<String> readFileToList(String path) {
        List<String> list = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                list.add(line);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    public static void mainTask(String[] args) {
        setParameters(args);
        if (parameters.containsKey("extraFailedMethodPath")) {
            extraFailedTestMethods = readFileToList(parameters.get("extraFailedMethodPath"));
        }
        if (parameters.containsKey("testFile")) {
            String filePath = parameters.get("testFile");
            testsToRun = readFile(filePath);
        } else if (parameters.containsKey("testStr")) {
            String testStr = parameters.get("testStr");
            testsToRun = Arrays.asList(testStr.trim().split(File.pathSeparator));
        }
        if (parameters.containsKey("runTestMethods") && parameters.get("runTestMethods").equals("true")) {
            testsToRun.removeAll(extraFailedTestMethods);
            runTestMethods(testsToRun);
        } else {
            if (savePath.contains("replicate")) {
                runTestsWithNoStop(testsToRun);
            } else {
                runTests(testsToRun);
            }
        }
    }

    public static String countTime(long startTime) {
        DecimalFormat dF = new DecimalFormat("0.0000");
        return dF.format((float) (System.currentTimeMillis() - startTime) / 1000);
    }

    private static void saveFailedMethods(String savePath) {
        writeLinesToFile(savePath, failedTestMethods, false);
    }

    public static void writeLinesToFile(String path, List<String> lines, boolean append) {
        String dirPath = path.substring(0, path.lastIndexOf("/"));
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println(String.format("%s does not exists, and are created now via mkdirs()", dirPath));
        }
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(path, append));
            for (String line : lines) {
                output.write(line + "\n");
            }
            output.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeLinesToFile(String path, String line, boolean append) {
        String dirPath = path.substring(0, path.lastIndexOf("/"));
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println(String.format("%s does not exists, and are created now via mkdirs()", dirPath));
        }
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(path, append));
            output.write(line + "\n");
            output.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static void runTestMethods(List<String> testMethods) {
        System.out.format("test methods size for execution: %d\n", testMethods.size());
        long startT = System.currentTimeMillis();
        int timeout = 20;
        DecimalFormat dF = new DecimalFormat("0.0000");
        for (String testMethod : testMethods) {
            runSingleMethod(testMethod, timeout);
            if (earlyExit) {
                if (!failedTestMethods.isEmpty()) {
                    System.out.format("failedTestMethods is not empty. early exit now.\n");
                    break;
                }
            }
        }
        System.out.format("[Junit test] failed test methods size after execution: %d\n", failedTestMethods.size());
        for (String failed : failedTestMethods) {
            System.out.format("[Junit test] failed test method: %s\n", failed);
        }
        System.out.format("[Junit test] Total time cost for running the test method(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT) / 1000));
    }

    private static void runSingleMethod(String testMethod, int timeout) {
        DecimalFormat dF = new DecimalFormat("0.0000");
        String className = testMethod.split("#")[0];
        String methodName = testMethod.split("#")[1];
        try {
            final Request request = Request.method(Class.forName(className), methodName);
            testCnt++;
            try {
                TimeOut.runWithTimeout(new Runnable() {

                    @Override
                    public void run() {
                        result = new JUnitCore().run(request);
                    }
                }, timeout, TimeUnit.SECONDS);
            } catch (Exception e1) {
                System.out.format("failed method execution timeout: %s\n", className + "#" + methodName);
                failedTestMethods.add(className + "#" + methodName);
                e1.printStackTrace();
                return;
            }
            if (result == null) {
                failedTestMethods.add(className + "#" + methodName);
                System.out.format("failed method execution: %s (result is null)\n", className + "#" + methodName);
                return;
            } else if (!result.wasSuccessful()) {
                failedTestMethods.add(className + "#" + methodName);
                if (printTrace) {
                    for (Failure failure : result.getFailures()) {
                        System.out.format("failed trace info: %s\n", failure.getTrace());
                        System.out.format("failed trace description: %s\n", failure.getDescription());
                    }
                }
                System.out.format("failed method execution time cost: %s\n", dF.format((float) result.getRunTime() / 1000));
                return;
            }
        } catch (ClassNotFoundException e) {
            failedTestMethods.add(className + "#" + methodName);
            e.printStackTrace();
            return;
        } catch (StackOverflowError e) {
            failedTestMethods.add(className + "#" + methodName);
            System.out.format("StackOverflowError. Now exit.\n");
            e.printStackTrace();
            return;
        } catch (java.lang.Error e) {
            failedTestMethods.add(className + "#" + methodName);
            System.out.format("Other error. Now exit.\n");
            e.printStackTrace();
            return;
        }
    }

    public static void runTests(List<String> tests) {
        List<String> failedTests = new ArrayList<>();
        System.out.format("tests size for execution: %d\n", tests.size());
        long startT = System.currentTimeMillis();
        int cnt = 0;
        int size = tests.size();
        int timeout = 600;
        DecimalFormat dF = new DecimalFormat("0.0000");
        for (String test : tests) {
            String className = test;
            try {
                final Request request = Request.aClass(Class.forName(className));
                System.out.format("current test: %s\n", className);
                try {
                    TimeOut.runWithTimeout(new Runnable() {

                        @Override
                        public void run() {
                            result = new JUnitCore().run(request);
                        }
                    }, timeout, TimeUnit.SECONDS);
                } catch (Exception e1) {
                    System.out.format("failed test class execution timeout: %s\n", className);
                    failedTests.add(className);
                    failedTestMethods.add(className);
                    e1.printStackTrace();
                    return;
                }
                if (result == null) {
                    failedTests.add(test);
                    failedTestMethods.add(className);
                    System.out.format("failed test class execution: %s (result is null)\n", className);
                    return;
                } else if (!result.wasSuccessful()) {
                    failedTests.add(test);
                    if (printTrace) {
                        for (Failure failure : result.getFailures()) {
                            System.out.format("failed trace info: %s\n", failure.getTrace());
                            System.out.format("failed trace description: %s\n", failure.getDescription());
                            String desp = failure.getDescription().toString().trim();
                            if (desp.contains("(") && desp.contains(")")) {
                                String failedTestClassName = desp.split("\\(")[1].split("\\)")[0];
                                String failedTestMethodName = desp.split("\\(")[0];
                                failedTestMethods.add(failedTestClassName + "#" + failedTestMethodName);
                            } else {
                                failedTestMethods.add(desp + "# ");
                            }
                        }
                    }
                    return;
                }
                cnt = cnt + result.getRunCount();
                testCnt++;
                System.out.format("[%d/%d] number of executed tests: %d, time cost: %s\n", testCnt, size, result.getRunCount(), dF.format((float) result.getRunTime() / 1000));
            } catch (ClassNotFoundException e) {
                failedTests.add(test);
                failedTestMethods.add(className);
                e.printStackTrace();
                return;
            } catch (StackOverflowError e) {
                failedTests.add(test);
                failedTestMethods.add(className);
                System.out.format("StackOverflowError. Now exit.\n");
                e.printStackTrace();
                return;
            } catch (java.lang.Error e) {
                failedTests.add(test);
                failedTestMethods.add(className);
                System.out.format("Other error. Now exit.\n");
                e.printStackTrace();
                return;
            }
        }
        System.out.format("[Junit test] total test cases in execution: %d\n", cnt);
        System.out.format("[Junit test] failed tests size after execution: %d\n", failedTests.size());
        for (String failed : failedTests) {
            System.out.format("[Junit test] failed test: %s\n", failed);
        }
        System.out.format("[Junit test] failed test methods size after execution: %d\n", failedTestMethods.size());
        for (String failed : failedTestMethods) {
            System.out.format("[Junit test] failed test method: %s\n", failed);
        }
        System.out.format("[Junit test] Total time cost for running the test(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT) / 1000));
    }

    public static void runTestsWithNoStop(List<String> tests) {
        List<String> failedTests = new ArrayList<>();
        System.out.format("tests size for execution: %d\n", tests.size());
        long startT = System.currentTimeMillis();
        int cnt = 0;
        int size = tests.size();
        int timeout = 600;
        DecimalFormat dF = new DecimalFormat("0.0000");
        for (String test : tests) {
            String className = test;
            try {
                final Request request = Request.aClass(Class.forName(className));
                System.out.format("current test: %s\n", className);
                try {
                    TimeOut.runWithTimeout(new Runnable() {

                        @Override
                        public void run() {
                            result = new JUnitCore().run(request);
                        }
                    }, timeout, TimeUnit.SECONDS);
                } catch (Exception e1) {
                    System.out.format("failed test class execution timeout: %s\n", className);
                    failedTests.add(className);
                    failedTestMethods.add(className);
                    e1.printStackTrace();
                    continue;
                }
                if (result == null) {
                    failedTests.add(test);
                    failedTestMethods.add(className);
                    System.out.format("failed test class execution: %s (result is null)\n", className);
                    return;
                } else if (!result.wasSuccessful()) {
                    failedTests.add(test);
                    if (printTrace) {
                        for (Failure failure : result.getFailures()) {
                            System.out.format("failed trace info: %s\n", failure.getTrace());
                            System.out.format("failed trace description: %s\n", failure.getDescription());
                            String desp = failure.getDescription().toString().trim();
                            if (desp.contains("(") && desp.contains(")")) {
                                String failedTestClassName = desp.split("\\(")[1].split("\\)")[0];
                                String failedTestMethodName = desp.split("\\(")[0];
                                failedTestMethods.add(failedTestClassName + "#" + failedTestMethodName);
                            } else {
                                failedTestMethods.add(desp + "# ");
                            }
                        }
                    }
                }
                cnt = cnt + result.getRunCount();
                testCnt++;
                System.out.format("[%d/%d] number of executed tests: %d, time cost: %s\n", testCnt, size, result.getRunCount(), dF.format((float) result.getRunTime() / 1000));
            } catch (ClassNotFoundException e) {
                failedTests.add(test);
                failedTestMethods.add(className);
                e.printStackTrace();
            } catch (StackOverflowError e) {
                failedTests.add(test);
                failedTestMethods.add(className);
                System.out.format("StackOverflowError. Now exit.\n");
                e.printStackTrace();
            } catch (java.lang.Error e) {
                failedTests.add(test);
                failedTestMethods.add(className);
                System.out.format("Other error. Now exit.\n");
                e.printStackTrace();
            }
        }
        System.out.format("[Junit test] total test cases in execution: %d\n", cnt);
        System.out.format("[Junit test] failed tests size after execution: %d\n", failedTests.size());
        for (String failed : failedTests) {
            System.out.format("[Junit test] failed test: %s\n", failed);
        }
        System.out.format("[Junit test] failed test methods size after execution: %d\n", failedTestMethods.size());
        for (String failed : failedTestMethods) {
            System.out.format("[Junit test] failed test method: %s\n", failed);
        }
        System.out.format("[Junit test] Total time cost for running the test(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT) / 1000));
    }

    public static void runTestMethodsWithoutStop(List<String> testMethods) {
        System.out.format("test methods size for execution: %d\n", testMethods.size());
        long startT = System.currentTimeMillis();
        int timeout = 200;
        DecimalFormat dF = new DecimalFormat("0.0000");
        for (String testMethod : testMethods) {
            String className = testMethod.split("#")[0];
            String methodName = testMethod.split("#")[1];
            try {
                final Request request = Request.method(Class.forName(className), methodName);
                try {
                    TimeOut.runWithTimeout(new Runnable() {

                        @Override
                        public void run() {
                            result = new JUnitCore().run(request);
                        }
                    }, timeout, TimeUnit.SECONDS);
                } catch (Exception e1) {
                    System.out.format("failed method execution timeout: %s\n", className + "#" + methodName);
                    failedTestMethods.add(className + "#" + methodName);
                    e1.printStackTrace();
                    continue;
                }
                if (result == null) {
                    failedTestMethods.add(className + "#" + methodName);
                    System.out.format("failed method execution: %s (result is null)\n", className + "#" + methodName);
                } else if (!result.wasSuccessful()) {
                    failedTestMethods.add(className + "#" + methodName);
                    if (printTrace) {
                        for (Failure failure : result.getFailures()) {
                            System.out.format("failed trace info: %s\n", failure.getTrace());
                            System.out.format("failed trace description: %s\n", failure.getDescription());
                        }
                    }
                    System.out.format("failed method execution time cost: %s\n", dF.format((float) result.getRunTime() / 1000));
                }
            } catch (ClassNotFoundException e) {
                failedTestMethods.add(className + "#" + methodName);
                e.printStackTrace();
            } catch (StackOverflowError e) {
                failedTestMethods.add(className + "#" + methodName);
                System.out.format("StackOverflowError. Now exit.\n");
                e.printStackTrace();
            } catch (java.lang.Error e) {
                failedTestMethods.add(className + "#" + methodName);
                System.out.format("Other error. Now exit.\n");
                e.printStackTrace();
            }
        }
        System.out.format("[Junit test] failed test methods size after execution: %d\n", failedTestMethods.size());
        for (String failed : failedTestMethods) {
            System.out.format("[Junit test] failed test method: %s\n", failed);
        }
        System.out.format("[Junit test] Total time cost for running the test method(s): %s\n", dF.format((float) (System.currentTimeMillis() - startT) / 1000));
    }

    public static void runCmdNoOutput(String cmd) {
        try {
            String[] commands = { "bash", "-c", cmd };
            Process proc = Runtime.getRuntime().exec(commands);
            proc.getInputStream();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private static List<String> readFile(String path) {
        List<String> list = new ArrayList<>();
        try {
            final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0)
                    System.err.println(String.format("Empty line in %s", path));
                if (line.trim().endsWith(",false") || line.trim().endsWith(",true")) {
                    list.add(line.split(",")[0]);
                } else {
                    list.add(line);
                }
            }
            in.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static void setParameters(String[] args) {
        Option opt1 = new Option("testFile", "testFile", true, "a file containing tests like org.apache.commons.math3.stat.StatUtilsTest#testSumLog (test methods or tests)");
        opt1.setRequired(false);
        Option opt2 = new Option("testStr", "testStr", true, "some tests linked with : (test methods or tests)");
        opt2.setRequired(false);
        Option opt3 = new Option("runTestMethods", "runTestMethods", true, "true if run test methods");
        opt3.setRequired(false);
        Option opt4 = new Option("savePath", "savePath", true, "save failed test methods");
        opt4.setRequired(false);
        Option opt5 = new Option("extraFailedMethodPath", "extraFailedMethodPath", true, "file path which records the unexpectedly failed test methods.");
        opt5.setRequired(false);
        Option opt6 = new Option("earlyExit", "earlyExit", true, "return if a failed test is found.");
        opt6.setRequired(false);
        Options options = new Options();
        options.addOption(opt1);
        options.addOption(opt2);
        options.addOption(opt3);
        options.addOption(opt4);
        options.addOption(opt5);
        options.addOption(opt6);
        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        try {
            cli = cliParser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            helpFormatter.printHelp(">>>>>> options", options);
            e.printStackTrace();
        }
        if (cli.hasOption("testFile")) {
            parameters.put("testFile", cli.getOptionValue("testFile"));
        }
        if (cli.hasOption("testStr")) {
            parameters.put("testStr", cli.getOptionValue("testStr"));
        }
        if (cli.hasOption("runTestMethods")) {
            parameters.put("runTestMethods", cli.getOptionValue("runTestMethods"));
        }
        if (cli.hasOption("savePath")) {
            parameters.put("savePath", cli.getOptionValue("savePath"));
            savePath = parameters.get("savePath");
        }
        if (cli.hasOption("extraFailedMethodPath")) {
            parameters.put("extraFailedMethodPath", cli.getOptionValue("extraFailedMethodPath"));
        }
        if (cli.hasOption("earlyExit")) {
            earlyExit = Boolean.parseBoolean(cli.getOptionValue("earlyExit"));
        }
    }
}
