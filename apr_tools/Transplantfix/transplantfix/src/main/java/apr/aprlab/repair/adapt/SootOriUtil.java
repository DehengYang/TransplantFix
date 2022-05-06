package apr.aprlab.repair.adapt;

import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.graph.soot.SootRunner;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class SootOriUtil {

    public static SootClass getTargetClass(String className) {
        for (SootClass cls : Scene.v().getClasses()) if (cls.getName().equals(className))
            return cls;
        return null;
    }

    public static SootMethod getTargetMethod(String className, MethodSnippet ms) {
        SootClass sootClass = null;
        if (Globals.sootClassMap.containsKey(className)) {
            sootClass = Globals.sootClassMap.get(className);
        } else {
            int jdkVersion = 7;
            if (Globals.jvmPath.contains("jdk1.8")) {
                jdkVersion = 8;
            }
            SootRunner.runTargetClass(Globals.deps, className, jdkVersion);
            sootClass = getTargetClass(className);
            ExceptionUtil.assertNotNull(sootClass);
            Globals.sootClassMap.put(className, sootClass);
        }
        for (SootMethod sootMethod : sootClass.getMethods()) {
            if (!hasSameName(sootMethod, ms)) {
                continue;
            }
            if (inScope(sootMethod, ms)) {
                return sootMethod;
            }
        }
        return null;
    }

    private static boolean inScope(SootMethod sootMethod, MethodSnippet ms) {
        if (!sootMethod.hasActiveBody())
            return false;
        int mtdStart = sootMethod.getJavaSourceStartLineNumber();
        int mtdEnd = getMethodEndLine(sootMethod);
        int msStart = ms.getRange().getStartLineNo();
        int msEnd = ms.getRange().getEndLineNo();
        if (RangeUtil.rangeContains(mtdStart, mtdEnd, msStart, msEnd) >= 0 || RangeUtil.rangeContains(msStart, msEnd, mtdStart, mtdEnd) >= 0) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean hasSameName(SootMethod sootMethod, MethodSnippet ms) {
        if (ms.getMethodDeclaration().isConstructor()) {
            return sootMethod.getName().equals("<init>");
        } else {
            return ms.getMethodName().equals(sootMethod.getName());
        }
    }

    private static int getMethodEndLine(SootMethod sootMethod) {
        int end = -2;
        for (Unit unit : sootMethod.getActiveBody().getUnits()) {
            int line = unit.getJavaSourceStartLineNumber();
            if (line > end)
                end = line;
        }
        return end;
    }
}
