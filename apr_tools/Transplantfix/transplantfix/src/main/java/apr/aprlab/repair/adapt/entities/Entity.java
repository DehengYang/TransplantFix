package apr.aprlab.repair.adapt.entities;

import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;

public abstract class Entity {

    public abstract String getSignature();

    public abstract Entity generateEntityInASTNode(ASTNode astNode);

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public abstract List<ASTNode> getNodes();
}
