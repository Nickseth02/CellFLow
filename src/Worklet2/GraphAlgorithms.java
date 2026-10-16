package Worklet2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Cycle detection and topological sorting using Kahn's algorithm.
 *
 * The graph is a map:  cell -> cells that depend on it
 * example:  A1 -> {B1},  B1 -> {C1}   (B1 uses A1, C1 uses B1)
 *
 * Time complexity: O(V + E)
 *   - computing in-degrees visits every edge once        -> O(E)
 *   - every vertex enters and leaves the queue once      -> O(V)
 *   - when a vertex leaves, its outgoing edges are
 *     looked at once, so all edges together              -> O(E)
 */
public class GraphAlgorithms {

    /** Thrown when a topological order is requested for a cyclic graph. */
    public static class CircularDependencyException
            extends IllegalStateException {

        private final Set<String> cellsInCycle;

        public CircularDependencyException(Set<String> cellsInCycle) {
            super("Circular reference detected involving: " + cellsInCycle);
            this.cellsInCycle = Collections.unmodifiableSet(cellsInCycle);
        }

        /** Cells on a cycle, or blocked behind one. */
        public Set<String> getCellsInCycle() {
            return cellsInCycle;
        }
    }

    // ------------------------------------------------------------------
    // Required API (whole graph)
    // ------------------------------------------------------------------

    // Topological order of the whole graph.
    // Throws CircularDependencyException if a circular reference exists.
    public static List<String> topologicalSort(
            Map<String, Set<String>> graph) {

        return topologicalSort(graph, allVertices(graph));
    }

    // True if the whole graph contains a circular reference.
    public static boolean hasCycle(
            Map<String, Set<String>> graph) {

        return hasCycle(graph, allVertices(graph));
    }

    // ------------------------------------------------------------------
    // Subset versions (used for the affected part of the sheet)
    // ------------------------------------------------------------------

    // Topological order of the given vertices.
    // Edges that leave the given set are ignored, so this works on
    // just the affected part of the sheet.
    // Throws an exception if a circular reference exists.
    public static List<String> topologicalSort(
            Map<String, Set<String>> graph,
            Collection<String> vertices) {

        Set<String> vertexSet = new LinkedHashSet<>(vertices);
        List<String> order = kahn(graph, vertexSet);

        // some vertices were never processed -> they wait on each other
        if (order.size() != vertexSet.size()) {
            Set<String> stuck = new LinkedHashSet<>(vertexSet);
            stuck.removeAll(new HashSet<>(order));

            throw new CircularDependencyException(stuck);
        }

        return order;
    }

    // True if the given vertices contain a circular reference.
    public static boolean hasCycle(
            Map<String, Set<String>> graph,
            Collection<String> vertices) {

        Set<String> vertexSet = new LinkedHashSet<>(vertices);
        return kahn(graph, vertexSet).size() != vertexSet.size();
    }

    // ------------------------------------------------------------------
    // Helpers used by DependencyGraph / RecalculationEngineImpl
    // ------------------------------------------------------------------

    /** Result of a sort that reports cycles instead of throwing. */
    public static final class SortResult {
        private final List<String> order;
        private final Set<String> cyclicCells;

        SortResult(List<String> order, Set<String> cyclicCells) {
            this.order = Collections.unmodifiableList(order);
            this.cyclicCells = Collections.unmodifiableSet(cyclicCells);
        }

        /** Cells that can be calculated, in dependency order. */
        public List<String> getOrder() {
            return order;
        }

        /** Cells on a cycle, or blocked behind one (never ordered). */
        public Set<String> getCyclicCells() {
            return cyclicCells;
        }

        public boolean hasCycle() {
            return !cyclicCells.isEmpty();
        }
    }

    // Like topologicalSort(graph, vertices) but never throws: the cells
    // Kahn's algorithm cannot release are returned as cyclic cells so the
    // engine can flag them (#CIRCULAR!) and still calculate the rest.
    public static SortResult sortWithCycleInfo(
            Map<String, Set<String>> graph,
            Collection<String> vertices) {

        Set<String> vertexSet = new LinkedHashSet<>(vertices);
        List<String> order = kahn(graph, vertexSet);

        Set<String> stuck = new LinkedHashSet<>(vertexSet);
        stuck.removeAll(new HashSet<>(order));

        return new SortResult(order, stuck);
    }

