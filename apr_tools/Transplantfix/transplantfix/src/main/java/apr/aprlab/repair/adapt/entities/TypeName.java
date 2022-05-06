package apr.aprlab.repair.adapt.entities;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import apr.aprlab.utils.ast.RangeUtil;

public class TypeName extends Entity {

    private String typeName;

    private List<ASTNode> nodes = new ArrayList<ASTNode>();

    public TypeName(String typeName, ASTNode node) {
        this.nodes.add(node);
        this.typeName = typeName;
    }

    public TypeName(String typeName2, List<ASTNode> matchedNodes) {
        this.typeName = typeName2;
        this.nodes = matchedNodes;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return String.format("typeName: %s", typeName);
    }

    public void addType(ASTNode typeNode) {
        this.nodes.add(typeNode);
    }

    @Override
    public String getSignature() {
        return typeName;
    }

    @Override
    public Entity generateEntityInASTNode(ASTNode astNode) {
        List<ASTNode> matchedNodes = new ArrayList<ASTNode>();
        for (ASTNode node : nodes) {
            if (RangeUtil.containsDstNode(astNode, node)) {
                matchedNodes.add(node);
            }
        }
        if (matchedNodes.isEmpty()) {
            return null;
        } else {
            return new TypeName(typeName, matchedNodes);
        }
    }

    @Override
    public List<ASTNode> getNodes() {
        return nodes;
    }
}
