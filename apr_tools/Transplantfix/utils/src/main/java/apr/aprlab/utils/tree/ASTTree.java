package apr.aprlab.utils.tree;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.core.dom.ASTNode;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.ast.Range;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.StringUtil;

public class ASTTree {

    private ASTNode node;

    private ASTTree parent;

    private List<ASTTree> children;

    private Range range;

    private String nodeType;

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    public ASTTree(ASTNode node) {
        this.node = node;
        this.range = RangeUtil.getRange(node);
        this.children = new LinkedList<ASTTree>();
        setNodeType(node);
        contructTree(node);
    }

    private void setNodeType(ASTNode node) {
        String nodeType = node.getClass().toString();
        this.nodeType = StringUtil.getShortName(nodeType);
    }

    public void contructTree(ASTNode node) {
        for (ASTNode child : ASTUtil.getChildren(node)) {
            addChild(new ASTTree(child));
        }
    }

    public void addChild(ASTTree childTree) {
        childTree.parent = this;
        this.children.add(childTree);
    }

    public void addParent(ASTTree parentNode) {
        this.parent = parentNode;
        parentNode.children.add(this);
    }

    public int getLevel() {
        if (this.isRoot())
            return 0;
        else
            return parent.getLevel() + 1;
    }

    @Override
    public String toString() {
        ExceptionUtil.assertNotNull(node);
        String nodeStr = node.toString().replaceAll("\n", " ");
        int maxLen = 20;
        if (nodeStr.length() > maxLen) {
            nodeStr = nodeStr.substring(0, maxLen);
        }
        String string = String.format("%s@%s@%s", range, nodeType, nodeStr);
        return string;
    }

    public String toGraphString() {
        ExceptionUtil.assertNotNull(node);
        String nodeStr = node.toString().replace("\n", " ").replace("\"", "");
        int maxLen = 20;
        if (nodeStr.length() > maxLen) {
            nodeStr = nodeStr.substring(0, maxLen);
        }
        while (nodeStr.endsWith("\\")) {
            nodeStr = nodeStr.substring(0, nodeStr.length() - 1);
        }
        String string = String.format("%s@%s@%s", range, nodeType, nodeStr);
        return string;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        } else {
            ASTTree fsOther = (ASTTree) obj;
            return Objects.equals(this.node, fsOther.getNode());
        }
    }

    public ASTNode getNode() {
        return node;
    }

    public ASTTree getParent() {
        return parent;
    }

    public List<ASTTree> getChildren() {
        return children;
    }

    public Range getRange() {
        return range;
    }
}
