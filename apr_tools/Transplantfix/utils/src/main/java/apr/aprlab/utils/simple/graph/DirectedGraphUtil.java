package apr.aprlab.utils.simple.graph;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public class DirectedGraphUtil {

    public static <T> Set<Vertex<T>> depthFirstTraversal(DirectedGraph<T> graph, Vertex<T> root) {
        Set<Vertex<T>> visited = new LinkedHashSet<>();
        Stack<Vertex<T>> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Vertex<T> vertex = stack.pop();
            if (!visited.contains(vertex)) {
                visited.add(vertex);
                for (Vertex<T> v : graph.getAdjVertices().get(vertex)) {
                    stack.push(v);
                }
            }
        }
        return visited;
    }

    public static <T> Set<Vertex<T>> breadthFirstTraversal(DirectedGraph<T> graph, Vertex<T> root) {
        Set<Vertex<T>> visited = new LinkedHashSet<>();
        Queue<Vertex<T>> queue = new LinkedList<>();
        queue.add(root);
        visited.add(root);
        while (!queue.isEmpty()) {
            Vertex<T> vertex = queue.poll();
            for (Vertex<T> v : graph.getAdjVertices().get(vertex)) {
                if (!visited.contains(v)) {
                    visited.add(v);
                    queue.add(v);
                }
            }
        }
        return visited;
    }

    public static <T> void printGraph(DirectedGraph<T> graph) {
        Map<Vertex<T>, List<Vertex<T>>> adjVertices = graph.getAdjVertices();
        for (Vertex<T> vertex : adjVertices.keySet()) {
            List<Vertex<T>> adjList = adjVertices.get(vertex);
            if (!adjList.isEmpty()) {
                for (Vertex<T> v : adjList) {
                    System.out.format("%s -> %s\n", vertex, v);
                }
                System.out.format("\n");
            }
        }
    }
}
