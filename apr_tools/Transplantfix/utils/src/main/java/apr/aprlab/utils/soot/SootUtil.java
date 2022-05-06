package apr.aprlab.utils.soot;

import apr.aprlab.utils.general.RegexUtil;
import apr.aprlab.utils.graph.ddg.MethodCall;
import soot.Unit;
import soot.jimple.internal.JIfStmt;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SootUtil {

    public static final Logger logger = LogManager.getLogger(SootUtil.class);

    public static String parseDefineString(String string) {
        if (isMethodCall(string)) {
            return parseMethodCallString(string);
        } else if (isVarCall(string)) {
            return parseVarCallString(string);
        } else {
            return string;
        }
    }

    private static String parseVarCallString(String string) {
        List<String> matches = RegexUtil.findAll(string, Pattern.compile("(.*?)\\.<.* (.*?)>"));
        string = matches.get(0) + "." + matches.get(1);
        return string;
    }

    private static boolean isVarCall(String string) {
        boolean is = !isMethodCall(string) && string.contains(".<");
        return is;
    }

    private static boolean isMethodCall(String string) {
        boolean is = string.contains("virtualinvoke") || string.contains("specialinvoke") || string.contains("interfaceinvoke") || string.contains("staticinvoke");
        return is;
    }

    public static String parseMethodCallString(String unitString) {
        MethodCall mc = new MethodCall(unitString);
        return "invoke " + mc.toString();
    }

    public static void extractMethodCalls(Unit unit, List<MethodCall> methodCalls) {
        if (unit instanceof JIfStmt) {
            return;
        }
        MethodCall mc = getMethodCallInfo(unit);
        if (mc != null) {
            mc.setLineNo(unit.getJavaSourceStartLineNumber());
            methodCalls.add(mc);
        }
    }

    public static MethodCall getMethodCallInfo(Unit unit) {
        String unitString = unit.toString();
        if (!unitString.contains("invoke ")) {
            return null;
        }
        return new MethodCall(unit.toString());
    }

    public static MethodCall getMethodCallInfo(String methodCallString) {
        if (!methodCallString.contains("invoke ")) {
            return null;
        }
        return new MethodCall(methodCallString);
    }
}
