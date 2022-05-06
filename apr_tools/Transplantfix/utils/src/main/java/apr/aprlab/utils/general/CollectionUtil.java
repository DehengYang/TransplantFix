package apr.aprlab.utils.general;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CollectionUtil {

    public static final Logger logger = LogManager.getLogger(CollectionUtil.class);

    public static <T> void addToMap(Map<String, List<T>> map, String key, T value) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<T>());
        }
        map.get(key).add(value);
    }

    public static <T> List<T> getIntersection(List<T> nodes, List<T> nodes2) {
        List<T> intersections = new ArrayList<T>();
        for (T node : nodes) {
            if (nodes2.contains(node)) {
                intersections.add(node);
            }
        }
        return intersections;
    }

    public static <T> void addToList(List<T> srcList, List<T> toBeAdded) {
        for (T element : toBeAdded) {
            if (!srcList.contains(element)) {
                srcList.add(element);
            }
        }
    }

    public static <T> void removeDuplicates(List<T> elements) {
        List<T> newList = new ArrayList<T>();
        List<Integer> duplicateIndexes = new ArrayList<Integer>();
        for (int i = 0; i < elements.size(); i++) {
            T element = elements.get(i);
            if (!newList.contains(element)) {
                newList.add(element);
            } else {
                duplicateIndexes.add(i);
            }
        }
        if (!duplicateIndexes.isEmpty()) {
            logger.debug("removed duplicates: {}", duplicateIndexes);
        }
        elements.clear();
        elements.addAll(newList);
    }

    public static <T> LinkedList<T> removeDuplicates(LinkedList<T> elements) {
        LinkedList<T> newList = new LinkedList<T>();
        List<Integer> duplicateIndexes = new ArrayList<Integer>();
        for (int i = 0; i < elements.size(); i++) {
            T element = elements.get(i);
            if (!newList.contains(element)) {
                newList.add(element);
            } else {
                duplicateIndexes.add(i);
            }
        }
        if (!duplicateIndexes.isEmpty()) {
            logger.debug("removed duplicates: {}", duplicateIndexes);
        }
        return newList;
    }

    public static <T> List<T> getUniqueInSrc(List<T> srcList, List<T> dstList) {
        List<T> uniqueInSrc = new ArrayList<T>();
        for (T node : srcList) {
            if (!dstList.contains(node)) {
                uniqueInSrc.add(node);
            }
        }
        return uniqueInSrc;
    }

    public static <T> List<T> getIntersection(Set<T> keySet, Set<T> keySet2) {
        return getIntersection(new ArrayList<T>(keySet), new ArrayList<T>(keySet2));
    }

    public static <T> boolean containsDstList(List<T> src, List<T> dst) {
        for (T node : dst) {
            if (!src.contains(node)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsStringTrim(List<String> strings, String dstString) {
        for (String string : strings) {
            if (string.trim().equals(dstString.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsStringTrimAllWhiteSpaces(List<String> strings, String dstString) {
        dstString = dstString.replace(" ", "");
        for (String string : strings) {
            string = string.replace(" ", "");
            if (string.trim().equals(dstString.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsSameString(String[] strings, String dst) {
        return containsSameString(strings, dst, true);
    }

    public static boolean containsSameString(String[] strings, String dst, boolean ignoreCase) {
        for (String string : strings) {
            if (string.equalsIgnoreCase(dst)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsPartString(String[] strings, String dst) {
        return containsSameString(strings, dst, true);
    }

    public static boolean containsPartString(String[] strings, String dst, boolean ignoreCase) {
        for (String string : strings) {
            if (string.toLowerCase().contains(dst.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
