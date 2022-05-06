package apr.aprlab.utils.graph.ddg;

import apr.aprlab.utils.graph.AliasQuery;
import apr.aprlab.utils.graph.ProgramEdge;
import apr.aprlab.utils.graph.ProgramGraph;
import apr.aprlab.utils.graph.ProgramNode;
import apr.aprlab.utils.graph.help.ProgressBar;
import soot.*;
import soot.jimple.GotoStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class DDG extends ProgramGraph {

    private boolean timeOut = false;

    private final int timeBudget;

    private final int barStep;

    private final long startTime;

    private ProgressBar progressBar;

    private UnitGraph cfg;

    public DDG(SootMethod mtd) {
        super(mtd);
        timeBudget = 20;
        barStep = 100;
        startTime = System.currentTimeMillis();
        construct(super.getBody());
    }

    private void construct(Body body) {
        cfg = new BriefUnitGraph(body);
        for (Unit unit : body.getUnits()) {
            if (!(unit instanceof GotoStmt)) {
                addNode(new DDGNode(unit));
            }
        }
        int nodeSize = numOfNodes();
        System.out.println("[INFO] node size: " + nodeSize);
        progressBar = new ProgressBar(nodeSize * nodeSize, 50);
        for (int i = 0; i < nodeSize; i++) {
            DDGNode srcNode = (DDGNode) getNode(i);
            for (int j = 0; j < nodeSize; j++) {
                if (checkTimeOut())
                    return;
                if (j % barStep == 0)
                    showProgressBar(i * nodeSize + j);
                DDGNode tgtNode = (DDGNode) getNode(j);
                if (srcNode.equals(tgtNode))
                    continue;
                for (Value def : srcNode.getDefs()) for (Value use : tgtNode.getUses()) if (equal(def, use) && immediateDefUse(srcNode, tgtNode, def))
                    addEdge(new DDGEdge(srcNode, tgtNode, def));
            }
        }
        System.out.println("[INFO] edge size: " + numOfEdges());
    }

    public Set<DDGNode> getPrevNodes(DDGNode node) {
        Set<DDGNode> prevs = new HashSet<>();
        for (ProgramEdge edge : getEdges()) {
            if (edge.getTgt().equals(node))
                prevs.add((DDGNode) edge.getSrc());
        }
        return prevs;
    }

    public Set<DDGNode> getPrevNodes(ProgramNode node) {
        return getPrevNodes((DDGNode) node);
    }

    public Set<DDGNode> getNextNodes(DDGNode node) {
        Set<DDGNode> next = new HashSet<>();
        for (ProgramEdge edge : getEdges()) {
            if (edge.getSrc().equals(node))
                next.add((DDGNode) edge.getTgt());
        }
        return next;
    }

    public Set<DDGNode> getNextNodes(ProgramNode node) {
        return getNextNodes((DDGNode) node);
    }

    private boolean checkTimeOut() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - startTime) / 1000 > timeBudget) {
            timeOut = true;
            System.out.println("[ERROR] time out!");
            return true;
        } else
            return false;
    }

    private void showProgressBar(int progress) {
        if (numOfNodes() < 300)
            return;
        progressBar.showBarByPoint(progress);
    }

    public boolean isTimeOut() {
        return timeOut;
    }

    private boolean equal(Value def, Value use) {
        if ((def instanceof Local) && (use instanceof Local))
            return def.equals(use);
        if ((def instanceof JInstanceFieldRef) && (use instanceof JInstanceFieldRef)) {
            JInstanceFieldRef iDef = (JInstanceFieldRef) def;
            JInstanceFieldRef iUse = (JInstanceFieldRef) use;
            if (iDef.getField() == null || iUse.getField() == null)
                return false;
            return AliasQuery.isAlias(iDef.getBase(), iUse.getBase()) && iDef.getField().equals(iUse.getField());
        }
        return false;
    }

    private boolean immediateDefUse(DDGNode srcNode, DDGNode tgtNode, Value value) {
        return findClearPath(srcNode.getUnit(), value, tgtNode.getUnit(), new Stack<>());
    }

    private boolean findClearPath(Unit src, Value value, Unit tgt, Stack<Unit> stack) {
        stack.push(src);
        for (Unit cur : cfg.getSuccsOf(src)) {
            if (cur.equals(tgt))
                return true;
            if (stack.contains(cur) || isOverride(cur, value))
                continue;
            if (findClearPath(cur, value, tgt, stack))
                return true;
        }
        stack.pop();
        return false;
    }

    private boolean isOverride(Unit unit, Value value) {
        DDGNode node = new DDGNode(unit);
        ArrayList<Value> defs = node.getDefs();
        for (Value def : defs) if (equal(def, value))
            return true;
        return false;
    }
}
