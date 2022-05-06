package apr.aprlab.utils.graph;

import soot.Body;
import soot.SootMethod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public abstract class ProgramGraph {

    private final ArrayList<ProgramNode> nodes;

    private final ArrayList<ProgramEdge> edges;

    private final Body currentBody;

    private final MethodDeclaration methodDeclaration;

    public ProgramGraph(SootMethod mtd) {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        currentBody = mtd.getActiveBody();
        methodDeclaration = null;
    }

    public void removeNodes(Collection<ProgramNode> rms) {
        nodes.removeAll(rms);
    }

    public void removeEdges(Collection<?> rms) {
        edges.removeAll(rms);
    }

    public Body getBody() {
        return currentBody;
    }

    public void addNode(ProgramNode node) {
        if (!nodes.contains(node))
            nodes.add(node);
    }

    public int numOfNodes() {
        return nodes.size();
    }

    public ArrayList<ProgramNode> getNodes() {
        return nodes;
    }

    public ArrayList<ProgramEdge> getEdges() {
        return edges;
    }

    public void addEdge(ProgramEdge edge) {
        if (!edges.contains(edge))
            edges.add(edge);
    }

    public int numOfEdges() {
        return edges.size();
    }

    public ProgramNode getNode(int index) {
        if (index >= nodes.size() || index < 0)
            return null;
        return nodes.get(index);
    }

    public Set<ProgramNode> getSrcs(ProgramNode node) {
        Set<ProgramNode> srcs = new HashSet<>();
        for (ProgramEdge edge : edges) {
            if (edge.getTgt().equals(node))
                srcs.add(edge.getSrc());
        }
        return srcs;
    }

    public Set<ProgramNode> getTgts(ProgramNode node) {
        Set<ProgramNode> tgts = new HashSet<>();
        for (ProgramEdge edge : edges) {
            if (edge.getSrc().equals(node))
                tgts.add(edge.getTgt());
        }
        return tgts;
    }

    public void removeNodeAndEdge(ProgramNode node) {
        nodes.remove(node);
        Set<ProgramEdge> rm = new HashSet<>();
        for (ProgramEdge edge : edges) {
            if (edge.getSrc().equals(node) || edge.getTgt().equals(node))
                rm.add(edge);
        }
        edges.removeAll(rm);
    }

    public Body getCurrentBody() {
        return currentBody;
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }
}
