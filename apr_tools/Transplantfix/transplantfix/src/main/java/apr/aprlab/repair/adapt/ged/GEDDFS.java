package apr.aprlab.repair.adapt.ged;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import apr.aprlab.repair.adapt.ged.action.extract.GraphUtil;
import apr.aprlab.repair.adapt.ged.util.AscendingOrderMyNodeComparator;
import apr.aprlab.repair.adapt.ged.util.EditPath;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.MyTree;
import apr.aprlab.repair.adapt.ged.util.UniversalEdgeHandler;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.repair.adapt.ged.util.MyTree.MyNode;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.adapt.ged.util.RepairActionCostFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GEDDFS {

    public static final Logger logger = LogManager.getLogger(GEDDFS.class);

    private MethodSnippet srcMs;

    private MethodSnippet dstMs;

    @SuppressWarnings("rawtypes")
    MyTree OPEN;

    private Graph G1;

    private Graph G2;

    private Node CurVertex;

    private int G2NbNodes;

    boolean debug;

    private double UBCOST;

    public EditPath UB;

    private int heuristicmethod;

    private AscendingOrderMyNodeComparator MyNodeComparator;

    private int ubmethod;

    protected boolean timeconstraint = false;

    @SuppressWarnings("unused")
    private Timer timer;

    private boolean issolutionoptimal = false;

    private TimerTask task;

    public static int MunkresUB = 0;

    public static int DummyUB = 1;

    private boolean variableordering;

    public GEDDFS(MethodSnippet srcMs, MethodSnippet dstMs) {
        this.srcMs = srcMs;
        this.dstMs = dstMs;
        int ubmethod = 0;
        boolean variableordering = true;
        boolean debug = false;
        Constants.timer = new Timer();
        Constants.timeconstraint = Globals.gedTimeLimit;
        Constants.edgeHandler = new UniversalEdgeHandler();
        Constants.costFunction = new RepairActionCostFunction();
        this.debug = debug;
        Graph graph = srcMs.getGedGraph();
        Graph clonedGraph = GraphUtil.cloneGraph(graph);
        this.G1 = clonedGraph;
        this.G2 = dstMs.getGedGraph();
        G2NbNodes = G2.size();
        UBCOST = Double.MAX_VALUE;
        heuristicmethod = EditPath.MunkresAssigmentHeuristic;
        this.ubmethod = ubmethod;
        this.variableordering = variableordering;
    }

    public Graph run() {
        long start = System.nanoTime();
        inittimer();
        Globals.munkresHasException = false;
        EditPath ROOT = new EditPath(G1, G2, this.heuristicmethod, variableordering);
        if (Globals.munkresHasException) {
            logger.error("EditPath ROOT = new EditPath(G1, G2, this.heuristicmethod, variableordering); has exception!");
            Globals.munkresHasException = false;
            return null;
        }
        MyNodeComparator = new AscendingOrderMyNodeComparator(heuristicmethod);
        OPEN = new MyTree<EditPath>(ROOT, MyNodeComparator);
        if (Constants.FirstUB == null || ubmethod != MunkresUB) {
            UB = ComputeUpperBound(ubmethod);
        } else {
            UB = Constants.FirstUB;
            this.UBCOST = UB.getTotalCosts();
            Constants.FirstUB = null;
        }
        init();
        UB = loop();
        task.cancel();
        Constants.timer.purge();
        Constants.edgecostmatrix = null;
        Constants.nodecostmatrix = null;
        long end = System.nanoTime();
        long executionTime_NanoSeconds = end - start;
        double executionTime_seconds = (double) executionTime_NanoSeconds / (double) 1000000000;
        logger.info("The distance is: " + this.getBestEditpath().getTotalCosts());
        logger.info("The best editpath is: \n" + this.getBestEditpath().bestMatchingNodesMapping());
        if (this.issolutionoptimal) {
            logger.info("The solution is optimal");
        } else {
            logger.info("The solution is not optimal");
        }
        logger.info("The execution time in seconds is: " + new DecimalFormat("##.########").format(executionTime_seconds));
        Graph actionGraph = GraphUtil.getActionGraph(G1, G2, this.getBestEditpath());
        GraphUtil.plot(actionGraph, Globals.donorGraphDir, srcMs.getDotFilePrefix() + "actionsGraph.dot");
        return actionGraph;
    }

    public boolean isTimeconstraintover() {
        return timeconstraint;
    }

    private void inittimer() {
        task = new TimerTask() {

            @Override
            public void run() {
                timeconstraint = true;
            }
        };
        Constants.timer.scheduleAtFixedRate(task, Constants.timeconstraint, Constants.timeconstraint);
    }

    @SuppressWarnings("static-access")
    private EditPath ComputeUpperBound(int upperboundmethod) {
        EditPath res = null;
        if (upperboundmethod == -1) {
            UBCOST = Double.MAX_VALUE;
            res = null;
        }
        if (upperboundmethod == MunkresUB) {
            MatrixGenerator mgen = new MatrixGenerator();
            Munkres munkres = new Munkres();
            MunkresRec munkresRec = new MunkresRec();
            mgen.setMunkres(munkresRec);
            double[][] matrix = mgen.getMatrix(this.G1, G2);
            munkres.setGraphs(G1, G2);
            UBCOST = munkres.getCosts(matrix);
            res = munkres.ApproximatedEditPath();
            UBCOST = res.getTotalCosts();
        }
        if (upperboundmethod == this.DummyUB) {
            EditPath ROOT = new EditPath(this.G1, this.G2);
            ROOT.setHeuristicmethod(this.heuristicmethod);
            res = this.dummyUpperBound(ROOT);
            UBCOST = res.getTotalCosts();
        }
        return res;
    }

    private EditPath dummyUpperBound(EditPath p) {
        EditPath ptmp = null;
        if (p.isComplete() == false) {
            ptmp = new EditPath(p);
            int G1NbNodes = ptmp.getUnUsedNodes1().size();
            int G2NbNodes = ptmp.getUnUsedNodes2().size();
            for (int i = 0; i < G1NbNodes; i++) {
                Node u = (Node) ptmp.getUnUsedNodes1().getFirst();
                if (i < G2NbNodes) {
                    Node v = (Node) ptmp.getUnUsedNodes2().getFirst();
                    ptmp.addDistortion(u, v);
                }
            }
            if (G1NbNodes > G2NbNodes) {
                int noOfDeletedNodes = G1NbNodes - G2NbNodes;
                for (int i = 0; i < noOfDeletedNodes; i++) {
                    Node u = ptmp.getNext();
                    ptmp.addDistortion(u, Constants.EPS_COMPONENT);
                }
            } else if (G1NbNodes < G2NbNodes) {
                int noOfInsertedNodes = G2NbNodes - G1NbNodes;
                for (int i = 0; i < noOfInsertedNodes; i++) {
                    Node u = ptmp.getNextG2();
                    ptmp.addDistortion(Constants.EPS_COMPONENT, u);
                }
            }
        } else {
            return p;
        }
        return ptmp;
    }

    @SuppressWarnings("unchecked")
    private EditPath loop() {
        boolean condition1;
        boolean condition2;
        if (OPEN.isEmpty() == true) {
            issolutionoptimal = true;
            return UB;
        }
        EditPath pmin = null;
        MyNode<EditPath> pminNode = null;
        MyNode<EditPath> CurNode = OPEN.root;
        while (true) {
            pminNode = OPEN.pollFirstLowestCost(CurNode);
            condition1 = (pminNode == null);
            condition2 = ((pminNode == null) && (CurNode.parent != null));
            while (condition1 && condition2) {
                CurNode = OPEN.BackTrack(CurNode);
                pminNode = OPEN.pollFirstLowestCost(CurNode);
                condition1 = (pminNode == null);
                condition2 = ((pminNode == null) && (CurNode.parent != null));
            }
            if (this.timeconstraint == true) {
                return UB;
            }
            if ((pminNode == null) && (CurNode.parent == null)) {
                issolutionoptimal = true;
                return UB;
            }
            pmin = pminNode.data;
            if (pmin.getTotalCosts() + pmin.ComputeHeuristicCosts(heuristicmethod) < UBCOST) {
                if (pmin.getUnUsedNodes1().size() > 0) {
                    this.CurVertex = pmin.getNext();
                    if (debug == true)
                        System.out.println("Current Node=" + this.CurVertex.getComponentId());
                    LinkedList<Node> UnUsedNodes2 = pmin.getUnUsedNodes2();
                    for (int i = 0; i < UnUsedNodes2.size(); i++) {
                        EditPath newpath = new EditPath(pmin);
                        Node w = UnUsedNodes2.get(i);
                        newpath.addDistortion(this.CurVertex, w);
                        AddTreeNode(pminNode, newpath, this.UBCOST);
                    }
                    EditPath newpath = new EditPath(pmin);
                    newpath.addDistortion(this.CurVertex, Constants.EPS_COMPONENT);
                    AddTreeNode(pminNode, newpath, this.UBCOST);
                } else {
                    EditPath newpath = new EditPath(pmin);
                    newpath.complete();
                    double g = newpath.getTotalCosts();
                    double h = newpath.ComputeHeuristicCosts(heuristicmethod);
                    double f = g + h;
                    if (f < UBCOST) {
                        UBCOST = f;
                        UB = newpath;
                    }
                }
            }
            CurNode = pminNode;
        }
    }

    @SuppressWarnings("unchecked")
    private void init() {
        if (G1.size() == 0 && G2.size() == 0) {
            System.out.println("G1 has no node inside");
            System.out.println("G2 has no node inside");
            System.out.println("I cannot work !!!");
            System.exit(0);
        }
        if (G1.size() == 0) {
            System.out.println("G1 has no node inside");
            this.G1 = G2;
            this.G2 = G1;
            G2NbNodes = G2.size();
        }
        for (int i = 0; i < G2NbNodes; i++) {
            EditPath p = new EditPath((EditPath) OPEN.root.data);
            Node v = (Node) G2.get(i);
            this.CurVertex = p.getNext();
            p.addDistortion(this.CurVertex, v);
            AddTreeNode(OPEN.root, p, this.UBCOST);
        }
        EditPath p = new EditPath((EditPath) OPEN.root.data);
        this.CurVertex = p.getNext();
        p.addDistortion(this.CurVertex, Constants.EPS_COMPONENT);
        AddTreeNode(OPEN.root, p, this.UBCOST);
    }

    @SuppressWarnings("unchecked")
    private void AddTreeNode(MyNode<EditPath> parent, EditPath p, double ubcost) {
        double g = p.getTotalCosts();
        double h = p.ComputeHeuristicCosts(heuristicmethod);
        double f = g + h;
        if (f < ubcost) {
            OPEN.Add(parent, p);
        }
    }

    @SuppressWarnings("unused")
    private static FilenameFilter gxlFileFilter = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return name.endsWith(".gxl");
        }
    };

    public EditPath getBestEditpath() {
        return UB;
    }

    public boolean isSolutionoptimal() {
        return issolutionoptimal;
    }

    private boolean memoryconstraint = false;

    public boolean isMemoryconstraintover() {
        return memoryconstraint;
    }

    public MethodSnippet getDstMs() {
        return dstMs;
    }
}
