package apr.aprlab.repair.adapt.entities;

import java.util.ArrayList;
import java.util.List;
import apr.aprlab.repair.adapt.ged.action.extract.TraversalUtil;
import apr.aprlab.repair.adapt.ged.util.Edge;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.ast.ASTWalk;
import apr.aprlab.utils.ast.Range;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import soot.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class MyUnitUtil {

    public static final Logger logger = LogManager.getLogger(MyUnitUtil.class);

    public static void updateLineNo(Graph graph) {
        List<Node> graphNodes = graph.getNodes();
        for (Node node : graphNodes) {
            int lineNo = node.getAttribute().getLineNo();
            CompilationUnit cu = node.getAttribute().getMethodSnippet().getCompilationUnit();
            if (lineNo == 679) {
                logger.debug("");
            }
            Statement astStmtInSameLine = getAstStmtInSameLine(node.getAttribute());
            if (astStmtInSameLine != null) {
                int startLineNo = RangeUtil.getStartLineNo(cu, astStmtInSameLine);
                int endLineNo = RangeUtil.getEndLineNo(cu, astStmtInSameLine);
                if (startLineNo != lineNo) {
                    logger.warn("the unit lineNo should be updated from {} to {}, the unit: {}", lineNo, startLineNo, node.getAttribute());
                    if (astStmtInSameLine instanceof DoStatement && lineNo == endLineNo) {
                    } else if (astStmtInSameLine instanceof SynchronizedStatement && lineNo == endLineNo) {
                    } else {
                        node.getAttribute().updateLineNo(startLineNo);
                    }
                }
            }
        }
    }

    public static void updateLineNoForElseIf(Graph graph) {
        List<Node> graphNodes = new ArrayList<Node>(graph.getNodes());
        List<Node> visited = new ArrayList<Node>();
        for (Node node : graphNodes) {
            if (visited.contains(node)) {
                continue;
            }
            int lineNo = node.getAttribute().getLineNo();
            CompilationUnit cu = node.getAttribute().getMethodSnippet().getCompilationUnit();
            if (node.isIf() && !node.isWhile()) {
                List<Node> nodesInSameLine = getNodesInSameLine(node, graph.getNodes());
                List<Node> ifNodes = getIfNodes(nodesInSameLine);
                visited.addAll(ifNodes);
                List<Node> nextNodes = new ArrayList<Node>();
                for (Node ifNode : ifNodes) {
                    for (Node nextNode : getNextNodesOfIf(ifNode)) {
                        if (!nextNodes.contains(nextNode)) {
                            nextNodes.add(nextNode);
                        }
                    }
                }
                if (nextNodes.size() > 2) {
                    for (Node ifNode : ifNodes) {
                        Node nextFalseBranchNode = ifNode.getIfBlockStart(false);
                        int nextFalseBranchNodeLineNo = nextFalseBranchNode.getAttribute().getLineNo();
                        if (nextFalseBranchNodeLineNo <= lineNo) {
                            continue;
                        }
                        ExceptionUtil.myAssert(nextFalseBranchNodeLineNo > lineNo);
                        for (int i = nextFalseBranchNodeLineNo - 1; i > lineNo; i--) {
                            ASTNode ifAstNode = ASTUtil.findUnitStmtByLineNo(cu, i);
                            if (ifAstNode instanceof IfStatement) {
                                logger.warn("update else ifNode lineNo from {} to {}", ifNode.getAttribute().getLineNo(), i);
                                ifNode.getAttribute().updateLineNo(i);
                                break;
                            }
                        }
                    }
                    for (int i = 1; i < ifNodes.size() - 1; i++) {
                        Node prevIf = ifNodes.get(i - 1);
                        Node curIf = ifNodes.get(i);
                        Node nextIf = ifNodes.get(i + 1);
                        int curLineNo = curIf.getAttribute().getLineNo();
                        int nextLineNo = nextIf.getAttribute().getLineNo();
                        if (curLineNo < prevIf.getAttribute().getLineNo()) {
                            logger.warn("update else ifNode lineNo from {} to {}", curLineNo, nextLineNo);
                            curIf.getAttribute().updateLineNo(nextLineNo);
                        }
                    }
                    for (Node curNode : nodesInSameLine) {
                        if (!curNode.isIf()) {
                            delNodeFromEdges(curNode, graph);
                        }
                    }
                    List<Node> ifVisited = new ArrayList<Node>();
                    for (int i = 1; i < ifNodes.size(); i++) {
                        Node curIf = ifNodes.get(i);
                        if (ifVisited.contains(curIf)) {
                            continue;
                        }
                        List<Node> ifNodesInSameLine = getNodesInSameLine(curIf, ifNodes);
                        List<Node> ifNextNodes = getUniqNextNodesOfIf(ifNodesInSameLine);
                        ifVisited.addAll(ifNextNodes);
                        while (ifNextNodes.size() > 2) {
                            logger.warn("incorrect lineno is detected (may due to multi else if & multi exprs). update lineno here.");
                            Node lastCurIfNode = ifNodesInSameLine.get(ifNodesInSameLine.size() - 1);
                            int lastCurIfIndex = ifNodes.indexOf(lastCurIfNode);
                            Node nextIfNode = ifNodes.get(lastCurIfIndex + 1);
                            logger.warn("update lineNo from {} to {}", lastCurIfNode, nextIfNode.getAttribute().getLineNo());
                            lastCurIfNode.getAttribute().updateLineNo(nextIfNode.getAttribute().getLineNo());
                            ifNodesInSameLine = getNodesInSameLine(curIf, ifNodes);
                            ifNextNodes = getUniqNextNodesOfIf(ifNodesInSameLine);
                        }
                    }
                }
            }
        }
    }

    private static List<Node> getUniqNextNodesOfIf(List<Node> ifNodesInSameLine) {
        List<Node> allUniqNextNodes = new ArrayList<Node>();
        for (Node ifNode : ifNodesInSameLine) {
            for (Node nextNode : getNextNodesOfIf(ifNode)) {
                if (!allUniqNextNodes.contains(nextNode)) {
                    allUniqNextNodes.add(nextNode);
                }
            }
        }
        return allUniqNextNodes;
    }

    private static List<Node> getNextNodesOfIf(Node ifNode) {
        List<Node> nextNodes = new ArrayList<Node>();
        for (Node node : ifNode.getNextNodes()) {
            if (node.getAttribute().getLineNo() != ifNode.getAttribute().getLineNo()) {
                nextNodes.add(node);
            }
        }
        return nextNodes;
    }

    public static List<Node> getIfNodes(List<Node> nodesInSameLine) {
        List<Node> ifNodes = new ArrayList<Node>();
        for (Node curNode : nodesInSameLine) {
            if (curNode.isIf()) {
                ifNodes.add(curNode);
            }
        }
        return ifNodes;
    }

    public static void alignAstNodes(Graph graph) {
        List<Node> graphNodes = new ArrayList<Node>(TraversalUtil.depthFirstTraversalForNewOrOld(graph, false));
        List<Node> visited = new ArrayList<Node>();
        for (Node node : graphNodes) {
            if (visited.contains(node)) {
                continue;
            }
            logger.debug("cur node: {}", node);
            if (node.getAttribute().getLineNo() == 1263) {
                logger.debug("");
            }
            if (node.getAttribute().getLineNo() == -1 || node.getAttribute().getLineNo() == -2) {
                if (node.getAttribute().isStartPara()) {
                    delNodeFromEdges(node, graph);
                } else {
                    setForNodeWithNegLineNo(node, graph);
                }
                visited.add(node);
                continue;
            }
            List<Node> nodesInSameLine = new ArrayList<Node>();
            getNeighborNodesInSameLine(node, nodesInSameLine);
            nodesInSameLine.sort(ComparatorUtil.nodeOldIdComparator);
            visited.addAll(nodesInSameLine);
            if (nodesInSameLine.size() == 1) {
                setStmtAstForNode(node, graph);
            } else if (nodesInSameLine.size() > 1) {
                if (unitsShouldMerge(nodesInSameLine)) {
                    mergeAndSetAstForNodes(nodesInSameLine, graph);
                } else {
                    setIfLoopAstForNodes(nodesInSameLine, graph);
                }
            }
        }
    }

    private static void setForNodeWithNegLineNo(Node node, Graph graph) {
        if (node.getAttribute().isStartThis() || node.getAttribute().isLastReturn()) {
        } else {
            delNodeFromEdges(node, graph);
        }
    }

    private static void setIfLoopAstForNodes(List<Node> nodesInSameLine, Graph graph) {
        if (!containsWhile(nodesInSameLine)) {
            int firstIfIndex = getIfIndexInNodes(nodesInSameLine, true);
            int lastIfIndex = getIfIndexInNodes(nodesInSameLine, false);
            List<Node> nonIfNodesBeforeIf = new ArrayList<Node>();
            for (int i = 0; i < firstIfIndex; i++) {
                Node node = nodesInSameLine.get(i);
                ExceptionUtil.myAssert(!node.isIf());
                nonIfNodesBeforeIf.add(node);
            }
            List<Node> nonIfNodesAfterIf = new ArrayList<Node>();
            for (int i = lastIfIndex + 1; i < nodesInSameLine.size(); i++) {
                Node node = nodesInSameLine.get(i);
                ExceptionUtil.myAssert(!node.isIf());
                nonIfNodesAfterIf.add(node);
            }
            List<Node> ifNodes = getIfNodes(nodesInSameLine);
            MyUnit myUnit = ifNodes.get(0).getAttribute();
            ASTNode stmt = getStmtForUnit(myUnit, true);
            if (stmt == null) {
                logger.warn("this if node has no ast node, it should be deleted: {}", ifNodes.get(0));
                if (!nonIfNodesBeforeIf.isEmpty()) {
                    getStmtForUnit(myUnit, true);
                    logger.debug("");
                }
                ExceptionUtil.myAssert(nonIfNodesBeforeIf.isEmpty());
                delNodeFromEdges(ifNodes, graph);
                mergeAndSetAstForNodes(nonIfNodesAfterIf, graph);
            } else {
                delNodeFromEdges(nonIfNodesBeforeIf, graph);
                setAstForIfNodes(ifNodes, nonIfNodesAfterIf, graph);
            }
        } else {
            ASTNode stmt = getStmtForUnit(nodesInSameLine.get(0).getAttribute(), true);
            if (stmt instanceof ForStatement) {
                ForStatement forStmt = (ForStatement) stmt;
                @SuppressWarnings("unchecked")
                List<ASTNode> inits = forStmt.initializers();
                @SuppressWarnings("unchecked")
                List<ASTNode> updaters = forStmt.updaters();
                int ifNodeIndex = getIfIndexInNodes(nodesInSameLine, true);
                for (int i = 0; i < ifNodeIndex - 1; i++) {
                    logger.warn("we delete extra init nodes here: {}", nodesInSameLine.get(0));
                    delNodeFromEdges(nodesInSameLine.get(0), graph);
                    nodesInSameLine.remove(0);
                }
                ifNodeIndex = getIfIndexInNodes(nodesInSameLine, true);
                if (inits.isEmpty() || ifNodeIndex == 0) {
                    for (int i = 0; i < ifNodeIndex; i++) {
                        logger.warn("we delete extra init nodes here: {}", nodesInSameLine.get(0));
                        delNodeFromEdges(nodesInSameLine.get(0), graph);
                        nodesInSameLine.remove(0);
                    }
                    nodesInSameLine.get(0).getAttribute().setAstNodeAndCollectEntities(forStmt.getExpression());
                    int cnt = 1;
                    for (ASTNode updater : updaters) {
                        nodesInSameLine.get(cnt++).getAttribute().setAstNodeAndCollectEntities(updater);
                    }
                } else {
                    nodesInSameLine.get(0).getAttribute().setAstNodeAndCollectEntities(inits.get(0));
                    nodesInSameLine.get(1).getAttribute().setAstNodeAndCollectEntities(forStmt.getExpression());
                    if (updaters.size() == 1) {
                        nodesInSameLine.get(2).getAttribute().setAstNodeAndCollectEntities(updaters.get(0));
                    }
                }
            } else if (stmt instanceof DoStatement) {
                DoStatement doStmt = (DoStatement) stmt;
                List<Node> exprNodes = new ArrayList<Node>();
                for (Node node : nodesInSameLine) {
                    if (!node.isIf()) {
                        delNodeFromEdges(node, graph);
                    } else {
                        exprNodes.add(node);
                    }
                }
                ExceptionUtil.myAssert(exprNodes.size() == 1);
                exprNodes.get(0).getAttribute().setAstNodeAndCollectEntities(doStmt.getExpression());
            } else if (stmt instanceof EnhancedForStatement) {
                EnhancedForStatement enhancedForStatement = (EnhancedForStatement) stmt;
                for (Node node : nodesInSameLine) {
                    node.getAttribute().setAstNodeAndCollectEntities(enhancedForStatement);
                }
            } else if (stmt instanceof WhileStatement) {
                WhileStatement whileStatement = (WhileStatement) stmt;
                List<Node> ifNodes = getIfNodes(nodesInSameLine);
                ExceptionUtil.myAssert(ifNodes.size() == 1);
                ifNodes.get(0).getAttribute().setAstNodeAndCollectEntities(whileStatement.getExpression());
                nodesInSameLine.removeAll(ifNodes);
                delNodeFromEdges(nodesInSameLine, graph);
            } else {
                Globals.hasTodoIssues = true;
                return;
            }
        }
    }

    private static int getIfIndexInNodes(List<Node> nodesInSameLine, boolean getFirstIfIndex) {
        int lastIfIndex = -1;
        for (int i = 0; i < nodesInSameLine.size(); i++) {
            Node node = nodesInSameLine.get(i);
            if (node.isIf()) {
                if (getFirstIfIndex) {
                    return i;
                } else {
                    lastIfIndex = i;
                }
            }
        }
        ExceptionUtil.myAssert(lastIfIndex != -1);
        return lastIfIndex;
    }

    private static void delNodeFromEdges(List<Node> nodes, Graph graph) {
        for (Node node : nodes) {
            delNodeFromEdges(node, graph);
        }
    }

    private static void setAstForIfNodes(List<Node> ifNodes, List<Node> nonIfNodesAfterIf, Graph graph) {
        MyUnit myUnit = ifNodes.get(0).getAttribute();
        ASTNode stmt = getStmtForUnit(myUnit, true);
        ExceptionUtil.assertTrue(ASTUtil.isLoopOrIfAstNode(stmt));
        if (stmt instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) stmt;
            Expression expr = ifStmt.getExpression();
            if (ifNodes.size() == 1) {
                ifNodes.get(0).getAttribute().setAstNodeAndCollectEntities(expr);
            } else {
                if (expr instanceof InfixExpression) {
                    InfixExpression infixExpr = (InfixExpression) expr;
                    if (ifNodes.size() == 2) {
                        ifNodes.get(0).getAttribute().setAstNodeAndCollectEntities(infixExpr.getLeftOperand());
                        ifNodes.get(1).getAttribute().setAstNodeAndCollectEntities(infixExpr.getRightOperand());
                    } else if (ifNodes.size() == 3) {
                        ExceptionUtil.myAssert(infixExpr.getLeftOperand() instanceof InfixExpression);
                        InfixExpression childInfixExpression = (InfixExpression) infixExpr.getLeftOperand();
                        ifNodes.get(0).getAttribute().setAstNodeAndCollectEntities(childInfixExpression.getLeftOperand());
                        ifNodes.get(1).getAttribute().setAstNodeAndCollectEntities(childInfixExpression.getRightOperand());
                        ifNodes.get(2).getAttribute().setAstNodeAndCollectEntities(infixExpr.getRightOperand());
                    } else {
                        Globals.hasTodoIssues = true;
                        return;
                    }
                } else {
                    Globals.hasTodoIssues = true;
                    return;
                }
            }
            if (!nonIfNodesAfterIf.isEmpty()) {
                Statement blockStmts = ifStmt.getThenStatement();
                if (blockStmts instanceof Block) {
                    Block block = (Block) blockStmts;
                    @SuppressWarnings("unchecked")
                    List<ASTNode> stmts = block.statements();
                    for (int i = 0; i < stmts.size(); i++) {
                        ASTNode curStmt = stmts.get(i);
                        nonIfNodesAfterIf.get(i).getAttribute().setAstNodeAndCollectEntities(curStmt);
                    }
                } else {
                    nonIfNodesAfterIf.get(0).getAttribute().setAstNodeAndCollectEntities(blockStmts);
                    nonIfNodesAfterIf.remove(0);
                    if (!nonIfNodesAfterIf.isEmpty()) {
                        delNodeFromEdges(nonIfNodesAfterIf, graph);
                    }
                }
            }
        } else {
            ExceptionUtil.raise();
        }
    }

    public static ASTNode getStmtForUnit(MyUnit myUnit, boolean forIfLoop) {
        List<ASTNode> astNodesInSameLine = getAstNodesInSameLine(myUnit, forIfLoop);
        Statement stmt = null;
        int cnt = 0;
        for (ASTNode node : astNodesInSameLine) {
            if (node instanceof Statement) {
                if (forIfLoop) {
                    if (ASTUtil.isLoopOrIfAstNode(node)) {
                        stmt = (Statement) node;
                        cnt++;
                    }
                } else {
                    stmt = (Statement) node;
                    cnt++;
                }
            }
        }
        if (cnt == 0) {
            if (myUnit.isField()) {
                ASTNode stmtOrField = ASTUtil.findUnitStmtByLineNo(myUnit.getMethodSnippet().getCompilationUnit(), myUnit.getLineNo());
                return stmtOrField;
            } else {
                logger.warn("unknown myUnit with no matched ast: {}", myUnit);
                return null;
            }
        } else {
            if (cnt != 1) {
                logger.debug("the unit contains multiple stmts: {}", myUnit);
            }
            return stmt;
        }
    }

    public static List<ASTNode> getAstNodesInSameLine(MyUnit myUnit) {
        return getAstNodesInSameLine(myUnit, false);
    }

    public static List<ASTNode> getAstNodesInSameLine(MyUnit myUnit, boolean forIfLoop) {
        List<ASTNode> astNodesInSameLine = new ArrayList<ASTNode>();
        MethodSnippet methodSnippet = myUnit.getMethodSnippet();
        List<ASTNode> astNodes = methodSnippet.getAstNodes();
        CompilationUnit cu = methodSnippet.getCompilationUnit();
        for (ASTNode astNode : astNodes) {
            Range range = RangeUtil.getRange(cu, astNode);
            if (range.getStartLineNo() == myUnit.getLineNo() || range.getEndLineNo() == myUnit.getLineNo()) {
                if (astNode instanceof Block) {
                    continue;
                }
                if (ASTUtil.isLoopOrIfAstNode(astNode)) {
                    if (forIfLoop) {
                        astNodesInSameLine.add(astNode);
                    }
                } else {
                    astNodesInSameLine.add(astNode);
                }
            }
        }
        return astNodesInSameLine;
    }

    public static Statement getAstStmtInSameLine(MyUnit myUnit) {
        List<Statement> astNodesInSameLine = new ArrayList<>();
        MethodSnippet methodSnippet = myUnit.getMethodSnippet();
        List<ASTNode> astNodes = methodSnippet.getAstNodes();
        CompilationUnit cu = methodSnippet.getCompilationUnit();
        for (ASTNode astNode : astNodes) {
            Range range = RangeUtil.getRange(cu, astNode);
            if (RangeUtil.rangeContains(range, new Range(myUnit.getLineNo(), myUnit.getLineNo())) >= 0) {
                if (astNode instanceof Block) {
                    continue;
                }
                if (astNode instanceof Statement) {
                    astNodesInSameLine.add((Statement) astNode);
                }
            }
        }
        astNodesInSameLine.sort(ComparatorUtil.astNodeRangeCompator);
        if (astNodesInSameLine.isEmpty()) {
            return null;
        } else {
            return astNodesInSameLine.get(0);
        }
    }

    private static boolean containsWhile(List<Node> nodesInSameLine) {
        for (Node node : nodesInSameLine) {
            if (node.isWhile()) {
                return true;
            }
        }
        return false;
    }

    private static void mergeAndSetAstForNodes(List<Node> nodesInSameLine, Graph graph) {
        Node keptNode = nodesInSameLine.get(nodesInSameLine.size() - 1);
        setStmtAstForNode(keptNode, graph);
        for (int i = 0; i < nodesInSameLine.size() - 1; i++) {
            Node node = nodesInSameLine.get(i);
            delNodeFromEdges(node, graph);
        }
    }

    private static void delNodeFromEdges(Node node, Graph graph) {
        List<Edge> toThisEdgesClone = new ArrayList<Edge>(node.getToThisEdges());
        List<Edge> fromThisEdges = new ArrayList<Edge>(node.getFromThisEdges());
        graph.getNodes().remove(node);
        graph.remove(node);
        for (Edge edge : toThisEdgesClone) {
            graph.getEdges().remove(edge);
        }
        for (Edge edge : fromThisEdges) {
            List<Node> prevNodes = node.getPrevNodes();
            graph.getEdges().remove(edge);
            for (Node prevNode : prevNodes) {
                Edge newEdge = new Edge(edge);
                if (prevNode.isIf() && !node.isIf()) {
                    Edge ifEdge = node.getPrevIfEdge(prevNode);
                    newEdge.setLabel(ifEdge.getLabel());
                    newEdge.setOldLabel(ifEdge.getOldLabel());
                }
                newEdge.setStartNode(prevNode);
                graph.getEdges().add(newEdge);
            }
        }
        graph.initFromToEdges();
    }

    private static boolean unitsShouldMerge(List<Node> nodesInSameLine) {
        for (Node node : nodesInSameLine) {
            if (node.isIf()) {
                return false;
            }
        }
        return true;
    }

    private static void setStmtAstForNode(Node node, Graph graph) {
        MyUnit myUnit = node.getAttribute();
        if (node.isIf()) {
            if (node.isWhile()) {
                ASTNode stmtInSameLine = getStmtForUnit(myUnit, true);
                if (stmtInSameLine instanceof WhileStatement) {
                    WhileStatement whileStmt = (WhileStatement) stmtInSameLine;
                    Expression exprNode = whileStmt.getExpression();
                    myUnit.setAstNodeAndCollectEntities(exprNode);
                } else if (stmtInSameLine instanceof DoStatement) {
                    DoStatement whileStmt = (DoStatement) stmtInSameLine;
                    Expression exprNode = whileStmt.getExpression();
                    myUnit.setAstNodeAndCollectEntities(exprNode);
                } else if (stmtInSameLine instanceof ForStatement) {
                    ForStatement forStatement = (ForStatement) stmtInSameLine;
                    Expression exprNode = forStatement.getExpression();
                    myUnit.setAstNodeAndCollectEntities(exprNode);
                } else if (stmtInSameLine instanceof EnhancedForStatement) {
                    EnhancedForStatement enhancedForStatement = (EnhancedForStatement) stmtInSameLine;
                    Expression exprNode = enhancedForStatement.getExpression();
                    myUnit.setAstNodeAndCollectEntities(exprNode);
                } else {
                    Globals.hasTodoIssues = true;
                    return;
                }
            } else {
                ASTNode stmtInSameLine = getStmtForUnit(myUnit, true);
                ExceptionUtil.assertTrue(stmtInSameLine instanceof IfStatement);
                Expression exprNode = ((IfStatement) stmtInSameLine).getExpression();
                myUnit.setAstNodeAndCollectEntities(exprNode);
            }
        } else {
            ASTNode stmtInSameLine = getStmtForUnit(myUnit, false);
            if (stmtInSameLine == null) {
                if (node.getAttribute().getUnit().toString().equals("return")) {
                    if (myUnit.getLineNo() != myUnit.getMethodSnippet().getRange().getEndLineNo()) {
                        logger.warn("[Warning] the last return is not the endLineNo of the method.");
                    }
                    myUnit.setIsLastReturn(true);
                } else {
                    delNodeFromEdges(node, graph);
                }
            } else {
                myUnit.setAstNodeAndCollectEntities(stmtInSameLine);
            }
        }
    }

    private static List<Node> getNodesInSameLine(Node node, List<Node> graphNodes) {
        int lineNo = node.getAttribute().getLineNo();
        List<Node> nodesInSameLine = new ArrayList<Node>();
        for (int i = 0; i < graphNodes.size(); i++) {
            Node curNode = graphNodes.get(i);
            if (lineNo == curNode.getAttribute().getLineNo()) {
                nodesInSameLine.add(curNode);
            }
        }
        return nodesInSameLine;
    }

    public static void labelWhileNodes(Graph graph) {
        for (Node node : graph.getNodes()) {
            boolean isWhile = checkIsWhile(node);
            node.getAttribute().setIsWhile(isWhile);
        }
    }

    private static boolean checkIsWhile(Node node) {
        if (node.getAttribute().getLineNo() == 252) {
            logger.debug("");
        }
        if (TraversalUtil.hasCircle(node)) {
            return true;
        }
        return false;
    }

    public static void addOmittedAstNodes(Graph graph) {
        MethodSnippet ms = graph.getEntryNode().getAttribute().getMethodSnippet();
        MethodDeclaration md = ms.getMethodDeclaration();
        List<Statement> stmtsInMd = new ArrayList<Statement>();
        for (ASTNode astNode : ASTWalk.getNodeIterable(md)) {
            if (consideredByOmitted(astNode)) {
                stmtsInMd.add((Statement) astNode);
            }
        }
        List<Statement> stmtsInGraph = new ArrayList<Statement>();
        for (Node node : graph.getNodes()) {
            MyUnit myUnit = node.getAttribute();
            ASTNode astNode = myUnit.getAstNode();
            if (consideredByOmitted(astNode)) {
                stmtsInGraph.add((Statement) astNode);
            }
        }
        List<Statement> stmtsOmitted = CollectionUtil.getUniqueInSrc(stmtsInMd, stmtsInGraph);
        excludeDeadCodeStmts(stmtsOmitted);
        if (!stmtsOmitted.isEmpty()) {
            logger.warn("the stmts are omitted: {}", stmtsOmitted);
            for (Statement stmt : stmtsOmitted) {
                logger.debug("current omitted stmt: {}", stmt);
                Range stmtRange = RangeUtil.getRange(stmt);
                MyUnit ommitedMyUnit = new MyUnit(stmt, ms);
                Node omittedNode = new Node(true, Globals.gedGraphStartIndex++, ommitedMyUnit);
                if (astNodeIsAlreadyAdded(stmtsInGraph, stmt)) {
                    continue;
                } else {
                    stmtsInGraph.add(stmt);
                }
                List<Node> nodesInSameLine = getNodesByLineNo(stmtRange.getStartLineNo(), graph.getNodes());
                if (!nodesInSameLine.isEmpty()) {
                    graph.getNodes().add(omittedNode);
                    graph.add(omittedNode);
                    if (stmt.getStartPosition() >= nodesInSameLine.get(0).getStartPos()) {
                        Globals.hasTodoIssues = true;
                        return;
                    }
                    Node nextNode = nodesInSameLine.get(0);
                    for (Edge edge : nextNode.getToThisEdges()) {
                        edge.setEndNode(omittedNode);
                    }
                    Edge nextEdge = new Edge(true, "");
                    nextEdge.setStartNode(omittedNode);
                    nextEdge.setEndNode(nextNode);
                    graph.getEdges().add(nextEdge);
                    graph.initFromToEdges();
                    continue;
                }
                logger.debug("stmt: {}", stmt);
                Node closestNode = getClosestNode(stmt, stmtRange.getStartLineNo(), graph.getNodes());
                if (closestNode == null) {
                    logger.error("closestNode is null for current stmt: {}", stmt);
                    continue;
                }
                graph.getNodes().add(omittedNode);
                graph.add(omittedNode);
                if (closestNode.getAttribute().getLineNo() < stmtRange.getStartLineNo()) {
                    for (Edge edge : closestNode.getFromThisEdges()) {
                        edge.setStartNode(omittedNode);
                    }
                    Edge prevEdge = new Edge(true, "");
                    prevEdge.setStartNode(closestNode);
                    prevEdge.setEndNode(omittedNode);
                    graph.getEdges().add(prevEdge);
                    graph.initFromToEdges();
                } else {
                    for (Edge edge : closestNode.getToThisEdges()) {
                        edge.setEndNode(omittedNode);
                    }
                    Edge nextEdge = new Edge(true, "");
                    nextEdge.setStartNode(omittedNode);
                    nextEdge.setEndNode(closestNode);
                    graph.getEdges().add(nextEdge);
                    graph.initFromToEdges();
                }
            }
        }
    }

    private static void excludeDeadCodeStmts(List<Statement> stmtsOmitted) {
        List<Statement> copyStmtsOmitted = new ArrayList<Statement>(stmtsOmitted);
        List<Statement> visited = new ArrayList<Statement>();
        for (Statement stmt : copyStmtsOmitted) {
            if (visited.contains(stmt)) {
                continue;
            }
            visited.add(stmt);
            ASTNode parent = stmt.getParent();
            if (parent instanceof Block) {
                @SuppressWarnings("unchecked")
                List<Statement> blockStmts = ((Block) parent).statements();
                if (CollectionUtil.containsDstList(stmtsOmitted, blockStmts)) {
                    logger.warn("dead code is observed.");
                    stmtsOmitted.removeAll(blockStmts);
                    visited.addAll(blockStmts);
                }
            }
        }
    }

    private static boolean astNodeIsAlreadyAdded(List<Statement> stmtsInGraph, Statement stmt) {
        for (Statement curStmt : stmtsInGraph) {
            if (RangeUtil.containsDstNode(curStmt, stmt)) {
                logger.debug("astNode is already added (or not omitted): {}", stmt);
                return true;
            }
        }
        return false;
    }

    private static Node getClosestNode(Statement stmt, int startLineNo, List<Node> nodes) {
        ASTNode parentAstNode = stmt.getParent();
        SwitchCase switchCase = null;
        if (parentAstNode instanceof SwitchStatement) {
            List<ASTNode> astNodes = ASTUtil.getChildren(parentAstNode);
            int curIndex = astNodes.indexOf(stmt);
            for (int i = curIndex; i >= 0; i--) {
                if (astNodes.get(i) instanceof SwitchCase) {
                    switchCase = (SwitchCase) astNodes.get(i);
                    break;
                }
            }
        }
        Node closestNode = null;
        int dist = Integer.MAX_VALUE;
        for (Node node : nodes) {
            int lineNo = node.getAttribute().getLineNo();
            ASTNode curAstNode = node.getAttribute().getAstNode();
            if (node.getAttribute().isLastReturn()) {
                continue;
            }
            if (lineNo > 0) {
                int curDist = Math.abs(lineNo - startLineNo);
                if (curDist < dist && RangeUtil.containsDstNode(parentAstNode, curAstNode)) {
                    if (switchCase != null) {
                        if (RangeUtil.isBeforeDstNode(switchCase, curAstNode)) {
                            closestNode = node;
                            dist = curDist;
                        }
                    } else {
                        closestNode = node;
                        dist = curDist;
                    }
                }
            }
        }
        return closestNode;
    }

    private static List<Node> getNodesByLineNo(int startLineNo, List<Node> nodes) {
        List<Node> nodesInLineNo = new ArrayList<Node>();
        for (Node node : nodes) {
            if (node.getAttribute().getLineNo() == startLineNo) {
                nodesInLineNo.add(node);
            }
        }
        return nodesInLineNo;
    }

    private static boolean consideredByOmitted(ASTNode astNode) {
        boolean is = astNode instanceof Statement && !(astNode instanceof Block) && !ASTUtil.isLoopOrIfAstNode(astNode) && !(astNode instanceof BreakStatement) && !(astNode instanceof ContinueStatement) && !(astNode instanceof SwitchCase);
        return is;
    }

    public static void mergeMultiIfExprNodes(Graph graph) {
        List<Node> visited = new ArrayList<Node>();
        List<Node> graphNodes = new ArrayList<Node>(graph.getNodes());
        for (Node node : graphNodes) {
            if (visited.contains(node)) {
                continue;
            }
            if (node.isIf()) {
                if (node.getAttribute().getLineNo() == 227) {
                    logger.debug("");
                }
                List<Node> nodesInSameLine = getNodesInSameLine(node, graph.getNodes());
                visited.addAll(nodesInSameLine);
                List<Node> ifNodes = new ArrayList<Node>();
                for (Node curNode : nodesInSameLine) {
                    if (curNode.isIf()) {
                        ifNodes.add(curNode);
                    }
                }
                if (ifNodes.size() > 1) {
                    int firstIfIndex = getIfIndexInNodes(nodesInSameLine, true);
                    int lastIfIndex = getIfIndexInNodes(nodesInSameLine, false);
                    for (int i = firstIfIndex + 1; i <= lastIfIndex; i++) {
                        delNodeFromEdges(nodesInSameLine.get(i), graph);
                    }
                    Node mergedIfNode = nodesInSameLine.get(firstIfIndex);
                    if (mergedIfNode.getAttribute().getLineNo() == 313) {
                        logger.debug("");
                    }
                    Statement stmt = getAstStmtInSameLine(mergedIfNode.getAttribute());
                    if (!ASTUtil.isLoopOrIfAstNode(stmt)) {
                        logger.warn("fake if node: {}", mergedIfNode);
                        if (mergedIfNode.getFromThisEdges().size() > 2) {
                            logger.warn("remove extra edges for fake if node in merging multi if.");
                            List<Node> uniqNextNodes = mergedIfNode.getUniqNextNodes();
                            graph.getEdges().removeAll(mergedIfNode.getFromThisEdges());
                            graph.getEdges().add(new Edge(mergedIfNode, uniqNextNodes.get(0), "false"));
                            graph.getEdges().add(new Edge(mergedIfNode, uniqNextNodes.get(1), "true"));
                            graph.initFromToEdges();
                        }
                        continue;
                    }
                    if (mergedIfNode.getFromThisEdges().size() > 2) {
                        logger.warn("mergedIfNode need to get the correct edge label: {}", mergedIfNode);
                        List<Node> uniqNextNodes = mergedIfNode.getUniqNextNodes();
                        if (uniqNextNodes.size() != 2) {
                            logger.debug("");
                            for (Node curNode : uniqNextNodes) {
                                if (curNode.getAttribute().getLineNo() == mergedIfNode.getAttribute().getLineNo()) {
                                    logger.warn("curNode may be one line if with lambda, so delete it: {}", curNode);
                                    delNodeFromEdges(curNode, graph);
                                }
                            }
                            uniqNextNodes = mergedIfNode.getUniqNextNodes();
                        }
                        if (uniqNextNodes.size() != 2) {
                            logger.warn("The merged if node may be else if with multi exprs: {}", mergedIfNode);
                            uniqNextNodes.sort(ComparatorUtil.lineNoCompForOld);
                            uniqNextNodes.remove(uniqNextNodes.size() - 1);
                        }
                        if (uniqNextNodes.size() != 2) {
                            logger.debug("");
                        }
                        ExceptionUtil.myAssert(uniqNextNodes.size() == 2);
                        graph.getEdges().removeAll(mergedIfNode.getFromThisEdges());
                        Node firstNode = uniqNextNodes.get(0);
                        Node secondNode = uniqNextNodes.get(1);
                        ExceptionUtil.myAssert(firstNode.getAttribute().getLineNo() != secondNode.getAttribute().getLineNo());
                        if (firstNode.getAttribute().getLineNo() < secondNode.getAttribute().getLineNo()) {
                            graph.getEdges().add(new Edge(mergedIfNode, firstNode, "false"));
                            graph.getEdges().add(new Edge(mergedIfNode, secondNode, "true"));
                            logger.debug("[multiple if expr] new edge: {}", new Edge(mergedIfNode, firstNode, "false"));
                        } else {
                            graph.getEdges().add(new Edge(mergedIfNode, firstNode, "true"));
                            graph.getEdges().add(new Edge(mergedIfNode, secondNode, "false"));
                            logger.debug("[multiple if expr] new edge: {}", new Edge(mergedIfNode, firstNode, "true"));
                        }
                    }
                    graph.initFromToEdges();
                }
            }
        }
    }

    public static void removeBadLineNoNodes(Graph graph) {
        List<Node> graphNodes = new ArrayList<Node>(graph.getNodes());
        ExceptionUtil.myAssert(graphNodes.get(0).getOldAttribute().getLineNo() == -1);
        int index = 0;
        for (int i = 0; i < graphNodes.size(); i++) {
            if (graphNodes.get(i).getOldAttribute().getLineNo() == -1) {
                index = i;
            } else {
                break;
            }
        }
        for (int i = index + 1; i < graphNodes.size(); i++) {
            Node node = graphNodes.get(i);
            if (node.getOldAttribute().getLineNo() == -1) {
                logger.debug("delete -1 lineno node: {}", node);
                delNodeFromEdges(node, graph);
            }
        }
    }

    public static void deleteExtraGraphs(Graph graph) {
        List<Node> allNodes = new ArrayList<Node>(graph.getNodes());
        List<List<Node>> groupedNodes = new ArrayList<List<Node>>();
        int containsStartThisOrParaIndex = -1;
        int hits = 0;
        int cnt = 0;
        while (!allNodes.isEmpty()) {
            List<Node> nodes = TraversalUtil.realDepthFirstTraversalForNewOrOld(allNodes.get(0), false);
            allNodes.removeAll(nodes);
            groupedNodes.add(nodes);
            for (Node node : nodes) {
                if (node.getAttribute().isStartThisOrPara()) {
                    hits++;
                    containsStartThisOrParaIndex = cnt;
                    break;
                }
            }
            cnt++;
        }
        if (hits == 0) {
            logger.warn("The cfg graph has no startThisOrPara node.");
            return;
        }
        for (int i = 0; i < groupedNodes.size(); i++) {
            if (i == containsStartThisOrParaIndex) {
                continue;
            }
            graph.removeAll(groupedNodes.get(i));
            graph.getNodes().removeAll(groupedNodes.get(i));
        }
        graph.updateEdges();
    }

    public static void removeCaughtExceptionNodes(Graph graph) {
        List<Node> nodes = new ArrayList<Node>(graph.getNodes());
        for (Node node : nodes) {
            MyUnit myUnit = node.getAttribute();
            Unit unit = myUnit.getUnit();
            if (unit.toString().contains("@caughtexception")) {
                ExceptionUtil.myAssert(node.getPrevNodes().isEmpty());
                ExceptionUtil.myAssert(node.getNextNodes().size() == 1);
                Node curNode = node;
                while (true) {
                    if (curNode.getNextNodes().isEmpty()) {
                        delNodeFromEdges(curNode, graph);
                        break;
                    }
                    Node nextNode = curNode.getNextNodes().get(0);
                    delNodeFromEdges(curNode, graph);
                    if (nextNode.getPrevNodes().isEmpty()) {
                        logger.warn("TODO: now we delete the stmt in caught exception block...");
                        curNode = nextNode;
                    } else {
                        break;
                    }
                }
            }
        }
    }

    public static void identifyFakeOneLineIfNodes(Graph graph) {
        for (Node node : new ArrayList<>(graph.getNodes())) {
            if (node.isIf()) {
                Node exitNode = TraversalUtil.getExitNode(node, false);
                if (isLambda(node) && exitNode != null && exitNode.hasSameLineNo(node)) {
                    logger.warn("a fake one-line if is found: {}", node);
                    List<Node> falseNodes = new ArrayList<Node>();
                    TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, false, false, exitNode, falseNodes);
                    List<Node> trueNodes = new ArrayList<Node>();
                    TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, false, true, exitNode, trueNodes);
                    if (falseNodes.size() == 1 && trueNodes.size() == 1) {
                        node.clearFromThisEdgeLabels();
                        node.getAttribute().setIsWhile(false);
                        node.getAttribute().setIsIf(false);
                        delNodeFromEdges(falseNodes, graph);
                        delNodeFromEdges(trueNodes, graph);
                    }
                }
            }
        }
    }

    public static void identifyFakeIfNodes(Graph graph) {
        for (Node node : new ArrayList<>(graph.getNodes())) {
            if (node.isIf()) {
                List<Node> nextNodes = node.getNextNodes();
                ASTNode astNode = getStmtForUnit(node.getAttribute(), true);
                if (astNode != null) {
                    continue;
                }
                if (isLambda(node)) {
                    logger.warn("a fake if is found: {}", node);
                    node.clearFromThisEdgeLabels();
                    node.getAttribute().setIsWhile(false);
                    node.getAttribute().setIsIf(false);
                    List<Node> nodes = new ArrayList<Node>();
                    getNeighborNodesInSameLine(node, nodes);
                    nodes.remove(node);
                    if (nodes.isEmpty()) {
                        graph.initFromToEdges();
                    } else {
                        delNodeFromEdges(nodes, graph);
                    }
                } else if (nextNodes.size() == 1) {
                    logger.warn("a fake if is found: {}", node);
                    List<Edge> fromEdges = node.getFromThisEdges();
                    graph.getEdges().removeAll(fromEdges);
                    Edge newEdge = new Edge(true, "");
                    newEdge.setStartNode(node);
                    newEdge.setEndNode(nextNodes.get(0));
                    graph.getEdges().add(newEdge);
                    node.getAttribute().setIsWhile(false);
                    node.getAttribute().setIsIf(false);
                    graph.initFromToEdges();
                } else if (nextNodes.size() == 2) {
                    logger.warn("a fake if is found: {}", node);
                    node.clearFromThisEdgeLabels();
                    node.getAttribute().setIsWhile(false);
                    node.getAttribute().setIsIf(false);
                    List<Node> nodes = new ArrayList<Node>();
                    getNeighborNodesInSameLine(node, nodes);
                    nodes.remove(node);
                    if (nodes.isEmpty()) {
                        graph.initFromToEdges();
                    } else {
                        delNodeFromEdges(nodes, graph);
                    }
                }
            }
        }
    }

    private static void getNeighborNodesInSameLine(Node node, List<Node> nodes) {
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
        List<Node> neighborNodes = node.getNeighborNodes();
        for (Node curNode : neighborNodes) {
            if (node.getAttribute().getLineNo() == curNode.getAttribute().getLineNo()) {
                if (!nodes.contains(curNode)) {
                    nodes.add(curNode);
                    getNeighborNodesInSameLine(curNode, nodes);
                }
            }
        }
    }

    private static boolean isLambda(Node node) {
        ExceptionUtil.myAssert(node.isIf());
        List<Node> nextNodes = node.getNextNodes();
        for (Node nextNode : nextNodes) {
            if (nextNode.getAttribute().getLineNo() != node.getAttribute().getLineNo()) {
                return false;
            }
        }
        return true;
    }

    public static void addThisStartNode(Graph graph) {
        Node entryNode = graph.getEntryNode();
        if (!entryNode.getAttribute().isStartThis()) {
            logger.warn("this graph has no startThis node, so we add one here.");
            MyUnit startUnit = new MyUnit(null, entryNode.getAttribute().getMethodSnippet(), true);
            Node startNode = new Node(true, Globals.gedGraphStartIndex++, startUnit);
            graph.add(0, startNode);
            graph.getNodes().add(0, startNode);
            Edge edge = new Edge(startNode, entryNode, "");
            graph.getEdges().add(edge);
            graph.initFromToEdges();
        }
    }

    public static void labelOneChildIf(Graph graph) {
        for (Node node : graph.getNodes()) {
            if (node.isIf() && node.getNextNodes().size() == 1 && node.getFromThisEdges().size() == 1) {
                logger.warn("one child if node is found: {}", node);
                ExceptionUtil.myAssert(node.getFromThisEdges().get(0).getLabel().length() == 0);
                node.getAttribute().setIsIf(false);
            }
        }
    }

    public static void removeOutOfMethodRangeNodes(Graph graph, Range range) {
        for (Node node : new ArrayList<>(graph.getNodes())) {
            int lineNo = node.getAttribute().getLineNo();
            if (lineNo != -1) {
                if (lineNo < range.getStartLineNo() || lineNo > range.getEndLineNo()) {
                    logger.warn("the node is out of method range (should be deleted): {}", node);
                    delNodeFromEdges(node, graph);
                }
            }
        }
    }

    public static void removeNullAstNodes(Graph graph) {
        for (Node node : new ArrayList<>(graph.getNodes())) {
            MyUnit myUnit = node.getAttribute();
            if (myUnit.getAstNode() == null) {
                if (myUnit.isLastReturn() || myUnit.isStartThisOrPara()) {
                } else {
                    logger.warn("delete null ast nodes even after addOmittedNodes(): {}", node);
                    delNodeFromEdges(node, graph);
                }
            }
        }
    }
}
