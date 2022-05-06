package apr.aprlab.repair.adapt.ged.patch;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.Pair;

public class PatchRankingUtil {

    public static final Logger logger = LogManager.getLogger(PatchRankingUtil.class);

    public static String rankingValidPatches(List<String> validPatches) {
        int leastDel = Integer.MAX_VALUE;
        int leastAdd = Integer.MAX_VALUE;
        int finalIndex = 0;
        for (int i = 0; i < validPatches.size(); i++) {
            String patch = validPatches.get(i);
            Pair<Integer, Integer> delAndAddSize = parsePatchDelAndAdd(patch);
            if (delAndAddSize.getLeft() < leastDel) {
                finalIndex = i;
                leastDel = delAndAddSize.getLeft();
                leastAdd = delAndAddSize.getRight();
            }else if (delAndAddSize.getLeft() == leastDel) {
                if (delAndAddSize.getRight() < leastAdd) {
                    finalIndex = i;
                    leastDel = delAndAddSize.getLeft();
                    leastAdd = delAndAddSize.getRight();
                }
            }
        }
        String finalPatch = validPatches.get(finalIndex);
        return finalPatch;
    }

    private static Pair<Integer, Integer> parsePatchDelAndAdd(String patch) {
        List<String> delStrings = new ArrayList<String>();
        List<String> addStrings = new ArrayList<String>();
        for (String patchLine : patch.split("\n")) {
            if (patchLine.startsWith("--- ") || patchLine.startsWith("+++ ")) {
                continue;
            }
            if (patchLine.startsWith("+")) {
                if (patchLine.substring(1).trim().length() != 0) {
                    addStrings.add(patchLine.substring(1).trim());
                }
            }
            if (patchLine.startsWith("-")) {
                if (patchLine.substring(1).trim().length() != 0) {
                    delStrings.add(patchLine.substring(1).trim());
                }
            }
        }
        List<String> commonStrings = CollectionUtil.getIntersection(delStrings, addStrings);
        delStrings.removeAll(commonStrings);
        addStrings.removeAll(commonStrings);
        Pair<Integer, Integer> delAndAddSize = new Pair<Integer, Integer>(delStrings.size(), addStrings.size());
        logger.debug("patch: {}, delAndAddSize: {}", patch, delAndAddSize);
        return delAndAddSize;
    }
}
