package apr.aprlab.repair.adapt.ged.util;

public interface ICostFunction {

    public double getCosts(GraphComponent start, GraphComponent end);

    public double getEdgeCosts();

    public double getNodeCosts();
}
