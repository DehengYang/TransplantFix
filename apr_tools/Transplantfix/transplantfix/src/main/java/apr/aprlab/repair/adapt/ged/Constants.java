package apr.aprlab.repair.adapt.ged;

import java.util.Timer;
import apr.aprlab.repair.adapt.ged.util.*;

public class Constants {

    public final static GraphComponent EPS_COMPONENT = new GraphComponent(Constants.EPS_ID);

    public final static String EPS_ID = "eps_id";

    public static ICostFunction costFunction;

    public static IEdgeHandler edgeHandler;

    public static double[][] nodecostmatrix = null;

    public static double[][] edgecostmatrix = null;

    public static long timeconstraint = 60000;

    public static long memoryconstraint = 100000;

    public static Timer timer;

    public static int MegaBytes = 1048576;

    public static EditPath FirstUB = null;

    public Constants(double nodeCosts, double edgeCosts, double alpha) {
        Constants.costFunction = (ICostFunction) new UnlabeledCostFunction(nodeCosts, edgeCosts, alpha);
        Constants.edgeHandler = new UniversalEdgeHandler();
    }
}
