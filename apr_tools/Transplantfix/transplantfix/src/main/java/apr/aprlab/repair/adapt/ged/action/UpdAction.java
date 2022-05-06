package apr.aprlab.repair.adapt.ged.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import apr.aprlab.repair.adapt.entities.MappedStringClass;
import apr.aprlab.repair.adapt.entities.MyUnitUtil;
import apr.aprlab.repair.adapt.ged.action.extract.ActionsExtract;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Statement;

public class UpdAction extends GedAction {

    public static final Logger logger = LogManager.getLogger(UpdAction.class);

    public enum ActionType {

        DEL, UPD, EXTRA_UPD
    }

    private ActionType actionType = ActionType.UPD;

    private Node startNode;

    private Node endNode;

    private List<Node> insNodes = new ArrayList<>();

    private List<Node> delNodes = new ArrayList<>();

    private List<EndBraceDelAction> endBraceDelActions = new ArrayList<>();

    private Map<Node, EndBraceDelAction> endBraceDelActionMap = new HashMap<Node, EndBraceDelAction>();

    private Map<String, List<EndBraceDelAction>> delStringEndBraceMap = new HashMap<>();

    private ASTNode ifOrLoopAstNode;

    public UpdAction(MethodSnippet srcMethodSnippet, Node startNode, Node endNode, List<Node> insNodes, List<Node> delNodes) {
        super(srcMethodSnippet);
        this.startNode = startNode;
        this.endNode = endNode;
        this.insNodes.addAll(insNodes);
        this.delNodes.addAll(delNodes);
        if (startNode != null && endNode != null) {
            if (!endNode.isInsAction()) {
                if (startNode.getOldAttribute().getAstNode() != null && endNode.getOldAttribute().getAstNode() != null) {
                    ASTNode startStmt = ASTUtil.getIfOrLoopAstNode(startNode.getOldAttribute().getAstNode());
                    ASTNode endStmt = ASTUtil.getIfOrLoopAstNode(endNode.getOldAttribute().getAstNode());
                    if (startStmt == endStmt) {
                        ifOrLoopAstNode = startStmt;
                    }
                }
            }
        }
        updatePosRange();
    }

    public UpdAction(MethodSnippet methodSnippet, Node extraMappedNode, ActionType del) {
        super(methodSnippet);
        posRange = new PosRange(extraMappedNode);
        this.actionType = del;
        newStrings.add("");
    }

    public UpdAction(MethodSnippet srcMethodSnippet, PosRange posRange, String newString) {
        super(srcMethodSnippet);
        this.posRange = posRange;
        newStrings.add(newString);
    }

    public boolean isUpdSingleNode() {
        boolean is = startNode == endNode;
        return is;
    }

    public boolean isUpdIfCond() {
        boolean is = startNode == endNode && startNode.isIf();
        return is;
    }

    @Override
    public String toString() {
        String insIds = "";
        for (Node node : insNodes) {
            insIds += " " + node.getComponentId();
        }
        String delIds = "";
        for (Node node : delNodes) {
            delIds += " " + node.getComponentId();
        }
        String string = String.format("[%s], startNode: %s, endNode: %s, insNodes: %s, delNodes: %s", posRange, startNode == null ? "null" : startNode.getComponentId(), endNode == null ? "null" : endNode.getComponentId(), insIds, delIds);
        return string;
    }

    @Override
    public List<String> getNewStrings() {
        return newStrings;
    }

