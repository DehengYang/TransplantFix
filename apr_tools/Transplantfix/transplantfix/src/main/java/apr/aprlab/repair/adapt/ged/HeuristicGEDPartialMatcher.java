package apr.aprlab.repair.adapt.ged;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import apr.aprlab.repair.adapt.ged.Constants;
import apr.aprlab.repair.adapt.ged.MatrixGenerator;
import apr.aprlab.repair.adapt.ged.Munkres;
import apr.aprlab.repair.adapt.ged.MunkresRec;
import apr.aprlab.repair.adapt.ged.util.EditPath;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.ICostFunction;
import apr.aprlab.repair.adapt.ged.util.IEdgeHandler;
import apr.aprlab.repair.adapt.ged.util.Node;

public class HeuristicGEDPartialMatcher {

    public static int MunkresUB = 0;

    public static int DummyUB = 1;

    public ArrayList<EditPath> OPEN;

    private Graph G1;

    private Graph G2;

    private Node CurNode;

    private int G2NbNodes;

    public int openCounterSize;

    public int editPathCounter;

    public int noOfEditPaths;

    boolean v1Sorting;

    public double UBCOST = Double.MAX_VALUE;

    public EditPath UBEditPath;

    int nbexlporednode;

    int maxopensize = 0;

    private EditPath BestEditpath = null;

    boolean debug;

    private int heuristicmethod;

    private boolean isthereanyfeasibleolutionfound = false;

    EditPath root;

    public boolean isIsthereanyfeasibleolutionfound() {
        return isthereanyfeasibleolutionfound;
    }

    public EditPath getBestEditpath() {
        return BestEditpath;
    }

    public HeuristicGEDPartialMatcher(Graph G1, Graph G2, int noOfEditPaths, ICostFunction costfunction, IEdgeHandler edgehandler, int heuristicmethod, boolean v1sorting, boolean debug, EditPath upperBound) {
        this.v1Sorting = v1sorting;
        this.debug = debug;
        this.G1 = G1;
        this.G2 = G2;
        this.heuristicmethod = heuristicmethod;
        this.noOfEditPaths = noOfEditPaths;
        G2NbNodes = G2.size();
        OPEN = new ArrayList<EditPath>();
        UBCOST = upperBound.getTotalCosts();
        UBEditPath = new EditPath(upperBound);
        init();
        nbexlporednode++;
        BestEditpath = loop();
    }

