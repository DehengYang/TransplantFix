package apr.aprlab.repair.adapt.ged.util;

public class Edge extends GraphComponent implements java.io.Serializable {

    private static final long serialVersionUID = -3966371565937416914L;

    private Node startNode;

    private Node endNode;

    private String label = "";

    boolean isInverted;

    public boolean isInverted() {
        return isInverted;
    }

    public void setInverted(boolean isInverted) {
        this.isInverted = isInverted;
    }

    public boolean isIDirected() {
        return isIDirected;
    }

    public void setIDirected(boolean isIDirected) {
        this.isIDirected = isIDirected;
    }

    boolean isIDirected;

    private boolean isDirected;

    private String oldLabel = null;

    public boolean isDirected() {
        return isDirected;
    }

    public void setDirected(boolean isDirected) {
        this.isDirected = isDirected;
    }

    public Edge(boolean isDirected, String label) {
        super();
        setIDirected(true);
        super.setIsNode(false);
        if (isDirected) {
            this.isDirected = true;
        }
        this.label = label;
        this.setComponentId("-1");
    }

    public Edge(Edge edge, ActionLabel plotLabel) {
        this(edge.isDirected, edge.getLabel());
        setStartNode(edge.getStartNode());
        setEndNode(edge.getEndNode());
        this.plotLabel = plotLabel;
        this.oldLabel = edge.oldLabel;
    }

    public Edge(Edge edge) {
        this(edge, edge.getPlotLabel());
    }

    public Edge(Node startNode, Node endNode, String label) {
        this(true, label);
        setStartNode(startNode);
        setEndNode(endNode);
        this.plotLabel = ActionLabel.NULL;
        this.oldLabel = label;
    }

    public Node getEndNode() {
        return endNode;
    }

    public void setEndNode(Node endNode) {
        this.endNode = endNode;
    }

    public Node getStartNode() {
        return startNode;
    }

    public void setStartNode(Node startNode) {
        this.startNode = startNode;
    }

    public Node getOtherEnd(Node n) {
        if (n.equals(this.startNode)) {
            return this.endNode;
        }
        return this.startNode;
    }

    @Override
    public String toString() {
        String str = String.format(" [%s -> %s] | %s ==> %s (label: %s)", startNode.getComponentId(), endNode.getComponentId(), startNode.toString(), endNode.toString(), label);
        return str;
    }

    public String toGraphString() {
        String colorString = getColorString();
        String plotLabelString = getPlotLabelString();
        String str = String.format(" %s -> %s [ label=\"%s %s\" %s ]", startNode.getComponentId(), endNode.getComponentId(), label, plotLabelString, colorString);
        return str;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setOldLabel(String oldLabel) {
        this.oldLabel = oldLabel;
    }

    public String getOldLabel() {
        if (oldLabel == null) {
            oldLabel = this.label;
        }
        return oldLabel;
    }

    public boolean isDelAction() {
        return plotLabel == ActionLabel.DEL;
    }

    public boolean isUpdAction() {
        return plotLabel == ActionLabel.UPD;
    }

    public boolean isInsAction() {
        return plotLabel == ActionLabel.INS;
    }

    public boolean isNullAction() {
        return plotLabel == ActionLabel.NULL;
    }

    public boolean isTrueEdge() {
        return getLabel().equals("true");
    }

    public boolean isFalseEdge() {
        return getLabel().equals("false");
    }

    public boolean isOldTrueEdge() {
        return getOldLabel().equals("true");
    }

    public boolean isOldFalseEdge() {
        return getOldLabel().equals("false");
    }

    public boolean belongsNewGraph() {
        boolean is = !isDelAction();
        return is;
    }

    public boolean belongsOldGraph() {
        boolean is = !isInsAction();
        return is;
    }

    public boolean isReversedUpdated() {
        if (isUpdAction()) {
            if (!oldLabel.equals(label)) {
                return true;
            }
        }
        return false;
    }

    public void reverseLabel() {
        if (label.equals("true")) {
            label = "false";
            oldLabel = "false";
        } else {
            label = "true";
            oldLabel = "true";
        }
    }

    public void reverseOldLabel() {
        if (label.equals("true")) {
            label = "false";
        } else {
            label = "true";
        }
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
        Edge other = (Edge) obj;
        return this.getStartNode() == other.getStartNode() && this.getEndNode() == other.getEndNode() && this.getLabel().equals(other.getLabel());
    }
}