    @Override
    public List<DependentAction> setNewStrings() {
        if (this.actionType == ActionType.DEL) {
            newStrings.add("");
            return null;
        } else if (this.actionType == ActionType.EXTRA_UPD) {
            return null;
        }
        List<DependentAction> dependentActions = new ArrayList<DependentAction>();
        addEndBraceDelActions();
        if (isUpdSingleNode()) {
            String newString = endNode.getMappedString(srcMethodSnippet);
            newStrings.add(newString);
        } else {
            if (insNodes.isEmpty()) {
                String newString = "";
                newStrings.add(newString);
                getDelStrings(delNodes, newStrings);
            } else {
                ActionsExtract.addNewStrings(srcMethodSnippet, insNodes, newStrings);
                dependentActions.addAll(addToNewStrings(insNodes, newStrings));
                List<List<Node>> insStmtList = ActionsExtract.groupInsNodesIntoStmts(insNodes);
                if (insStmtList.size() > 1) {
                    for (List<Node> insStmt : insStmtList) {
                        dependentActions.addAll(addToNewStrings(insStmt, newStrings));
                    }
                }
                if (insStmtList.size() > 1) {
                    for (List<Node> insStmt : insStmtList) {
                        List<Node> restStmts = getRestStmts(insStmt, insNodes);
                        if (restStmts.size() > 0) {
                            dependentActions.addAll(addToNewStrings(restStmts, newStrings));
                        }
                    }
                }
                String delString = StringUtil.substring(fileString, posRange.getStartPos(), posRange.getEndPos());
                delStringEndBraceMap.put(delString, new ArrayList<EndBraceDelAction>());
                for (String newString : new ArrayList<String>(newStrings)) {
                    String insAfterNew = newString + delString;
                    String insBeforeNew = delString + newString;
                    if (!newStrings.contains(insAfterNew) || !newStrings.contains(insBeforeNew)) {
                        if (delString.trim().startsWith("super(") || newString.trim().startsWith("return")) {
                            if (!newStrings.contains(insBeforeNew)) {
                                newStrings.add(insBeforeNew);
                                delStringEndBraceMap.put(insBeforeNew, new ArrayList<>());
                            }
                        } else {
                            if (!newStrings.contains(insAfterNew)) {
                                newStrings.add(insAfterNew);
                                delStringEndBraceMap.put(insBeforeNew, new ArrayList<>());
                            }
                        }
                    }
                }
                getDelStrings(delNodes, newStrings);
            }
        }
        CollectionUtil.removeDuplicates(newStrings);
        return dependentActions;
    }

    private List<Node> getRestStmts(List<Node> insStmt, List<Node> insNodes2) {
        List<Node> restNodes = new ArrayList<Node>();
        for (Node node : insNodes2) {
            if (!insStmt.contains(node)) {
                restNodes.add(node);
            }
        }
        return restNodes;
    }

    private void getDelStrings(List<Node> delNodes, List<String> newStrings) {
        List<Statement> stmts = ActionUtil.getStmtsFromDelNodes(delNodes);
        List<Node> ifNodes = MyUnitUtil.getIfNodes(delNodes);
        int startPos = posRange.getStartPos();
        int endPos = posRange.getEndPos();
        for (Statement stmt : stmts) {
            int curStartPos = stmt.getStartPosition();
            int curEndPos = stmt.getStartPosition() + stmt.getLength();
            if (RangeUtil.rangeContains(startPos, endPos, curStartPos, curEndPos) < 0) {
                logger.warn("cur stmt is larger than posRange, so skip.");
                continue;
            }
            String prevString = StringUtil.substring(fileString, startPos, curStartPos);
            String nextString = StringUtil.substring(fileString, curEndPos, endPos);
            String delString = prevString + nextString;
            if (newStrings.contains(delString)) {
                continue;
            }
            newStrings.add(delString);
            List<EndBraceDelAction> endBraces = new ArrayList<EndBraceDelAction>();
            for (Node ifNode : ifNodes) {
                ASTNode astNode = ifNode.getOldAttribute().getAstNode();
                PosRange ifNodeRange = new PosRange(astNode);
                if (new PosRange(curStartPos, curEndPos).contains(ifNodeRange)) {
                    if (endBraceDelActionMap.containsKey(ifNode)) {
                        EndBraceDelAction endBrace = endBraceDelActionMap.get(ifNode);
                        ExceptionUtil.myAssert(!endBraces.contains(endBrace));
                        endBraces.add(endBrace);
                    }
                }
            }
            delStringEndBraceMap.put(delString, endBraces);
        }
    }

    private void addEndBraceDelActions() {
        String fileString = srcMethodSnippet.getFileString();
        for (Node node : delNodes) {
            if (node.isIf()) {
                ASTNode loopStmt = node.getOldAttribute().getIfCondition();
                int endPos = loopStmt.getStartPosition() + loopStmt.getLength();
                if (loopStmt instanceof DoStatement) {
                    DoStatement doStmt = (DoStatement) loopStmt;
                    int doEndPos = doStmt.getBody().getStartPosition();
                    Statement doBody = doStmt.getBody();
                    if (doBody instanceof Block) {
                        ASTNode firstStmt = (ASTNode) ((Block) doBody).statements().get(0);
                        doEndPos = firstStmt.getStartPosition();
                    }
                    EndBraceDelAction endBraceDelAction = new EndBraceDelAction(srcMethodSnippet, new PosRange(doStmt.getStartPosition(), doEndPos));
                    endBraceDelActions.add(endBraceDelAction);
                    endBraceDelActionMap.put(node, endBraceDelAction);
                } else {
                    if (StringUtil.substring(fileString, endPos - 1, endPos).equals("}")) {
                        EndBraceDelAction endBraceDelAction = new EndBraceDelAction(srcMethodSnippet, new PosRange(endPos - 1, endPos));
                        endBraceDelActions.add(endBraceDelAction);
                        endBraceDelActionMap.put(node, endBraceDelAction);
                    } else {
                        logger.error("loopStmt last char is not } : {}", loopStmt);
                    }
                }
            }
        }
    }

