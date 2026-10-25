package worklet2.tests;

import worklet2.graph.DependencyGraph;
import java.util.Set;

public class DependencyGraphTest {

    public static void main(String[] args) {

        DependencyGraph graph =
                new DependencyGraph();

        // A1 -> B1 -> C1 -> D1
        graph.addDependency("A1", "B1");
        graph.addDependency("B1", "C1");
        graph.addDependency("C1", "D1");

        System.out.println("=== Dependency Graph ===");
        graph.printGraph();

        System.out.println();

        System.out.println(
                "Dependents of A1: "
                        + graph.getDependents("A1")
        );

        System.out.println(
                "Dependencies of B1: "
                        + graph.getDependencies("B1")
        );

        System.out.println(
                "Affected by A1: "
                        + graph.findAffectedCells("A1")
        );

        Set<String> affected =
                graph.findAffectedCells("A1");

        System.out.println(
                "Calculation order: "
                        + graph.topologicalSort(affected)
        );

        System.out.println(
                "Has cycle: "
                        + graph.hasCycle()
        );
    }
}
