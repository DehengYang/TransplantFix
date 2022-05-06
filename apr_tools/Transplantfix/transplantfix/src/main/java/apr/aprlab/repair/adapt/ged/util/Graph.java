package apr.aprlab.repair.adapt.ged.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.general.CollectionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Graph extends LinkedList<GraphComponent> {

    public static final Logger logger = LogManager.getLogger(Graph.class);

    private static final long serialVersionUID = 1L;

    private String classId;

    private String id;

    private String edgeId;

    private String edgeMode;

    private LinkedList<Edge> edges;

    private LinkedList<Node> nodes;

    public Graph() {
        super();
        this.edges = new LinkedList<Edge>();
    }

    public String getEdgeId() {
        return edgeId;
    }

    public String getEdgeMode() {
        return edgeMode;
    }

    public String getId() {
        return id;
    }

    public LinkedList<Edge> getEdges() {
        return edges;
    }

    public void setEdges(LinkedList<Edge> edges) {
        this.edges = edges;
    }

    public void setEdgeId(String edgeids) {
        this.edgeId = edgeids;
    }

    public void setEdgeMode(String edgemode) {
        this.edgeMode = edgemode;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("nodes:\n");
        for (Node node : getNodes()) {
            sb.append(node.toGraphString() + "\n");
        }
        sb.append("edges:\n");
        for (Edge edge : edges) {
            sb.append(edge.toGraphString() + "\n");
        }
        return sb.toString();
    }

    public List<Node> getNodes() {
        if (nodes == null) {
            setNodes();
        }
        return nodes;
    }

    public List<Node> getNodes(boolean forNew) {
        List<Node> newOrOldNodes = new ArrayList<Node>();
        for (Node node : nodes) {
            if (forNew) {
                if (!node.isDelAction()) {
                    newOrOldNodes.add(node);
                }
            } else {
                if (!node.isInsAction()) {
                    newOrOldNodes.add(node);
                }
            }
        }
        return newOrOldNodes;
    }

    public String getDotLinesString() {
        StringBuilder sb = new StringBuilder();
        for (Node node : getNodes()) {
            sb.append(node.toGraphString() + "\n");
        }
        for (Edge edge : edges) {
            sb.append(edge.toGraphString() + "\n");
        }
        return sb.toString();
    }

    public void setNodes() {
        LinkedList<Node> nodes = new LinkedList<Node>();
        Iterator<GraphComponent> iter = this.iterator();
        while (iter.hasNext()) {
            Node n = (Node) iter.next();
            nodes.add(n);
        }
        this.nodes = nodes;
    }

    public int getSize() {
        return getNodes().size();
    }

    public Node getEntryNode() {
        return getNodes().get(0);
    }

    public void initFromToEdges() {
        edges = CollectionUtil.removeDuplicates(getEdges());
        for (Node node : getNodes()) {
            node.setFromToEdges(getEdges());
        }
    }

    public void updateEdges() {
        List<Edge> allEdges = new ArrayList<>(edges);
        for (Edge edge : allEdges) {
            if (!nodes.contains(edge.getStartNode()) && !nodes.contains(edge.getEndNode())) {
                logger.debug("remove the edge: {}", edge);
                edges.remove(edge);
            }
        }
    }

    public void setMappedStrings(MethodSnippet methodSnippet) {
        for (Node node : nodes) {
            node.getAttribute().setMappedStrings(methodSnippet);
        }
    }
}
