package apr.aprlab.repair.adapt.ged.util;

import java.util.*;
import apr.aprlab.repair.adapt.ged.Constants;
import apr.aprlab.repair.adapt.ged.MatrixGenerator;
import apr.aprlab.repair.adapt.ged.Munkres;
import apr.aprlab.repair.adapt.ged.MunkresRec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class EditPath {

    public static final Logger logger = LogManager.getLogger(EditPath.class);

    double totalCosts;

    static public int NoHeuristic = 0;

    static public int NoAssigmentHeuristic = 1;

    static public int MunkresAssigmentHeuristic = 2;

    static public int LAPAssigmentHeuristic = 3;

    static public int HausdorffEDHeuristic = 4;

    private MunkresRec munkresRec;

    private int noOfDistortedNodes;

    private String editPathId;

    private double HeuristicCosts;

    private boolean isheuristiccomputed;

    public double getHeuristicCosts() {
        return HeuristicCosts;
    }

    public int getNoOfDistortedNodes() {
        return noOfDistortedNodes;
    }

    public void setID(String editPathId) {
        this.editPathId = editPathId;
    }

    public String getID() {
        return editPathId;
    }

    public void setNoOfDistortedNodes(int noOfDistortedNodes) {
        this.noOfDistortedNodes = noOfDistortedNodes;
    }

    public void setHeuristicCosts(double heuristicCosts) {
        HeuristicCosts = heuristicCosts;
    }

    public double ComputeHeuristicCosts(int heuristicmethod) {
        if (this.isheuristiccomputed == false) {
            if (heuristicmethod == MunkresAssigmentHeuristic)
                ComputeHeuristicCostsAssignmentMunkres();
            if (heuristicmethod == NoHeuristic)
                HeuristicCosts = 0;
            isheuristiccomputed = true;
        }
        return HeuristicCosts;
    }

    public double ComputeNoAssigmentHeuristic() {
        HeuristicCosts = 0.0;
        int n1, n2;
        int e1, e2;
        n1 = this.unUsedNodes1.size();
        n2 = this.unUsedNodes2.size();
        e1 = this.unUsedEdges1.size();
        e2 = this.unUsedEdges2.size();
        int nbdeletions = Math.max(0, n1 - n2);
        HeuristicCosts += ComputeNbDeletion(unUsedNodes1, nbdeletions);
        int nbinsertion = Math.max(0, n2 - n1);
        HeuristicCosts += ComputeNbInsertion(unUsedNodes2, nbinsertion);
        nbdeletions = Math.max(0, e1 - e2);
        HeuristicCosts += ComputeNbDeletion(unUsedEdges1, nbdeletions);
        nbinsertion = Math.max(0, e2 - e1);
        HeuristicCosts += ComputeNbInsertion(unUsedEdges2, nbinsertion);
        return HeuristicCosts;
    }

    public EditPath(Graph source, Graph target, int heuristicmethod, boolean sortG1) {
        this.heuristicmethod = heuristicmethod;
        munkresRec = new MunkresRec();
        this.init();
        this.source = source;
        this.target = target;
        LinkedList tmpunUsedNodes1 = new LinkedList();
        this.unUsedNodes1 = new LinkedList();
        this.unUsedNodes2.addAll(target);
        this.unUsedEdges1.addAll(source.getEdges());
        this.unUsedEdges2.addAll(target.getEdges());
        if (sortG1 == false) {
            this.unUsedNodes1.addAll(source);
            if (Constants.nodecostmatrix == null || Constants.edgecostmatrix == null) {
                Constants.nodecostmatrix = ComputeCostMatrixNM(unUsedNodes1, unUsedNodes2, true);
                Constants.edgecostmatrix = ComputeCostMatrixNM(unUsedEdges1, unUsedEdges2, false);
            }
        } else {
            tmpunUsedNodes1.addAll(source);
            if (Constants.nodecostmatrix == null || Constants.edgecostmatrix == null) {
                Constants.nodecostmatrix = ComputeCostMatrixNM(tmpunUsedNodes1, unUsedNodes2, true);
                Constants.edgecostmatrix = ComputeCostMatrixNM(unUsedEdges1, unUsedEdges2, false);
            }
            sortunUsedNodes1(tmpunUsedNodes1);
        }
    }

    private void sortunUsedNodes1(LinkedList tmpunUsedNodes1) {
        double[][] matrix = this.ComputeCostMatrix(tmpunUsedNodes1, unUsedNodes2, true);
        int[][] starredmatrix = this.ComputeStarredMatrix(matrix, true);
        for (int i = 0; i < matrix.length; i++) {
            double minval = Double.POSITIVE_INFINITY;
            int minindex = -1;
            for (int j = 0; j < starredmatrix.length; j++) {
                int r = starredmatrix[j][0];
                int c = starredmatrix[j][1];
                if (minval >= matrix[r][c]) {
                    minval = matrix[r][c];
                    minindex = j;
                }
            }
            int r = starredmatrix[minindex][0];
            int c = starredmatrix[minindex][1];
            matrix[r][c] = Double.POSITIVE_INFINITY;
            GraphComponent gc1;
            if (r >= tmpunUsedNodes1.size()) {
                gc1 = Constants.EPS_COMPONENT;
            } else {
                gc1 = (GraphComponent) tmpunUsedNodes1.get(r);
                this.unUsedNodes1.addLast(gc1);
            }
        }
    }

    private int[][] ComputeStarredMatrix(double[][] nodecostmatric, boolean isnode) {
        MatrixGenerator mgen = new MatrixGenerator();
        Munkres munkres = new Munkres();
        MunkresRec munkresRec = new MunkresRec();
        mgen.setMunkres(munkresRec);
        double[][] matrix = mgen.getMatrix(source, target);
        munkres.setGraphs(source, target);
        @SuppressWarnings("unused")
        double cost = munkres.getCosts(matrix);
        Constants.FirstUB = munkres.getBestEditPath();
        return munkres.getStarredIndices();
    }

    private double[][] ComputeCostMatrixNM(LinkedList unUsedNodes12, LinkedList unUsedNodes22, boolean isnode) {
        int n1 = unUsedNodes12.size();
        int n2 = unUsedNodes22.size();
        GraphComponent node1;
        GraphComponent node2;
        double[][] matrix = new double[n1 + 2][n2 + 2];
        for (int i = 0; i < n1; i++) {
            node1 = (GraphComponent) unUsedNodes12.get(i);
            node1.id = i;
            node1.belongtosourcegraph = true;
            for (int j = 0; j < n2; j++) {
                node2 = (GraphComponent) unUsedNodes22.get(j);
                node2.id = j;
                node2.belongtosourcegraph = false;
                matrix[i][j] = (float) Constants.costFunction.getCosts(node1, node2);
            }
        }
        for (int j = 0; j < n2; j++) {
            node2 = (GraphComponent) unUsedNodes22.get(j);
            matrix[n1][j] = (float) Constants.costFunction.getCosts(Constants.EPS_COMPONENT, node2);
        }
        for (int j = 0; j < n2; j++) {
            node2 = (GraphComponent) unUsedNodes22.get(j);
            matrix[n1 + 1][j] = (float) Constants.costFunction.getCosts(node2, Constants.EPS_COMPONENT);
        }
        for (int i = 0; i < n1; i++) {
            node1 = (GraphComponent) unUsedNodes12.get(i);
            matrix[i][n2] = (float) Constants.costFunction.getCosts(Constants.EPS_COMPONENT, node1);
        }
        for (int i = 0; i < n1; i++) {
            node1 = (GraphComponent) unUsedNodes12.get(i);
            matrix[i][n2 + 1] = (float) Constants.costFunction.getCosts(node1, Constants.EPS_COMPONENT);
        }
        return matrix;
    }

    private double[][] ComputeCostMatrix(LinkedList unUsedNodes12, LinkedList unUsedNodes22, boolean isnode) {
        int sSize = unUsedNodes12.size();
        int tSize = unUsedNodes22.size();
        int dim = sSize + tSize;
        double[][] matrix = new double[dim][dim];
        double[][] edgeMatrix;
        Node u;
        Node v;
        for (int i = 0; i < sSize; i++) {
            u = (Node) this.source.get(i);
            for (int j = 0; j < tSize; j++) {
                v = (Node) this.target.get(j);
                double costs = Constants.costFunction.getCosts(u, v);
                edgeMatrix = this.getEdgeMatrix(u, v);
                costs += this.munkresRec.getCosts(edgeMatrix);
                matrix[i][j] = costs;
            }
        }
        for (int i = sSize; i < dim; i++) {
            for (int j = 0; j < tSize; j++) {
                if ((i - sSize) == j) {
                    v = (Node) this.target.get(j);
                    double costs = Constants.costFunction.getCosts(Constants.EPS_COMPONENT, v);
                    for (int k = 0; k < v.getEdges().size(); k++) {
                        Edge e = (Edge) v.getEdges().get(k);
                        costs += Constants.costFunction.getCosts(Constants.EPS_COMPONENT, e);
                    }
                    matrix[i][j] = costs;
                } else {
                    matrix[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }
        for (int i = 0; i < sSize; i++) {
            u = (Node) this.source.get(i);
            for (int j = tSize; j < dim; j++) {
                if ((j - tSize) == i) {
                    double costs = Constants.costFunction.getCosts(u, Constants.EPS_COMPONENT);
                    for (int k = 0; k < u.getEdges().size(); k++) {
                        Edge e = (Edge) u.getEdges().get(k);
                        costs += Constants.costFunction.getCosts(e, Constants.EPS_COMPONENT);
                    }
                    matrix[i][j] = costs;
                } else {
                    matrix[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }
        for (int i = sSize; i < dim; i++) {
            for (int j = tSize; j < dim; j++) {
                matrix[i][j] = Double.POSITIVE_INFINITY;
            }
        }
        return matrix;
    }

    private double[][] getEdgeMatrix(Node u, Node v) {
        int uSize = u.getEdges().size();
        int vSize = v.getEdges().size();
        int dim = uSize + vSize;
        double[][] edgeMatrix = new double[dim][dim];
        Edge e_u;
        Edge e_v;
        for (int i = 0; i < uSize; i++) {
            e_u = (Edge) u.getEdges().get(i);
            for (int j = 0; j < vSize; j++) {
                e_v = (Edge) v.getEdges().get(j);
                double costs = Constants.costFunction.getCosts(e_u, e_v);
                edgeMatrix[i][j] = costs;
            }
        }
        for (int i = uSize; i < dim; i++) {
            for (int j = 0; j < vSize; j++) {
                if ((i - uSize) == j) {
                    e_v = (Edge) v.getEdges().get(j);
                    double costs = Constants.costFunction.getCosts(Constants.EPS_COMPONENT, e_v);
                    edgeMatrix[i][j] = costs;
                } else {
                    edgeMatrix[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }
        for (int i = 0; i < uSize; i++) {
            e_u = (Edge) u.getEdges().get(i);
            for (int j = vSize; j < dim; j++) {
                if ((j - vSize) == i) {
                    double costs = Constants.costFunction.getCosts(e_u, Constants.EPS_COMPONENT);
                    edgeMatrix[i][j] = costs;
                } else {
                    edgeMatrix[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }
        for (int i = uSize; i < dim; i++) {
            for (int j = vSize; j < dim; j++) {
                edgeMatrix[i][j] = 0.0;
            }
        }
        return edgeMatrix;
    }

    private double ComputeNbDeletion(LinkedList unUsedNodes12, int nbdeletions) {
        GraphComponent node2 = Constants.EPS_COMPONENT;
        double res = 0.0;
        for (int i = 0; i < nbdeletions; i++) {
            GraphComponent node1 = (GraphComponent) unUsedNodes12.get(i);
            res += Constants.costFunction.getCosts(node1, node2);
        }
        return res;
    }

    private double ComputeNbInsertion(LinkedList unUsedNodes12, int nbdeletions) {
        GraphComponent node2 = Constants.EPS_COMPONENT;
        double res = 0.0;
        for (int i = 0; i < nbdeletions; i++) {
            GraphComponent node1 = (GraphComponent) unUsedNodes12.get(i);
            res += Constants.costFunction.getCosts(node2, node1);
        }
        return res;
    }

    @SuppressWarnings("unused")
    private double ComputeMinSubstituionMunkres(LinkedList unUsedNodes12, LinkedList unUsedNodes22) {
        int n = Math.max(unUsedNodes12.size(), unUsedNodes22.size());
        double[][] matrix = new double[n][n];
        int[] rowsol, colsol;
        double[] u, v;
        u = new double[n];
        v = new double[n];
        rowsol = new int[n];
        colsol = new int[n];
        for (int i = 0; i < n; i++) {
            GraphComponent node1 = new GraphComponent(Constants.EPS_ID);
            if (i < unUsedNodes12.size()) {
                node1 = (GraphComponent) unUsedNodes12.get(i);
            } else {
                node1 = Constants.EPS_COMPONENT;
            }
            for (int j = 0; j < n; j++) {
                GraphComponent node2 = new GraphComponent(Constants.EPS_ID);
                if (j < unUsedNodes22.size()) {
                    node2 = (GraphComponent) unUsedNodes22.get(j);
                } else {
                    node2 = Constants.EPS_COMPONENT;
                }
                double minval = Constants.costFunction.getCosts(node1, node2);
                GraphComponent epsgc = Constants.EPS_COMPONENT;
                if (!node1.equals(Constants.EPS_COMPONENT) && !node2.equals(Constants.EPS_COMPONENT)) {
                    double valdel = Constants.costFunction.getCosts(node1, epsgc);
                    double valins = Constants.costFunction.getCosts(epsgc, node2);
                    minval = Math.min(minval, valdel + valins);
                }
                matrix[i][j] = minval;
            }
        }
        MunkresRec munkresRec;
        munkresRec = new MunkresRec();
        double cost = munkresRec.getCosts(matrix);
        return cost;
    }

    private LinkedList unUsedNodes1, unUsedNodes2;

    private LinkedList unUsedEdges1, unUsedEdges2;

    public Hashtable distortions;

    private Hashtable EdgeInverted;

    public Hashtable getEdgeInverted() {
        return EdgeInverted;
    }

    public void setEdgeInverted(Hashtable edgeInverted) {
        EdgeInverted = edgeInverted;
    }

    private Graph source, target;

    private int heuristicmethod;

    public int getHeuristicmethod() {
        return heuristicmethod;
    }

    public void setHeuristicmethod(int heuristicmethod) {
        this.heuristicmethod = heuristicmethod;
    }

    public EditPath(Graph source, Graph target) {
        this.init();
        this.source = source;
        this.target = target;
        this.unUsedNodes1.addAll(source);
        this.unUsedNodes2.addAll(target);
        this.unUsedEdges1.addAll(source.getEdges());
        this.unUsedEdges2.addAll(target.getEdges());
        this.setHeuristicmethod(NoHeuristic);
    }

    public EditPath(EditPath e) {
        this.init();
        this.source = e.getSource();
        this.target = e.getTarget();
        this.unUsedNodes1.addAll(e.getUnUsedNodes1());
        this.unUsedNodes2.addAll(e.getUnUsedNodes2());
        this.unUsedEdges1.addAll(e.getUnUsedEdges1());
        this.unUsedEdges2.addAll(e.getUnUsedEdges2());
        this.totalCosts = e.getTotalCosts();
        this.distortions.putAll(e.getDistortions());
        this.EdgeInverted.putAll(e.getEdgeInverted());
        this.noOfDistortedNodes = e.getNoOfDistortedNodes();
        this.HeuristicCosts = e.HeuristicCosts;
        this.isheuristiccomputed = e.isheuristiccomputed;
        this.heuristicmethod = e.heuristicmethod;
    }

    private void init() {
        this.unUsedEdges1 = new LinkedList();
        this.unUsedEdges2 = new LinkedList();
        this.unUsedNodes1 = new LinkedList();
        this.unUsedNodes2 = new LinkedList();
        this.distortions = new Hashtable();
        EdgeInverted = new Hashtable();
        this.totalCosts = 0.0;
        this.noOfDistortedNodes = 0;
        HeuristicCosts = 0.0;
        this.isheuristiccomputed = false;
    }

    public void addDistortion(GraphComponent sComp, GraphComponent tComp) {
        isheuristiccomputed = false;
        this.totalCosts += Constants.costFunction.getCosts(sComp, tComp);
        if (sComp.getComponentId().equals(Constants.EPS_ID)) {
            GraphComponent eps = new GraphComponent(Constants.EPS_ID);
            this.distortions.put(eps, tComp);
        } else {
            this.distortions.put(sComp, tComp);
        }
        if (sComp.isNode() == true) {
            this.noOfDistortedNodes++;
        }
        if (sComp.isNode() || tComp.isNode()) {
            this.unUsedNodes1.remove(sComp);
            Constants.edgeHandler.handleEdges(this, sComp, tComp);
        } else {
            this.unUsedEdges1.remove(sComp);
        }
        if (tComp.isNode()) {
            this.unUsedNodes2.remove(tComp);
        } else {
            this.unUsedEdges2.remove(tComp);
        }
    }

    public boolean isComplete() {
        int remaining = this.unUsedNodes1.size();
        remaining += this.unUsedNodes2.size();
        return (remaining == 0);
    }

    public boolean hasStartNodes() {
        return (this.unUsedNodes1.size() > 0);
    }

    public double getTotalCosts() {
        return totalCosts;
    }

    public LinkedList getUnUsedNodes1() {
        return unUsedNodes1;
    }

    public LinkedList getUnUsedNodes2() {
        return unUsedNodes2;
    }

    public LinkedList getUnUsedEdges1() {
        return unUsedEdges1;
    }

    public LinkedList getUnUsedEdges2() {
        return unUsedEdges2;
    }

    public Node getNext() {
        return (Node) this.unUsedNodes1.getFirst();
    }

    public Node getNextG2() {
        return (Node) this.unUsedNodes2.getFirst();
    }

    public void complete() {
        LinkedList tempList = new LinkedList();
        tempList.addAll(this.unUsedNodes2);
        Iterator iter = tempList.iterator();
        while (iter.hasNext()) {
            Node w = (Node) iter.next();
            this.addDistortion(Constants.EPS_COMPONENT, w);
        }
    }

    public Hashtable getDistortions() {
        return distortions;
    }

    public GraphComponent getStart(GraphComponent mapped) {
        Enumeration enumeration = this.distortions.keys();
        while (enumeration.hasMoreElements()) {
            GraphComponent key = (GraphComponent) enumeration.nextElement();
            GraphComponent value = (GraphComponent) this.distortions.get(key);
            if (value.equals(mapped)) {
                return key;
            }
        }
        return null;
    }

    public void printMe() {
        System.out.println("source-label: " + this.source.getId());
        System.out.println("target-label: " + this.target.getId());
        Enumeration enumeration = this.distortions.keys();
        while (enumeration.hasMoreElements()) {
            GraphComponent key = (GraphComponent) enumeration.nextElement();
            GraphComponent value = (GraphComponent) this.distortions.get(key);
            System.out.print(key.getComponentId() + "\t --> \t" + value.getComponentId() + "\t\t" + Constants.costFunction.getCosts(key, value));
            System.out.println();
        }
    }

    public boolean allUsed() {
        int remaining = this.unUsedNodes1.size();
        remaining += this.unUsedNodes2.size();
        remaining += this.unUsedEdges1.size();
        remaining += this.unUsedEdges2.size();
        if (remaining != 0) {
            System.out.println(((Edge) this.unUsedEdges2.getFirst()).getComponentId());
        }
        return (remaining == 0);
    }

    public int getNumberOfNodeOps() {
        int space = 0;
        Enumeration enumeration = this.distortions.keys();
        while (enumeration.hasMoreElements()) {
            GraphComponent key = (GraphComponent) enumeration.nextElement();
            GraphComponent value = (GraphComponent) this.distortions.get(key);
            if (key.isNode() || value.isNode()) {
                space++;
            }
        }
        return space;
    }

    public Graph getSource() {
        return source;
    }

    public Graph getTarget() {
        return target;
    }

    @SuppressWarnings("unused")
    public double ComputeHeuristicCostsAssignmentMunkres() {
        HeuristicCosts = 0.0;
        int n1, n2;
        int e1, e2;
        n1 = this.unUsedNodes1.size();
        n2 = this.unUsedNodes2.size();
        e1 = this.unUsedEdges1.size();
        e2 = this.unUsedEdges2.size();
        double leastexpensivenodesub = ComputeMinSubstituionMunkres(unUsedNodes1, unUsedNodes2);
        HeuristicCosts = leastexpensivenodesub;
        double leastexpensiveedgesub = ComputeMinSubstituionMunkres(unUsedEdges1, unUsedEdges2);
        HeuristicCosts += leastexpensiveedgesub;
        return HeuristicCosts;
    }

    public String bestMatchingNodesMapping() {
        String nodeMappings = "";
        String edgeMappings = "";
        Enumeration enumeration = this.distortions.keys();
        while (enumeration.hasMoreElements()) {
            GraphComponent key = (GraphComponent) enumeration.nextElement();
            GraphComponent value = (GraphComponent) this.distortions.get(key);
            if (key.isNode() == true || value.isNode() == true) {
                nodeMappings += String.format("%s -> %s = %s \n", key.getComponentId(), value.getComponentId(), Constants.costFunction.getCosts(key, value));
            } else {
                edgeMappings += String.format("%s -> %s = %s \n", key.getComponentId(), value.getComponentId(), Constants.costFunction.getCosts(key, value));
            }
        }
        return String.format("Nodes: \n%s\n\nEdges: \n%s\n", nodeMappings, edgeMappings);
    }

    public String g2Indices1(Graph G1) {
        String mappings = "";
        for (int i = 0; i < G1.size(); i++) {
            Node node = (Node) G1.get(i);
            String node1 = node.getComponentId().toString();
            Enumeration enumeration = this.distortions.keys();
            while (enumeration.hasMoreElements()) {
                GraphComponent key = (GraphComponent) enumeration.nextElement();
                if (key.isNode() == true) {
                    if (key.getComponentId().equals(node1)) {
                        GraphComponent value = (GraphComponent) this.distortions.get(key);
                        if (value.getComponentId().equals(Constants.EPS_ID)) {
                            mappings = mappings.concat("-1 ");
                        } else if (!value.getComponentId().equals(Constants.EPS_ID)) {
                            mappings = mappings.concat(value.getComponentId() + " ");
                        } else {
                        }
                        break;
                    }
                }
            }
        }
        return mappings;
    }

    public void setheuristiccomputed(boolean b) {
        this.isheuristiccomputed = b;
    }

    @SuppressWarnings("unused")
    public double ComputeTotalCostsWithoutDeletionAndInsertion() {
        double res = Double.MAX_VALUE;
        double count = 0;
        Enumeration enumeration = this.distortions.keys();
        while (enumeration.hasMoreElements()) {
            GraphComponent key = (GraphComponent) enumeration.nextElement();
            GraphComponent value = (GraphComponent) this.distortions.get(key);
            if (!key.getComponentId().equals(Constants.EPS_ID) && !value.getComponentId().equals(Constants.EPS_ID)) {
                if (res == Double.MAX_VALUE) {
                    res = 0;
                }
                count++;
                res += Constants.costFunction.getCosts(key, value);
            }
        }
        return res;
    }

    public double ComputeAverageCostsWithoutDeletionAndInsertion() {
        double res = 0;
        double count = 0;
        Enumeration enumeration = this.distortions.keys();
        while (enumeration.hasMoreElements()) {
            GraphComponent key = (GraphComponent) enumeration.nextElement();
            GraphComponent value = (GraphComponent) this.distortions.get(key);
            if (!key.getComponentId().equals(Constants.EPS_ID) && !value.getComponentId().equals(Constants.EPS_ID)) {
                count++;
                res += Constants.costFunction.getCosts(key, value);
            }
        }
        if (count > 0)
            return res / count;
        if (count == 0)
            return Double.MAX_VALUE;
        return res;
    }
}
