package apr.aprlab.utils.tree;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class TreeNotUsed<T> {

    public T data;

    public TreeNotUsed<T> parent;

    public List<TreeNotUsed<T>> children;

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    public TreeNotUsed(T data) {
        this.data = data;
        this.children = new LinkedList<TreeNotUsed<T>>();
    }

    public void addChild(TreeNotUsed<T> childNode) {
        childNode.parent = this;
        this.children.add(childNode);
    }

    public void addParent(TreeNotUsed<T> parentNode) {
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
        return data != null ? data.toString() : "[data null]";
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
            @SuppressWarnings("unchecked")
            TreeNotUsed<T> fsOther = (TreeNotUsed<T>) obj;
            return Objects.equals(this.data, fsOther.data);
        }
    }
}
