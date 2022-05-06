package apr.aprlab.utils.graph.ddg;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.RegexUtil;

public class MethodCall {

    private int lineNo;

    private String methodName;

    private String receiverType;

    private String classType;

    private String returnType;

    private List<String> parameterTypes;

    private List<String> realParameterTypes;

    private boolean isStaticInvoke = false;

    public void init(String methodName, String receiverType, String classType, String returnType, List<String> parameterTypes, List<String> realParameterTypes) {
        this.methodName = methodName;
        this.receiverType = receiverType;
        this.classType = classType;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.realParameterTypes = realParameterTypes;
    }

    public MethodCall(String unitString) {
        List<String> parameterTypes = null;
        List<String> realParameterTypes = null;
        if (unitString.contains("virtualinvoke") || unitString.contains("specialinvoke") || unitString.contains("interfaceinvoke")) {
            List<String> methodCallInfo = RegexUtil.findAll(unitString, Pattern.compile("invoke (.*?).<(.*?): (.*?) (.*?)\\((.*?)\\)>\\((.*?)\\)"));
            ExceptionUtil.assertFalse(methodCallInfo.isEmpty(), unitString);
            String receiverType = methodCallInfo.get(0);
            String classType = methodCallInfo.get(1);
            String returnType = methodCallInfo.get(2);
            String methodName = methodCallInfo.get(3);
            parameterTypes = Arrays.asList(methodCallInfo.get(4).split(","));
            realParameterTypes = Arrays.asList(methodCallInfo.get(5).split(","));
            init(methodName, receiverType, classType, returnType, parameterTypes, realParameterTypes);
        } else if (unitString.contains("staticinvoke")) {
            isStaticInvoke = true;
            List<String> methodCallInfo = RegexUtil.findAll(unitString, Pattern.compile("invoke <(.*?): (.*?) (.*?)\\((.*?)\\)>\\((.*?)\\)"));
            ExceptionUtil.assertFalse(methodCallInfo.isEmpty(), unitString);
            String receiverType = methodCallInfo.get(0);
            String classType = methodCallInfo.get(0);
            String returnType = methodCallInfo.get(1);
            String methodName = methodCallInfo.get(2);
            parameterTypes = Arrays.asList(methodCallInfo.get(3).split(","));
            realParameterTypes = Arrays.asList(methodCallInfo.get(4).split(","));
            init(methodName, receiverType, classType, returnType, parameterTypes, realParameterTypes);
        } else {
            ExceptionUtil.raise("not implemented yet: %s", unitString);
        }
    }

    public String getReceiverType() {
        return receiverType;
    }

    public String getClassType() {
        return classType;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        String paraString = getParaString(parameterTypes);
        String string = "";
        if (receiverType == null) {
            string = String.format("<%s: %s %s(%s)>", classType, returnType, methodName, paraString);
        } else {
            string = String.format("%s.<%s: %s %s(%s)>", receiverType, classType, returnType, methodName, paraString);
        }
        return string;
    }

    private String getParaString(List<String> parameterTypes) {
        String paraString = "";
        for (String para : parameterTypes) {
            paraString += para + ",";
        }
        if (paraString.endsWith(",")) {
            paraString = paraString.substring(0, paraString.length() - 1);
        }
        return paraString;
    }

    public String getString() {
        String paraString = getParaString(realParameterTypes);
        String methodString = methodName;
        if (methodName.equals("<init>")) {
            if (receiverType == null) {
                ExceptionUtil.raise();
            } else {
                methodString = "";
            }
        } else {
            if (receiverType != null) {
                methodString = "." + methodName;
            }
        }
        String string = "";
        if (receiverType == null) {
            string = String.format("%s(%s)", methodString, paraString);
        } else {
            string = String.format("%s%s(%s)", receiverType, methodString, paraString);
        }
        return string;
    }

    public String getMethodCallSignature() {
        String paraString = getParaString(parameterTypes);
        String sig = String.format("%s %s(%s)", returnType, methodName, paraString);
        return sig;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public boolean isStaticInvoke() {
        return isStaticInvoke;
    }
}
