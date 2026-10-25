package Worklet2;

import java.util.*;

public final class GraphAlgorithms {
    private GraphAlgorithms() {}

    public static final class CircularDependencyException
            extends RuntimeException {
        private final Set<String> cellsInCycle;

        public CircularDependencyException(Set<String> cells) {
            super("Circular dependency detected: " + cells);
            cellsInCycle = Collections.unmodifiableSet(new TreeSet<>(cells));
        }

        public Set<String> getCellsInCycle() {
            return cellsInCycle;
        }
    }

    public static List<String> topologicalSort(
            Map<String, Set<String>> graph) {
        Map<String, Set<String>> adjacency = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();

        for (String node : graph.keySet()) {
            adjacency.put(node, new TreeSet<>());
            indegree.put(node, 0);
        }

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            String from = entry.getKey();
            adjacency.computeIfAbsent(from, k -> new TreeSet<>());
            indegree.putIfAbsent(from, 0);

            if (entry.getValue() == null) continue;

            for (String to : entry.getValue()) {
                adjacency.computeIfAbsent(to, k -> new TreeSet<>());
                indegree.putIfAbsent(to, 0);

                if (adjacency.get(from).add(to)) {
                    indegree.put(to, indegree.get(to) + 1);
                }
            }
        }

        PriorityQueue<String> ready = new PriorityQueue<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) ready.add(entry.getKey());
        }

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String current = ready.remove();
            order.add(current);

            for (String next : adjacency.get(current)) {
                int remaining = indegree.get(next) - 1;
                indegree.put(next, remaining);
                if (remaining == 0) ready.add(next);
            }
        }

        if (order.size() != indegree.size()) {
            Set<String> cyclic = new TreeSet<>();
            for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
                if (entry.getValue() > 0) cyclic.add(entry.getKey());
            }
            throw new CircularDependencyException(cyclic);
        }

        return order;
    }

    public static boolean wouldCreateCycle(
            Map<String, Set<String>> graph, String from, String to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Cell references must not be null");
        }
        if (from.equals(to)) return true;

        Deque<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.push(to);

        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (!visited.add(current)) continue;
            if (current.equals(from)) return true;

            Set<String> neighbours = graph.get(current);
            if (neighbours != null) {
                for (String next : neighbours) pending.push(next);
            }
        }
        return false;
    }
}