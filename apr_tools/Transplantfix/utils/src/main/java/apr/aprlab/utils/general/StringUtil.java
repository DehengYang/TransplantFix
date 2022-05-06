package apr.aprlab.utils.general;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StringUtil {

    public static final Logger logger = LogManager.getLogger(StringUtil.class);

    public static String getShortName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        } else {
            return className;
        }
    }

    public static String getFormattedPatchDiff(String patchDiff) {
        String formattedPatchDiff = "";
        for (String line : patchDiff.split("\n")) {
            if (line.startsWith("--- ")) {
                ExceptionUtil.myAssert(line.contains(".java"));
                int index = line.indexOf(".java");
                line = line.substring(0, index + 5);
            } else if (line.startsWith("+++ ")) {
                line = "";
            }
            formattedPatchDiff += line + "\n";
        }
        return formattedPatchDiff;
    }

    public static boolean isPrimitiveType(String buggyReturnType) {
        List<String> primitiveList = new ArrayList<String>(Arrays.asList("byte", "short", "int", "long", "float", "double", "boolean", "char", "void"));
        boolean is = primitiveList.contains(buggyReturnType);
        return is;
    }

    public static String getPrimitiveString(String buggyReturnType) {
        String returnString = "";
        if (buggyReturnType.equals("byte")) {
            returnString = "0";
        } else if (buggyReturnType.equals("short")) {
            returnString = "0";
        } else if (buggyReturnType.equals("int")) {
            returnString = "0";
        } else if (buggyReturnType.equals("long")) {
            returnString = "0L";
        } else if (buggyReturnType.equals("float")) {
            returnString = "0.0f";
        } else if (buggyReturnType.equals("double")) {
            returnString = "0.0d";
        } else if (buggyReturnType.equals("boolean")) {
            returnString = "true";
        } else if (buggyReturnType.equals("char")) {
            returnString = "0";
        } else if (buggyReturnType.equals("void")) {
            returnString = "";
        } else {
            ExceptionUtil.raise("impossible type");
        }
        return returnString;
    }

    public static String getFileName(String filePath) {
        String fileName = new File(filePath).getName();
        ExceptionUtil.myAssert(fileName.endsWith(".java"));
        fileName = fileName.substring(0, fileName.length() - ".java".length());
        return fileName;
    }

    public static String getPackageName(String className) {
        if (className.contains(".")) {
            int lastIndex = className.lastIndexOf(".");
            String packageName = className.substring(0, lastIndex);
            return packageName;
        }
        ExceptionUtil.raise();
        return null;
    }

    public static String substring(String fileString, int startPos, int endPos) {
        if (startPos > endPos) {
            logger.error("substring error: {} > {}", startPos, endPos);
            return "";
        }
        String subString = fileString.substring(startPos, endPos);
        return subString;
    }
}
