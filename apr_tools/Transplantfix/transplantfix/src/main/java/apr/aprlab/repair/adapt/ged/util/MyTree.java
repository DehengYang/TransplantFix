package apr.aprlab.repair.adapt.ged.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MyTree<T> {

    public MyNode<T> root;

    public static Comparator compar;

    public static int MaxNodeByFloor = 200;

    public MyTree(T rootData, Comparator cmp) {
        root = new MyNode<T>();
        root.parent = null;
        root.data = rootData;
        compar = cmp;
        root.children = new ArrayList<MyNode<T>>(MaxNodeByFloor);
    }

    public static class MyNode<T> {

        public T data;

        public MyNode<T> parent;

        public ArrayList<MyNode<T>> children = new ArrayList<MyNode<T>>(MaxNodeByFloor);

        public boolean issorted = false;
    }

    public MyNode<T> Add(MyNode<T> parent, T childdata) {
        MyNode<T> child = new MyNode<T>();
        child.parent = parent;
        child.data = childdata;
        parent.children.add(child);
        return child;
    }

    @SuppressWarnings("static-access")
    public MyNode<T> pollFirstLowestCost(MyNode<T> parent) {
        if (parent.issorted == false) {
            Collections.sort(parent.children, this.compar);
            parent.issorted = true;
        }
        if (parent.children.size() > 0) {
            MyNode<T> res = parent.children.get(0);
            parent.children.remove(0);
            return res;
        }
        return null;
    }

    public MyNode<T> BackTrack(MyNode<T> child) {
        return child.parent;
    }

    public boolean isEmpty() {
        if (this.root.children.size() == 0) {
            return true;
        }
        return false;
    }
}
