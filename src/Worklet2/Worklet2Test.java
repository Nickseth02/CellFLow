
package Worklet2;

import api.Cell;
import api.EvaluationContext;
import api.Spreadsheet;

import java.util.*;

import worklet1.ExpressionEvaluator;

/**
 * Anshul — Testing and Error Handling
 * Standalone test runner for Worklet 2 and integration testing.
 *
 * Covers:
 * 1. Adding and removing dependency edges.
 * 2. Topological ordering.
 * 3. Circular-dependency prevention and detection.
 * 4. Recalculation of dependent cells upon changes.
 * 5. Division-by-zero errors in formula evaluation.
 * 6. Preservation of existing formulas when circular updates are rejected.
 */
public class Worklet2Test {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" WORKLET 2 DEMONSTRATION & TEST SUITE");
        System.out.println("==================================================");

        // 1. Dependency Graph: Add, Remove, Defensive Copies
        test("Add dependency edges (A1 -> B1 -> C1)",
                Worklet2Test::testAddDependency);
        test("Remove dependency edges (A1 -> B1)",
                Worklet2Test::testRemoveDependency);
        test("Returned dependent set is a defensive copy",
                Worklet2Test::testDefensiveCopy);

        // 2. Topological Ordering
        test("Topological sort returns valid calculation order",
                Worklet2Test::testTopologicalSortOrder);
        test("Subset topological sort on affected cells",
                Worklet2Test::testSubsetTopologicalSort);

        // 3. Circular Dependency Detection
        test("Self-dependency rejection (A1 -> A1)",
                Worklet2Test::testSelfDependency);
        test("Multi-cell circular dependency prevention",
                Worklet2Test::testMultiCellCycleDetection);
        test("CircularDependencyException on cyclic topological sort",
                Worklet2Test::testCircularDependencyExceptionThrown);
        test("Cycle prevention check (wouldCreateCycle)",
                Worklet2Test::testWouldCreateCycle);

        // 4. Recalculation of Dependent Cells
        test("RecalculationEngine updates dependent cells",
                Worklet2Test::testRecalculationEngineFlow);

        // 5. Formula update regression test
        test("Rejected circular formula update preserves old formula",
                Worklet2Test::testRejectedFormulaUpdatePreservesState);

        // 6. Division-by-Zero Errors
        test("Division-by-zero throws ArithmeticException",
                Worklet2Test::testDivisionByZero);

        // Summary
        System.out.println("\n--------------------------------------------------");
        System.out.println("Execution Summary:");
        System.out.println("  PASSED: " + passed);
        System.out.println("  FAILED: " + failed);
        System.out.println("--------------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- 1. Edge Operations ---

    private static void testAddDependency() {
        DependencyGraph graph = new DependencyGraph();

        graph.addDependency("A1", "B1");
        graph.addDependency("B1", "C1");

        assertSetEquals(
                setOf("B1"),
                graph.getDependents("A1"),
                "A1 should have dependent B1"
        );

        assertSetEquals(
                setOf("A1"),
                graph.getDependencies("B1"),
                "B1 should depend on A1"
        );

        assertSetEquals(
                setOf("C1"),
                graph.getDependents("B1"),
                "B1 should have dependent C1"
        );

        assertSetEquals(
                setOf("B1"),
                graph.getDependencies("C1"),
                "C1 should depend on B1"
        );
    }

    private static void testRemoveDependency() {
        DependencyGraph graph = new DependencyGraph();

        graph.addDependency("A1", "B1");
        graph.removeDependency("A1", "B1");

        assertSetEquals(
                Collections.emptySet(),
                graph.getDependents("A1"),
                "A1 should have no dependents"
        );

        assertSetEquals(
                Collections.emptySet(),
                graph.getDependencies("B1"),
                "B1 should have no dependencies"
        );
    }

    private static void testDefensiveCopy() {
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency("A1", "B1");

        Set<String> dependents = graph.getDependents("A1");
        dependents.add("FAKE_CELL");

        assertSetEquals(
                setOf("B1"),
                graph.getDependents("A1"),
                "Mutating the returned set must not alter graph internals"
        );
    }

    // --- 2. Topological Ordering ---

    private static void testTopologicalSortOrder() {
        Map<String, Set<String>> graph = new HashMap<>();

        addEdge(graph, "A1", "B1");
        addEdge(graph, "B1", "C1");
        addEdge(graph, "A1", "C1");
        addEdge(graph, "C1", "D1");

        List<String> order = GraphAlgorithms.topologicalSort(graph);

        assertTrue(
                order.indexOf("A1") < order.indexOf("B1"),
                "A1 must come before B1"
        );

        assertTrue(
                order.indexOf("B1") < order.indexOf("C1"),
                "B1 must come before C1"
        );

        assertTrue(
                order.indexOf("C1") < order.indexOf("D1"),
                "C1 must come before D1"
        );
    }

    private static void testSubsetTopologicalSort() {
        DependencyGraph graph = new DependencyGraph();

        graph.addDependency("A1", "B1");
        graph.addDependency("B1", "C1");
        graph.addDependency("B1", "D1");

        Set<String> affected = graph.findAffectedCells("A1");
        List<String> order = graph.topologicalSort(affected);

        assertTrue(
                order.indexOf("B1") < order.indexOf("C1"),
                "B1 must precede C1"
        );

        assertTrue(
                order.indexOf("B1") < order.indexOf("D1"),
                "B1 must precede D1"
        );
    }

    // --- 3. Circular Dependency Detection ---

    private static void testSelfDependency() {
        DependencyGraph graph = new DependencyGraph();
        boolean threw = false;

        try {
            graph.addDependency("A1", "A1");
        } catch (IllegalArgumentException e) {
            threw = true;
        }

        assertTrue(
                threw,
                "Self-dependency A1 -> A1 must throw IllegalArgumentException"
        );

        assertTrue(
                !graph.hasCycle(),
                "Rejected self-dependency must not leave a cycle in the graph"
        );
    }

    private static void testMultiCellCycleDetection() {
        DependencyGraph graph = new DependencyGraph();

        // Add the first two edges of A1 -> B1 -> C1.
        graph.addDependency("A1", "B1");
        graph.addDependency("B1", "C1");

        // The third edge would create A1 -> B1 -> C1 -> A1.
        boolean threw = false;

        try {
            graph.addDependency("C1", "A1");
        } catch (IllegalArgumentException e) {
            threw = true;
        }

        assertTrue(
                threw,
                "Adding C1 -> A1 must be rejected because it creates a cycle"
        );

        assertTrue(
                !graph.hasCycle(),
                "The graph must remain acyclic after rejecting the edge"
        );

        assertSetEquals(
                Collections.emptySet(),
                graph.getDependents("C1"),
                "C1 must not retain A1 as a dependent after rejection"
        );
    }

    private static void testCircularDependencyExceptionThrown() {
        // Deliberately construct a cyclic map directly.
        // This tests GraphAlgorithms independently of DependencyGraph's
        // cycle-prevention checks.
        Map<String, Set<String>> cyclicGraph = new HashMap<>();

        addEdge(cyclicGraph, "A1", "B1");
        addEdge(cyclicGraph, "B1", "A1");

        boolean threwCorrectException = false;

        try {
            GraphAlgorithms.topologicalSort(cyclicGraph);
        } catch (GraphAlgorithms.CircularDependencyException e) {
            threwCorrectException = true;

            assertTrue(
                    e.getCellsInCycle().contains("A1")
                            && e.getCellsInCycle().contains("B1"),
                    "Reported cyclic cells must include A1 and B1"
            );
        }

        assertTrue(
                threwCorrectException,
                "A cyclic graph must throw CircularDependencyException"
        );
    }

    private static void testWouldCreateCycle() {
        Map<String, Set<String>> graph = new HashMap<>();

        addEdge(graph, "A1", "B1");
        addEdge(graph, "B1", "C1");

        assertTrue(
                GraphAlgorithms.wouldCreateCycle(graph, "C1", "A1"),
                "Adding C1 -> A1 should be flagged as creating a cycle"
        );

        assertTrue(
                !GraphAlgorithms.wouldCreateCycle(graph, "A1", "D1"),
                "Adding A1 -> D1 does not create a cycle"
        );
    }

    // --- 4. Recalculation Engine ---

    private static void testRecalculationEngineFlow() {
        Spreadsheet sheet = new Spreadsheet() {
            private final Map<String, String> cells = new HashMap<>();

            @Override
            public void setCell(String reference, String content) {
                cells.put(reference.toUpperCase(Locale.ROOT), content);
            }

            @Override
            public Object getValue(String reference) {
                String value = cells.get(reference.toUpperCase(Locale.ROOT));

                if (value == null) {
                    return null;
                }

                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return value;
                }
            }

            @Override
            public String getDisplayValue(String reference) {
                Object value = getValue(reference);
                return value == null ? "" : value.toString();
            }

            @Override
            public String getError(String reference) {
                return null;
            }

            @Override
            public Cell getCell(String reference) {
                return null;
            }
        };

        RecalculationEngineImpl engine = new RecalculationEngineImpl(sheet);

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1+5");
        engine.setCell("C1", "=B1*2");

        // Verify initial values.
        assertEquals(
                10.0, toDouble(engine.getValue("A1")), 0.001,
                "Initial A1"
        );

        assertEquals(
                15.0, toDouble(engine.getValue("B1")), 0.001,
                "Initial B1"
        );

        assertEquals(
                30.0, toDouble(engine.getValue("C1")), 0.001,
                "Initial C1"
        );

        // Change A1; B1 and C1 should recalculate.
        engine.setCell("A1", "20");

        assertEquals(
                20.0, toDouble(engine.getValue("A1")), 0.001,
                "Updated A1"
        );

        assertEquals(
                25.0, toDouble(engine.getValue("B1")), 0.001,
                "Recalculated B1"
        );

        assertEquals(
                50.0, toDouble(engine.getValue("C1")), 0.001,
                "Recalculated C1"
        );
    }

    // --- 5. Circular Formula Update Regression Test ---

    private static void testRejectedFormulaUpdatePreservesState() {
        Spreadsheet sheet = new Spreadsheet() {
            private final Map<String, String> cells = new HashMap<>();

            @Override
            public void setCell(String reference, String content) {
                cells.put(reference.toUpperCase(Locale.ROOT), content);
            }

            @Override
            public Object getValue(String reference) {
                String value = cells.get(reference.toUpperCase(Locale.ROOT));

                if (value == null) {
                    return null;
                }

                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return value;
                }
            }

            @Override
            public String getDisplayValue(String reference) {
                Object value = getValue(reference);
                return value == null ? "" : value.toString();
            }

            @Override
            public String getError(String reference) {
                return null;
            }

            @Override
            public Cell getCell(String reference) {
                return null;
            }
        };

        RecalculationEngineImpl engine = new RecalculationEngineImpl(sheet);

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1+5");
        engine.setCell("C1", "=B1*2");

        // Attempt to replace B1 with a formula that depends on C1.
        // Since C1 already depends on B1, this would create a cycle.
        boolean rejected = false;

        try {
            engine.setCell("B1", "=C1+5");
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        assertTrue(
                rejected,
                "Circular formula update must be rejected"
        );

        // The old formula for B1 must still evaluate to 15.
        assertEquals(
                15.0, toDouble(engine.getValue("B1")), 0.001,
                "B1's original formula must be preserved"
        );

        // C1 must retain its original value of 30.
        assertEquals(
                30.0, toDouble(engine.getValue("C1")), 0.001,
                "C1's value must remain correct"
        );

        // The original dependency A1 -> B1 must remain.
        assertSetEquals(
                setOf("A1"),
                engine.getDependencyGraph().getDependencies("B1"),
                "B1 must retain its original dependency on A1"
        );

        // The original dependency B1 -> C1 must remain.
        assertSetEquals(
                setOf("C1"),
                engine.getDependencyGraph().getDependents("B1"),
                "C1 must remain a dependent of B1"
        );

        assertTrue(
                !engine.getDependencyGraph().hasCycle(),
                "The graph must remain acyclic after the rejected update"
        );
    }

    // --- 6. Division by Zero ---

    private static void testDivisionByZero() {
        EvaluationContext context = ref -> 0.0;
        boolean threwArithmetic = false;

        try {
            ExpressionEvaluator.evaluate("=100 / 0", context);
        } catch (ArithmeticException e) {
            threwArithmetic = true;
        }

        assertTrue(
                threwArithmetic,
                "ExpressionEvaluator must throw ArithmeticException when dividing by zero"
        );
    }

    // --- Test Harness Utilities ---

    private static void test(String name, Runnable testCase) {
        try {
            testCase.run();
            passed++;
            System.out.println("PASS | " + name);
        } catch (Throwable error) {
            failed++;
            System.out.println(
                    "FAIL | " + name + " | "
                            + error.getClass().getSimpleName()
                            + ": " + error.getMessage()
            );
        }
    }

    private static void addEdge(
            Map<String, Set<String>> graph,
            String from,
            String to
    ) {
        graph.computeIfAbsent(from, key -> new HashSet<>()).add(to);
    }

    private static Set<String> setOf(String... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    private static double toDouble(Object value) {
        if (value == null) {
            throw new AssertionError("Value to evaluate cannot be null");
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return Double.parseDouble(String.valueOf(value));
    }

    private static void assertSetEquals(
            Set<String> expected,
            Set<String> actual,
            String message
    ) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                    message + " [expected=" + expected + ", actual=" + actual + "]"
            );
        }
    }

    private static void assertEquals(
            double expected,
            double actual,
            double delta,
            String message
    ) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(
                    message + " [expected=" + expected + ", actual=" + actual + "]"
            );
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
