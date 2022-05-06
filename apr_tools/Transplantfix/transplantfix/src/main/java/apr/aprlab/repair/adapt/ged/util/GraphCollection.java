package apr.aprlab.repair.adapt.ged.util;

import java.util.LinkedList;

@SuppressWarnings({ "serial", "rawtypes" })
public class GraphCollection extends LinkedList {

    private String collectionName;

    public GraphCollection() {
        super();
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
