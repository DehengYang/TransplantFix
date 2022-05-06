package apr.aprlab.repair.adapt;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.general.DirUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.TimeOut;
import apr.aprlab.repair.adapt.ged.GEDDFS;
import apr.aprlab.repair.adapt.ged.action.GedAction;
import apr.aprlab.repair.adapt.ged.action.GedGlobals;
import apr.aprlab.repair.adapt.ged.action.SearchSpace;
import apr.aprlab.repair.adapt.ged.action.extract.ActionsExtract;
import apr.aprlab.repair.adapt.ged.patch.PatchRankingUtil;
import apr.aprlab.repair.adapt.ged.repair.BaselineRepair;
import apr.aprlab.repair.adapt.ged.util.Graph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MethodAdaption {

    public static final Logger logger = LogManager.getLogger(MethodAdaption.class);

    private MethodSnippet methodSnippet;

    private List<MethodSnippet> candMsList = new ArrayList<MethodSnippet>();

    public MethodAdaption(MethodSnippet methodSnippet, List<MethodSnippet> candMsList) {
        this.methodSnippet = methodSnippet;
        this.candMsList.addAll(candMsList);
        Globals.suspGraphDir = DirUtil.createNumberedDir(Globals.initGraphDir, "susp_", "");
        Globals.hasTraversalIssues = false;
        Globals.hasTodoIssues = false;
        methodSnippet.contructAndCollect(true);
        if (Globals.hasTraversalIssues) {
            logger.error("This method hasTraversalIssues.");
        }
        if (Globals.hasTodoIssues) {
            logger.error("This method hasTodoIssues.");
        }
    }

    public boolean adapt() {
        BaselineRepair baselineRepair = new BaselineRepair(methodSnippet);
        baselineRepair.setCovInfoMap();
        for (int i = 0; i < candMsList.size(); i++) {
            MethodSnippet ms = candMsList.get(i);
            if (methodSnippet.getGedGraph() == null) {
                logger.error("current susp method snippet fail to get the associated gedGraph: {}, so skip", methodSnippet);
                continue;
            }
            if (Globals.debugMode && !Globals.debugMsNumer.isEmpty()) {
                if (Globals.debugMsNumer.get(0) != i) {
                    continue;
                } else {
                    Globals.debugMsNumer.remove(0);
                }
            }
            logger.debug("candidate method snippet: {} {}", i, ms);
            FileUtil.writeToFile(Globals.runInfoPath, String.format("candidate method snippet: (%s) %s\n\n", Globals.donorMCnt++, ms.getMethodSignature()));
            if (!ms.equals(methodSnippet)) {
                if (ms.getRange().getEndLineNo() - ms.getRange().getStartLineNo() > 300) {
                    logger.warn("method snippets larger than 300 may result in error: [CmdUtil:101 INFO ] error output of the cmd: dot: graph is too large for cairo-renderer bitmaps.");
                    continue;
                }
                Globals.donorPatchDir = DirUtil.createNumberedDir(Globals.suspPatchDir, "donor_", "");
                Globals.donorGraphDir = DirUtil.createNumberedDir(Globals.suspGraphDir, "donor_", "");
                Globals.donorCandPatchPath = Paths.get(Globals.donorPatchDir, "candidatePatches.diff").toString();
                Globals.hasTraversalIssues = false;
                Globals.hasTodoIssues = false;
                ms.contructAndCollect(false);
                if (Globals.hasTraversalIssues) {
                    logger.error("This method hasTraversalIssues.");
                }
                if (Globals.hasTodoIssues) {
                    logger.error("This method hasTodoIssues.");
                }
                if (ms.getHyperGraph() == null) {
                    continue;
                }
                GedGlobals.getMappings(methodSnippet, ms);
                ms.getGedGraph().setMappedStrings(methodSnippet);
                GEDDFS geddfs = new GEDDFS(methodSnippet, ms);
                logger.debug("start to run geddfs");
                int timeout = 30;
                List<Graph> actionGraphs = new ArrayList<Graph>();
                try {
                    TimeOut.runWithTimeout(new Runnable() {

                        @Override
                        public void run() {
                            actionGraphs.add(geddfs.run());
                        }
                    }, timeout, TimeUnit.SECONDS);
                } catch (Exception e1) {
                    e1.printStackTrace();
                } catch (Error er) {
                    er.printStackTrace();
                } finally {
                }
                if (actionGraphs.isEmpty()) {
                    logger.error("actionGraphs is empty");
                    continue;
                }
                Graph actionGraph = actionGraphs.get(0);
                if (actionGraph == null) {
                    continue;
                }
                List<GedAction> actions = ActionsExtract.extractActionsFromGraph(actionGraph, methodSnippet);
                SearchSpace searchSpace = new SearchSpace(actions);
                baselineRepair.generateAndValidate(searchSpace);
            }
        }
        if (!Globals.validPatches.isEmpty()) {
            logger.info("valid patches size: {}", Globals.validPatches.size());
            FileUtil.writeToFile(Globals.allValidPatchFilePath, PrintUtil.listToString(Globals.validPatches), false);
            String finalPatch = PatchRankingUtil.rankingValidPatches(Globals.validPatches);
            FileUtil.writeToFile(Globals.finalPatchFilePath, finalPatch, false);
            return true;
        }
        return false;
    }

    public List<MethodSnippet> getCandMsList() {
        return candMsList;
    }

    public MethodSnippet getMethodSnippet() {
        return methodSnippet;
    }
}
