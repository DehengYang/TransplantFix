package apr.aprlab.repair.fl.metric;

public interface RankingMetric {

    double value(int ef, int ep, int nf, int np);
}
