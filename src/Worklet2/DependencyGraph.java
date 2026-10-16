package Worklet2;

import java.util.*;

public class DependencyGraph {

    // source cell -> cells that depend on it
    private final Map<String, Set<String>> graph = new HashMap<>();

    public void addCell(String cell) {
        cell = cell.toUpperCase();
        graph.putIfAbsent(cell, new HashSet<>());
    }

    public void addDependency(String source, String dependent) {

        source = source.toUpperCase();
        dependent = dependent.toUpperCase();

        if (source.equals(dependent)) {
            throw new IllegalArgumentException(
                    "A cell cannot depend on itself."
            );
        }

        addCell(source);
        addCell(dependent);

        graph.get(source).add(dependent);
    }

    public void removeDependency(String source, String dependent) {

        source = source.toUpperCase();
        dependent = dependent.toUpperCase();

        if (graph.containsKey(source)) {
            graph.get(source).remove(dependent);
        }
    }

    public Set<String> getDependents(String cell) {

        cell = cell.toUpperCase();

        if (!graph.containsKey(cell)) {
            return Collections.emptySet();
        }

        return new HashSet<>(graph.get(cell));
    }

    public Set<String> getDependencies(String cell) {

        cell = cell.toUpperCase();

        Set<String> dependencies = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {

            if (entry.getValue().contains(cell)) {
                dependencies.add(entry.getKey());
            }
        }

        return dependencies;
    }

    /*
     * Find all cells affected by a change.
     *
     * Example:
     *
     * A1 -> B1 -> C1 -> D1
     *
     * findAffectedCells(A1)
     * returns:
     * B1, C1, D1
     */
    public Set<String> findAffectedCells(String cell) {

        cell = cell.toUpperCase();

        Set<String> affected = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(cell);

        while (!queue.isEmpty()) {

            String current = queue.remove();

            for (String dependent : getDependents(current)) {

                if (affected.add(dependent)) {
                    queue.add(dependent);
                }
            }
        }

        return affected;
    }

    /*
     * Detect whether the graph contains a cycle.
     */
    public boolean hasCycle() {

        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String cell : graph.keySet()) {

            if (detectCycle(cell, visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    private boolean detectCycle(
            String cell,
            Set<String> visited,
            Set<String> recursionStack) {

        if (recursionStack.contains(cell)) {
            return true;
        }

        if (visited.contains(cell)) {
            return false;
        }

        visited.add(cell);
        recursionStack.add(cell);

        for (String dependent : getDependents(cell)) {

            if (detectCycle(
                    dependent,
                    visited,
                    recursionStack)) {

                return true;
            }
        }

        recursionStack.remove(cell);

        return false;
    }

    /*
     * Topological sort using Kahn's algorithm.
     *
     * Returns cells in an order where dependencies
     * are calculated before dependents.
     */
    public List<String> topologicalSort(Set<String> cells) {

        Map<String, Integer> inDegree = new HashMap<>();

        for (String cell : cells) {

            int degree = 0;

            for (String dependency : getDependencies(cell)) {

                if (cells.contains(dependency)) {
                    degree++;
                }
            }

            inDegree.put(cell, degree);
        }

        Queue<String> ready = new LinkedList<>();

        for (String cell : cells) {

            if (inDegree.get(cell) == 0) {
                ready.add(cell);
            }
        }

        List<String> result = new ArrayList<>();

        while (!ready.isEmpty()) {

            String current = ready.remove();

            result.add(current);

            for (String dependent : getDependents(current)) {

                if (!cells.contains(dependent)) {
                    continue;
                }

                int newDegree =
                        inDegree.get(dependent) - 1;

                inDegree.put(dependent, newDegree);

                if (newDegree == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (result.size() != cells.size()) {

            throw new IllegalStateException(
                    "Circular reference detected."
            );
        }

        return result;
    }

    public void printGraph() {

        System.out.println("Dependency Graph:");

        for (Map.Entry<String, Set<String>> entry
                : graph.entrySet()) {

            System.out.println(
                    entry.getKey() + " -> " + entry.getValue()
            );
        }
    }
}