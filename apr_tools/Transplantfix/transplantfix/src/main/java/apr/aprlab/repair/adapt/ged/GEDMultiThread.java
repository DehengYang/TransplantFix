package apr.aprlab.repair.adapt.ged;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import apr.aprlab.repair.adapt.ged.Constants;
import apr.aprlab.repair.adapt.ged.MatrixGenerator;
import apr.aprlab.repair.adapt.ged.Munkres;
import apr.aprlab.repair.adapt.ged.MunkresRec;
import apr.aprlab.repair.adapt.ged.AscendingOrderNonStaticNodeComparator;
import apr.aprlab.repair.adapt.ged.HeuristicGEDPartialMatcher;
import apr.aprlab.repair.adapt.ged.util.AscendingHeuristicEditPathComparator;
import apr.aprlab.repair.adapt.ged.util.EditPath;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.ICostFunction;
import apr.aprlab.repair.adapt.ged.util.IEdgeHandler;
import apr.aprlab.repair.adapt.ged.util.MyTree;
import apr.aprlab.repair.adapt.ged.util.MyTree.MyNode;
import apr.aprlab.repair.adapt.ged.util.Node;

@SuppressWarnings({ "rawtypes", "unused" })
public class GEDMultiThread {

    MyTree OPEN;

    private Graph G1;

    private Graph G2;

    private Node CurVertex;

    private int G2NbNodes;

    boolean debug;

    private double UBCOST;

    private int heuristicmethod;

    private AscendingOrderNonStaticNodeComparator MyNodeComparator;

    private int ubmethod;

