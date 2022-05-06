package apr.aprlab.repair.adapt.ged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import apr.aprlab.repair.adapt.ged.util.EditPath;

@SuppressWarnings({ "unused", "unchecked", "rawtypes" })
public class NonStaticTree<T> {

    public NonStaticNode<T> root;

    public Comparator compar;

    public int MaxNodeByFloor = 200;

    public int nbnodes = 0;

    public double globalworkload = 0;

    public NonStaticTree(T rootData, Comparator cmp) {
        root = new NonStaticNode<T>();
        root.parent = null;
        root.data = rootData;
        compar = cmp;
        root.children = new ArrayList<NonStaticNode<T>>(MaxNodeByFloor);
    }

    public NonStaticNode<T> Add(NonStaticNode<T> parent, T childdata) {
        NonStaticNode<T> child = new NonStaticNode<T>();
        child.parent = parent;
        child.data = childdata;
        parent.children.add(child);
        nbnodes++;
        child.workload = ((EditPath) child.data).getUnUsedNodes1().size() + ((EditPath) child.data).getUnUsedNodes2().size();
        globalworkload += child.workload;
        return child;
    }

    public NonStaticNode<T> pollFirstLowestCost(NonStaticNode<T> parent) {
        if (parent.issorted == false) {
            Collections.sort(parent.children, this.compar);
            parent.issorted = true;
        }
        if (parent.children.size() > 0) {
            NonStaticNode<T> res = parent.children.get(0);
            parent.children.remove(0);
            nbnodes--;
            globalworkload -= res.workload;
            return res;
        }
        return null;
    }

    public NonStaticNode<T> pollLowestCost(NonStaticNode<T> parent, int isLighThread) {
        if (parent.issorted == false) {
            Collections.sort(parent.children, this.compar);
            parent.issorted = true;
        }
        if (parent.children.size() > 0) {
            if (parent.pointer < parent.children.size()) {
                NonStaticNode<T> res = parent.children.get(parent.pointer);
                nbnodes--;
                globalworkload -= res.workload;
                if (isLighThread == 1) {
                    parent.children.remove(parent.pointer);
                } else {
                    parent.pointer = parent.pointer + 1;
                }
                return res;
            }
        }
        return null;
    }

    public NonStaticNode<T> pollNLowestCost(NonStaticNode<T> parent) {
        if (parent.issorted == false) {
            Collections.sort(parent.children, this.compar);
            parent.issorted = true;
        }
        if (parent.children.size() > 0) {
            if (parent.pointer < parent.children.size()) {
                NonStaticNode<T> res = parent.children.get(0);
                parent.children.remove(0);
                nbnodes--;
                globalworkload -= res.workload;
                return res;
            }
        }
        return null;
    }

    public ArrayList<NonStaticNode> pollNLowestCost(NonStaticNode CurNode, int numberOfEditPaths) {
        ArrayList<NonStaticNode> res = new ArrayList<NonStaticNode>();
        NonStaticNode CurN = CurNode;
        for (int i = 0; i < numberOfEditPaths; i++) {
            NonStaticNode pminNode = pollNLowestCost(CurN);
            boolean condition1 = (pminNode == null);
            boolean condition2 = ((pminNode == null) && (CurN.parent != null));
            while (condition1 && condition2) {
                CurN = BackTrack(CurN);
                pminNode = pollNLowestCost(CurN);
                condition1 = (pminNode == null);
                condition2 = ((pminNode == null) && (CurN.parent != null));
            }
            if ((pminNode == null) && (CurN.parent == null)) {
                return res;
            } else {
                res.add(pminNode);
            }
        }
        return res;
    }

    public ArrayList<NonStaticNode> searchLowestCost(NonStaticNode CurNode, int islightthread) {
        ArrayList<NonStaticNode> res = new ArrayList<NonStaticNode>();
        NonStaticNode CurN = CurNode;
        NonStaticNode pminNode = pollLowestCost(CurN, islightthread);
        boolean condition1 = (pminNode == null);
        boolean condition2 = ((pminNode == null) && (CurN.parent != null));
        while (condition1 && condition2) {
            CurN = BackTrack(CurN);
            pminNode = pollLowestCost(CurN, islightthread);
            condition1 = (pminNode == null);
            condition2 = ((pminNode == null) && (CurN.parent != null));
        }
        if ((pminNode == null) && (CurN.parent == null)) {
            return res;
        } else {
            res.add(pminNode);
            res.add(CurN);
        }
        return res;
    }

    public NonStaticNode<T> BackTrack(NonStaticNode<T> child) {
        return child.parent;
    }

    public boolean isEmpty() {
        if (this.root.children.size() == 0) {
            return true;
        }
        return false;
    }

    private void removeSomeEditPaths(ArrayList children, int j) {
        int size = children.size();
        int delta = size - j;
        if (delta > 0) {
            for (int i = 0; i < delta; i++) {
                children.remove(size - i - 1);
            }
        }
    }
}
