package apr.aprlab.repair.adapt.ged.action.extract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import apr.aprlab.repair.adapt.entities.MappedStringClass;
import apr.aprlab.repair.adapt.entities.MappingUtil;
import apr.aprlab.repair.adapt.entities.MyUnit;
import apr.aprlab.repair.adapt.entities.VariableName;
import apr.aprlab.repair.adapt.ged.action.DependentAction;
import apr.aprlab.repair.adapt.ged.action.GedAction;
import apr.aprlab.repair.adapt.ged.action.GedGlobals;
import apr.aprlab.repair.adapt.ged.action.ReversedIfAction;
import apr.aprlab.repair.adapt.ged.action.UpdAction;
import apr.aprlab.repair.adapt.ged.util.Edge;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.repair.adapt.ged.util.GraphComponent.ActionLabel;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;

public class ActionsExtract {

    public static final Logger logger = LogManager.getLogger(ActionsExtract.class);

    public static List<GedAction> extractActionsFromGraph(Graph actionGraph, MethodSnippet methodSnippet) {
        for (Node node : actionGraph.getNodes()) {
            if (node.isInsAction() || node.isUpdAction()) {
                MyUnit myUnit = node.getAttribute();
                myUnit.updateMappedStringClasses(methodSnippet);
            }
        }
        List<GedAction> allActions = new ArrayList<GedAction>();
        getMatchPairs(allActions, actionGraph);
        List<DependentAction> dependentActions = new ArrayList<DependentAction>();
        List<GedAction> finalAllActions = new ArrayList<>();
        int cnt = 0;
        for (GedAction action : allActions) {
            cnt++;
            logger.debug("cur action: {}, {}", cnt, action);
            List<DependentAction> curDepActions = action.setNewStrings();
            if (curDepActions != null && !curDepActions.isEmpty()) {
                dependentActions.addAll(curDepActions);
            }
            if (action.getNewStrings().isEmpty()) {
                logger.warn("cur action has no newStrings: {}", action);
            } else {
                finalAllActions.add(action);
            }
        }
        finalAllActions.addAll(dependentActions);
        return finalAllActions;
    }

