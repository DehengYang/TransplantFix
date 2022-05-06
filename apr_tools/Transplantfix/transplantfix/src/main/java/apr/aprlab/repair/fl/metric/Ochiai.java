package apr.aprlab.repair.fl.metric;

public class Ochiai implements RankingMetric {

    @Override
    public double value(int ef, int ep, int nf, int np) {
        if (ef + ep == 0 || ef + nf == 0) {
            return 0;
        }
        return ef / Math.sqrt((ef + ep) * (ef + nf));
    }
}
