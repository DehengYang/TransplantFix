package apr.aprlab.repair.adapt.ged.util;

import apr.aprlab.repair.adapt.ged.action.GedGlobals;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.similarity.SimilarityUtil;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RepairActionCostFunction implements ICostFunction {

    public static final Logger logger = LogManager.getLogger(RepairActionCostFunction.class);

    @Override
    public double getCosts(GraphComponent start, GraphComponent end) {
        if (GedGlobals.costMap.containsKey(start) && GedGlobals.costMap.get(start).containsKey(end)) {
            double cost = GedGlobals.costMap.get(start).get(end);
            return cost;
        }
        double distance = -1;
        if (start.isNode() || end.isNode()) {
            if (start.getComponentId().equals("0") && end.getComponentId().equals("23")) {
                logger.debug("");
            }
            distance = GedGlobals.mapAndCompare(start, end);
        } else {
            if (end.getComponentId().equals("6==7")) {
                logger.debug("");
            }
            double dist1 = 0;
            double dist2 = 0;
            Node startNode1 = null, startNode2 = null;
            Node endNode1 = null, endNode2 = null;
            double cost = 0.5;
            if (!start.isNullEps()) {
                startNode1 = ((Edge) start).getStartNode();
                endNode1 = ((Edge) start).getEndNode();
            } else {
                distance = cost;
            }
            if (!end.isNullEps()) {
                startNode2 = ((Edge) end).getStartNode();
                endNode2 = ((Edge) end).getEndNode();
            } else {
                distance = cost;
            }
            if (!start.isNullEps() && !end.isNullEps()) {
                Edge startEdge = (Edge) start;
                Edge endEdge = (Edge) end;
                if (startEdge.getComponentId().equals("5==6")) {
                    if (endEdge.getComponentId().equals("16==27")) {
                        logger.debug("");
                    }
                }
                dist1 = GedGlobals.mapAndCompare(startNode1, startNode2);
                dist2 = GedGlobals.mapAndCompare(endNode1, endNode2);
                float dist3 = 1 - SimilarityUtil.compare(startEdge.getLabel(), endEdge.getLabel());
                float finalDist = (float) ((dist1 + dist2 + dist3) / 3);
                return finalDist;
            }
        }
        if (GedGlobals.costMap.containsKey(start)) {
            ExceptionUtil.assertFalse(GedGlobals.costMap.get(start).containsKey(end));
            GedGlobals.costMap.get(start).put(end, distance);
        } else {
            GedGlobals.costMap.put(start, new HashMap<>());
            GedGlobals.costMap.get(start).put(end, distance);
        }
        return distance;
    }

    @Override
    public double getEdgeCosts() {
        return 1;
    }

    @Override
    public double getNodeCosts() {
        return 1;
    }
}
