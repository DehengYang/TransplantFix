package apr.aprlab.repair.adapt.ged;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TimerTask;
import apr.aprlab.repair.adapt.ged.Constants;
import apr.aprlab.repair.adapt.ged.AscendingOrderNonStaticNodeComparator;
import apr.aprlab.repair.adapt.ged.util.EditPath;
import apr.aprlab.repair.adapt.ged.NonStaticNode;
import apr.aprlab.repair.adapt.ged.NonStaticTree;
import apr.aprlab.repair.adapt.ged.util.Node;

@SuppressWarnings("unused")
public class GEDDFSThread extends Thread {

    double idletime = 0;

    double starttime;

    double endtime;

    double cputime = 0;

    @SuppressWarnings("rawtypes")
    NonStaticTree OPEN;

    private Node CurVertex;

    public int openCounterSize;

    public int editPathCounter;

    public int nbexlporednode = 0;

    public int maxopensize = 0;

    public double noOfIterations = 0.0;

    public double varianceSum = 0.0;

    boolean debug;

    private int heuristicmethod;

    private boolean issolutionoptimal = false;

    private TimerTask task;

    public static int MunkresUB = 0;

    public static int DummyUB = 1;

    public double getIdletime() {
        return idletime;
    }

    public double getCputime() {
        return cputime;
    }

    public GEDDFSThread(int heuristic, boolean debug) {
        this.debug = debug;
        heuristicmethod = heuristic;
    }

    void SetRootNode(EditPath ROOT, AscendingOrderNonStaticNodeComparator MyNodeComparator) {
        OPEN = new NonStaticTree<EditPath>(ROOT, MyNodeComparator);
    }

    public boolean isTimeconstraintover() {
        return GlobalVar.timeconstraint;
    }

