
package worklet2.graph;

import java.util.*;

public final class GraphAlgorithms {

    private GraphAlgorithms() {}

    public static final class CircularDependencyException
            extends RuntimeException {

        private final Set<String> cellsInCycle;
        private final List<String> cyclePath;

        public CircularDependencyException(Set<String> cells) {
            this(findCyclePathFromCells(cells));
        }

        public CircularDependencyException(List<String> cyclePath) {
            super("Circular dependency detected: " + cyclePath);

            if (cyclePath == null || cyclePath.size() < 2) {
                throw new IllegalArgumentException(
                        "Cycle path must contain at least two entries."
                );
            }

            this.cyclePath = Collections.unmodifiableList(
                    new ArrayList<>(cyclePath)
            );

            // Exclude the repeated final cell before creating the set.
            Set<String> cells = new TreeSet<>(
                    cyclePath.subList(0, cyclePath.size() - 1)
            );

            this.cellsInCycle = Collections.unmodifiableSet(cells);
        }

        public Set<String> getCellsInCycle() {
            return cellsInCycle;
        }

        /**
         * Returns the ordered cycle path.
         * The first cell is repeated at the end.
         */
        public List<String> getCyclePath() {
            return cyclePath;
        }

        private static List<String> findCyclePathFromCells(
                Set<String> cells) {

            if (cells == null || cells.isEmpty()) {
                throw new IllegalArgumentException(
                        "Cycle cells must not be empty."
                );
            }

            List<String> path = new ArrayList<>(new TreeSet<>(cells));
            path.add(path.get(0));

            return path;
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

            if (entry.getValue() == null) {
                continue;
            }

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
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }

        List<String> order = new ArrayList<>();

        while (!ready.isEmpty()) {
            String current = ready.remove();
            order.add(current);

            for (String next : adjacency.get(current)) {
                int remaining = indegree.get(next) - 1;
                indegree.put(next, remaining);

                if (remaining == 0) {
                    ready.add(next);
                }
            }
        }

        if (order.size() != indegree.size()) {
            List<String> cyclePath = findCyclePath(adjacency);

            throw new CircularDependencyException(cyclePath);
        }

        return order;
    }

    /**
     * Finds one actual cycle in the supplied directed graph.
     * The returned path repeats the starting cell at the end.
     */
    public static List<String> findCyclePath(
            Map<String, Set<String>> graph) {

        Objects.requireNonNull(graph, "Graph must not be null");

        Map<String, Set<String>> adjacency = new TreeMap<>();

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            String from = Objects.requireNonNull(
                    entry.getKey(),
                    "Graph node must not be null"
            );

            adjacency.putIfAbsent(from, new TreeSet<>());

            if (entry.getValue() != null) {
                for (String to : entry.getValue()) {
                    Objects.requireNonNull(
                            to,
                            "Dependency node must not be null"
                    );

                    adjacency.putIfAbsent(to, new TreeSet<>());
                    adjacency.get(from).add(to);
                }
            }
        }

        Set<String> visited = new HashSet<>();
        Set<String> active = new HashSet<>();
        List<String> path = new ArrayList<>();

        for (String cell : adjacency.keySet()) {
            List<String> cycle = findCycleFrom(
                    cell,
                    adjacency,
                    visited,
                    active,
                    path
            );

            if (cycle != null) {
                return cycle;
            }
        }

        return Collections.emptyList();
    }

    private static List<String> findCycleFrom(
            String cell,
            Map<String, Set<String>> graph,
            Set<String> visited,
            Set<String> active,
            List<String> path) {

        if (active.contains(cell)) {
            int cycleStart = path.indexOf(cell);

            List<String> cycle = new ArrayList<>(
                    path.subList(cycleStart, path.size())
            );

            cycle.add(cell);

            return cycle;
        }

        if (visited.contains(cell)) {
            return null;
        }

        visited.add(cell);
        active.add(cell);
        path.add(cell);

        for (String next : graph.getOrDefault(
                cell,
                Collections.emptySet())) {

            List<String> cycle = findCycleFrom(
                    next,
                    graph,
                    visited,
                    active,
                    path
            );

            if (cycle != null) {
                return cycle;
            }
        }

        path.remove(path.size() - 1);
        active.remove(cell);

        return null;
    }

    public static boolean wouldCreateCycle(
            Map<String, Set<String>> graph,
            String from,
            String to) {

        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Cell references must not be null"
            );
        }

        if (from.equals(to)) {
            return true;
        }

        Deque<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        pending.push(to);

        while (!pending.isEmpty()) {
            String current = pending.pop();

            if (!visited.add(current)) {
                continue;
            }

            if (current.equals(from)) {
                return true;
            }

            Set<String> neighbours = graph.get(current);

            if (neighbours != null) {
                for (String next : neighbours) {
                    pending.push(next);
                }
            }
        }

        return false;
    }
}
