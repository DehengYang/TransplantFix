package apr.aprlab.repair.fl.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.fl.SuspiciousStmt;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.Pair;
import apr.aprlab.utils.general.PrintUtil;

public class StandardFLParser {

    private static final Logger logger = LogManager.getLogger(StandardFLParser.class);

    public static List<TestCase> readTestFile(String path) {
        List<TestCase> testsList = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0)
                    logger.error("Empty line in %s", path);
                String testName = line.split(",")[0];
                boolean passed = true;
                if (line.split(",")[1].equals("false")) {
                    passed = false;
                }
                TestCase testImpl = new TestCase(testName, passed);
                testsList.add(testImpl);
            }
            in.close();
        } catch (final IOException e) {
            ExceptionUtil.raise("IOExcepiton");
        }
        return testsList;
    }

    public static List<SuspiciousStmt> readStmtFile(String path) {
        List<SuspiciousStmt> stmtList = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0) {
                    ExceptionUtil.raise("should not be an empty line");
                }
                SuspiciousStmt sl = new SuspiciousStmt(line.split(":")[0], Integer.parseInt(line.split(":")[1]));
                stmtList.add(sl);
            }
            logger.info(String.format("The total suspicious statements: %d", stmtList.size()));
            in.close();
        } catch (final IOException e) {
            ExceptionUtil.raise("IOExcepiton");
        }
        return stmtList;
    }

    public static List<SuspiciousStmt> readBuggylocFile(String path) {
        List<SuspiciousStmt> buggyLocs = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0) {
                    ExceptionUtil.raise("Empty line in %s", path);
                }
                String[] loc = line.split(",")[0].split(":");
                SuspiciousStmt sl = new SuspiciousStmt(loc[0], Integer.parseInt(loc[1]));
                buggyLocs.add(sl);
            }
            in.close();
        } catch (final IOException e) {
            ExceptionUtil.raise("IOExcepiton");
        }
        return buggyLocs;
    }

    public static List<SuspiciousStmt> readBuggylocFileFromRecoderFl(String path) {
        List<SuspiciousStmt> buggyLocs = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0) {
                    logger.warn("Empty line in {}", path);
                    continue;
                }
                String[] loc = line.split("\\s+")[0].split("#");
                double suspValue = Double.parseDouble(line.split("\\s+")[1]);
                SuspiciousStmt sl = new SuspiciousStmt(loc[0], Integer.parseInt(loc[1]), suspValue);
                buggyLocs.add(sl);
            }
            in.close();
        } catch (final IOException e) {
            ExceptionUtil.raise("IOExcepiton");
        }
        return buggyLocs;
    }

    public static List<Pair<List<Integer>, String>> readMatrixFile(String path) {
        List<Pair<List<Integer>, String>> matrixList = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            int cnt = 0;
            int unrelatedTestCnt = 0;
            while ((line = in.readLine()) != null) {
                if (line.length() == 0)
                    logger.error(String.format("Empty line in %s", path));
                String testResult = line.substring(line.length() - 1);
                if (!testResult.equals("+") && !testResult.equals("-")) {
                    logger.error(String.format("Unknown testResult: %s", testResult));
                }
                List<Integer> coveredStmtIndexList = new ArrayList<>();
                String coverage = line.replace(" ", "");
                int index = -1;
                while ((index = coverage.indexOf("1", index + 1)) >= 0) {
                    coveredStmtIndexList.add(index);
                }
                if (coveredStmtIndexList.size() == 0) {
                    logger.info(String.format("The test case (index: %s) is not executed by any stmts in Spectra", cnt));
                    unrelatedTestCnt++;
                }
                cnt++;
                matrixList.add(new Pair<>(coveredStmtIndexList, testResult));
            }
            logger.info(String.format("The unrelated test cases: %d", unrelatedTestCnt));
            logger.info(String.format("The total test cases: %d", cnt));
            in.close();
        } catch (final IOException e) {
            ExceptionUtil.raise("IOExcepiton");
        }
        return matrixList;
    }

    public static Pair<Integer, Integer> parseMatrixFile(String coveragePath, List<SuspiciousStmt> slList, List<TestCase> testList) {
        int specSize = slList.size();
        List<Integer> fakeTestCaseIndices = new ArrayList<>();
        int totalPassedCnt = 0;
        int totalFailedCnt = 0;
        for (int i = 0; i < testList.size(); i++) {
            TestCase tc = testList.get(i);
            if (tc.isSuccessful()) {
                totalPassedCnt++;
            } else {
                if (Globals.oriFailedMethods.contains(tc.getTestName())) {
                    totalFailedCnt++;
                    logger.debug("failed test case: {}", tc);
                } else {
                    fakeTestCaseIndices.add(i);
                    logger.debug("extra failed test case: {}", tc);
                }
            }
        }
        List<String> bitSetStrings = FileUtil.readFileToList(coveragePath);
        List<BitSet> bitSets = toBitSets(bitSetStrings);
        ExceptionUtil.myAssert(bitSets.size() == specSize);
        for (int stmtIndex = 0; stmtIndex < specSize; stmtIndex++) {
            BitSet coverage = bitSets.get(stmtIndex);
            BitSet newCoverage = removeFakeTestCaseIndices(coverage, fakeTestCaseIndices);
            SuspiciousStmt sl = slList.get(stmtIndex);
            sl.setCoverage(newCoverage);
            sl.calculateSuspicious(testList, totalPassedCnt, totalFailedCnt);
        }
        slList.sort(ComparatorUtil.suspComparator);
        FileUtil.writeToFile(Paths.get(Globals.outputDir, "fl_ranking_list.txt"), PrintUtil.listToStringForStorage(slList), false);
        return new Pair<>(totalPassedCnt, totalFailedCnt);
    }

    private static BitSet removeFakeTestCaseIndices(BitSet coverage, List<Integer> fakeTestCaseIndices) {
        BitSet newCoverage = new BitSet();
        for (int j = coverage.nextSetBit(0); j >= 0; j = coverage.nextSetBit(j + 1)) {
            if (j == Integer.MAX_VALUE) {
                ExceptionUtil.raise();
                break;
            }
            if (!fakeTestCaseIndices.contains(j)) {
                newCoverage.set(j);
            }
        }
        return newCoverage;
    }

    private static List<BitSet> toBitSets(List<String> bitSetStrings) {
        List<BitSet> bitSets = new ArrayList<BitSet>();
        for (String bitSetString : bitSetStrings) {
            ExceptionUtil.myAssert(bitSetString.startsWith("{"));
            ExceptionUtil.myAssert(bitSetString.endsWith("}"));
            bitSetString = bitSetString.substring(1, bitSetString.length() - 1);
            String[] bits = bitSetString.split(", ");
            BitSet bitSet = new BitSet(bits.length);
            for (String bit : bits) {
                bitSet.set(Integer.parseInt(bit));
            }
            bitSets.add(bitSet);
        }
        return bitSets;
    }
}
