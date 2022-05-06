package apr.aprlab.repair.adapt.ged;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import apr.aprlab.repair.adapt.ged.util.EditPath;

public class GlobalVar {

    static public int selectedthread;

    static public double UBCOST;

    static public EditPath UB;

    static public Semaphore sem;

    static public Semaphore mutex;

    static public boolean timeconstraint = false;

    static public boolean memoryconstraint = false;

    static public GEDDFSThread[] tabthreads;

    static public double[] threadvariance;

    static public double[] threadNoOfIterations;

    public static double averageworkload;

    public static int lightthread;

    public static final ReentrantLock ubupdatelock = new ReentrantLock();

    public static final ReentrantLock loadbalancelock = new ReentrantLock();
}
