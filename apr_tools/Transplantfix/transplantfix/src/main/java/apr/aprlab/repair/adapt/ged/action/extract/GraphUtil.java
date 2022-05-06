package apr.aprlab.repair.adapt.ged.action.extract;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import apr.aprlab.repair.adapt.ged.Constants;
import apr.aprlab.repair.adapt.ged.util.Edge;
import apr.aprlab.repair.adapt.ged.util.EditPath;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.GraphComponent;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.adapt.ged.util.GraphComponent.ActionLabel;
import apr.aprlab.utils.general.CmdUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GraphUtil {

    public static final Logger logger = LogManager.getLogger(GraphUtil.class);

    public static void plot(Graph graph, String fileDir, String fileName) {
        StringBuilder sb = new StringBuilder();
        ExceptionUtil.assertFileExists(fileDir);
        String filePath = Paths.get(fileDir, fileName).toString();
        sb.append("digraph \"graph\" {\n");
        sb.append("    label=\"graph\";\n");
        sb.append("node [shape=box];\n");
        sb.append(graph.getDotLinesString());
        sb.append("}");
        FileUtil.writeToFile(filePath, sb.toString(), false);
        String savePngPath = Paths.get(fileDir, fileName + ".png").toString();
        String cmd = String.format("dot -Tpng %s -o %s", filePath, savePngPath);
        CmdUtil.runCmd(cmd);
    }

    @SuppressWarnings("unchecked")
    public static Graph getActionGraph(Graph graph, Graph donorGraph, EditPath bestEditpath) {
        Hashtable<GraphComponent, GraphComponent> hashtable = bestEditpath.getDistortions();
        List<Node> nodes = graph.getNodes();
        List<Edge> edges = graph.getEdges();
        for (GraphComponent key : hashtable.keySet()) {
            GraphComponent value = hashtable.get(key);
            double cost = Constants.costFunction.getCosts(key, value);
            if (key.isNode() == true || value.isNode() == true) {
                if (key.isNullEps()) {
                    nodes.add(new Node(value, ActionLabel.INS));
                } else if (value.isNullEps()) {
                    for (Node node : nodes) {
                        if (node.getComponentId().equals(key.getComponentId())) {
                            node.setPlotLabel(ActionLabel.DEL);
                            break;
                        }
                    }
                } else {
                    for (Node node : nodes) {
                        if (node.getComponentId().equals(key.getComponentId()) && !node.isChangedInPlot()) {
                            if (cost != 0) {
                                node.setPlotLabel(ActionLabel.UPD);
                            } else {
                            }
                            node.setOldAttribute(key.getAttribute());
                            node.setOldComponentId(node.getComponentId());
                            node.setComponentId(value.getComponentId());
                            node.setAttribute(value.getAttribute());
                            node.setChangedInPlot(true);
                            break;
                        }
                    }
                }
            }
        }
        List<Edge> newEdges = new ArrayList<Edge>();
        for (GraphComponent key : hashtable.keySet()) {
            GraphComponent value = hashtable.get(key);
            double cost = Constants.costFunction.getCosts(key, value);
            if (key.getComponentId().equals("2==4")) {
                logger.debug("");
            }
            if (key.getComponentId().equals("15==13")) {
                logger.debug("");
            }
            if (!key.isNode() && !value.isNode()) {
                if (key.isNullEps()) {
                    edges.add(new Edge((Edge) value, ActionLabel.INS));
                } else if (value.isNullEps()) {
                    Edge keyEdge = (Edge) key;
                    for (Edge edge : edges) {
                        if (edge.getStartNode().getComponentId().equals(keyEdge.getStartNode().getComponentId()) && edge.getEndNode().getComponentId().equals(keyEdge.getEndNode().getComponentId())) {
                            edge.setPlotLabel(ActionLabel.DEL);
                            break;
                        }
                    }
                } else {
                    Edge keyEdge = (Edge) key;
                    Edge valueEdge = (Edge) value;
                    for (Edge edge : edges) {
                        if (edge.getStartNode().getComponentId().equals(keyEdge.getStartNode().getComponentId()) && edge.getEndNode().getComponentId().equals(keyEdge.getEndNode().getComponentId())) {
                            ExceptionUtil.assertTrue(edge.getLabel().equals(keyEdge.getLabel()));
                            if (cost != 0) {
                                if (keyEdge.getStartNode().getComponentId().equals(valueEdge.getEndNode().getComponentId()) && keyEdge.getEndNode().getComponentId().equals(valueEdge.getStartNode().getComponentId())) {
                                    edge.setPlotLabel(ActionLabel.DEL);
                                    newEdges.add(new Edge(valueEdge, ActionLabel.INS));
                                    continue;
                                } else {
                                    edge.setPlotLabel(ActionLabel.UPD);
                                    edge.setOldLabel(keyEdge.getLabel());
                                }
                            }
                            if (cost == 0) {
                                if (edge.getStartNode().getComponentIdInt(true) == valueEdge.getEndNode().getComponentIdInt(true) && edge.getEndNode().getComponentIdInt(true) == valueEdge.getStartNode().getComponentIdInt(true)) {
                                    edge.setPlotLabel(ActionLabel.DEL);
                                    newEdges.add(new Edge(valueEdge, ActionLabel.INS));
                                    continue;
                                }
                            }
                            edge.setStartNode(valueEdge.getStartNode());
                            edge.setEndNode(valueEdge.getEndNode());
                            edge.setLabel(valueEdge.getLabel());
                            break;
                        }
                    }
                }
            }
        }
        edges.addAll(newEdges);
        Map<String, Node> idNodeMap = new HashMap<>();
        for (Node node : nodes) {
            node.getEdges().clear();
            if (idNodeMap.containsKey(node.getComponentId())) {
                logger.debug("");
            }
            ExceptionUtil.assertFalse(idNodeMap.containsKey(node.getComponentId()));
            idNodeMap.put(node.getComponentId(), node);
        }
        for (Edge edge : edges) {
            String startId = edge.getStartNode().getComponentId();
            String endId = edge.getEndNode().getComponentId();
            Node startNode = idNodeMap.get(startId);
            Node endNode = idNodeMap.get(endId);
            String before = edge.toString();
            edge.setStartNode(startNode);
            edge.setEndNode(endNode);
            String after = edge.toString();
            if (!before.equals(after)) {
                logger.debug("");
            }
            startNode.getEdges().add(edge);
            endNode.getEdges().add(edge);
        }
        graph.initFromToEdges();
        return graph;
    }

    public static Graph cloneGraph(Graph graph) {
        Graph cloneGraph = new Graph();
        Map<String, Node> idNodeMap = new HashMap<>();
        for (Node node : graph.getNodes()) {
            Node clonedNode = new Node(node);
            cloneGraph.add(clonedNode);
            idNodeMap.put(clonedNode.getComponentId(), clonedNode);
        }
        for (Edge edge : graph.getEdges()) {
            Edge clonedEdge = new Edge(edge);
            String startId = clonedEdge.getStartNode().getComponentId();
            String endId = clonedEdge.getEndNode().getComponentId();
            Node startNode = idNodeMap.get(startId);
            Node endNode = idNodeMap.get(endId);
            clonedEdge.setStartNode(startNode);
            clonedEdge.setEndNode(endNode);
            clonedEdge.setComponentId(startId + "==" + endId);
            if (startNode == null || startNode.getEdges() == null || endNode == null || clonedEdge == null || endNode.getEdges() == null) {
                logger.debug("");
            }
            startNode.getEdges().add(clonedEdge);
            endNode.getEdges().add(clonedEdge);
            cloneGraph.getEdges().add(clonedEdge);
        }
        cloneGraph.initFromToEdges();
        return cloneGraph;
    }
}
