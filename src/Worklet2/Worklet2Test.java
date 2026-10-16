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
 * 2. Correct topological ordering (via GraphAlgorithms & DependencyGraph).
 * 3. Circular-dependency detection (self-loops, multi-cell loops, Kahn rejection).
 * 4. Recalculation of dependent cells upon changes (RecalculationEngineImpl).
 * 5. Division-by-zero errors in formula evaluation (ExpressionEvaluator).
 */
public class Worklet2Test {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" WORKLET 2 DEMONSTRATION & TEST SUITE");
        System.out.println("==================================================");

        // 1. Dependency Graph: Add, Remove, Defensive Copies
        test("Add dependency edges (A1 -> B1 -> C1)", Worklet2Test::testAddDependency);
        test("Remove dependency edges (A1 -> B1)", Worklet2Test::testRemoveDependency);
        test("Returned dependent set is a defensive copy", Worklet2Test::testDefensiveCopy);

        // 2. Topological Ordering
        test("Topological sort returns valid calculation order", Worklet2Test::testTopologicalSortOrder);
        test("Subset topological sort on affected cells", Worklet2Test::testSubsetTopologicalSort);

        // 3. Circular-Dependency Detection
        test("Self-dependency rejection (A1 -> A1)", Worklet2Test::testSelfDependency);
        test("Multi-cell circular dependency detection via hasCycle()", Worklet2Test::testMultiCellCycleDetection);
        test("CircularDependencyException thrown on cyclic topological sort", Worklet2Test::testCircularDependencyExceptionThrown);
        test("Cycle prevention check (wouldCreateCycle)", Worklet2Test::testWouldCreateCycle);

        // 4. Recalculation of Dependent Cells
        test("RecalculationEngine updates dependent cells when upstream cell changes", Worklet2Test::testRecalculationEngineFlow);

        // 5. Division-by-Zero Errors
        test("Division-by-zero throws ArithmeticException in ExpressionEvaluator", Worklet2Test::testDivisionByZero);

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

        assertSetEquals(setOf("B1"), graph.getDependents("A1"), "A1 should have dependent B1");
        assertSetEquals(setOf("A1"), graph.getDependencies("B1"), "B1 should depend on A1");
        assertSetEquals(setOf("C1"), graph.getDependents("B1"), "B1 should have dependent C1");
        assertSetEquals(setOf("B1"), graph.getDependencies("C1"), "C1 should depend on B1");
    }

    private static void testRemoveDependency() {
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency("A1", "B1");
        graph.removeDependency("A1", "B1");

        assertSetEquals(Collections.emptySet(), graph.getDependents("A1"), "A1 should have no dependents");
        assertSetEquals(Collections.emptySet(), graph.getDependencies("B1"), "B1 should have no dependencies");
    }

    private static void testDefensiveCopy() {
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency("A1", "B1");

        Set<String> dependents = graph.getDependents("A1");
        dependents.add("FAKE_CELL");

        assertSetEquals(setOf("B1"), graph.getDependents("A1"),
                "Mutating the returned set must not alter graph internals");
    }

    // --- 2. Topological Ordering ---

    private static void testTopologicalSortOrder() {
        Map<String, Set<String>> graph = new HashMap<>();
        addEdge(graph, "A1", "B1");
        addEdge(graph, "B1", "C1");
        addEdge(graph, "A1", "C1");
        addEdge(graph, "C1", "D1");

        List<String> order = GraphAlgorithms.topologicalSort(graph);

        assertTrue(order.indexOf("A1") < order.indexOf("B1"), "A1 must come before B1");
        assertTrue(order.indexOf("B1") < order.indexOf("C1"), "B1 must come before C1");
        assertTrue(order.indexOf("C1") < order.indexOf("D1"), "C1 must come before D1");
    }

    private static void testSubsetTopologicalSort() {
        DependencyGraph dg = new DependencyGraph();
        dg.addDependency("A1", "B1");
        dg.addDependency("B1", "C1");
        dg.addDependency("B1", "D1");

        Set<String> affected = dg.findAffectedCells("A1"); // {B1, C1, D1}
        List<String> order = dg.topologicalSort(affected);

        assertTrue(order.indexOf("B1") < order.indexOf("C1"), "B1 must precede C1");
        assertTrue(order.indexOf("B1") < order.indexOf("D1"), "B1 must precede D1");
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
        assertTrue(threw, "Self-dependency (A1 -> A1) must throw IllegalArgumentException");
    }

    private static void testMultiCellCycleDetection() {
        DependencyGraph graph = new DependencyGraph();
        // A1 -> B1 -> C1 -> A1
        graph.addDependency("A1", "B1");
        graph.addDependency("B1", "C1");
        graph.addDependency("C1", "A1");

        assertTrue(graph.hasCycle(), "Graph containing A1 -> B1 -> C1 -> A1 must return hasCycle() == true");
    }

    private static void testCircularDependencyExceptionThrown() {
        Map<String, Set<String>> cyclicGraph = new HashMap<>();
        addEdge(cyclicGraph, "A1", "B1");
        addEdge(cyclicGraph, "B1", "A1");

        boolean threwCorrectException = false;
        try {
            GraphAlgorithms.topologicalSort(cyclicGraph);
        } catch (GraphAlgorithms.CircularDependencyException e) {
            threwCorrectException = true;
            assertTrue(e.getCellsInCycle().contains("A1") && e.getCellsInCycle().contains("B1"),
                    "Reported cyclic cells must include A1 and B1");
        }

        assertTrue(threwCorrectException, "Cyclic graph must throw CircularDependencyException on topologicalSort");
    }

    private static void testWouldCreateCycle() {
        Map<String, Set<String>> graph = new HashMap<>();
        addEdge(graph, "A1", "B1");
        addEdge(graph, "B1", "C1");

        assertTrue(GraphAlgorithms.wouldCreateCycle(graph, "C1", "A1"),
                "Adding edge C1 -> A1 should be flagged as creating a cycle");
        assertTrue(!GraphAlgorithms.wouldCreateCycle(graph, "A1", "D1"),
                "Adding edge A1 -> D1 does not create a cycle");
    }

    // --- 4. Recalculation Engine ---

    private static void testRecalculationEngineFlow() {
        Spreadsheet sheet = new Spreadsheet() {
            private final Map<String, String> cells = new HashMap<>();

            @Override
            public void setCell(String reference, String content) {
                cells.put(reference.toUpperCase(), content);
            }

            @Override
            public Object getValue(String reference) {
                String val = cells.get(reference.toUpperCase());
                if (val == null) return null;
                try {
                    return Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    return val;
                }
            }

            @Override
            public String getDisplayValue(String reference) {
                Object val = getValue(reference);
                return val == null ? "" : val.toString();
            }

            @Override
            public String getError(String reference) { return null; }

            @Override
            public Cell getCell(String reference) { return null; }
        };

        RecalculationEngineImpl engine = new RecalculationEngineImpl(sheet);

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1+5");
        engine.setCell("C1", "=B1*2");

        // Validate values after initial setup
        assertEquals(10.0, toDouble(engine.getValue("A1")), 0.001, "Initial A1");
        assertEquals(15.0, toDouble(engine.getValue("B1")), 0.001, "Initial B1");
        assertEquals(30.0, toDouble(engine.getValue("C1")), 0.001, "Initial C1");

        // Update upstream cell: A1 = 20 -> triggers recalculation cascade
        engine.setCell("A1", "20");

        assertEquals(20.0, toDouble(engine.getValue("A1")), 0.001, "Updated A1");
        assertEquals(25.0, toDouble(engine.getValue("B1")), 0.001, "Recalculated B1");
        assertEquals(50.0, toDouble(engine.getValue("C1")), 0.001, "Recalculated C1");
    }

    // --- 5. Division by Zero ---

    private static void testDivisionByZero() {
        EvaluationContext context = ref -> 0.0;
        boolean threwArithmetic = false;

        try {
            ExpressionEvaluator.evaluate("=100 / 0", context);
        } catch (ArithmeticException e) {
            threwArithmetic = true;
        }

        assertTrue(threwArithmetic, "ExpressionEvaluator must throw ArithmeticException when dividing by zero");
    }

    // --- Test Harness Utilities ---

    private static void test(String name, Runnable testCase) {
        try {
            testCase.run();
            passed++;
            System.out.println("PASS | " + name);
        } catch (Throwable error) {
            failed++;
            System.out.println("FAIL | " + name + " | " + error.getMessage());
        }
    }

    private static void addEdge(Map<String, Set<String>> graph, String from, String to) {
        graph.computeIfAbsent(from, k -> new HashSet<>()).add(to);
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

    private static void assertSetEquals(Set<String> expected, Set<String> actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(msg + " [expected=" + expected + ", actual=" + actual + "]");
        }
    }

    private static void assertEquals(double expected, double actual, double delta, String msg) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(msg + " [expected=" + expected + ", actual=" + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError(msg);
        }
    }
}