    @SuppressWarnings("static-access")
    public GEDMultiThread(Graph G1, Graph G2, ICostFunction costfunction, IEdgeHandler edgehandler, int heuristic, int ubmethod, boolean variableordering, int nbthread, int numberofInitialEditPaths, boolean debug) {
        inittimer();
        GlobalVar.selectedthread = -1;
        GlobalVar.lightthread = -1;
        GlobalVar.timeconstraint = false;
        GlobalVar.memoryconstraint = false;
        GlobalVar.averageworkload = 0;
        GlobalVar.tabthreads = null;
        GlobalVar.UBCOST = Double.MAX_VALUE;
        GlobalVar.UB = null;
        this.debug = debug;
        this.G1 = G1;
        this.G2 = G2;
        G2NbNodes = G2.size();
        UBCOST = Double.MAX_VALUE;
        heuristicmethod = heuristic;
        this.ubmethod = ubmethod;
        if (numberofInitialEditPaths < G1.size() || numberofInitialEditPaths < G2.size()) {
            numberofInitialEditPaths = Math.max(G1.size(), G2.size()) + 1;
        }
        GlobalVar.sem = new Semaphore(-nbthread + 1);
        GlobalVar.mutex = new Semaphore(0);
        MyNodeComparator = new AscendingOrderNonStaticNodeComparator(heuristicmethod);
        EditPath ROOT1 = new EditPath(G1, G2, this.heuristicmethod, variableordering);
        HeuristicGEDPartialMatcher HGED = new HeuristicGEDPartialMatcher(G1, G2, numberofInitialEditPaths, Constants.costFunction, Constants.edgeHandler, heuristicmethod, ROOT1, ubmethod, variableordering, debug);
        AscendingHeuristicEditPathComparator EDComparator = new AscendingHeuristicEditPathComparator(heuristicmethod);
        Collections.sort(HGED.OPEN, EDComparator);
        ArrayList<EditPath> somejobs = HGED.OPEN;
        GlobalVar.tabthreads = new GEDDFSThread[nbthread];
        GlobalVar.threadNoOfIterations = new double[nbthread];
        GlobalVar.threadvariance = new double[nbthread];
        for (int i = 0; i < nbthread; i++) {
            GlobalVar.threadNoOfIterations[i] = 0.0;
            GlobalVar.threadvariance[i] = 0.0;
            GlobalVar.tabthreads[i] = new GEDDFSThread(heuristicmethod, debug);
            GlobalVar.tabthreads[i].SetRootNode(ROOT1, MyNodeComparator);
            GlobalVar.tabthreads[i].SetIdThead(i);
        }
        DispatchJobs(GlobalVar.tabthreads, somejobs);
        for (int i = 0; i < nbthread; i++) {
            GlobalVar.tabthreads[i].start();
        }
        try {
            Thread.currentThread().sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            GlobalVar.sem.acquire();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        task.cancel();
        Constants.timer.purge();
        Constants.edgecostmatrix = null;
        Constants.nodecostmatrix = null;
    }

    private void DispatchJobs(GEDDFSThread[] tabthreads, ArrayList<EditPath> somejobs) {
        for (int i = 0; i < somejobs.size(); i++) {
            int index = i % tabthreads.length;
            tabthreads[index].addJob(somejobs.get(i));
        }
    }

    private Timer timer;

    private boolean issolutionoptimal = false;

    private TimerTask task;

    public static int MunkresUB = 0;

    public static int DummyUB = 1;

    public boolean isTimeconstraintover() {
        return GlobalVar.timeconstraint;
    }

    private void inittimer() {
        task = new TimerTask() {

            @Override
            public void run() {
                GlobalVar.timeconstraint = true;
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
            EditPath ROOT = new EditPath(this.G1, this.G2, this.heuristicmethod, false);
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
    private ArrayList<EditPath> init() {
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
        ArrayList<EditPath> res = new ArrayList<EditPath>();
        for (int i = 0; i < G2NbNodes; i++) {
            EditPath p = new EditPath((EditPath) OPEN.root.data);
            Node v = (Node) G2.get(i);
            this.CurVertex = p.getNext();
            p.addDistortion(this.CurVertex, v);
            if (debug == true)
                System.out.println("Substitution CurNode G1 and ith node of G2= (" + this.CurVertex.getComponentId() + "  " + v.getComponentId() + ")   Cost =" + p.getTotalCosts());
            res.add(p);
            AddTreeNode(OPEN.root, p, this.UBCOST);
        }
        EditPath p = new EditPath((EditPath) OPEN.root.data);
        this.CurVertex = p.getNext();
        p.addDistortion(this.CurVertex, Constants.EPS_COMPONENT);
        res.add(p);
        AddTreeNode(OPEN.root, p, this.UBCOST);
        return res;
    }

    @SuppressWarnings("unchecked")
    private void AddTreeNode(MyNode<EditPath> parent, EditPath p, double ubcost2) {
        double g = p.getTotalCosts();
        double h = p.ComputeHeuristicCosts(heuristicmethod);
        double f = g + h;
        if (f < ubcost2) {
            OPEN.Add(parent, p);
        }
    }

    private static FilenameFilter gxlFileFilter = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return name.endsWith(".gxl");
        }
    };

    public EditPath getBestEditpath() {
        return GlobalVar.UB;
    }

    public double getVariance() {
        double maxNoOfIterations = -1;
        int threadIndex = -1;
        for (int i = 0; i < GlobalVar.tabthreads.length; i++) {
            if (GlobalVar.threadNoOfIterations[i] > maxNoOfIterations) {
                maxNoOfIterations = GlobalVar.threadNoOfIterations[i];
                threadIndex = i;
            }
        }
        double averageVariance = GlobalVar.threadvariance[threadIndex] / maxNoOfIterations;
        return averageVariance;
    }

    public double getMaxIteration() {
        double maxNoOfIterations = -1;
        int threadIndex = -1;
        for (int i = 0; i < GlobalVar.tabthreads.length; i++) {
            if (GlobalVar.threadNoOfIterations[i] > maxNoOfIterations) {
                maxNoOfIterations = GlobalVar.threadNoOfIterations[i];
                threadIndex = i;
            }
        }
        return maxNoOfIterations;
    }

    public double getIDLEtime() {
        double res = 0;
        for (int i = 0; i < GlobalVar.tabthreads.length; i++) {
            res += GlobalVar.tabthreads[i].getIdletime();
        }
        return res;
    }

    public int getNbExploredNode() {
        int res = 0;
        for (int i = 0; i < GlobalVar.tabthreads.length; i++) {
            res += GlobalVar.tabthreads[i].getNbExploredNode();
        }
        return res;
    }

    public double getCputime() {
        double res = 0;
        for (int i = 0; i < GlobalVar.tabthreads.length; i++) {
            res += GlobalVar.tabthreads[i].getCputime();
        }
        return res;
    }

    public int getMaxSizeOpen() {
        int res = 0;
        for (int i = 0; i < GlobalVar.tabthreads.length; i++) {
            res += GlobalVar.tabthreads[i].getMaxSizeOpen();
        }
        return res;
    }

    public boolean isSolutionoptimal() {
        if (isTimeconstraintover() == true) {
            return false;
        } else {
            return true;
        }
    }
}
