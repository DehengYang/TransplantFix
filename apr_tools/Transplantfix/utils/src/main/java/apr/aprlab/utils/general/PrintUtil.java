package apr.aprlab.utils.general;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;

public class PrintUtil {

    public static <T> void printList(List<T> list, int maxSize, String message) {
        if (maxSize < list.size()) {
            printList(list.subList(0, maxSize));
        } else {
            printList(list);
        }
    }

    public static <T> void printList(List<T> list, String message) {
        System.out.format("%s\n", message);
        printList(list);
    }

    public static <T> void printList(List<T> list) {
        String string = listToString(list);
        System.out.println(string);
    }

    public static <T> String listToString(List<T> list, String message) {
        return String.format("%s:\n%s\n", message, listToString(list));
    }

    public static <T> String listToString(List<T> list) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (T t : list) {
            sb.append(String.format("[%s] %s\n", i++, t.toString()));
        }
        if (list.isEmpty()) {
            sb.append("Empty List.\n");
        } else {
            sb.append("\n");
        }
        return sb.toString();
    }

    public static <T> String listToString(Set<T> set) {
        return listToString(new ArrayList<T>(set));
    }

    public static <T> String listToString(Set<T> set, String message) {
        return String.format("%s:\n%s\n", message, listToString(set));
    }

    public static <T> String listToStringForStorage(List<T> list) {
        StringBuilder sb = new StringBuilder();
        for (T t : list) {
            sb.append(String.format("%s\n", t.toString()));
        }
        return sb.toString();
    }

    public static <T> String mapToStr(Map<String, T> map) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String t : map.keySet()) {
            sb.append(String.format("[%s] key: %s; value: %s\n", i++, t, map.get(t).toString()));
        }
        if (map.isEmpty()) {
            sb.append("Empty map.\n");
        } else {
            sb.append("\n");
        }
        return sb.toString();
    }

    public static <T> String mapToStr(Map<String, T> map, String message) {
        return String.format("%s:\n%s\n", message, mapToStr(map));
    }

    public static <T> String listmapToStr(Map<String, List<T>> map, String message) {
        return String.format("%s:\n%s\n", message, listmapToStr(map));
    }

    public static <T> String listmapToStr(Map<String, List<T>> map) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String t : map.keySet()) {
            sb.append(String.format("[%s] key: %s; value (size: %s): %s", i++, t, map.get(t).size(), listToString(map.get(t))));
        }
        if (map.isEmpty()) {
            sb.append("Empty map.\n");
        } else {
            sb.append("\n");
        }
        return sb.toString();
    }

    public static <T> String listToString(List<T> list, int maxSize) {
        if (maxSize < list.size()) {
            return listToString(list.subList(0, maxSize));
        } else {
            return listToString(list);
        }
    }

    public static <T> String mapToStrForNodeKey(Map<ASTNode, T> map, String message) {
        Map<String, T> stringMap = new HashMap<String, T>();
        for (ASTNode t : map.keySet()) {
            stringMap.put(t.toString(), map.get(t));
        }
        return mapToStr(stringMap, message);
    }
}
