package apr.aprlab.repair.adapt.ged;

import java.util.TimerTask;
import apr.aprlab.repair.adapt.ged.util.Edge;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.Node;

@SuppressWarnings({ "unused" })
public class MatrixGenerator {

    static TimerTask task;

    private void inittimer() {
        task = new TimerTask() {

            @Override
            public void run() {
                Munkres.timeconstraint = true;
            }
        };
        Constants.timer.scheduleAtFixedRate(task, Constants.timeconstraint, Constants.timeconstraint);
    }

    private Graph source, target;

    private MunkresRec munkresRec;

    public double[][] getMatrix(Graph sourceGraph, Graph targetGraph) {
        this.inittimer();
        this.source = sourceGraph;
        this.target = targetGraph;
        int sSize = sourceGraph.size();
        int tSize = targetGraph.size();
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
                matrix[i][j] = 0.0;
            }
        }
        return matrix;
    }

    public double getCVE_DI_old(Graph sourceGraph, Graph targetGraph) {
        this.source = sourceGraph;
        this.target = targetGraph;
        int sSize = sourceGraph.size();
        int tSize = targetGraph.size();
        int dim = sSize + tSize;
        Node u;
        Node v;
        double vedi = 0;
        for (int i = sSize; i < dim; i++) {
            for (int j = 0; j < tSize; j++) {
                if ((i - sSize) == j) {
                    v = (Node) this.target.get(j);
                    double costs = Constants.costFunction.getCosts(Constants.EPS_COMPONENT, v);
                    for (int k = 0; k < v.getEdges().size(); k++) {
                        Edge e = (Edge) v.getEdges().get(k);
                        costs += Constants.costFunction.getCosts(Constants.EPS_COMPONENT, e);
                    }
                    vedi += costs;
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
                    vedi += costs;
                }
            }
        }
        return vedi;
    }

    public double getCVE_DI(Graph sourceGraph, Graph targetGraph) {
        this.source = sourceGraph;
        this.target = targetGraph;
        int sSize = sourceGraph.size();
        int tSize = targetGraph.size();
        int dim = sSize + tSize;
        Node u;
        Node v;
        double vedi = 0;
        for (int i = 0; i < source.size(); i++) {
            u = (Node) this.source.get(i);
            vedi += Constants.costFunction.getCosts(u, Constants.EPS_COMPONENT);
            for (int k = 0; k < u.getEdges().size(); k++) {
                Edge e = (Edge) u.getEdges().get(k);
                vedi += Constants.costFunction.getCosts(e, Constants.EPS_COMPONENT);
            }
        }
        for (int i = 0; i < target.size(); i++) {
            v = (Node) this.target.get(i);
            vedi += Constants.costFunction.getCosts(Constants.EPS_COMPONENT, v);
            for (int k = 0; k < v.getEdges().size(); k++) {
                Edge e = (Edge) v.getEdges().get(k);
                vedi += Constants.costFunction.getCosts(Constants.EPS_COMPONENT, e);
            }
        }
        return vedi;
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

    public double[][] getSimpleMatrix(Graph sourceGraph, Graph targetGraph) {
        this.source = sourceGraph;
        this.target = targetGraph;
        double[][] matrix = new double[this.source.size()][this.target.size()];
        double[][] edgeMatrix;
        Node u;
        Node v;
        for (int i = 0; i < this.source.size(); i++) {
            u = (Node) this.source.get(i);
            for (int j = 0; j < this.target.size(); j++) {
                v = (Node) this.target.get(i);
                double costs = Constants.costFunction.getCosts(u, v);
                matrix[i][j] = costs;
            }
        }
        return matrix;
    }

    public float[][] getCQ1eMatrix(Graph sourceGraph, Graph targetGraph) {
        this.source = sourceGraph;
        this.target = targetGraph;
        float[][] matrix = new float[this.source.size()][this.target.size()];
        double[][] edgeMatrix;
        Node u;
        Node v;
        for (int i = 0; i < this.source.size(); i++) {
            u = (Node) this.source.get(i);
            for (int j = 0; j < this.target.size(); j++) {
                v = (Node) this.target.get(j);
                double costs = Constants.costFunction.getCosts(u, v);
                edgeMatrix = this.getEdgeMatrix(u, v);
                costs += this.munkresRec.getCosts(edgeMatrix);
                double insertcost = 0;
                insertcost = Constants.costFunction.getCosts(Constants.EPS_COMPONENT, v);
                for (int k = 0; k < v.getEdges().size(); k++) {
                    Edge e = (Edge) v.getEdges().get(k);
                    insertcost += Constants.costFunction.getCosts(Constants.EPS_COMPONENT, e);
                }
                double delcost = Constants.costFunction.getCosts(u, Constants.EPS_COMPONENT);
                for (int k = 0; k < u.getEdges().size(); k++) {
                    Edge e = (Edge) u.getEdges().get(k);
                    delcost += Constants.costFunction.getCosts(e, Constants.EPS_COMPONENT);
                }
                matrix[i][j] = (float) (1000000 + costs - (insertcost + delcost));
            }
        }
        return matrix;
    }

    private double[][] getSimpleEdgeMatrix(Node u, Node v) {
        int uSize = u.getEdges().size();
        int vSize = v.getEdges().size();
        double[][] edgeMatrix = new double[uSize][vSize];
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
        return edgeMatrix;
    }

    public void setMunkres(MunkresRec munkresRec) {
        this.munkresRec = munkresRec;
    }

    public void printMatrix(double[][] m) {
        System.out.println("MATRIX:");
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                if (m[i][j] < Double.POSITIVE_INFINITY) {
                    System.out.print(m[i][j] + "\t");
                } else {
                    System.out.print("inf\t");
                }
            }
            System.out.println();
        }
    }
}
