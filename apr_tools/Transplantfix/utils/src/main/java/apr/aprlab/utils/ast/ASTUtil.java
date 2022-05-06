package apr.aprlab.utils.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;
import apr.aprlab.utils.ast.ASTWalk.WalkOrder;
import apr.aprlab.utils.general.ExceptionUtil;

public class ASTUtil {

    public static final Logger logger = LogManager.getLogger(ASTUtil.class);

    public static CompilationUnit getCompilationUnit(ASTNode node) {
        if (node instanceof CompilationUnit) {
            return (CompilationUnit) node;
        }
        ASTNode parent = node.getParent();
        while (!(parent instanceof CompilationUnit)) {
            parent = parent.getParent();
        }
        ExceptionUtil.assertNotNull(parent);
        return (CompilationUnit) parent;
    }

    private static String getMethodString(MethodDeclaration node) {
        String name = node.getName().toString();
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> paras = node.parameters();
        List<String> paraList = new ArrayList<>();
        for (SingleVariableDeclaration para : paras) {
            Type paraType = para.getType();
            String paraIdentifier = getTypeIdentifier(paraType);
            paraList.add(paraIdentifier);
        }
        String methodStr = String.format("%s(%s)", name, String.join(", ", paraList.toArray(new String[paraList.size()])));
        return methodStr;
    }

    private static String getTypeIdentifier(Type paraType) {
        if (paraType instanceof ArrayType) {
            return getTypeIdentifier(((ArrayType) paraType).getElementType());
        } else if (paraType instanceof ParameterizedType) {
            return getTypeIdentifier(((ParameterizedType) paraType).getType());
        } else {
            return paraType.toString();
        }
    }

    public static List<ASTNode> getChildren(ASTNode node) {
        List<ASTNode> children = new ArrayList<>();
        @SuppressWarnings("rawtypes")
        List list = node.structuralPropertiesForType();
        for (int i = 0; i < list.size(); i++) {
            Object child = node.getStructuralProperty((StructuralPropertyDescriptor) list.get(i));
            if (child instanceof ASTNode) {
                children.add((ASTNode) child);
                continue;
            }
            if (child == null) {
                continue;
            }
            String childClass = child.getClass().getName();
            if (childClass.equals("org.eclipse.jdt.core.dom.ASTNode$NodeList")) {
                @SuppressWarnings("unchecked")
                List<ASTNode> childList = (List<ASTNode>) child;
                children.addAll(childList);
            }
        }
        return children;
    }

    public static List<MethodDeclaration> matchMethod(CompilationUnit cu, String methodName) {
        List<MethodDeclaration> matchedMdList = new ArrayList<>();
        Consumer<MethodDeclaration> consumer = node -> {
            String methodStr = getMethodString(node);
            logger.debug("methodStr: {}, methodName: {}", methodStr, methodName);
            if (methodStr.equals(methodName)) {
                matchedMdList.add(node);
            }
        };
        ASTWalk.findall(cu, MethodDeclaration.class, consumer);
        return matchedMdList;
    }

    public static ASTNode cloneNode(ASTNode astNode) {
        ASTNode nodeClone = ASTNode.copySubtree(astNode.getAST(), astNode);
        return nodeClone;
    }

    public static MethodDeclaration findMethodByLineNo(CompilationUnit cu, int lineNo) {
        Iterator<ASTNode> iter = ASTWalk.getNodeIterator(WalkOrder.BREADTHFIRST, cu);
        while (iter.hasNext()) {
            ASTNode node = iter.next();
            if (node instanceof MethodDeclaration) {
                Range nodeRange = RangeUtil.getRange(cu, node);
                if (RangeUtil.lineInRange(nodeRange, lineNo)) {
                    return (MethodDeclaration) node;
                }
            }
        }
        return null;
    }

    public static String getSuperClassName(ITypeBinding binding) {
        ITypeBinding superClassBinding = binding.getSuperclass();
        if (superClassBinding != null) {
            return superClassBinding.getQualifiedName();
        } else {
            return null;
        }
    }

    public static List<String> getInterfaceNameList(ITypeBinding binding) {
        List<String> interfaceNameList = new ArrayList<String>();
        ITypeBinding[] interfaceBindingArray = binding.getInterfaces();
        for (ITypeBinding itb : interfaceBindingArray) {
            String interfaceName = itb.getQualifiedName();
            interfaceNameList.add(interfaceName);
        }
        return interfaceNameList;
    }

    public static ASTNode findUnitStmtByLineNo(CompilationUnit cu, int lineNo) {
        Iterator<ASTNode> iter = ASTWalk.getNodeIterator(WalkOrder.BREADTHFIRST, cu);
        ASTNode stmt = null;
        while (iter.hasNext()) {
            ASTNode node = iter.next();
            if (node instanceof Statement || node instanceof FieldDeclaration) {
                if (node instanceof Block) {
                    continue;
                }
                if (RangeUtil.getStartLineNo(cu, node) == lineNo) {
                    stmt = node;
                    break;
                }
            }
        }
        if (stmt == null) {
            logger.debug("");
        }
        return stmt;
    }

    public static ASTNode getIfOrLoopAstNode(ASTNode astNode) {
        ASTNode parent = astNode.getParent();
        while (!isLoopOrIfAstNode(parent) && !(parent instanceof MethodDeclaration)) {
            parent = parent.getParent();
        }
        if (parent instanceof MethodDeclaration) {
            return null;
        } else {
            return parent;
        }
    }

    public static boolean isLoopOrIfAstNode(ASTNode parent) {
        boolean is = isIfASTNode(parent) || isLoopASTNode(parent);
        return is;
    }

    public static boolean isLoopASTNode(ASTNode parent) {
        if (parent instanceof DoStatement || parent instanceof ForStatement || parent instanceof WhileStatement || parent instanceof EnhancedForStatement) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isIfASTNode(ASTNode parent) {
        return parent instanceof IfStatement;
    }

    public static Statement getParentStmt(ASTNode astNode) {
        ASTNode parent = astNode;
        while (!(parent instanceof Statement)) {
            parent = parent.getParent();
        }
        ExceptionUtil.assertNotNull(parent);
        return (Statement) parent;
    }

    public static TypeDeclaration getParentTd(ASTNode astNode) {
        ASTNode parent = astNode;
        while (!(parent instanceof TypeDeclaration) && parent != null) {
            parent = parent.getParent();
        }
        if (parent == null) {
            return null;
        } else {
            return (TypeDeclaration) parent;
        }
    }
}
