package apr.aprlab.repair.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import apr.aprlab.repair.adapt.ged.action.DependentAction;
import soot.SootClass;

public class Globals {

    public static int suspMCnt = 0;

    public static int donorMCnt = 0;

    public static boolean isPerfectFLMode = true;

    public static boolean considerSiblingRelation = true;

    public static List<String> siblingsAndParents = new ArrayList<>();

    public static boolean includeParentClasses = true;

    public static boolean considerAllFiles = true;

    public static long gedTimeLimit = 3000;

    public static int maxCandidateSnippetCnt = 50;

    public static int saveCandidateSnippetCnt = 300;

    public static String[] inclusionFilter = new String[] {};

    public static boolean debugMode = false;

    public static List<Integer> debugSuspMethodNumer = new ArrayList<Integer>();

    public static List<Integer> debugMsNumer = new ArrayList<Integer>();

    public static String debugDonorSnippet = "";

    public static String debugSuspSnippet = "";

    public static String patchInfoDir;

    public static String buggyDir;

    public static String dataset;

    public static String srcJavaDir;

    public static String binJavaDir;

    public static String binTestDir;

    public static List<String> oriFailedMethods;

    public static String d4jDir;

    public static String outputDir;

    public static String deps;

    public static List<String> depList;

    public static String infoPath;

    public static String runInfoPath;

    public static String localizeInfoDir;

    public static String siblingDir;

    public static String initGraphDir;

    public static String donorGraphDir;

    public static String suspGraphDir;

    public static String donorPatchDir;

    public static String initPatchDir;

    public static String suspPatchDir;

    public static String validateJarPath;

    public static String jvmPath;

    public static int allTestMethodCnt;

    public static int patchCnt = 0;

    public static String allCandPatchPath;

    public static String donorCandPatchPath;

    public static boolean munkresHasException = false;

    public static int gedGraphStartIndex;

    public static List<DependentAction> partialFixes = new ArrayList<>();

    public static List<Integer> partialFixesPatternIndices = new ArrayList<Integer>();

    public static List<String> passedFailedCases = new ArrayList<String>();

    public static String isStartThisOrPara = "isStartThisOrPara";

    public static String finalPatchFilePath;

    public static String allValidPatchFilePath;

    public static String partialFixDir;

    public static List<String> patchDiffs = new ArrayList<String>();

    public static List<String> validPatches = new ArrayList<String>();

    public static int debuggingMsCnt;

    public static int msCnt = 0;

    public static Map<String, SootClass> sootClassMap = new HashMap<>();

    public static String flTxtPath;

    public static boolean isForD4j2;

    public static int singleTestTimeout = 50;

    public static int allTestTimeout = 250;

    public static boolean hasTraversalIssues = false;

    public static boolean hasTodoIssues = false;

    public static boolean isStillPartialFix() {
        boolean is = passedFailedCases.size() != Globals.oriFailedMethods.size();
        return is;
    }

    public static boolean isInInclusionFilter(String className) {
        for (String includedClassName : inclusionFilter) {
            if (className.contains(includedClassName)) {
                return true;
            }
        }
        return false;
    }

    public static void addActionPatternIndices(List<DependentAction> actions) {
        partialFixes.addAll(actions);
        for (DependentAction dependentAction : actions) {
            partialFixesPatternIndices.add(dependentAction.getCurPatternIndex());
        }
    }

    public static void restoreActionPatternIndices() {
        for (int i = 0; i < partialFixes.size(); i++) {
            DependentAction action = partialFixes.get(i);
            int patternIndex = partialFixesPatternIndices.get(i);
            action.setPatternIndex(patternIndex);
        }
    }
}
