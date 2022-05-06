package apr.aprlab.repair.adapt.ged.action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import apr.aprlab.repair.adapt.entities.MappedStringClass;
import apr.aprlab.repair.adapt.entities.MyUnit;
import apr.aprlab.repair.adapt.ged.util.GraphComponent;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.general.Pair;
import apr.aprlab.utils.similarity.SimilarityUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GedGlobals {

    public static final Logger logger = LogManager.getLogger(GedGlobals.class);

    public static final Map<GraphComponent, Map<GraphComponent, Double>> costMap = new HashMap<>();

    public static MethodSnippet srcMethodSnippet;

    public static Map<String, String> classSubNameMapping = new HashMap<String, String>();

    public static int LARGE_INT = 10;

    public static float mapAndCompare(GraphComponent start, GraphComponent end) {
        float dist = 0;
        if (start.isNullEps()) {
            Node endNode = (Node) end;
            dist = SimilarityUtil.getNormDistance("", endNode.getAttribute().getAstString());
        } else if (end.isNullEps()) {
            Node startNode = (Node) start;
            dist = SimilarityUtil.getNormDistance(startNode.getAttribute().getAstString(), "");
        } else {
            Node startNode = (Node) start;
            Node endNode = (Node) end;
            MyUnit startUnit = startNode.getAttribute();
            MyUnit endUnit = endNode.getAttribute();
            if (startUnit.isStartThis() || endUnit.isStartThis()) {
                if (startUnit.isStartThis() && endUnit.isStartThis()) {
                    return 0;
                } else {
                    return LARGE_INT;
                }
            }
            if (startUnit.isLastReturn() || endUnit.isLastReturn()) {
                if (startUnit.isLastReturn() && endUnit.isLastReturn()) {
                    return 0;
                } else {
                    return LARGE_INT;
                }
            }
            if (startNode.getComponentIdInt() == 6 && endNode.getComponentIdInt() == 53) {
                logger.debug("");
            }
            float distThreshold = 0.4f;
            if (unitsMatched(startUnit, endUnit)) {
                List<Pair<MappedStringClass, Float>> matchedPairs = new ArrayList<>();
                List<MappedStringClass> mappedStringClasses = endUnit.getMappedStringClasses();
                for (MappedStringClass mappedStringClass : mappedStringClasses) {
                    String mappedString = mappedStringClass.getMappedString();
                    String startAstString = startUnit.getAstString().trim();
                    if (startUnit.isIf() && endUnit.isIf()) {
                        if (startAstString.contains(mappedString) || mappedString.contains(startAstString)) {
                            dist = distThreshold;
                        } else {
                            dist = SimilarityUtil.getNormDistance(startAstString, mappedString.trim());
                        }
                    } else {
                        dist = SimilarityUtil.getNormDistance(startAstString, mappedString.trim());
                    }
                    matchedPairs.add(new Pair<>(mappedStringClass, dist));
                }
                matchedPairs.sort(new Comparator<Pair<MappedStringClass, Float>>() {

                    @Override
                    public int compare(Pair<MappedStringClass, Float> o1, Pair<MappedStringClass, Float> o2) {
                        return Float.compare(o1.getRight(), o2.getRight());
                    }
                });
                dist = matchedPairs.get(0).getRight();
                if (dist > distThreshold) {
                    dist = LARGE_INT;
                } else {
                    endUnit.addExtraNewString(startNode, startUnit.getAstString().trim());
                }
            } else {
                dist = LARGE_INT;
            }
        }
        return dist;
    }

    private static boolean unitsMatched(MyUnit startUnit, MyUnit endUnit) {
        boolean ifOrLoopMatched = startUnit.isIf() == endUnit.isIf();
        boolean exprStmtNotMatched = exprStmtNotMatched(startUnit, endUnit);
        return ifOrLoopMatched && !exprStmtNotMatched;
    }

    private static boolean exprStmtNotMatched(MyUnit startUnit, MyUnit endUnit) {
        boolean exprStmtNotMatched = false;
        if (startUnit.isIf() && !startUnit.isWhile()) {
            if (!endUnit.isIf()) {
                exprStmtNotMatched = true;
            }
        }
        if (endUnit.isIf() && !endUnit.isWhile()) {
            if (!startUnit.isIf()) {
                exprStmtNotMatched = true;
            }
        }
        return exprStmtNotMatched;
    }

    public static void getMappings(MethodSnippet srcMs, MethodSnippet dstMs) {
        srcMethodSnippet = srcMs;
        dstMs.getEntityInMethod().clearMappings();
        dstMs.clearMyUnitMappings();
        dstMs.getEntityInMethod().getMappings(srcMs.getEntityInMethod());
    }
}
