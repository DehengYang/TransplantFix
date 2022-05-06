package apr.aprlab.repair.adapt.ged.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jdt.core.dom.Statement;
import apr.aprlab.repair.adapt.entities.MappedStringClass;
import apr.aprlab.repair.adapt.entities.MyUnit;
import apr.aprlab.repair.adapt.ged.action.UpdAction;
import apr.aprlab.repair.adapt.ged.action.UpdAction.ActionType;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.general.ExceptionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Node extends GraphComponent implements java.io.Serializable {

    public static final Logger logger = LogManager.getLogger(Node.class);

    private static final long serialVersionUID = 1L;

    private LinkedList<Edge> edges;

    private boolean isDirected = false;

    private boolean isChangedInPlot;

    private MyUnit oldAttribute = null;

    private String oldComponentId;

    private List<Edge> fromThisEdges = new ArrayList<Edge>();

    private List<Edge> toThisEdges = new ArrayList<Edge>();

    public boolean isDirected() {
        return isDirected;
    }

    public void setDirected(boolean isDirected) {
        this.isDirected = isDirected;
    }

    public Node(boolean directed, int id, MyUnit myUnit) {
        this(directed, String.valueOf(id), myUnit);
    }

    public Node(boolean directed, String id, MyUnit myUnit) {
        super();
        super.setIsNode(true);
        if (directed) {
            this.isDirected = true;
        }
        this.edges = new LinkedList<Edge>();
        setComponentId(id);
        setAttribute(myUnit);
    }

    public Node(GraphComponent node, ActionLabel plotLabel) {
        this(true, node.getComponentId(), node.getAttribute());
        this.plotLabel = plotLabel;
    }

    public Node(Node node) {
        this(node, ActionLabel.NULL);
    }

    public LinkedList<Edge> getEdges() {
        return edges;
    }

    public void setEdges(LinkedList<Edge> edges) {
        this.edges = edges;
    }

    @Override
    public String toString() {
        String string = String.format("[%s] %s", getComponentId(), attribute);
        return string;
    }

    public String toGraphString() {
        String colorString = getColorString();
        String plotLabelString = getPlotLabelString();
        String string = String.format(" %s [ label=\"%s %s %s\" %s ]", getComponentId(), getComponentId(), attribute != null ? attribute.toGraphString() : "", plotLabelString, colorString);
        return string;
    }

    public boolean isChangedInPlot() {
        boolean is = plotLabel.length() > 0 || isChangedInPlot;
        return is;
    }

    public void setChangedInPlot(boolean changed) {
        this.isChangedInPlot = changed;
    }

    public void setOldAttribute(MyUnit oldAttribute) {
        this.oldAttribute = oldAttribute;
    }

    public MyUnit getOldAttribute() {
        ExceptionUtil.assertFalse(plotLabel == ActionLabel.INS);
        if (oldAttribute == null) {
            oldAttribute = attribute;
        }
        return oldAttribute;
    }

    public boolean hasAst() {
        boolean is = attribute.getAstNode() != null;
        return is;
    }

    public boolean isDelAction() {
        return getPlotLabel() == ActionLabel.DEL;
    }

    public boolean isInsAction() {
        return getPlotLabel() == ActionLabel.INS;
    }

    public boolean isMovAction() {
        return getPlotLabel() == ActionLabel.MOV;
    }

    public boolean isUpdAction() {
        return getPlotLabel() == ActionLabel.UPD;
    }

    public boolean isNullAction() {
        return getPlotLabel() == ActionLabel.NULL;
    }

    public int getStartPos() {
        if (attribute.getAstNode() == null) {
            logger.debug("");
        }
        ExceptionUtil.assertNotNull(attribute.getAstNode());
        return attribute.getAstNode().getStartPosition();
    }

    public int getEndPos() {
        ExceptionUtil.assertNotNull(attribute.getAstNode());
        return attribute.getAstNode().getStartPosition() + attribute.getAstNode().getLength();
    }

    public int getOldStartPos() {
        if (getOldAttribute().getAstNode() == null) {
            return getOldAttribute().getPosRange().getLeft();
        }
        ExceptionUtil.assertNotNull(getOldAttribute().getAstNode());
        return oldAttribute.getAstNode().getStartPosition();
    }

    public int getOldEndPos() {
        if (getOldAttribute().getAstNode() == null) {
            return getOldAttribute().getPosRange().getRight();
        }
        ExceptionUtil.assertNotNull(getOldAttribute().getAstNode());
        return oldAttribute.getAstNode().getStartPosition() + oldAttribute.getAstNode().getLength();
    }

    public int getStartZeroPos() {
        ExceptionUtil.assertNotNull(getOldAttribute().getAstNode());
        return attribute.getPosRange().getLeft();
    }

    public int getEndZeroPos() {
        ExceptionUtil.assertNotNull(getOldAttribute().getAstNode());
        return attribute.getPosRange().getRight();
    }

    public int getOldStartZeroPos() {
        return oldAttribute.getPosRange().getLeft();
    }

    public int getOldEndZeroPos() {
        ExceptionUtil.assertNotNull(getOldAttribute().getAstNode());
        return oldAttribute.getPosRange().getRight();
    }

    public boolean isIf() {
        return attribute.isIf();
    }

    public boolean isWhile() {
        return attribute.isWhile();
    }

    public Node getIfBlockStart() {
        return getIfBlockStart(true);
    }

    public Node getIfBlockStart(boolean forNew) {
        ExceptionUtil.assertTrue(isIf());
        Node ifBlockStart = null;
        if (forNew) {
            for (Edge edge : getFromThisEdges()) {
                if (edge.getLabel().equals("false") && !edge.isDelAction()) {
                    ifBlockStart = edge.getEndNode();
                    break;
                }
            }
        } else {
            for (Edge edge : getFromThisEdges()) {
                if (edge.getLabel().equals("false") && !edge.isInsAction()) {
                    ifBlockStart = edge.getEndNode();
                }
            }
        }
        return ifBlockStart;
    }

    public Node getElseBlockStart() {
        ExceptionUtil.assertTrue(isIf());
        Node elseBlockStart = null;
        for (Edge edge : getFromThisEdges()) {
            if (edge.getLabel().equals("true")) {
                elseBlockStart = edge.getEndNode();
            }
        }
        return elseBlockStart;
    }

    public List<Edge> getFromThisEdges() {
        return fromThisEdges;
    }

    public List<Edge> getToThisEdges() {
        return toThisEdges;
    }

    public void setFromToEdges(List<Edge> graphEdges) {
        fromThisEdges.clear();
        toThisEdges.clear();
        edges.clear();
        for (Edge edge : graphEdges) {
            if (edge.getStartNode() == this) {
                fromThisEdges.add(edge);
                edges.add(edge);
            } else if (edge.getEndNode() == this) {
                toThisEdges.add(edge);
                edges.add(edge);
            } else {
            }
        }
    }

    public boolean isStmt() {
        boolean is = attribute.isStmt();
        return is;
    }

    public boolean isAction() {
        return plotLabel.length() > 0;
    }

    public Node getExprNodes() {
        List<Node> exprNodes = new ArrayList<Node>();
        for (Edge edge : getToThisEdges()) {
            if (!edge.isDelAction()) {
                Node startNode = edge.getStartNode();
                if (!startNode.isStmt()) {
                    exprNodes.add(startNode);
                }
            }
        }
        ExceptionUtil.assertTrue(exprNodes.size() == 1);
        return exprNodes.get(0);
    }

    public boolean isUpdateIf() {
        boolean is = getPlotLabel() == ActionLabel.UPD && getOldAttribute().isIf();
        return is;
    }

    public Node getWhileBlockStart() {
        ExceptionUtil.assertTrue(isWhile());
        Node whileBlockStart = null;
        for (Edge edge : getFromThisEdges()) {
            if (edge.getLabel().equals("false")) {
                whileBlockStart = edge.getEndNode();
            }
        }
        return whileBlockStart;
    }

    public Node getWhileExitNode() {
        ExceptionUtil.assertTrue(isWhile());
        Node whileExitNode = null;
        for (Edge edge : getFromThisEdges()) {
            if (edge.getLabel().equals("true")) {
                whileExitNode = edge.getEndNode();
            }
        }
        return whileExitNode;
    }

    public String getMappedString(MethodSnippet srcMethodSnippet) {
        return getMappedString(0, new ArrayList<>(), new ArrayList<>(), srcMethodSnippet);
    }

    public String getMappedString(int curPatternCnt, List<MappedStringClass> usedMappedStringClasses, List<UpdAction> updActions, MethodSnippet srcMethodSnippet) {
        String mappedString;
        if (!isAction()) {
            mappedString = oldAttribute.getAstString();
            if (oldAttribute.isStmt()) {
                if (mappedString.endsWith("\n")) {
                    mappedString += "\n";
                }
            }
        } else {
            if (attribute.toString().contains("[99] unit: that = (org.jfree.chart.util.P")) {
                if (curPatternCnt == 0) {
                    logger.debug("");
                }
            }
            MappedStringClass mappedStringclass = attribute.getMappedStringClass(curPatternCnt, usedMappedStringClasses, srcMethodSnippet);
            if (mappedStringclass == null) {
                return null;
            }
            mappedString = mappedStringclass.getMappedString();
            if (mappedString != null) {
                if (attribute.isStmt()) {
                    if (!mappedString.endsWith("\n")) {
                        mappedString += "\n";
                    }
                }
                if (mappedStringclass.isExtraMappedString()) {
                    Node extraMappedNode = mappedStringclass.getExtraMappedNode();
                    UpdAction updAction = new UpdAction(extraMappedNode.getOldAttribute().getMethodSnippet(), extraMappedNode, ActionType.DEL);
                    updActions.add(updAction);
                }
            }
        }
        return mappedString;
    }

    public boolean isMatchedNode() {
        return isUpdAction() || isNullAction();
    }

    public Node getBranchStartNode(boolean forNew, boolean getTrueBranch) {
        ExceptionUtil.assertTrue(isIf());
        for (Edge edge : getFromThisEdges()) {
            if (forNew) {
                if (!edge.belongsNewGraph()) {
                    continue;
                }
            } else {
                if (!edge.belongsOldGraph()) {
                    continue;
                }
            }
            if (getTrueBranch) {
                if (edge.isTrueEdge()) {
                    return edge.getEndNode();
                }
            } else {
                if (edge.isFalseEdge()) {
                    return edge.getEndNode();
                }
            }
        }
        ExceptionUtil.raise();
        return null;
    }

    public boolean hasReversedEdge() {
        for (Edge edge : getFromThisEdges()) {
            if (edge.isReversedUpdated()) {
                return true;
            }
        }
        return false;
    }

    public void reverseEdgeLabelsForOld() {
        for (Edge edge : getFromThisEdges()) {
            if (edge.belongsOldGraph()) {
                if (edge.isDelAction() || edge.isNullAction()) {
                    edge.reverseLabel();
                } else if (edge.isUpdAction()) {
                    edge.setOldLabel(edge.getLabel());
                    edge.setPlotLabel(ActionLabel.NULL);
                }
            }
        }
    }

    public void reverseEdgeLabelsForNew() {
        for (Edge edge : getFromThisEdges()) {
            if (edge.belongsNewGraph()) {
                if (edge.isInsAction() || edge.isNullAction()) {
                    edge.reverseLabel();
                } else if (edge.isUpdAction()) {
                    edge.setLabel(edge.getOldLabel());
                    edge.setPlotLabel(ActionLabel.NULL);
                }
            }
        }
    }

    public int getOldStartPosForAction() {
        int pos = -1;
        if (getOldAttribute().isIf()) {
            pos = getOldStartZeroPos();
        } else {
            pos = getOldStartPos();
        }
        return pos;
    }

    public int getOldEndPosForAction() {
        int pos = -1;
        if (isIf()) {
            pos = getOldEndZeroPos();
        } else {
            pos = getOldEndPos();
        }
        return pos;
    }

    public boolean parentIsStmtForNew() {
        boolean is = attribute.getAstNode().getParent() instanceof Statement;
        return is;
    }

    public boolean parentIsStmtForOld() {
        boolean is = oldAttribute.getAstNode().getParent() instanceof Statement;
        return is;
    }

    public int getComponentIdInt() {
        return getComponentIdInt(false);
    }

    public int getComponentIdInt(boolean forNew) {
        if (forNew) {
            return Integer.parseInt(getComponentId());
        } else {
            return Integer.parseInt(getOldComponentId());
        }
    }

    public String getOldComponentId() {
        ExceptionUtil.assertFalse(plotLabel == ActionLabel.INS);
        if (oldComponentId == null) {
            oldComponentId = getComponentId();
        }
        return oldComponentId;
    }

    public void setOldComponentId(String oldComponentId) {
        this.oldComponentId = oldComponentId;
    }

    public List<Node> getNextNodes() {
        return getNextNodes(false);
    }

    public List<Node> getNextNodes(boolean forNew) {
        List<Node> nextNodes = new ArrayList<Node>();
        for (Edge edge : getFromThisEdges()) {
            if (forNew) {
                if (!edge.isDelAction()) {
                    Node endNode = edge.getEndNode();
                    if (!nextNodes.contains(endNode)) {
                        nextNodes.add(endNode);
                    }
                }
            } else {
                if (!edge.isInsAction()) {
                    Node endNode = edge.getEndNode();
                    if (!nextNodes.contains(endNode)) {
                        nextNodes.add(endNode);
                    }
                }
            }
        }
        return nextNodes;
    }

    public List<Node> getPrevNodes() {
        return getPrevNodes(false);
    }

    public List<Node> getPrevNodes(boolean forNew) {
        List<Node> prevNodes = new ArrayList<Node>();
        for (Edge edge : getToThisEdges()) {
            if (forNew) {
                if (!edge.isDelAction()) {
                    prevNodes.add(edge.getStartNode());
                }
            } else {
                if (!edge.isInsAction()) {
                    prevNodes.add(edge.getStartNode());
                }
            }
        }
        return prevNodes;
    }

    public Edge getPrevIfEdge(Node prevNode) {
        for (Edge edge : getToThisEdges()) {
            if (edge.getStartNode() == prevNode) {
                return edge;
            }
        }
        ExceptionUtil.raise();
        return null;
    }

    public boolean isSwitch() {
        return getAttribute().isSwitch();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        return this == other;
    }

    public String getEdgeLabel(Node exitNode) {
        for (Edge edge : fromThisEdges) {
            if (edge.getEndNode() == exitNode) {
                return edge.getLabel();
            }
        }
        ExceptionUtil.raise();
        return null;
    }

    public void setEdgeLabel(Node exitNode, String toExitLabel) {
        ExceptionUtil.myAssert(fromThisEdges.size() == 2);
        for (Edge edge : fromThisEdges) {
            if (edge.getEndNode() == exitNode) {
                if (!edge.getLabel().equals(toExitLabel)) {
                    logger.warn("edge ({}) label should be: {}", edge, toExitLabel);
                    for (Edge curEdge : fromThisEdges) {
                        curEdge.reverseOldLabel();
                    }
                    break;
                }
            }
        }
    }

    public MyUnit getAttribute(boolean forNew) {
        if (forNew) {
            return attribute;
        } else {
            return getOldAttribute();
        }
    }

    public void clearFromThisEdgeLabels() {
        ExceptionUtil.myAssert(isIf());
        for (Edge edge : fromThisEdges) {
            edge.setLabel("");
        }
    }

    public List<Node> getNeighborNodes() {
        List<Node> neighborNodes = new ArrayList<Node>();
        neighborNodes.addAll(getPrevNodes(false));
        neighborNodes.addAll(getNextNodes(false));
        return neighborNodes;
    }

    public List<Node> getUniqNextNodes() {
        return new ArrayList<Node>(new HashSet<>(getNextNodes()));
    }

    public boolean hasSameLineNo(Node otherNode) {
        boolean is = this.getAttribute().getLineNo() == otherNode.getAttribute().getLineNo();
        return is;
    }
}