    @SuppressWarnings("unchecked")
    private EditPath loop() throws FileNotFoundException {
        boolean condition1, condition2;
        String threadFile = "" + Thread.currentThread().getName() + ".csv";
        EditPath pmin = null;
        NonStaticNode<EditPath> pminNode = null;
        NonStaticNode<EditPath> CurNode = OPEN.root;
        while (true) {
            if (GlobalVar.timeconstraint == true) {
                return FinishThread(false);
            }
            ProcessHeavyThread(CurNode);
            pminNode = OPEN.pollFirstLowestCost(CurNode);
            condition1 = (pminNode == null);
            condition2 = ((pminNode == null) && (CurNode.parent != null));
            while (condition1 && condition2) {
                CurNode = OPEN.BackTrack(CurNode);
                pminNode = OPEN.pollFirstLowestCost(CurNode);
                condition1 = (pminNode == null);
                condition2 = ((pminNode == null) && (CurNode.parent != null));
            }
            if ((pminNode == null) && (CurNode.parent == null)) {
                if (GlobalVar.selectedthread != this.IdThread) {
                    if (GlobalVar.timeconstraint == true)
                        return this.FinishThread(false);
                    GlobalVar.loadbalancelock.lock();
                    this.starttime = System.currentTimeMillis();
                    GlobalVar.lightthread = this.IdThread;
                    GlobalVar.selectedthread = findthemostloadedthread();
                    if (GlobalVar.lightthread == GlobalVar.selectedthread || GlobalVar.selectedthread == -1) {
                        return FinishWaitingThreadOptimality(true);
                    }
                    double avg = GlobalVar.tabthreads[GlobalVar.selectedthread].OPEN.globalworkload;
                    if (avg == 0) {
                        return FinishWaitingThreadOptimality(true);
                    }
                    try {
                        GlobalVar.mutex.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (GlobalVar.timeconstraint == true) {
                        return FinishWaitingThreadOptimality(false);
                    }
                    if (GlobalVar.memoryconstraint == true) {
                        return FinishWaitingThreadOptimality(false);
                    }
                    pminNode = OPEN.pollFirstLowestCost(this.OPEN.root);
                    if (pminNode == null && this.ComputerAverageWorkLoad(false) == 0) {
                        return FinishWaitingThreadOptimality(true);
                    }
                    if (pminNode == null) {
                        return FinishWaitingThreadOptimality(false);
                    }
                    this.endtime = System.currentTimeMillis();
                    this.idletime += endtime - starttime;
                    GlobalVar.lightthread = -1;
                    GlobalVar.selectedthread = -1;
                    GlobalVar.loadbalancelock.unlock();
                }
            }
            if (pminNode == null) {
                if (this.ComputerAverageWorkLoad(false) == 0) {
                    return FinishThread(true);
                } else {
                    System.out.println("error= " + this.IdThread);
                    System.out.println(" balancing=");
                    ComputerAverageWorkLoad(true);
                }
            }
            if ((pminNode == null) && (CurNode.parent == null)) {
                return FinishThread(true);
            }
            pmin = pminNode.data;
            if (pmin.getTotalCosts() + pmin.ComputeHeuristicCosts(heuristicmethod) < GlobalVar.UBCOST) {
                this.nbexlporednode++;
                maxopensize = Math.max(pminNode.children.size(), this.maxopensize);
                if (pmin.getUnUsedNodes1().size() > 0) {
                    this.CurVertex = pmin.getNext();
                    if (debug == true)
                        System.out.println("Current Node=" + this.CurVertex.getComponentId());
                    LinkedList<Node> UnUsedNodes2 = pmin.getUnUsedNodes2();
                    for (int i = 0; i < UnUsedNodes2.size(); i++) {
                        EditPath newpath = new EditPath(pmin);
                        Node w = UnUsedNodes2.get(i);
                        newpath.addDistortion(this.CurVertex, w);
                        AddTreeNode(pminNode, newpath, GlobalVar.UBCOST);
                    }
                    EditPath newpath = new EditPath(pmin);
                    newpath.addDistortion(this.CurVertex, Constants.EPS_COMPONENT);
                    AddTreeNode(pminNode, newpath, GlobalVar.UBCOST);
                } else {
                    EditPath newpath = new EditPath(pmin);
                    newpath.complete();
                    double g = newpath.getTotalCosts();
                    double h = newpath.ComputeHeuristicCosts(heuristicmethod);
                    double f = g + h;
                    if (f < GlobalVar.UBCOST) {
                        GlobalVar.ubupdatelock.lock();
                        if (f < GlobalVar.UBCOST) {
                            GlobalVar.UBCOST = f;
                            GlobalVar.UB = newpath;
                        }
                        GlobalVar.ubupdatelock.unlock();
                    }
                }
            }
            CurNode = pminNode;
        }
    }

    @SuppressWarnings("static-access")
    private void ProcessHeavyThread(NonStaticNode<EditPath> CurNode) {
        if (this.IdThread == GlobalVar.selectedthread) {
            this.starttime = System.currentTimeMillis();
            try {
                this.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (GlobalVar.memoryconstraint == true || GlobalVar.timeconstraint == true || GlobalVar.selectedthread == -1 || GlobalVar.lightthread == -1 || GlobalVar.selectedthread == GlobalVar.lightthread || GlobalVar.tabthreads[GlobalVar.selectedthread].OPEN.globalworkload == 0) {
                GlobalVar.selectedthread = -1;
                GlobalVar.lightthread = -1;
                GlobalVar.mutex.release();
            } else {
                GlobalVar.averageworkload = this.ComputerAverageWorkLoad(false);
                loadbalance(GlobalVar.lightthread, GlobalVar.selectedthread, CurNode);
                GlobalVar.lightthread = -1;
                GlobalVar.selectedthread = -1;
                GlobalVar.mutex.release();
            }
            this.endtime = System.currentTimeMillis();
            this.idletime += endtime - starttime;
        }
    }

    @SuppressWarnings("unchecked")
    private EditPath FinishThread(boolean optimal) {
        GlobalVar.tabthreads[this.IdThread].OPEN.globalworkload = 0;
        GlobalVar.threadNoOfIterations[this.IdThread] = noOfIterations;
        GlobalVar.threadvariance[this.IdThread] = varianceSum;
        if (this.IdThread == GlobalVar.selectedthread)
            GlobalVar.mutex.release();
        this.issolutionoptimal = optimal;
        GlobalVar.sem.release();
        return GlobalVar.UB;
    }

    @SuppressWarnings("unchecked")
    private EditPath FinishWaitingThreadOptimality(boolean optimal) {
        GlobalVar.tabthreads[this.IdThread].OPEN.globalworkload = 0;
        if (this.IdThread == GlobalVar.selectedthread)
            GlobalVar.mutex.release();
        GlobalVar.selectedthread = -1;
        GlobalVar.threadNoOfIterations[this.IdThread] = noOfIterations;
        GlobalVar.threadvariance[this.IdThread] = varianceSum;
        issolutionoptimal = optimal;
        this.endtime = System.currentTimeMillis();
        this.idletime += endtime - starttime;
        GlobalVar.lightthread = -1;
        GlobalVar.loadbalancelock.unlock();
        GlobalVar.sem.release();
        return GlobalVar.UB;
    }

    @SuppressWarnings("rawtypes")
    private void loadbalance(int lightthread, int hevyidthread, NonStaticNode CurNode) {
        NonStaticNode CurNode2 = CurNode;
        double halfworkload = GlobalVar.tabthreads[hevyidthread].OPEN.globalworkload / 2.0;
        if (lightthread == -1) {
            System.out.println("The thread requested jobs has been set to -1. This is not normal ? " + "Well it can happen cause first the say yes this thread is selected then we change " + "our mind but the selected thread caught the notification so to limit this i put" + " a sleep when the notification is caucght and before checking the light thread " + "still needs jobs");
            return;
        }
        int isLightThread = 1;
        while (GlobalVar.tabthreads[lightthread].OPEN.globalworkload < halfworkload) {
            ArrayList nodes;
            if (isLightThread == 1) {
                nodes = GlobalVar.tabthreads[hevyidthread].OPEN.searchLowestCost(CurNode2, 1);
                isLightThread = 0;
            } else {
                nodes = GlobalVar.tabthreads[hevyidthread].OPEN.searchLowestCost(CurNode2, 0);
                isLightThread = 1;
            }
            if (nodes == null || nodes.size() == 0) {
                break;
            }
            NonStaticNode node = (NonStaticNode) nodes.get(0);
            CurNode2 = (NonStaticNode) nodes.get(1);
            GlobalVar.tabthreads[lightthread].addJob((EditPath) node.data);
        }
    }

    private int findthemostloadedthread() {
        double maxvalue = -1;
        int maxindex = -1;
        for (int i = 0; i < GlobalVar.tabthreads.length; i++) {
            if (maxvalue < GlobalVar.tabthreads[i].OPEN.globalworkload) {
                maxvalue = GlobalVar.tabthreads[i].OPEN.globalworkload;
                maxindex = i;
            }
        }
        return maxindex;
    }

    private double ComputerAverageWorkLoad(boolean b) {
        double avg = 0;
        for (int i = 0; i < GlobalVar.tabthreads.length; i++) {
            if (b == true)
                System.out.print(GlobalVar.tabthreads[i].OPEN.globalworkload + "  ");
            avg += GlobalVar.tabthreads[i].OPEN.globalworkload;
        }
        if (b == true)
            System.out.println();
        return avg / (double) GlobalVar.tabthreads.length;
    }

    @SuppressWarnings("unchecked")
    private void AddTreeNode(NonStaticNode<EditPath> parent, EditPath p, double ubcost2) {
        double g = p.getTotalCosts();
        double h = p.ComputeHeuristicCosts(heuristicmethod);
        double f = g + h;
        if (f < ubcost2) {
            OPEN.Add(parent, p);
        }
    }

    public EditPath getBestEditpath() {
        return GlobalVar.UB;
    }

    public int getNbExploredNode() {
        return this.nbexlporednode;
    }

    public int getMaxSizeOpen() {
        return this.maxopensize;
    }

    public boolean isSolutionoptimal() {
        return issolutionoptimal;
    }

    private GEDMultiThread motherclass;

    private int IdThread;

    public boolean isMemoryconstraintover() {
        return GlobalVar.memoryconstraint;
    }

    @Override
    public void run() {
        this.cputime = System.currentTimeMillis();
        try {
            loop();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.cputime = System.currentTimeMillis() - cputime;
    }

    @SuppressWarnings("unchecked")
    public void addJob(EditPath editPath) {
        AddTreeNode(this.OPEN.root, editPath, Double.MAX_VALUE);
        this.OPEN.root.issorted = false;
    }

    public void setMotherClass(GEDMultiThread gedMultiThread) {
        this.motherclass = gedMultiThread;
    }

    public void SetIdThead(int i) {
        this.IdThread = i;
    }
}