    private void updatePosRange() {
        if (isUpdSingleNode()) {
            if (startNode.parentIsStmtForOld()) {
                posRange = new PosRange(startNode.getOldStartPos(), startNode.getOldEndPos());
            } else {
                posRange = new PosRange(startNode);
            }
        } else {
            ExceptionUtil.assertFalse(startNode == endNode);
            if (delNodes.isEmpty()) {
                if (endNode != null) {
                    int startPos = endNode.getOldStartPosForAction();
                    posRange = new PosRange(startPos, startPos);
                } else {
                    ExceptionUtil.myAssert(startNode != null);
                    int startPos = startNode.getOldEndPosForAction();
                    posRange = new PosRange(startPos, startPos);
                }
            } else {
                if (delNodes.size() == 1) {
                    posRange = new PosRange(delNodes.get(0));
                } else {
                    posRange = new PosRange(delNodes, true);
                }
            }
        }
    }

    private List<DependentAction> addToNewStrings(List<Node> insNodes, List<String> newStrings) {
        List<DependentAction> dependentActions = new ArrayList<DependentAction>();
        int maxPatternCnt = getMaxPatternCnt(insNodes);
        for (int curPatternCnt = 0; curPatternCnt < maxPatternCnt; curPatternCnt++) {
            List<Node> visited = new ArrayList<Node>();
            List<MappedStringClass> usedMappedStringClasses = new ArrayList<MappedStringClass>();
            List<String> successFlags = new ArrayList<String>();
            List<UpdAction> updActions = new ArrayList<>();
            String newString = ActionsExtract.getNewString(insNodes.get(0), insNodes, visited, curPatternCnt, usedMappedStringClasses, successFlags, updActions, srcMethodSnippet);
            if (successFlags.contains("fail")) {
                continue;
            } else {
                if (visited.size() != insNodes.size()) {
                    logger.warn("visited size is smaller than insNodes size");
                }
            }
            if (!updActions.isEmpty()) {
                UpdAction thisUpdAction = new UpdAction(srcMethodSnippet, posRange, newString);
                updActions.add(thisUpdAction);
                if (!hasIntersection(updActions)) {
                    DependentAction dependentAction = new DependentAction(updActions);
                    dependentActions.add(dependentAction);
                }
            } else {
                if (newString.startsWith("if") && newString.contains("{\n}")) {
                    String curString = newString.replace("{\n}", " ");
                    if (!newStrings.contains(curString)) {
                        newStrings.add(curString);
                    }
                    curString = newString.replace("{\n}", "{ ");
                    if (!newStrings.contains(curString)) {
                        newStrings.add(curString);
                    }
                } else {
                    if (!newStrings.contains(newString)) {
                        newStrings.add(newString);
                    }
                }
            }
        }
        return dependentActions;
    }

    private boolean hasIntersection(List<UpdAction> updActions) {
        updActions.sort(ComparatorUtil.actionComparator);
        for (int i = 0; i < updActions.size() - 1; i++) {
            UpdAction curAction = updActions.get(i);
            UpdAction nextAction = updActions.get(i + 1);
            if (curAction.getSrcPosRange().hasIntersection(nextAction.getSrcPosRange())) {
                logger.warn("updActions has intersection: {}", PrintUtil.listToString(updActions));
                return true;
            }
        }
        return false;
    }

    private int getMaxPatternCnt(List<Node> insNodes) {
        int maxCombinedCnt = 1;
        for (Node insNode : insNodes) {
            maxCombinedCnt = Math.max(maxCombinedCnt, insNode.getAttribute().getMappedStringClasses().size());
        }
        return maxCombinedCnt;
    }

    @Override
    public PosRange getSrcPosRange() {
        return posRange;
    }

    public Node getStartNode() {
        return startNode;
    }

    public Node getEndNode() {
        return endNode;
    }

    public List<Node> getInsNodes() {
        return insNodes;
    }

    public List<Node> getDelNodes() {
        return delNodes;
    }

    public List<EndBraceDelAction> getEndBraceDelActions() {
        return endBraceDelActions;
    }

    public ASTNode getIfOrLoopAstNode() {
        return ifOrLoopAstNode;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public Map<String, List<EndBraceDelAction>> getDelStringEndBraceMap() {
        return delStringEndBraceMap;
    }
}
