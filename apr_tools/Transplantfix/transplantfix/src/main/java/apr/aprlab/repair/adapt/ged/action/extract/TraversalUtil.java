package apr.aprlab.repair.adapt.ged.action.extract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import apr.aprlab.repair.adapt.entities.MyUnitUtil;
import apr.aprlab.repair.adapt.ged.util.Edge;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.adapt.ged.util.GraphComponent.ActionLabel;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.PrintUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EnhancedForStatement;

public class TraversalUtil {

    public static final Logger logger = LogManager.getLogger(TraversalUtil.class);

    public static List<Node> realDepthFirstTraversalForNewOrOld(Node node, boolean forNew) {
        List<Node> visited = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            Node curNode = stack.pop();
            if (!visited.contains(curNode)) {
                visited.add(curNode);
            } else {
                continue;
            }
            for (Edge edge : curNode.getToThisEdges()) {
                if (forNew) {
                    if (!edge.isDelAction()) {
                        stack.push(edge.getStartNode());
                    }
                } else {
                    if (!edge.isInsAction()) {
                        stack.push(edge.getStartNode());
                    }
                }
            }
            for (Edge edge : curNode.getFromThisEdges()) {
                if (forNew) {
                    if (!edge.isDelAction()) {
                        stack.push(edge.getEndNode());
                    }
                } else {
                    if (!edge.isInsAction()) {
                        stack.push(edge.getEndNode());
                    }
                }
            }
        }
        return visited;
    }

    public static List<Node> depthFirstTraversalForNewOrOld(Graph graph, boolean forNew) {
        List<Node> nodes = depthFirstTraversalForNewOrOld(graph.getEntryNode(), forNew);
        if (nodes.size() != new HashSet<>(nodes).size()) {
            List<Node> nodeSet = new ArrayList<Node>(new HashSet<Node>(nodes));
            for (Node node : nodeSet) {
                nodes.remove(node);
            }
            logger.debug(PrintUtil.listToString(nodes));
        }
        if (graph.getNodes(forNew).size() != nodes.size()) {
            if (graph.getNodes(forNew).size() > nodes.size()) {
                logger.debug(PrintUtil.listToString(CollectionUtil.getUniqueInSrc(graph.getNodes(forNew), nodes)));
            } else {
                logger.debug(PrintUtil.listToString(CollectionUtil.getUniqueInSrc(nodes, graph.getNodes(forNew))));
            }
        }
        return nodes;
    }

    private static List<Node> depthFirstTraversalForNewOrOld(Node node, boolean forNew) {
        List<Node> visisted = new ArrayList<Node>();
        depthFirstTraversalForNewOrOld(node, forNew, null, visisted);
        return visisted;
    }

    public static List<Node> depthFirstTraversalForNewOrOld(Node node, boolean forNew, Node exitNode) {
        List<Node> visisted = new ArrayList<Node>();
        depthFirstTraversalForNewOrOld(node, forNew, exitNode, visisted);
        return visisted;
    }

    public static void traversalPrint(Node node, boolean forNew) {
        List<Node> nodes = depthFirstTraversalForNewOrOld(node, forNew);
        logger.debug("line-order based dfs: \n");
        for (Node curNode : nodes) {
            logger.debug("node: {}", curNode.toString());
        }
    }

    public static List<Node> depthFirstTraversalForNewOrOld(Node node, boolean forNew, Node exitNode2, List<Node> visited) {
        Stack<Node> stack = new Stack<>();
        stack.push(node);
        Node exitNode = null;
        while (!stack.isEmpty()) {
            Node curNode = stack.pop();
            if (curNode == exitNode2) {
                break;
            }
            if (!visited.contains(curNode)) {
                visited.add(curNode);
                if (curNode.isIf()) {
                    if (curNode.getAttribute().getLineNo() == 296) {
                        logger.debug("");
                    }
                    exitNode = getExitNode(curNode, forNew);
                    if (exitNode != null) {
                        depthFirstTraversalForNewOrOldForBranchStopAtNode(curNode, forNew, false, exitNode, visited);
                        depthFirstTraversalForNewOrOldForBranchStopAtNode(curNode, forNew, true, exitNode, visited);
                        stack.push(exitNode);
                    } else {
                        depthFirstTraversalForNewOrOldForBranchStopAtNode(curNode, forNew, false, exitNode2, visited);
                        depthFirstTraversalForNewOrOldForBranchStopAtNode(curNode, forNew, true, exitNode2, visited);
                    }
                } else if (curNode.isSwitch()) {
                    exitNode = ActionsExtract.getExitNodeForSwitch(curNode, forNew);
                    List<Node> nextNodes = curNode.getNextNodes(forNew);
                    if (forNew) {
                        nextNodes.sort(ComparatorUtil.lineNoCompForNew);
                    } else {
                        nextNodes.sort(ComparatorUtil.lineNoCompForOld);
                    }
                    for (Node nextNode : nextNodes) {
                        depthFirstTraversalForNewOrOldStopAtNode(nextNode, forNew, exitNode, visited);
                    }
                    if (exitNode != null) {
                        stack.push(exitNode);
                    }
                } else {
                    for (Edge edge : curNode.getFromThisEdges()) {
                        Node nextNode = edge.getEndNode();
                        if (forNew) {
                            if (!edge.isDelAction()) {
                                if (!visited.contains(nextNode)) {
                                    stack.push(edge.getEndNode());
                                }
                            }
                        } else {
                            if (!edge.isInsAction()) {
                                if (!visited.contains(nextNode)) {
                                    stack.push(edge.getEndNode());
                                }
                            }
                        }
                    }
                }
            }
        }
        return visited;
    }

    private static List<Node> depthFirstTraversalForNewOrOldStopAtNode(Node nextNode, boolean forNew, Node exitNode, List<Node> visited) {
        List<Node> nodes = depthFirstTraversalForNewOrOld(nextNode, forNew, exitNode, visited);
        return nodes;
    }

    private static Node getNextNodeForNewOrOldForTrueOrFalseBranch(Edge edge, boolean forNew, boolean forTrueBranch) {
        if (forTrueBranch) {
            if (forNew) {
                if (!edge.isDelAction() && edge.isTrueEdge()) {
                    return edge.getEndNode();
                }
            } else {
                if (!edge.isInsAction() && edge.isOldTrueEdge()) {
                    return edge.getEndNode();
                }
            }
        } else {
            if (forNew) {
                if (!edge.isDelAction() && edge.isFalseEdge()) {
                    return edge.getEndNode();
                }
            } else {
                if (!edge.isInsAction() && edge.isOldFalseEdge()) {
                    return edge.getEndNode();
                }
            }
        }
        return null;
    }

    public static List<Node> depthFirstTraversalForNewOrOldForBranchStopAtNode(Node ifCondNode, boolean forNew, boolean forTrueBranch, Node exitNode) {
        List<Node> visited = new ArrayList<Node>();
        depthFirstTraversalForNewOrOldForBranchStopAtNode(ifCondNode, forNew, forTrueBranch, exitNode, visited);
        return visited;
    }

    public static void depthFirstTraversalForNewOrOldForBranchStopAtNode(Node ifCondNode, boolean forNew, boolean forTrueBranch, Node exitNode, List<Node> visited) {
        Stack<Node> stack = new Stack<>();
        stack.push(ifCondNode);
        ExceptionUtil.assertTrue(ifCondNode.isIf());
        while (!stack.isEmpty()) {
            Node curNode = stack.pop();
            if (exitNode == curNode) {
                break;
            }
            if (curNode == ifCondNode) {
                for (Edge edge : curNode.getFromThisEdges()) {
                    Node nextNode = getNextNodeForNewOrOldForTrueOrFalseBranch(edge, forNew, forTrueBranch);
                    if (nextNode != null && nextNode != curNode) {
                        stack.push(nextNode);
                    }
                }
            } else {
                if (!visited.contains(curNode)) {
                    depthFirstTraversalForNewOrOld(curNode, forNew, exitNode, visited);
                }
            }
        }
    }

    public static List<Node> depthFirstTraversalForNewForBranch(Node ifNode, boolean forTrueBranch) {
        List<Node> visited = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        stack.push(ifNode);
        int cnt = 0;
        ExceptionUtil.assertTrue(ifNode.isIf());
        while (!stack.isEmpty()) {
            Node gc = stack.pop();
            if (!visited.contains(gc)) {
                visited.add(gc);
                for (Edge edge : gc.getFromThisEdges()) {
                    if (gc == ifNode) {
                        if (forTrueBranch) {
                            if (!edge.isDelAction() && edge.isFalseEdge()) {
                                cnt++;
                                continue;
                            }
                        } else {
                            if (!edge.isDelAction() && edge.isTrueEdge()) {
                                cnt++;
                                continue;
                            }
                        }
                    }
                    if (!edge.isDelAction()) {
                        stack.push(edge.getEndNode());
                    }
                }
            }
        }
        if (cnt != 1) {
            logger.debug("");
        }
        ExceptionUtil.assertTrue(cnt == 1);
        visited.remove(ifNode);
        return visited;
    }

    public static List<Node> depthFirstTraversalForNewOrOldExcludeNodes(Node node, List<Node> excludeNodes, boolean forNew) {
        List<Node> visited = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            Node gc = stack.pop();
            if (excludeNodes.contains(gc)) {
                continue;
            }
            if (!visited.contains(gc)) {
                visited.add(gc);
                for (Edge edge : gc.getFromThisEdges()) {
                    if (forNew) {
                        if (!edge.isDelAction()) {
                            stack.push(edge.getEndNode());
                        }
                    } else {
                        if (!edge.isInsAction()) {
                            stack.push(edge.getEndNode());
                        }
                    }
                }
            }
        }
        return visited;
    }

    public static boolean hasCircle(Node ifNode) {
        if (ifNode.isIf()) {
            List<Node> visited = new ArrayList<>();
            Stack<Node> stack = new Stack<>();
            stack.push(ifNode);
            int ifNodeHits = 0;
            while (!stack.isEmpty()) {
                Node gc = stack.pop();
                if (gc == ifNode) {
                    ifNodeHits++;
                }
                if (!visited.contains(gc)) {
                    visited.add(gc);
                    for (Edge edge : gc.getFromThisEdges()) {
                        stack.push(edge.getEndNode());
                    }
                }
            }
            if (ifNodeHits > 1) {
                List<Node> trueNodes = TraversalUtil.depthFirstTraversalForNewForBranch(ifNode, true);
                List<Node> falseNodes = TraversalUtil.depthFirstTraversalForNewForBranch(ifNode, false);
                if (CollectionUtil.getIntersection(trueNodes, falseNodes).isEmpty()) {
                    ASTNode stmt = MyUnitUtil.getStmtForUnit(ifNode.getAttribute(), true);
                    if (ASTUtil.isLoopASTNode(stmt)) {
                        if (stmt instanceof EnhancedForStatement) {
                            ifNode.getAttribute().setIsEnhancedFor(true);
                            logger.info("ifNode is a while node (enhanced for): {}", ifNode);
                        } else {
                            logger.info("ifNode is a while node: {}", ifNode);
                        }
                        return true;
                    } else {
                        ExceptionUtil.myAssert(ASTUtil.isIfASTNode(stmt));
                    }
                } else {
                    ASTNode stmt = MyUnitUtil.getStmtForUnit(ifNode.getAttribute(), true);
                    if (ASTUtil.isLoopASTNode(stmt)) {
                        return true;
                    } else {
                        if (stmt == null) {
                            logger.warn("ifNode has no if or loop ast: {}", ifNode);
                        } else {
                            ExceptionUtil.myAssert(ASTUtil.isIfASTNode(stmt));
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Node getExitNode(Node node, boolean forNew) {
        ExceptionUtil.assertTrue(node.isIf());
        if (node.getAttribute().getLineNo() == 1334) {
            logger.debug("");
        }
        List<List<Node>> twoBranches = new ArrayList<List<Node>>();
        for (Edge edge : node.getFromThisEdges()) {
            if (forNew) {
                if (edge.getPlotLabel() != ActionLabel.DEL) {
                    twoBranches.add(depthFirstTraversalForNewOrOldExcludeNodes(edge.getEndNode(), new ArrayList<Node>(Arrays.asList(node)), true));
                }
            } else {
                if (edge.getPlotLabel() != ActionLabel.INS) {
                    twoBranches.add(depthFirstTraversalForNewOrOldExcludeNodes(edge.getEndNode(), new ArrayList<Node>(Arrays.asList(node)), false));
                }
            }
        }
        if (twoBranches.size() != 2) {
            logger.error("getExitNode twoBranches size: {}", twoBranches.size());
            Globals.hasTraversalIssues = true;
        } else {
            for (Node branchNode : twoBranches.get(0)) {
                if (twoBranches.get(1).contains(branchNode)) {
                    return branchNode;
                }
            }
        }
        return null;
    }

    public static Node getMutliIfExprIntersectionNode(List<Node> ifNodes) {
        ExceptionUtil.myAssert(ifNodes.size() > 1);
        List<Node> inters = new ArrayList<Node>(ifNodes.get(0).getNextNodes());
        for (int i = 1; i < ifNodes.size(); i++) {
            inters = CollectionUtil.getIntersection(inters, ifNodes.get(i).getNextNodes());
        }
        ExceptionUtil.myAssert(inters.size() == 1);
        return inters.get(0);
    }

    public static void excludeNodeAndItsAfter(Node node, List<Node> branchNodes) {
        int index = branchNodes.indexOf(node);
        if (index >= 0) {
            int size = branchNodes.size();
            for (int i = index; i < size; i++) {
                branchNodes.remove(index);
            }
        }
    }
}
