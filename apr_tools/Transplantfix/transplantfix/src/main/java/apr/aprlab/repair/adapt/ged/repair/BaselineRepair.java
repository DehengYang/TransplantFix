package apr.aprlab.repair.adapt.ged.repair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import apr.aprlab.repair.adapt.ged.action.DependentAction;
import apr.aprlab.repair.adapt.ged.action.SearchSpace;
import apr.aprlab.repair.adapt.ged.patch.Patch;
import apr.aprlab.repair.adapt.patch.validate.strategy.TestUtil;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.general.DirUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaselineRepair {

    public static final Logger logger = LogManager.getLogger(BaselineRepair.class);

    private List<String> coveredTestCases = new ArrayList<>();

    private List<String> oriFailedCases = new ArrayList<>();

    private List<String> coveredPosTestCases = new ArrayList<>();

    private String coveredPosFilePath;

    private MethodSnippet methodSnippet;

    public BaselineRepair(MethodSnippet methodSnippet) {
        this.methodSnippet = methodSnippet;
        setOriFailedCases();
    }

    public boolean generateAndValidate(SearchSpace searchSpace) {
        int cnt = 0;
        for (DependentAction depAction : searchSpace.getDependentActions()) {
            if (cnt == 6) {
                logger.debug("");
            }
            logger.debug("[search space] action index: {}, action: {}", cnt, depAction);
            cnt++;
            generateAndValidateSinglePatch(depAction);
        }
        return false;
    }

    private Patch generateAndValidateSinglePatch(List<DependentAction> actions, boolean usePattern) {
        Patch patch = new Patch(actions, coveredPosFilePath, usePattern);
        patch.run();
        return patch;
    }

    public Patch generateAndValidateSinglePatch(DependentAction action) {
        return generateAndValidateSinglePatch(new ArrayList<DependentAction>(Arrays.asList(action)));
    }

    public Patch generateAndValidateSinglePatch(List<DependentAction> actions) {
        return generateAndValidateSinglePatch(actions, true);
    }

    private void setAndSaveCoveredPosTestCases() {
        boolean coveredByFailedCases = false;
        for (String testCase : coveredTestCases) {
            if (oriFailedCases.contains(testCase)) {
                coveredByFailedCases = true;
            } else {
                coveredPosTestCases.add(testCase);
            }
        }
        coveredPosFilePath = DirUtil.createNumberedFile(Globals.suspPatchDir, "coveredPosTestCases_", ".txt");
        FileUtil.writeToFile(coveredPosFilePath, coveredPosTestCases, false);
        if (!coveredByFailedCases) {
            logger.warn("this buggy location is not covered by any failed tests.");
        }
    }

    private void setOriFailedCases() {
        if (Globals.isForD4j2) {
            oriFailedCases = TestUtil.obtainOriFailedMethods(Globals.buggyDir);
        } else {
            oriFailedCases = FileUtil.readFileToList(Globals.localizeInfoDir + "/expected_failed_test_replicate.txt");
        }
        ExceptionUtil.myAssert(!oriFailedCases.isEmpty());
    }

    private void setCoveredTestCases(MethodSnippet ms) {
        coveredTestCases.addAll(ms.getSuspiciousMethod().getCoveredTestCases());
    }

    public List<String> getCoveredTestCases() {
        return coveredTestCases;
    }

    public List<String> getOriFailedCases() {
        return oriFailedCases;
    }

    public List<String> getCoveredPosTestCases() {
        return coveredPosTestCases;
    }

    public String getCoveredPosFilePath() {
        return coveredPosFilePath;
    }

    public void setCovInfoMap() {
        setCoveredTestCases(methodSnippet);
        setAndSaveCoveredPosTestCases();
        logger.info("coveredTestCases size: {}, oriFailedCases size: {}, coveredPosTestCases size: {}.", coveredPosTestCases.size(), oriFailedCases.size(), coveredPosTestCases.size());
    }
}
