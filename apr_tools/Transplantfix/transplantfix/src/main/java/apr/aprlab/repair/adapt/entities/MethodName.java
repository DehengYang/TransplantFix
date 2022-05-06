package apr.aprlab.repair.adapt.entities;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.graph.ddg.MethodCall;

public class MethodName extends Entity {

    private MethodCall methodCall;

    private String methodName;

    private List<ASTNode> simpleNameNodes = new ArrayList<>();

    private List<ASTNode> wholeNodes = new ArrayList<>();

    public MethodName(ASTNode simpleNameNode, ASTNode wholeMethodNode, MethodCall mc) {
        if (simpleNameNode instanceof SimpleName) {
            this.methodName = ((SimpleName) simpleNameNode).getIdentifier();
        } else if (simpleNameNode instanceof Type) {
            this.methodName = ((Type) simpleNameNode).toString();
        }
        this.methodCall = mc;
        this.simpleNameNodes.add(simpleNameNode);
        this.wholeNodes.add(wholeMethodNode);
    }

    public MethodName(String apiName, ASTNode wholeMethodNode, MethodCall mc) {
        this.methodName = apiName;
        this.methodCall = mc;
        this.wholeNodes.add(wholeMethodNode);
    }

    public MethodName(String methodName, List<ASTNode> matchedSimpleNodes, List<ASTNode> matchedWholeNodes, MethodCall methodCall) {
        this.methodName = methodName;
        this.methodCall = methodCall;
        this.simpleNameNodes = matchedSimpleNodes;
        this.wholeNodes = matchedWholeNodes;
    }

    public MethodCall getMethodCall() {
        return methodCall;
    }

    @Override
    public String toString() {
        return String.format("method signature: %s", methodCall.getMethodCallSignature());
    }

    public String getMethodName() {
        return methodName;
    }

    public void addMethod(ASTNode methodSimpleName, ASTNode wholeMethodNode) {
        this.simpleNameNodes.add(methodSimpleName);
        this.wholeNodes.add(wholeMethodNode);
    }

    @Override
    public String getSignature() {
        return methodCall.getMethodCallSignature();
    }

    @Override
    public Entity generateEntityInASTNode(ASTNode astNode) {
        List<ASTNode> matchedWholeNodes = new ArrayList<ASTNode>();
        List<ASTNode> matchedSimpleNodes = new ArrayList<>();
        for (int i = 0; i < wholeNodes.size(); i++) {
            ASTNode node = wholeNodes.get(i);
            if (RangeUtil.containsDstNode(astNode, node)) {
                matchedWholeNodes.add(node);
                matchedSimpleNodes.add(simpleNameNodes.get(i));
            }
        }
        if (matchedWholeNodes.isEmpty()) {
            return null;
        } else {
            return new MethodName(methodName, matchedSimpleNodes, matchedWholeNodes, methodCall);
        }
    }

    @Override
    public List<ASTNode> getNodes() {
        return new ArrayList<ASTNode>(simpleNameNodes);
    }

    public List<ASTNode> getSimpleNameNodes() {
        return simpleNameNodes;
    }

    public List<ASTNode> getWholeNodes() {
        return wholeNodes;
    }
}
