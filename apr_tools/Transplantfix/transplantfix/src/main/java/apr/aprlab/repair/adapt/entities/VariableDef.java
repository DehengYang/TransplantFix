package apr.aprlab.repair.adapt.entities;

import org.eclipse.jdt.core.dom.ASTNode;

public class VariableDef {

    private String varName;

    private String type;

    private ASTNode defNode;

    public VariableDef(ASTNode defNode, String varName, String type) {
        this.varName = varName;
        this.type = type;
        this.defNode = defNode;
    }

    public String getVarName() {
        return varName;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "VariableName [varName=" + varName + ", type=" + type + "]";
    }

    public ASTNode getDefNode() {
        return defNode;
    }
}
