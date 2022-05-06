package apr.aprlab.utils.simple.graph;

import java.util.Objects;

public class Vertex<T> {

    public T label;

    public Vertex(T label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label.toString();
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
            Vertex<T> fsOther = (Vertex<T>) obj;
            return Objects.equals(label, fsOther.label);
        }
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }
}
