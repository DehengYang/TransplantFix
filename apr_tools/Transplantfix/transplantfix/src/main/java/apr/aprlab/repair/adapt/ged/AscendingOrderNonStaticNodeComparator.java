package apr.aprlab.repair.adapt.ged;

import java.util.Comparator;
import apr.aprlab.repair.adapt.ged.util.EditPath;

@SuppressWarnings("rawtypes")
public class AscendingOrderNonStaticNodeComparator implements Comparator<NonStaticNode> {

    double g1;

    double h1;

    double f1;

    double g2;

    double h2;

    double f2;

    private int heuristicmethod;

    public AscendingOrderNonStaticNodeComparator(int heuristicmethod) {
        this.heuristicmethod = heuristicmethod;
    }

    public static void main(String[] args) {
    }

    @Override
    public int compare(final NonStaticNode o1, final NonStaticNode o2) {
        EditPath ed1 = (EditPath) o1.data;
        EditPath ed2 = (EditPath) o2.data;
        g1 = ed1.getTotalCosts();
        h1 = ed1.ComputeHeuristicCosts(heuristicmethod);
        f1 = g1 + h1;
        g2 = ed2.getTotalCosts();
        h2 = ed2.ComputeHeuristicCosts(heuristicmethod);
        f2 = g2 + h2;
        if (f2 > f1) {
            return -1;
        }
        if (f2 < f1) {
            return 1;
        } else {
            return 0;
        }
    }
}
