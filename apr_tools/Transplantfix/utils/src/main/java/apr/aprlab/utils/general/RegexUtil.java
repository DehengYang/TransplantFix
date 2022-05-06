package apr.aprlab.utils.general;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {

    public static String getFormattedString(String content) {
        String fomattedContent = content.replaceAll("[A-Za-z0-9_\\$]+", "ID");
        return fomattedContent;
    }

    public static List<String> splitCamelCase(String className) {
        String[] r = className.split("(?=[A-Z])");
        return Arrays.asList(r);
    }

    public static Set<String> splitCamelCaseToSet(String className) {
        return new HashSet<String>(splitCamelCase(className));
    }

    public static List<String> findAll(String str, Pattern pattern) {
        Matcher matcher = pattern.matcher(str);
        List<String> listMatches = new ArrayList<String>();
        if (matcher.find()) {
            int matchCnt = matcher.groupCount();
            for (int i = 1; i < matchCnt + 1; i++) {
                String match = matcher.group(i);
                listMatches.add(match);
            }
        }
        return listMatches;
    }

    public static List<Pair<String, String>> findAllPairs(String str, Pattern pattern) {
        Matcher matcher = pattern.matcher(str);
        List<Pair<String, String>> listMatches = new ArrayList<>();
        while (matcher.find()) {
            int matchCnt = matcher.groupCount();
            ExceptionUtil.myAssert(matchCnt == 2);
            listMatches.add(new Pair<>(matcher.group(1), matcher.group(2)));
        }
        return listMatches;
    }

    public static String remainAllNumAndLetters(String parentString) {
        return parentString.replaceAll("[^A-Za-z0-9_]", " ").replaceAll("\\s+", " ");
    }

    public static Set<String> splitPackageToSet(String packageName) {
        String[] pkgChars = packageName.split("\\.");
        return new HashSet<String>(Arrays.asList(pkgChars));
    }
}
