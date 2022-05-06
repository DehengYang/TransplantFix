package apr.aprlab.repair.adapt.ged.util;

import apr.aprlab.repair.adapt.ged.Constants;

public class UnlabeledCostFunction implements ICostFunction {

    private double nodeCosts;

    private double edgeCosts;

    private double alpha;

    public UnlabeledCostFunction(double nodeCosts, double edgeCosts, double alpha) {
        this.nodeCosts = nodeCosts;
        this.edgeCosts = edgeCosts;
        this.alpha = alpha;
    }

    @Override
    public double getCosts(GraphComponent start, GraphComponent end) {
        if (start.isNode() || end.isNode()) {
            if (start.getComponentId().equals(Constants.EPS_ID) || end.getComponentId().equals(Constants.EPS_ID)) {
                return this.alpha * this.nodeCosts;
            } else {
                return 0.0;
            }
        } else {
            if (start.getComponentId().equals(Constants.EPS_ID) || end.getComponentId().equals(Constants.EPS_ID)) {
                return (1 - this.alpha) * this.edgeCosts;
            } else {
                return 0.0;
            }
        }
    }

    @Override
    public double getEdgeCosts() {
        return this.edgeCosts;
    }

    @Override
    public double getNodeCosts() {
        return this.nodeCosts;
    }
}
