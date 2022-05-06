package apr.aprlab.repair.adapt.ged;

import java.util.ArrayList;

public class NonStaticNode<T> {

    public T data;

    public NonStaticNode<T> parent;

    public ArrayList<NonStaticNode<T>> children = new ArrayList<NonStaticNode<T>>();

    public boolean issorted = false;

    public double workload = 0;

    public int pointer = 0;
}
