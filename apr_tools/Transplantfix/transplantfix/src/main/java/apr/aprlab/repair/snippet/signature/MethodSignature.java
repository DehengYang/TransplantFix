package apr.aprlab.repair.snippet.signature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.simmetrics.metrics.JaccardSimilarity;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.RegexUtil;
import apr.aprlab.utils.general.StringUtil;
import apr.aprlab.utils.simple.graph.Vertex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MethodSignature {

    public static final Logger logger = LogManager.getLogger(MethodSignature.class);

    private String className;

    private String packageName;

    private String superClassName;

    private List<String> interfaceNameList = new ArrayList<String>();

    private Javadoc javadoc;

    private List<ASTNode> modifierList;

    private List<SingleVariableDeclaration> parameters;

    private Type returnType;

    private SimpleName methodName;

    private List<SimpleType> throwExpList;

    @SuppressWarnings("unchecked")
    public MethodSignature(MethodDeclaration md, String className2) {
        ITypeBinding binding = null;
        TypeDeclaration clazz = ASTUtil.getParentTd(md);
        if (clazz != null) {
            binding = clazz.resolveBinding();
            this.className = binding.getQualifiedName();
            this.packageName = binding.getPackage().getName();
            this.superClassName = ASTUtil.getSuperClassName(binding);
            this.interfaceNameList = ASTUtil.getInterfaceNameList(binding);
        } else {
            this.className = className2;
            this.packageName = StringUtil.getPackageName(className2);
            this.superClassName = null;
        }
        this.javadoc = md.getJavadoc();
        this.modifierList = md.modifiers();
        this.parameters = md.parameters();
        this.returnType = md.getReturnType2();
        this.methodName = md.getName();
        this.throwExpList = md.thrownExceptionTypes();
    }

    public Javadoc getJavadoc() {
        return javadoc;
    }

    public List<ASTNode> getModifierList() {
        return modifierList;
    }

    public List<SingleVariableDeclaration> getParameters() {
        return parameters;
    }

    public Type getReturnType() {
        return returnType;
    }

    public String getReturnTypeString() {
        if (returnType == null) {
            return "null";
        }
        return returnType.toString();
    }

    public SimpleName getMethodName() {
        return methodName;
    }

    public List<SimpleType> getThrowExpList() {
        return throwExpList;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public String simplifyClassName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        } else {
            return className;
        }
    }

    public String getCompareString() {
        return getCompareString(true);
    }

    public String getCompareString(boolean containJavaDoc) {
        StringBuilder sb = new StringBuilder();
        sb.append(simplifyClassName(className) + " ");
        sb.append(simplifyClassName(packageName) + " ");
        if (superClassName != null) {
            sb.append(simplifyClassName(superClassName) + " ");
        }
        for (String interfaceName : interfaceNameList) {
            sb.append(simplifyClassName(interfaceName) + " ");
        }
        sb.append(methodName + " ");
        if (containJavaDoc) {
            if (javadoc != null) {
                sb.append(javadoc.toString() + " ");
            }
        }
        for (ASTNode modifier : modifierList) {
            if (modifier instanceof Modifier) {
                sb.append(((Modifier) modifier).getKeyword() + " ");
            } else if (modifier instanceof Annotation) {
                sb.append(((Annotation) modifier).getTypeName() + " ");
            }
        }
        for (SingleVariableDeclaration svd : parameters) {
            sb.append(svd.getType() + " ");
        }
        if (returnType != null) {
            sb.append(returnType.toString() + " ");
        }
        for (SimpleType st : throwExpList) {
            sb.append(st.getName() + " ");
        }
        return sb.toString();
    }

    public List<String> getSignatureString() {
        List<String> list = new ArrayList<String>();
        list.addAll(RegexUtil.splitCamelCase(simplifyClassName(className)));
        list.addAll(RegexUtil.splitCamelCase(methodName.toString()));
        for (SingleVariableDeclaration svd : parameters) {
            list.addAll(RegexUtil.splitCamelCase(svd.getType().toString()));
        }
        if (returnType != null) {
            list.addAll(RegexUtil.splitCamelCase(returnType.toString()));
        }
        for (SimpleType st : throwExpList) {
            list.addAll(RegexUtil.splitCamelCase(st.getName().toString()));
        }
        return list;
    }

    private float computePureMethodSignatureWithJaccard(MethodSignature ms) {
        JaccardSimilarity<String> jaccard = new JaccardSimilarity<String>();
        float clsSim = 0.0f;
        float pkgSim = 0.0f;
        if (Globals.considerSiblingRelation && Globals.siblingsAndParents.contains(ms.getClassName())) {
            clsSim = 1.0f;
            pkgSim = 1.0f;
        } else {
            Set<String> clsSet = RegexUtil.splitCamelCaseToSet(simplifyClassName(className));
            Set<String> otherClsSet = RegexUtil.splitCamelCaseToSet(simplifyClassName(ms.getClassName()));
            clsSim = jaccard.compare(clsSet, otherClsSet);
            Set<String> pkgSet = RegexUtil.splitPackageToSet(packageName);
            Set<String> otherPkgSet = RegexUtil.splitPackageToSet(ms.getPackageName());
            pkgSim = jaccard.compare(pkgSet, otherPkgSet);
        }
        Set<String> methodSet = RegexUtil.splitCamelCaseToSet(methodName.toString());
        Set<String> otherMethodSet = RegexUtil.splitCamelCaseToSet(ms.getMethodName().toString());
        float methodSim = jaccard.compare(methodSet, otherMethodSet);
        List<String> paras = new ArrayList<String>();
        for (SingleVariableDeclaration svd : parameters) {
            paras.addAll(RegexUtil.splitCamelCase(svd.getType().toString()));
        }
        List<String> otherParas = new ArrayList<String>();
        for (SingleVariableDeclaration svd : ms.getParameters()) {
            otherParas.addAll(RegexUtil.splitCamelCase(svd.getType().toString()));
        }
        float paraSim = jaccard.compare(new HashSet<String>(paras), new HashSet<String>(otherParas));
        float sum = methodSim + paraSim + clsSim + pkgSim;
        float sim = sum / 4;
        return sim;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("className: " + className + " ");
        sb.append("packageName: " + packageName + " ");
        if (superClassName != null) {
            sb.append("superClassName: " + superClassName + " ");
        }
        for (String interfaceName : interfaceNameList) {
            sb.append("interfaceName: " + interfaceName + " ");
        }
        if (javadoc != null) {
            sb.append("javadoc: " + javadoc.toString() + " ");
        }
        sb.append("modifierList: ");
        for (ASTNode modifier : modifierList) {
            if (modifier instanceof Modifier) {
                sb.append(((Modifier) modifier).getKeyword() + " ");
            } else if (modifier instanceof Annotation) {
                sb.append(((Annotation) modifier).getTypeName() + " ");
            }
        }
        sb.append("methodName: " + methodName + " ");
        sb.append("parameters: ");
        for (SingleVariableDeclaration svd : parameters) {
            sb.append(svd.getType() + " ");
        }
        if (returnType != null) {
            sb.append("returnType: " + returnType.toString() + " ");
        }
        sb.append("throwExpList: ");
        for (SimpleType st : throwExpList) {
            sb.append(st.getName() + " ");
        }
        return sb.toString();
    }

    public float compareSimilarity(MethodSignature methodSignature) {
        float sim = 0;
        sim = computePureMethodSignatureWithJaccard(methodSignature);
        ExceptionUtil.myAssert(sim >= 0);
        return sim;
    }

    public List<String> getInterfaceNameList() {
        return interfaceNameList;
    }
}
