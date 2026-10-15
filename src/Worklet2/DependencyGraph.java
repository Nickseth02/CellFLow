
import java.util.*;

public class DependencyGraph {

    // Maps each cell to the cells that depend on it
    private final Map<String, Set<String>> graph = new HashMap<>();

    // Add a cell
    public void addCell(String cell) {
        graph.putIfAbsent(cell, new HashSet<>());
    }

    // Add dependency: source -> dependent
    public void addDependency(String source, String dependent) {
        if (source.equals(dependent)) {
            throw new IllegalArgumentException(
                "A cell cannot depend on itself."
            );
        }

        addCell(source);
        addCell(dependent);
        graph.get(source).add(dependent);
    }

    // Remove dependency
    public void removeDependency(String source, String dependent) {
        if (graph.containsKey(source)) {
            graph.get(source).remove(dependent);
        }
    }

    // Get cells that depend on the given cell
    public Set<String> getDependents(String cell) {
        if (!graph.containsKey(cell)) {
            return Collections.emptySet();
        }

        return new HashSet<>(graph.get(cell));
    }

    // Get cells that the given cell depends on
    public Set<String> getDependencies(String cell) {
        Set<String> dependencies = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getValue().contains(cell)) {
                dependencies.add(entry.getKey());
            }
        }

        return dependencies;
    }

    // Print the graph
    public void printGraph() {
        System.out.println("Dependency Graph:");

        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    // Demonstration
    public static void main(String[] args) {
        DependencyGraph dg = new DependencyGraph();

        // Add A1 -> B1 and B1 -> C1
        dg.addDependency("A1", "B1");
        dg.addDependency("B1", "C1");

        System.out.println("Initial graph:");
        dg.printGraph();

        System.out.println("\nDependents of A1: "
                + dg.getDependents("A1"));
        System.out.println("Dependencies of B1: "
                + dg.getDependencies("B1"));
        System.out.println("Dependents of B1: "
                + dg.getDependents("B1"));
        System.out.println("Dependencies of C1: "
                + dg.getDependencies("C1"));

        // Replace B1's formula dependency:
        // Remove A1 -> B1 and add D1 -> B1
        System.out.println("\nReplacing B1 formula dependencies:");

        dg.removeDependency("A1", "B1");
        dg.addDependency("D1", "B1");

        dg.printGraph();

        System.out.println("\nNew dependencies of B1: "
                + dg.getDependencies("B1"));
        System.out.println("Dependents of A1: "
                + dg.getDependents("A1"));
    }
}
