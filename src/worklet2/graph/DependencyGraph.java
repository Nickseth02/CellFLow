
package worklet2.graph;

import java.util.*;

public class DependencyGraph {

    // Source cell -> cells that depend on it.
    private final Map<String, Set<String>> graph = new HashMap<>();

    private String normalize(String cell) {
        if (cell == null || cell.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cell reference must not be null or empty."
            );
        }

        return cell.trim().toUpperCase(Locale.ROOT);
    }

    public void addCell(String cell) {
        cell = normalize(cell);
        graph.putIfAbsent(cell, new HashSet<>());
    }

    /**
     * Adds a dependency edge: source -> dependent.
     * Rejects edges that would introduce a cycle.
     */
    public void addDependency(String source, String dependent) {
        source = normalize(source);
        dependent = normalize(dependent);

        if (source.equals(dependent)) {
            throw new IllegalArgumentException(
                    "A cell cannot depend on itself."
            );
        }

        if (canReach(dependent, source, graph)) {
            throw new IllegalArgumentException(
                    "Adding dependency " + source + " -> " + dependent
                            + " would create a circular reference."
            );
        }

        addCell(source);
        addCell(dependent);
        graph.get(source).add(dependent);
    }

    /**
     * Atomically replaces all dependencies of one cell.
     *
     * The existing graph is not modified unless every proposed
     * dependency is valid and the replacement remains acyclic.
     *
     * Example:
     * B1 previously depends on A1.
     * replaceDependencies("B1", Set.of("C1"))
     * replaces A1 -> B1 with C1 -> B1.
     */
    public void replaceDependencies(
            String dependent,
            Set<String> newDependencies) {

        dependent = normalize(dependent);

        if (newDependencies == null) {
            throw new IllegalArgumentException(
                    "Dependencies must not be null."
            );
        }

        // Work on a copy so a failed replacement cannot corrupt
        // the original graph.
        Map<String, Set<String>> candidate = copyGraph();

        candidate.putIfAbsent(dependent, new HashSet<>());

        // Remove all existing incoming edges to this cell.
        for (Set<String> dependents : candidate.values()) {
            dependents.remove(dependent);
        }

        // Validate and build the proposed dependencies on the copy.
        Set<String> normalizedDependencies = new TreeSet<>();

        for (String dependency : newDependencies) {
            normalizedDependencies.add(normalize(dependency));
        }

        for (String source : normalizedDependencies) {

            if (source.equals(dependent)) {
                throw new IllegalArgumentException(
                        "A cell cannot depend on itself."
                );
            }

            candidate.putIfAbsent(source, new HashSet<>());
            candidate.putIfAbsent(dependent, new HashSet<>());

            if (canReach(dependent, source, candidate)) {
                throw new IllegalArgumentException(
                        "Replacing dependencies for " + dependent
                                + " would create a circular reference "
                                + "through " + source + "."
                );
            }

            candidate.get(source).add(dependent);
        }

        // Commit only after all proposed edges pass validation.
        graph.clear();

        for (Map.Entry<String, Set<String>> entry
                : candidate.entrySet()) {
            graph.put(
                    entry.getKey(),
                    new HashSet<>(entry.getValue())
            );
        }
    }

    public void removeDependency(String source, String dependent) {
        source = normalize(source);
        dependent = normalize(dependent);

        Set<String> dependents = graph.get(source);

        if (dependents != null) {
            dependents.remove(dependent);
        }
    }

    public Set<String> getDependents(String cell) {
        cell = normalize(cell);

        Set<String> dependents = graph.get(cell);

        if (dependents == null) {
            return new HashSet<>();
        }

        return new HashSet<>(dependents);
    }

    /**
     * Returns the cells that the specified cell depends on.
     */
    public Set<String> getDependencies(String cell) {
        cell = normalize(cell);

        Set<String> dependencies = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry
                : graph.entrySet()) {
            if (entry.getValue().contains(cell)) {
                dependencies.add(entry.getKey());
            }
        }

        return dependencies;
    }

    /**
     * Returns all direct and indirect dependents of a cell.
     * The starting cell itself is excluded.
     */
    public Set<String> findAffectedCells(String cell) {
        cell = normalize(cell);

        Set<String> affected = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        queue.add(cell);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            for (String dependent : getDependents(current)) {
                if (affected.add(dependent)) {
                    queue.add(dependent);
                }
            }
        }

        return affected;
    }

    /**
     * Checks whether the graph currently contains a cycle.
     */
    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> active = new HashSet<>();

        for (String cell : graph.keySet()) {
            if (hasCycleFrom(cell, visited, active)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCycleFrom(
            String cell,
            Set<String> visited,
            Set<String> active) {

        if (active.contains(cell)) {
            return true;
        }

        if (visited.contains(cell)) {
            return false;
        }

        visited.add(cell);
        active.add(cell);

        for (String dependent : getDependents(cell)) {
            if (hasCycleFrom(dependent, visited, active)) {
                return true;
            }
        }

        active.remove(cell);
        return false;
    }

    /**
     * Returns true if adding source -> dependent would create a cycle.
     */
    public boolean wouldCreateCycle(String source, String dependent) {
        source = normalize(source);
        dependent = normalize(dependent);

        if (source.equals(dependent)) {
            return true;
        }

        return canReach(dependent, source, graph);
    }

    private boolean canReach(
            String start,
            String target,
            Map<String, Set<String>> currentGraph) {

        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(target)) {
                return true;
            }

            for (String next :
                    currentGraph.getOrDefault(
                            current,
                            Collections.emptySet())) {

                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return false;
    }

    /**
     * Topologically sorts the supplied subset of cells.
     * Dependencies outside the subset do not affect its ordering.
     */
    public List<String> topologicalSort(Set<String> cells) {
        Set<String> selected = new TreeSet<>();

        for (String cell : cells) {
            selected.add(normalize(cell));
        }

        Map<String, Integer> indegree = new HashMap<>();

        for (String cell : selected) {
            indegree.put(cell, 0);
        }

        for (String source : selected) {
            for (String dependent : getDependents(source)) {
                if (selected.contains(dependent)) {
                    indegree.put(
                            dependent,
                            indegree.get(dependent) + 1
                    );
                }
            }
        }

        Queue<String> ready = new PriorityQueue<>();

        for (String cell : selected) {
            if (indegree.get(cell) == 0) {
                ready.add(cell);
            }
        }

        List<String> order = new ArrayList<>();

        while (!ready.isEmpty()) {
            String current = ready.poll();
            order.add(current);

            for (String dependent : getDependents(current)) {
                if (!selected.contains(dependent)) {
                    continue;
                }

                int remaining = indegree.get(dependent) - 1;
                indegree.put(dependent, remaining);

                if (remaining == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (order.size() != selected.size()) {
            throw new IllegalStateException(
                    "Circular dependency detected in selected cells."
            );
        }

        return order;
    }

    private Map<String, Set<String>> copyGraph() {
        Map<String, Set<String>> copy = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry
                : graph.entrySet()) {
            copy.put(
                    entry.getKey(),
                    new HashSet<>(entry.getValue())
            );
        }

        return copy;
    }

    public void printGraph() {
        System.out.println("Dependency Graph:");

        List<String> cells = new ArrayList<>(graph.keySet());
        Collections.sort(cells);

        for (String cell : cells) {
            List<String> dependents =
                    new ArrayList<>(getDependents(cell));

            Collections.sort(dependents);

            System.out.println(cell + " -> " + dependents);
        }
    }
}
