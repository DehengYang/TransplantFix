package apr.aprlab.utils.graph.cfg;

import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.graph.ProgramEdge;
import apr.aprlab.utils.graph.ProgramGraph;
import apr.aprlab.utils.graph.ProgramNode;
import apr.aprlab.utils.graph.ddg.MethodCall;
import apr.aprlab.utils.soot.SootUtil;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.internal.JIfStmt;
import soot.toolkits.graph.BriefUnitGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CFG extends ProgramGraph {

    public static final Logger logger = LogManager.getLogger(CFG.class);

    private List<MethodCall> methodCalls = new ArrayList<>();

    public CFG(SootMethod mtd) {
        super(mtd);
        construct(mtd.getActiveBody());
        removeGoTo();
    }

    private void construct(Body body) {
        BriefUnitGraph cfg = new BriefUnitGraph(body);
        Set<Unit> visited = new HashSet<>();
        ArrayList<Unit> worklist = new ArrayList<>();
        for (Unit unit : body.getUnits()) {
            CFGNode cfgNode = new CFGNode(unit);
            addNode(cfgNode);
            SootUtil.extractMethodCalls(unit, methodCalls);
        }
        worklist.addAll(cfg.getHeads());
        while (!worklist.isEmpty()) {
            Unit cur = worklist.remove(0);
            if (visited.contains(cur))
                continue;
            visited.add(cur);
            CFGNode curNode = getNodeByUnit(cur);
            List<Unit> childs = cfg.getSuccsOf(cur);
            if (cur.getJavaSourceStartLineNumber() == 1802) {
                logger.debug("");
            }
            if (cur instanceof JIfStmt && childs.size() == 2) {
                CFGNode firstChild = getNodeByUnit(childs.get(0));
                CFGNode secondChild = getNodeByUnit(childs.get(1));
                int firstIndex = getNodes().indexOf(firstChild);
                int secondIndex = getNodes().indexOf(secondChild);
                ExceptionUtil.myAssert(firstIndex != secondIndex);
                if (firstIndex < secondIndex) {
                    addEdge(new CFGEdge(curNode, firstChild, "false"));
                    addEdge(new CFGEdge(curNode, secondChild, "true"));
                } else {
                    addEdge(new CFGEdge(curNode, firstChild, "true"));
                    addEdge(new CFGEdge(curNode, secondChild, "false"));
                }
                worklist.addAll(childs);
            } else {
                for (Unit unit : childs) {
                    worklist.add(unit);
                    CFGNode tgtNode = getNodeByUnit(unit);
                    addEdge(new CFGEdge(curNode, tgtNode, ""));
                }
            }
        }
    }

    private void removeGoTo() {
        ArrayList<ProgramNode> gotoNodes = new ArrayList<>();
        for (ProgramNode node : getNodes()) {
            if (node.getUnit() instanceof GotoStmt)
                gotoNodes.add(node);
        }
        for (ProgramNode node : gotoNodes) {
            Set<ProgramNode> srcs = getSrcs(node);
            Set<ProgramNode> tgts = getTgts(node);
            for (ProgramNode src : srcs) for (ProgramNode tgt : tgts) {
                if (src.getUnit() instanceof JIfStmt) {
                    CFGEdge oriEdge = getEdgeByUnit(src, node);
                    CFGEdge edge = new CFGEdge(src, tgt, oriEdge.getLabel());
                    addEdge(edge);
                } else {
                    CFGEdge edge = new CFGEdge(src, tgt, "");
                    addEdge(edge);
                }
            }
            removeNodeAndEdge(node);
        }
    }

    private CFGEdge getEdgeByUnit(ProgramNode src, ProgramNode tgt) {
        for (ProgramEdge edge : getEdges()) {
            if (edge.getSrc() == src && edge.getTgt() == tgt) {
                return (CFGEdge) edge;
            }
        }
        ExceptionUtil.raise();
        return null;
    }

    private CFGNode getNodeByUnit(Unit unit) {
        for (ProgramNode node : getNodes()) if (node.getUnit().equals(unit))
            return (CFGNode) node;
        return null;
    }

    public List<MethodCall> getMethodCalls() {
        return methodCalls;
    }
}
