package apr.aprlab.utils.simple.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectedGraph<T> {

    private Map<Vertex<T>, List<Vertex<T>>> adjVertices = new HashMap<>();

    public void addVertex(Vertex<T> vertex) {
        adjVertices.putIfAbsent(vertex, new ArrayList<>());
    }

    public void removeVertex(Vertex<T> vertex) {
        adjVertices.values().stream().forEach(e -> e.remove(vertex));
        adjVertices.remove(vertex);
    }

    public void addEdge(Vertex<T> v1, Vertex<T> v2) {
        List<Vertex<T>> adjList = adjVertices.get(v1);
        if (!adjList.contains(v2)) {
            adjList.add(v2);
        }
    }

    public void removeEdge(Vertex<T> v1, Vertex<T> v2) {
        List<Vertex<T>> eV1 = adjVertices.get(v1);
        if (eV1 != null) {
            eV1.remove(v2);
        }
    }

    public Map<Vertex<T>, List<Vertex<T>>> getAdjVertices() {
        return adjVertices;
    }
}
