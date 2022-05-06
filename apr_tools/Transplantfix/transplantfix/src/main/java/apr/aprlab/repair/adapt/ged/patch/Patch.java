package apr.aprlab.repair.adapt.ged.patch;

import apr.aprlab.repair.adapt.ged.action.DependentAction;
import apr.aprlab.repair.adapt.ged.action.GedAction;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.DirUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.StringUtil;
import apr.aprlab.utils.general.TimeUtil;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Patch {

    public static final Logger logger = LogManager.getLogger(Patch.class);

    private SingleFilePatch singleFilePatch;

    private String patchDirPath;

    private String compileOutputDir;

    private String diffFilePath;

    private int patternCnt;

    private boolean usePattern = true;

    private List<SingleFilePatch> singleFilePatches = new ArrayList<>();

    private boolean isMultiFilePatch = false;

    private boolean passCompilation = false;

    private String patchDiff;

    private List<DependentAction> actions;

    private String coveredPosFilePath;

    private boolean partialFixesPassAll = false;

    public Patch(List<DependentAction> actions, String coveredPosFilePath, boolean usePattern) {
        setPatchDir();
        setCompileOutputDir();
        this.actions = actions;
        this.coveredPosFilePath = coveredPosFilePath;
        this.usePattern = usePattern;
        Map<String, List<DependentAction>> fileActions = groupFileActions(actions);
        ExceptionUtil.myAssert(fileActions.size() > 0);
        if (fileActions.size() > 1) {
            isMultiFilePatch = true;
            ExceptionUtil.myAssert(!usePattern);
            for (String filePath : fileActions.keySet()) {
                SingleFilePatch singleFilePatch = new SingleFilePatch(fileActions.get(filePath), compileOutputDir, patchDirPath, diffFilePath, coveredPosFilePath, false);
                singleFilePatches.add(singleFilePatch);
            }
            singleFilePatch = singleFilePatches.get(0);
        } else {
            ExceptionUtil.myAssert(fileActions.values().size() == 1);
            List<DependentAction> singleFileActions = fileActions.get(fileActions.keySet().toArray()[0]);
            patternCnt = getPatternCnt(singleFileActions);
            singleFilePatch = new SingleFilePatch(singleFileActions, compileOutputDir, patchDirPath, diffFilePath, coveredPosFilePath, true);
            singleFilePatches.add(singleFilePatch);
        }
    }

    public void run() {
        if (usePattern) {
            ExceptionUtil.myAssert(singleFilePatches.size() == 1);
            if (patternCnt > 8) {
                logger.debug("");
            }
            for (int patternIndex = 0; patternIndex < patternCnt; patternIndex++) {
                logger.debug("The patternIndex: {}", patternIndex);
                mutate(singleFilePatch, patternIndex);
                boolean shouldSkip = createAndSave();
                if (shouldSkip) {
                    continue;
                }
                passCompilation = singleFilePatch.compile();
                if (passCompilation) {
                    if (singleFilePatch.test()) {
                        saveCurDiff();
                        savePatchDiff();
                        Globals.validPatches.add(patchDiff);
                        singleFilePatch.getTestPrioritization().resetFields();
                    } else {
                        if (isPartialFix()) {
                            if (!CollectionUtil.containsDstList(Globals.passedFailedCases, getPassedFailedCases())) {
                                CollectionUtil.addToList(Globals.passedFailedCases, getPassedFailedCases());
                                Globals.addActionPatternIndices(actions);
                                if (!Globals.isStillPartialFix()) {
                                    Globals.restoreActionPatternIndices();
                                    logger.info("run combined partial fixes now!");
                                    Patch combinedPatialFixes = new Patch(Globals.partialFixes, coveredPosFilePath, false);
                                    combinedPatialFixes.run();
                                    if (combinedPatialFixes.isPassAll()) {
                                        combinedPatialFixes.savePatchDiff();
                                        partialFixesPassAll = true;
                                        break;
                                    } else {
                                        logger.error("todo error: the combined partial fixes still fail to pass all tests.");
                                    }
                                }
                            }
                        } else {
                            saveCurDiff();
                        }
                    }
                } else {
                    saveCurDiff();
                }
            }
        } else {
            boolean shouldSkip = createAndSave();
            if (shouldSkip) {
                return;
            }
            passCompilation = compile();
            if (passCompilation) {
                if (test()) {
                    savePatchDiff();
                }
            }
            saveCurDiff();
        }
    }

    private void saveCurDiff() {
        String patchDiffInfo = String.format("(compile: %s, isPartialFix: %s, passFailed: %s, passSelected: %s, passAll: %s)", passCompilation, isPartialFix(), isPassFailed(), isPassSelected(), isPassAll());
        if (!passCompilation) {
            patchDiffInfo = String.format("(compile: %s, isPartialFix: %s, passFailed: %s, passSelected: %s, passAll: %s)", passCompilation, false, false, false, false);
        }
        String curPatchInfo = String.format("========= %s Patch Index: (suspM: %s)-(donorM: %s) %s %s =========\n%s\n", TimeUtil.getReadableTime(), Globals.suspMCnt, Globals.donorMCnt, Globals.patchCnt, patchDiffInfo, patchDiff);
        FileUtil.writeToFile(Globals.allCandPatchPath, curPatchInfo, true);
        FileUtil.writeToFile(Globals.donorCandPatchPath, curPatchInfo, true);
    }

    private void savePatchDiff() {
        FileUtil.writeToFile(Globals.finalPatchFilePath, patchDiff, false);
    }

    private boolean test() {
        return singleFilePatch.test();
    }

    private boolean compile() {
        for (SingleFilePatch singleFilePatch : singleFilePatches) {
            singleFilePatch.compile();
            if (!singleFilePatch.isPassCompilation()) {
                return false;
            }
        }
        return true;
    }

    private boolean createAndSave() {
        for (SingleFilePatch singleFilePatch : singleFilePatches) {
            singleFilePatch.createPatch();
            singleFilePatch.savePatchFile();
            singleFilePatch.setPatchDiff();
        }
        patchDiff = mergePatchDiffs();
        String formattedPatchDiff = StringUtil.getFormattedPatchDiff(patchDiff);
        if (Globals.patchDiffs.contains(formattedPatchDiff)) {
            logger.info("cur patch diff already validated: {}", formattedPatchDiff);
            return true;
        }
        Globals.patchDiffs.add(formattedPatchDiff);
        Globals.patchCnt++;
        logger.info("cur patch to be validated[{}]: {}", Globals.patchCnt, patchDiff);
        return false;
    }

    private String mergePatchDiffs() {
        String multipleFilePatchDiff = "";
        for (SingleFilePatch singleFilePatch : singleFilePatches) {
            multipleFilePatchDiff += singleFilePatch.getPatchDiff() + "\n";
        }
        return multipleFilePatchDiff;
    }

    private Map<String, List<DependentAction>> groupFileActions(List<DependentAction> actions) {
        Map<String, List<DependentAction>> fileActions = new HashMap<String, List<DependentAction>>();
        for (DependentAction action : actions) {
            String filePath = action.getFilePath();
            if (fileActions.containsKey(filePath)) {
                fileActions.get(filePath).add(action);
            } else {
                fileActions.put(filePath, new ArrayList<DependentAction>(Arrays.asList(action)));
            }
        }
        for (String filePath : fileActions.keySet()) {
            fileActions.get(filePath).sort(ComparatorUtil.actionComparator);
        }
        return fileActions;
    }

    private void mutate(SingleFilePatch singleFilePatch, int patternIndex) {
        singleFilePatch.setPatternIndex(patternIndex);
    }

    private int getPatternCnt(List<DependentAction> actions) {
        int patternCnt = Integer.MIN_VALUE;
        for (GedAction action : actions) {
            patternCnt = Math.max(patternCnt, action.getNewStrings().size());
        }
        return patternCnt;
    }

    private void setPatchDir() {
        patchDirPath = DirUtil.createNumberedDir(Globals.donorPatchDir, "patch_", "");
        diffFilePath = Paths.get(patchDirPath, "patch.diff").toString();
    }

    private void setCompileOutputDir() {
        compileOutputDir = Paths.get(patchDirPath, "compile").toString();
    }

    public boolean isPassFailed() {
        return singleFilePatch.isPassFailed();
    }

    public boolean isPassSelected() {
        return singleFilePatch.isPassSelected();
    }

    public boolean isPassAll() {
        return singleFilePatch.isPassAll();
    }

    public boolean isPartialFix() {
        return singleFilePatch.getTestPrioritization().isPartialFix();
    }

    public List<String> getPassedFailedCases() {
        return singleFilePatch.getTestPrioritization().getPassedFailedCases();
    }

    public boolean isMultiFilePatch() {
        return isMultiFilePatch;
    }

    public boolean isPartialFixesPassAll() {
        return partialFixesPassAll;
    }
}