    public HeuristicGEDPartialMatcher(Graph G1, Graph G2, int noOfEditPaths, ICostFunction costfunction, IEdgeHandler edgehandler, int heuristicmethod, EditPath root, int ubmethod, boolean v1sorting, boolean debug) {
        this.root = root;
        this.v1Sorting = v1sorting;
        this.debug = debug;
        this.G1 = G1;
        this.G2 = G2;
        this.heuristicmethod = heuristicmethod;
        this.noOfEditPaths = noOfEditPaths;
        G2NbNodes = G2.size();
        OPEN = new ArrayList<EditPath>();
        if (Constants.FirstUB == null || ubmethod != MunkresUB) {
            GlobalVar.UB = ComputeUpperBound(ubmethod);
            if (ubmethod == -1) {
                GlobalVar.UBCOST = Double.MAX_VALUE;
                this.UBCOST = Double.MAX_VALUE;
            } else {
                GlobalVar.UBCOST = GlobalVar.UB.getTotalCosts();
                this.UBCOST = GlobalVar.UB.getTotalCosts();
            }
        } else {
            GlobalVar.UB = Constants.FirstUB;
            GlobalVar.UBCOST = GlobalVar.UB.getTotalCosts();
            this.UBCOST = GlobalVar.UB.getTotalCosts();
            Constants.FirstUB = null;
        }
        init();
        nbexlporednode++;
        BestEditpath = loop();
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
    private EditPath loop() {
        while (true) {
            if (OPEN.isEmpty() == true) {
                return null;
            }
            if (OPEN.size() > noOfEditPaths) {
                return null;
            }
            EditPath pmin = findmincostEditPathWithHeuristic(OPEN);
            this.nbexlporednode++;
            OPEN.remove(pmin);
            if (pmin.isComplete() == true) {
                UBCOST = pmin.getTotalCosts();
                UBEditPath = new EditPath(pmin);
                GlobalVar.UBCOST = pmin.getTotalCosts();
                GlobalVar.UB = new EditPath(pmin);
                OPEN.clear();
                return pmin;
            } else {
                if (pmin.getUnUsedNodes1().size() > 0) {
                    this.CurNode = pmin.getNext();
                    LinkedList<Node> UnUsedNodes2 = pmin.getUnUsedNodes2();
                    for (int i = 0; i < UnUsedNodes2.size(); i++) {
                        EditPath newpath = new EditPath(pmin);
                        Node w = UnUsedNodes2.get(i);
                        newpath.addDistortion(this.CurNode, w);
                        if (debug == true)
                            System.out.println("***Substitution ");
                        if (newpath.getTotalCosts() + newpath.ComputeHeuristicCosts(this.heuristicmethod) < UBCOST) {
                            this.OPEN.add(newpath);
                            if (debug == true)
                                System.out.println("Substitution " + newpath.toString());
                        }
                    }
                    EditPath newpath = new EditPath(pmin);
                    newpath.addDistortion(this.CurNode, Constants.EPS_COMPONENT);
                    if (newpath.getTotalCosts() + newpath.ComputeHeuristicCosts(this.heuristicmethod) < UBCOST) {
                        this.OPEN.add(newpath);
                    }
                    if (debug == true)
                        TrackOpenEditPathSize();
                } else {
                    EditPath newpath = new EditPath(pmin);
                    newpath.complete();
                    if (newpath.getTotalCosts() + newpath.ComputeHeuristicCosts(this.heuristicmethod) < UBCOST) {
                        UBCOST = newpath.getTotalCosts();
                        UBEditPath = newpath;
                        GlobalVar.UBCOST = UBCOST;
                        GlobalVar.UB = newpath;
                        if (debug == true)
                            System.out.println("modifying upper bound" + UBCOST);
                    }
                }
            }
        }
    }

    private EditPath findmincostEditPathWithHeuristic(ArrayList<EditPath> oPEN2) {
        int i = 0;
        int nbpaths = OPEN.size();
        double minvalue = Double.MAX_VALUE;
        double h = 0.0, g = 0.0;
        int indexmin = -1;
        for (i = 0; i < nbpaths; i++) {
            EditPath p = OPEN.get(i);
            g = p.getTotalCosts();
            h = p.ComputeHeuristicCosts(this.heuristicmethod);
            if ((g + h) < minvalue) {
                minvalue = g + h;
                indexmin = i;
            }
        }
        return OPEN.get(indexmin);
    }

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
            EditPath p = new EditPath(root);
            Node v = (Node) G2.get(i);
            this.CurNode = p.getNext();
            p.addDistortion(this.CurNode, v);
            if (debug == true)
                System.out.println("Substitution " + p.toString());
            OPEN.add(p);
        }
        EditPath p = new EditPath(root);
        this.CurNode = p.getNext();
        p.addDistortion(this.CurNode, Constants.EPS_COMPONENT);
        if (debug == true)
            System.out.println("Deletion " + p.toString());
        OPEN.add(p);
    }

    private void TrackOpenEditPathSize() {
        this.editPathCounter++;
        if (openCounterSize < OPEN.size()) {
            openCounterSize = OPEN.size();
        }
    }

    @SuppressWarnings("unused")
    private static FilenameFilter gxlFileFilter = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return name.endsWith(".gxl");
        }
    };

    public int getNbExploredNode() {
        return this.nbexlporednode;
    }

    public int getMaxSizeOpen() {
        return this.maxopensize;
    }

    protected boolean timeconstraint = false;

    private Timer timer;

    public boolean isTimeconstraintover() {
        return timeconstraint;
    }

    @SuppressWarnings("unused")
    private void inittimer() {
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                timeconstraint = true;
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(task, 60000, 60000);
    }

    private boolean memoryconstraint = false;

    private boolean issolutionoptimal = false;

    public boolean isMemoryconstraintover() {
        return memoryconstraint;
    }

    @SuppressWarnings("unused")
    private boolean memorylimittest() {
        long val = Runtime.getRuntime().freeMemory();
        if (val < 1000000) {
            memoryconstraint = true;
            return true;
        }
        return false;
    }

    public boolean isSolutionoptimal() {
        return issolutionoptimal;
    }
}