    // All cells that depend directly or indirectly on 'start' (BFS).
    // 'start' is not included unless it lies on a cycle.
    // The returned set is mutable (the engine adds to it).
    public static Set<String> findAffectedCells(
            Map<String, Set<String>> graph,
            String start) {

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            String current = queue.remove();

            for (String next : graph.getOrDefault(
                    current, Collections.<String>emptySet())) {

                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return visited;
    }

    // True if adding the edge from -> to would create a cycle.
    public static boolean wouldCreateCycle(
            Map<String, Set<String>> graph,
            String from,
            String to) {

        return from.equals(to)
                || findAffectedCells(graph, to).contains(from);
    }

    // ------------------------------------------------------------------
    // Kahn's algorithm
    // ------------------------------------------------------------------

    // Returns the vertices it managed to process.
    // If the list is shorter than 'vertices', there is a cycle.
    private static List<String> kahn(
            Map<String, Set<String>> graph,
            Set<String> vertices) {

        // step 1: in-degree = number of edges coming into each vertex
        Map<String, Integer> inDegree = new HashMap<>();

        for (String vertex : vertices) {
            inDegree.put(vertex, 0);
        }

        for (String from : vertices) {
            for (String to : graph.getOrDefault(
                    from, Collections.<String>emptySet())) {

                if (vertices.contains(to)) {
                    inDegree.put(to, inDegree.get(to) + 1);
                }
            }
        }

        // step 2: vertices with in-degree 0 wait for nobody
        Queue<String> queue = new LinkedList<>();

        for (String vertex : vertices) {
            if (inDegree.get(vertex) == 0) {
                queue.add(vertex);
            }
        }

        // step 3: process a vertex, then remove its outgoing edges
        List<String> order = new ArrayList<>();

        while (!queue.isEmpty()) {
            String current = queue.remove();
            order.add(current);

            for (String next : graph.getOrDefault(
                    current, Collections.<String>emptySet())) {

                if (!vertices.contains(next)) {
                    continue;
                }

                int remaining = inDegree.get(next) - 1;
                inDegree.put(next, remaining);

                if (remaining == 0) {
                    queue.add(next);
                }
            }
        }

        return order;
    }

    // every vertex: keys AND targets (C1 may appear only as a target)
    private static Set<String> allVertices(Map<String, Set<String>> graph) {
        Set<String> vertices = new LinkedHashSet<>();

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            vertices.add(entry.getKey());
            vertices.addAll(entry.getValue());
        }

        return vertices;
    }

    // ------------------------------------------------------------------
    // Demonstration
    // ------------------------------------------------------------------

    public static void main(String[] args) {

        // 1. acyclic graph -> valid order
        Map<String, Set<String>> acyclic = new LinkedHashMap<>();
        addEdge(acyclic, "A1", "B1");
        addEdge(acyclic, "A1", "C1");
        addEdge(acyclic, "B1", "C1");
        addEdge(acyclic, "C1", "D1");

        List<String> order = topologicalSort(acyclic);

        System.out.println("Acyclic graph order: " + order);
        System.out.println("Has cycle? " + hasCycle(acyclic));
        System.out.println("Every edge goes forward? "
                + isValidOrder(acyclic, order));

        // 2. A1 -> B1 -> A1 -> rejected
        Map<String, Set<String>> cyclic = new LinkedHashMap<>();
        addEdge(cyclic, "A1", "B1");
        addEdge(cyclic, "B1", "A1");

        System.out.println("\nA1 -> B1 -> A1");
        System.out.println("Has cycle? " + hasCycle(cyclic));

        try {
            topologicalSort(cyclic);
        } catch (IllegalStateException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        // 3. self reference A1 -> A1 is also a cycle
        Map<String, Set<String>> selfLoop = new LinkedHashMap<>();
        addEdge(selfLoop, "A1", "A1");

        System.out.println("\nA1 -> A1");
        System.out.println("Has cycle? " + hasCycle(selfLoop));
    }

    private static void addEdge(
            Map<String, Set<String>> graph, String from, String to) {

        graph.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
    }

    // checks that for every edge from -> to, 'from' comes first in the order
    private static boolean isValidOrder(
            Map<String, Set<String>> graph, List<String> order) {

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            for (String to : entry.getValue()) {
                if (order.indexOf(entry.getKey()) > order.indexOf(to)) {
                    return false;
                }
            }
        }

        return true;
    }
}
