package apr.aprlab.repair.adapt.entities;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import apr.aprlab.utils.ast.RangeUtil;

public class VariableName extends Entity {

    private String varName;

    private List<SimpleName> nodes = new ArrayList<SimpleName>();

    private List<String> types = new ArrayList<String>();

    private List<VariableDef> varDefs = new ArrayList<VariableDef>();

    public VariableName(SimpleName node, VariableDef varDef) {
        this.varName = node.getIdentifier();
        this.nodes.add(node);
        this.types.add(varDef.getType());
        this.varDefs.add(varDef);
    }

    public VariableName(String varName2, List<SimpleName> matchedNodes, List<String> matchedTypes, List<VariableDef> matchedVarDefs) {
        this.varName = varName2;
        this.nodes = matchedNodes;
        this.types = matchedTypes;
        this.varDefs = matchedVarDefs;
    }

    public String getVarName() {
        return varName;
    }

    @Override
    public String toString() {
        return String.format("varName: %s, types: %s", varName, types);
    }

    public void addVar(SimpleName varNode, VariableDef varDef) {
        this.nodes.add(varNode);
        this.types.add(varDef.getType());
        this.varDefs.add(varDef);
    }

    @Override
    public String getSignature() {
        return varName;
    }

    @Override
    public Entity generateEntityInASTNode(ASTNode astNode) {
        List<SimpleName> matchedNodes = new ArrayList<>();
        List<String> matchedTypes = new ArrayList<>();
        List<VariableDef> matchedVarDefs = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            SimpleName node = nodes.get(i);
            if (RangeUtil.containsDstNode(astNode, node)) {
                matchedNodes.add(node);
                matchedTypes.add(types.get(i));
                matchedVarDefs.add(varDefs.get(i));
            }
        }
        if (matchedNodes.isEmpty()) {
            return null;
        } else {
            return new VariableName(varName, matchedNodes, matchedTypes, matchedVarDefs);
        }
    }

    @Override
    public List<ASTNode> getNodes() {
        return new ArrayList<ASTNode>(nodes);
    }

    public String getType() {
        return types.get(0);
    }

    public List<String> getTypes() {
        return types;
    }

    public List<VariableDef> getVarDefs() {
        return varDefs;
    }
}
