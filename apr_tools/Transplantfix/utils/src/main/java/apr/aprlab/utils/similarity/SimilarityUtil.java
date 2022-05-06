package apr.aprlab.utils.similarity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.simmetrics.StringMetric;
import org.simmetrics.StringMetrics;
import org.simmetrics.metrics.CosineSimilarity;
import org.simmetrics.metrics.JaccardSimilarity;
import org.simmetrics.metrics.Levenshtein;
import apr.aprlab.utils.general.RegexUtil;

public class SimilarityUtil {

    public static float compare(String a, String b) {
        StringMetric metric = StringMetrics.levenshtein();
        float similarity = metric.compare(a, b);
        return similarity;
    }

    public static float distance(String a, String b) {
        Levenshtein metric = new Levenshtein();
        float distance = metric.distance(a, b);
        return distance;
    }

    public static float getNormDistance(String a, String b) {
        return 1 - compare(a, b);
    }

    public static float getCosineSimilarity(List<String> srcList, List<String> dstList) {
        CosineSimilarity<String> cosineSimilarity = new CosineSimilarity<String>();
        float sim = cosineSimilarity.compare(new HashSet<String>(srcList), new HashSet<String>(dstList));
        return sim;
    }

    public static float getCosineSimilarity(String src, String dst) {
        return getCosineSimilarity(RegexUtil.splitCamelCase(src), RegexUtil.splitCamelCase(dst));
    }

    public static float getJaccardSimilarity(String src, String dst) {
        JaccardSimilarity<String> jaccard = new JaccardSimilarity<String>();
        Set<String> clsSet = RegexUtil.splitCamelCaseToSet(src);
        Set<String> otherClsSet = RegexUtil.splitCamelCaseToSet(dst);
        float sim = jaccard.compare(clsSet, otherClsSet);
        return sim;
    }

    public static float getJaccardSimilarity(List<String> dstParas, List<String> srcParas) {
        JaccardSimilarity<String> jaccard = new JaccardSimilarity<String>();
        float sim = jaccard.compare(new HashSet<String>(dstParas), new HashSet<String>(srcParas));
        return sim;
    }
}