    private static void addActionFromMatchedPair(Pair<Node, Node> matchedPair, List<GedAction> allActions, List<Node> srcGraphNodes, List<Node> dstGraphNodes, Graph actionGraph) {
        Node node = matchedPair.getLeft();
        Node nextMatchedNode = matchedPair.getRight();
        logger.debug("current node->node: {}->{}", node.getComponentId(), nextMatchedNode == null ? "null" : nextMatchedNode.getComponentId());
        if (node.isUpdAction()) {
            addUpdActionForSingleNode(node, node, new ArrayList<Node>(), new ArrayList<Node>(), allActions);
        }
        if (node.isIf() && !node.isWhile() && node.hasReversedEdge()) {
            node.reverseEdgeLabelsForOld();
            logger.debug("srcGraphNode is updated here.");
            srcGraphNodes.clear();
            srcGraphNodes.addAll(TraversalUtil.depthFirstTraversalForNewOrOld(actionGraph, false));
            addReversedIfAction(node, allActions);
            for (Node tmpNode : srcGraphNodes) {
                logger.debug("updated graph node: {}", tmpNode.getComponentId());
            }
        }
        List<Node> delNodes = new ArrayList<Node>();
        List<Node> insNodes = new ArrayList<Node>();
        if (nextMatchedNode != null) {
            insNodes = getInsNodes(dstGraphNodes, node, nextMatchedNode);
        }
        if (node.isIf()) {
            if (nextMatchedNode != null) {
                Boolean fromFalseBranch = nodeInFalseBranch(node, nextMatchedNode);
                delNodes = getDelNodes(srcGraphNodes, node, nextMatchedNode);
                Node exitNode = TraversalUtil.getExitNode(node, true);
                if (fromFalseBranch == null) {
                    if (nextMatchedNode == exitNode) {
                        addTwoBranchesAction(node, nextMatchedNode, allActions);
                    } else {
                        addTwoBranchesAction(node, exitNode, allActions);
                        if (exitNode != null) {
                            List<Node> extraInsNodes = getInsNodesIncludeFirst(dstGraphNodes, exitNode, nextMatchedNode);
                            if (!extraInsNodes.isEmpty()) {
                                addUpdAction(null, nextMatchedNode, extraInsNodes, new ArrayList<Node>(), allActions);
                            }
                        }
                    }
                } else if (fromFalseBranch) {
                    addUpdAction(node, nextMatchedNode, insNodes, delNodes, allActions);
                } else {
                    addTwoBranchesAction(node, nextMatchedNode, allActions);
                }
            } else {
                delNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, false, false, null);
                insNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, true, false, null);
                addUpdAction(node, nextMatchedNode, insNodes, delNodes, allActions);
                delNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, false, true, null);
                insNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, true, true, null);
                addUpdAction(node, nextMatchedNode, insNodes, delNodes, allActions);
            }
        } else {
            if (nextMatchedNode != null) {
                delNodes = getDelNodes(srcGraphNodes, node, nextMatchedNode);
                addUpdAction(node, nextMatchedNode, insNodes, delNodes, allActions);
            } else {
                delNodes = getDelNodes(srcGraphNodes, node);
                insNodes.clear();
                for (int index = dstGraphNodes.indexOf(node) + 1; index < dstGraphNodes.size(); index++) {
                    insNodes.add(dstGraphNodes.get(index));
                }
                addUpdAction(node, nextMatchedNode, insNodes, delNodes, allActions);
            }
        }
    }

    private static void getMatchPairs(List<GedAction> allActions, Graph actionGraph) {
        Node entryNode = actionGraph.getEntryNode();
        ExceptionUtil.assertTrue(entryNode.isUpdAction() || entryNode.isNullAction());
        List<Node> dstGraphNodes = TraversalUtil.depthFirstTraversalForNewOrOld(actionGraph, true);
        List<Node> srcGraphNodes = TraversalUtil.depthFirstTraversalForNewOrOld(actionGraph, false);
        List<Node> dstMatchedNodes = getMatchedNodes(dstGraphNodes);
        List<Node> srcMatchedNodes = getMatchedNodes(srcGraphNodes);
        for (int i = 0; i < dstMatchedNodes.size(); i++) {
            Node node = dstMatchedNodes.get(i);
            if (node.getComponentIdInt(true) == 32) {
                logger.debug("");
            }
            Node nextMatchedNode = null;
            int matchedIndex = -1;
            for (int j = i + 1; j < dstMatchedNodes.size(); j++) {
                if (checkOrder(node, dstMatchedNodes.get(j), srcGraphNodes)) {
                    nextMatchedNode = dstMatchedNodes.get(j);
                    matchedIndex = j;
                    break;
                } else {
                }
            }
            if (nextMatchedNode == null) {
                addActionFromMatchedPair(new Pair<>(node, nextMatchedNode), allActions, srcGraphNodes, dstGraphNodes, actionGraph);
                logger.debug("matched node pair: {} - {}", node, "null");
                break;
            } else {
                ExceptionUtil.myAssert(matchedIndex != -1);
            }
            int bestMatchedIndex = matchedIndex;
            int srcSkippedMatchedCnt = getSkippedMatchedNodesCnt(node, nextMatchedNode, srcMatchedNodes);
            int dstSkippedMatchedCnt = getSkippedMatchedNodesCnt(node, nextMatchedNode, dstMatchedNodes);
            for (int m = matchedIndex + 1; m < dstMatchedNodes.size(); m++) {
                Node curNode = dstMatchedNodes.get(m);
                int curSrcSkippedMatchedCnt = getSkippedMatchedNodesCnt(node, curNode, srcMatchedNodes);
                int curDstSkippedMatchedCnt = getSkippedMatchedNodesCnt(node, curNode, dstMatchedNodes);
                if (!checkOrder(node, curNode, srcGraphNodes)) {
                    continue;
                }
                if (curSrcSkippedMatchedCnt + curDstSkippedMatchedCnt < srcSkippedMatchedCnt + dstSkippedMatchedCnt) {
                    srcSkippedMatchedCnt = curSrcSkippedMatchedCnt;
                    dstSkippedMatchedCnt = curDstSkippedMatchedCnt;
                    bestMatchedIndex = m;
                }
            }
            addActionFromMatchedPair(new Pair<>(node, dstMatchedNodes.get(bestMatchedIndex)), allActions, srcGraphNodes, dstGraphNodes, actionGraph);
            logger.debug("matched node pair: {} - {}", node.getComponentId(), dstMatchedNodes.get(bestMatchedIndex).getComponentId());
            i = bestMatchedIndex - 1;
        }
    }

    private static int getSkippedMatchedNodesCnt(Node node, Node nextMatchedNode, List<Node> srcMatchedNodes) {
        return srcMatchedNodes.indexOf(nextMatchedNode) - srcMatchedNodes.indexOf(node) - 1;
    }

    private static List<Node> getMatchedNodes(List<Node> dstGraphNodes) {
        List<Node> matchedNodes = new ArrayList<Node>();
        for (Node node : dstGraphNodes) {
            if (node.isMatchedNode()) {
                matchedNodes.add(node);
            }
        }
        return matchedNodes;
    }

    private static List<Node> getInsNodesIncludeFirst(List<Node> nodes, Node node, Node nextMatchedNode) {
        List<Node> insNodes = new ArrayList<Node>();
        int startIndex = nodes.indexOf(node);
        int endIndex = nodes.indexOf(nextMatchedNode);
        if (!(startIndex < endIndex)) {
            return insNodes;
        }
        for (int i = startIndex; i < endIndex; i++) {
            insNodes.add(nodes.get(i));
        }
        return insNodes;
    }

    private static void addTwoBranchesAction(Node node, Node nextMatchedNode, List<GedAction> allActions) {
        List<Node> delNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, false, false, null);
        removeNextMatchedNodeFromDelNodes(nextMatchedNode, delNodes);
        List<Node> insNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, true, false, null);
        addUpdAction(node, null, insNodes, delNodes, allActions);
        delNodes.clear();
        List<Node> oldTrueBranchNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, false, true, nextMatchedNode);
        if (!oldTrueBranchNodes.isEmpty()) {
            oldTrueBranchNodes.add(0, node);
            oldTrueBranchNodes.add(nextMatchedNode);
            delNodes = getDelNodes(oldTrueBranchNodes, node, nextMatchedNode);
        }
        insNodes.clear();
        List<Node> newTrueBranchNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, true, true, nextMatchedNode);
        if (!newTrueBranchNodes.isEmpty()) {
            newTrueBranchNodes.add(0, node);
            newTrueBranchNodes.add(nextMatchedNode);
            insNodes = getInsNodes(newTrueBranchNodes, node, nextMatchedNode);
        }
        addUpdAction(node, nextMatchedNode, insNodes, delNodes, allActions);
    }

    private static void removeNextMatchedNodeFromDelNodes(Node nextMatchedNode, List<Node> delNodes) {
        int index = delNodes.indexOf(nextMatchedNode);
        List<Node> newDelNodes = new ArrayList<Node>();
        if (index >= 0) {
            for (int i = 0; i < index; i++) {
                newDelNodes.add(delNodes.get(i));
            }
            delNodes.clear();
            delNodes.addAll(newDelNodes);
        }
    }

    private static void addReversedIfAction(Node node, List<GedAction> allActions) {
        ReversedIfAction reversedIfAction = new ReversedIfAction(GedGlobals.srcMethodSnippet, node);
        allActions.add(reversedIfAction);
        logger.debug("create an reversedIfAction: {}", reversedIfAction.toString());
    }

    private static void addUpdAction(Node node, Node nextMatchedNode, List<Node> insNodes, List<Node> delNodes, List<GedAction> allActions) {
        if (insNodes.isEmpty() && delNodes.isEmpty()) {
            return;
        }
        UpdAction updAction = new UpdAction(GedGlobals.srcMethodSnippet, node, nextMatchedNode, insNodes, delNodes);
        allActions.add(updAction);
        logger.debug("create an updAction: {}", updAction.toString());
    }

    private static void addUpdActionForSingleNode(Node node, Node nextMatchedNode, List<Node> insNodes, List<Node> delNodes, List<GedAction> allActions) {
        UpdAction updAction = new UpdAction(GedGlobals.srcMethodSnippet, node, nextMatchedNode, insNodes, delNodes);
        allActions.add(updAction);
        logger.debug("create an updAction: {}", updAction.toString());
    }

    private static Boolean nodeInFalseBranch(Node node, Node nextMatchedNode) {
        Node exitNode = TraversalUtil.getExitNode(node, true);
        List<Node> falseBranchNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, true, false, exitNode);
        List<Node> trueBranchNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, true, true, exitNode);
        if (falseBranchNodes.contains(nextMatchedNode)) {
            return true;
        } else if (trueBranchNodes.contains(nextMatchedNode)) {
            return false;
        } else {
            return null;
        }
    }

    private static boolean checkOrder(Node node, Node nextMatchedNode, List<Node> srcGraphNodes) {
        int startIndex = srcGraphNodes.indexOf(node);
        int endIndex = srcGraphNodes.indexOf(nextMatchedNode);
        boolean is = startIndex < endIndex;
        if (!is) {
            logger.debug("node: {}, nextMatchedNode: {}, incorrect order.", node.getComponentId(), nextMatchedNode.getComponentId());
        }
        return is;
    }

    private static List<Node> getDelNodes(List<Node> srcGraphNodes, Node node, Node nextMatchedNode) {
        List<Node> delNodes = new ArrayList<Node>();
        int startIndex = srcGraphNodes.indexOf(node);
        int endIndex = srcGraphNodes.indexOf(nextMatchedNode);
        ExceptionUtil.assertTrue(startIndex <= endIndex, String.format("%s - %s", startIndex, endIndex));
        for (int i = startIndex + 1; i < endIndex; i++) {
            delNodes.add(srcGraphNodes.get(i));
        }
        return delNodes;
    }

    private static List<Node> getDelNodes(List<Node> srcGraphNodes, Node node) {
        List<Node> delNodes = new ArrayList<Node>();
        int curNodeIndex = srcGraphNodes.indexOf(node);
        for (int index = curNodeIndex + 1; index < srcGraphNodes.size(); index++) {
            Node srcNode = srcGraphNodes.get(index);
            int skipIndex = 0;
            if (srcNode.getAttribute().getLineNo() < 0) {
                ExceptionUtil.assertTrue(srcNode.getAttribute().getLineNo() == -1);
                ExceptionUtil.assertTrue(index - skipIndex == 1);
            } else {
                delNodes.add(srcGraphNodes.get(index));
            }
        }
        return delNodes;
    }

    private static List<Node> getInsNodes(List<Node> nodes, Node node, Node nextMatchedNode) {
        List<Node> insNodes = new ArrayList<Node>();
        int startIndex = nodes.indexOf(node);
        int endIndex = nodes.indexOf(nextMatchedNode);
        ExceptionUtil.assertTrue(startIndex <= endIndex);
        for (int i = startIndex + 1; i < endIndex; i++) {
            insNodes.add(nodes.get(i));
        }
        return insNodes;
    }

    public static String getNewString(Node node, List<Node> blockNodes, List<Node> visited, int curPatternCnt, List<MappedStringClass> usedMappedStringClasses, List<String> successFlags, List<UpdAction> delActions, MethodSnippet srcMethodSnippet) {
        String string = "";
        if (!successFlags.isEmpty()) {
            return "";
        }
        if (node == null) {
            return "";
        }
        if (!blockNodes.contains(node)) {
            return "";
        }
        if (visited.contains(node)) {
            return "";
        }
        visited.add(node);
        if (node.isIf()) {
            if (node.isWhile()) {
                String whileCond = node.getMappedString(curPatternCnt, usedMappedStringClasses, delActions, srcMethodSnippet);
                if (whileCond == null) {
                    successFlags.add("fail");
                    return "";
                }
                String whileBlock = getNewString(node.getWhileBlockStart(), blockNodes, visited, curPatternCnt, usedMappedStringClasses, successFlags, delActions, srcMethodSnippet);
                if (whileBlock.trim().length() == 0) {
                    successFlags.add("fail");
                    return "";
                }
                string += String.format("while (%s) {\n%s}\n", whileCond, whileBlock);
                string += getNewString(node.getWhileExitNode(), blockNodes, visited, curPatternCnt, usedMappedStringClasses, successFlags, delActions, srcMethodSnippet);
            } else {
                String ifCond = node.getMappedString(curPatternCnt, usedMappedStringClasses, delActions, srcMethodSnippet);
                if (ifCond == null) {
                    successFlags.add("fail");
                    return "";
                }
                String ifBlock = "";
                String elseBlock = "";
                String exitBlock = "";
                Node exitNode = TraversalUtil.getExitNode(node, true);
                if (exitNode != null) {
                    if (exitNode.isAction()) {
                        exitBlock = getNewString(exitNode, blockNodes, visited, curPatternCnt, usedMappedStringClasses, successFlags, delActions, srcMethodSnippet);
                    }
                }
                ifBlock = getNewString(node.getIfBlockStart(), blockNodes, visited, curPatternCnt, usedMappedStringClasses, successFlags, delActions, srcMethodSnippet);
                elseBlock += getNewString(node.getElseBlockStart(), blockNodes, visited, curPatternCnt, usedMappedStringClasses, successFlags, delActions, srcMethodSnippet);
                String wholeIfString = String.format("if (%s) {\n%s}\nelse {\n%s}\n%s\n", ifCond, ifBlock, elseBlock, exitBlock);
                if (elseBlock.length() == 0) {
                    wholeIfString = String.format("if (%s) {\n%s}\n%s\n", ifCond, ifBlock, exitBlock);
                }
                string += wholeIfString;
            }
        } else {
            if (node.isStmt()) {
                String newString = node.getMappedString(curPatternCnt, usedMappedStringClasses, delActions, srcMethodSnippet);
                if (newString == null) {
                    successFlags.add("fail");
                    return "";
                }
                string += newString;
                string += getNewString(getNextNode(node), blockNodes, visited, curPatternCnt, usedMappedStringClasses, successFlags, delActions, srcMethodSnippet);
            } else if (node.parentIsStmtForNew()) {
                Node nextNode = getNextNode(node);
                String newString = node.getMappedString(curPatternCnt, usedMappedStringClasses, delActions, srcMethodSnippet);
                if (newString == null) {
                    successFlags.add("fail");
                    return "";
                }
                string += newString + ";";
                string += getNewString(nextNode, blockNodes, visited, curPatternCnt, usedMappedStringClasses, successFlags, delActions, srcMethodSnippet);
            } else {
                logger.debug("node: {}", node);
                ExceptionUtil.raise();
            }
        }
        return string;
    }

    private static Node getNextNode(Node node) {
        List<Edge> edges = new ArrayList<Edge>();
        for (Edge edge : node.getFromThisEdges()) {
            if (edge.getPlotLabel() != ActionLabel.DEL) {
                edges.add(edge);
            }
        }
        ExceptionUtil.assertTrue(!node.isIf() && !node.isWhile());
        if (!edges.isEmpty()) {
            return edges.get(0).getEndNode();
        } else {
            return null;
        }
    }

    public static Node getExitNodeForSwitch(Node node, boolean forNew) {
        List<List<Node>> branches = new ArrayList<List<Node>>();
        for (Edge edge : node.getFromThisEdges()) {
            if (forNew) {
                if (edge.getPlotLabel() != ActionLabel.DEL) {
                    branches.add(TraversalUtil.depthFirstTraversalForNewOrOldExcludeNodes(edge.getEndNode(), new ArrayList<Node>(Arrays.asList(node)), true));
                }
            } else {
                if (edge.getPlotLabel() != ActionLabel.INS) {
                    branches.add(TraversalUtil.depthFirstTraversalForNewOrOldExcludeNodes(edge.getEndNode(), new ArrayList<Node>(Arrays.asList(node)), false));
                }
            }
        }
        ExceptionUtil.myAssert(branches.size() >= 2);
        Node exitNode = null;
        boolean isNull = false;
        for (int i = 0; i < branches.size() - 1; i++) {
            List<Node> intersections = CollectionUtil.getIntersection(branches.get(i), branches.get(i + 1));
            if (intersections.isEmpty()) {
                isNull = true;
            } else {
                if (exitNode != null) {
                    if (intersections.get(0).getAttribute().getLineNo() > exitNode.getAttribute().getLineNo()) {
                        logger.warn("better exitNode for switch: {} (old: {})", intersections.get(0), exitNode);
                        exitNode = intersections.get(0);
                    }
                } else {
                    exitNode = intersections.get(0);
                }
            }
        }
        if (isNull && exitNode != null) {
            logger.warn("switch isNull is true but has exitNode: {}", exitNode);
        }
        return exitNode;
    }

    public static List<String> getNewStrings(MyUnit delUnit, MyUnit insUnit) {
        List<String> newStrings = new ArrayList<String>();
        List<Pair<ASTNode, String>> mappings = new ArrayList<>();
        newStrings.add(insUnit.getMappedString());
        Map<String, List<String>> varMapping = insUnit.getVarMapping();
        for (VariableName vn : insUnit.getVars()) {
            String insVarName = vn.getVarName();
            for (VariableName delVn : delUnit.getVars()) {
                String delVarName = delVn.getVarName();
                if (varMapping.containsKey(insVarName) && varMapping.get(insVarName).contains(delVarName)) {
                    List<ASTNode> insNodes = new ArrayList<ASTNode>();
                    for (ASTNode node : vn.getNodes()) {
                        insNodes.add(node.getParent());
                    }
                    for (ASTNode node : delVn.getNodes()) {
                        mappings.add(new Pair<>(node, insNodes.get(0).toString()));
                    }
                    String newString = MappingUtil.applyMappings(mappings, delUnit.getAstNode(), delUnit.getMethodSnippet().getFileString());
                    newStrings.add(newString);
                    mappings.clear();
                }
            }
        }
        return newStrings;
    }

    public static void addNewStrings(MethodSnippet srcMethodSnippet, List<Node> insNodes, List<String> newStrings) {
        List<String> varNameStrings = srcMethodSnippet.getEntityInMethod().getVarNameStrings();
        if (varNameStrings.isEmpty()) {
            return;
        }
        Map<String, List<String>> varMapping = insNodes.get(0).getAttribute().getVarMapping();
        List<String> dstMappedVars = new ArrayList<String>(varMapping.keySet());
        List<String> curNodesVars = new ArrayList<String>();
        for (Node node : insNodes) {
            for (VariableName vn : node.getAttribute().getVars()) {
                curNodesVars.add(vn.getVarName());
            }
        }
        Map<String, String> newVarMapping = new HashMap<String, String>();
        if (CollectionUtil.getIntersection(curNodesVars, dstMappedVars).size() == 0) {
            for (String curVarString : curNodesVars) {
                newVarMapping.put(curVarString, varNameStrings.get(0));
            }
        }
        for (Node node : insNodes) {
            node.getAttribute().addMappingAndSetString(newVarMapping);
        }
    }

    public static List<List<Node>> groupInsNodesIntoStmts(List<Node> insNodes) {
        List<List<Node>> stmts = new ArrayList<List<Node>>();
        List<Node> visited = new ArrayList<Node>();
        for (Node node : insNodes) {
            if (visited.contains(node)) {
                continue;
            }
            List<Node> nodes = findAlignedStmt(node, insNodes);
            stmts.add(nodes);
            visited.addAll(nodes);
        }
        return stmts;
    }

    private static List<Node> findAlignedStmt(Node node, List<Node> insNodes) {
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(node);
        if (node.isIf() && !node.isWhile()) {
            ASTNode ifStmt = node.getAttribute().getIfStmt();
            for (Node cur : insNodes) {
                if (cur == node) {
                    continue;
                }
                if (cur.getAttribute().getAstNode() == null) {
                    continue;
                }
                if (RangeUtil.containsDstNode(ifStmt, cur.getAttribute().getAstNode())) {
                    nodes.add(cur);
                }
            }
        }
        return nodes;
    }
}
