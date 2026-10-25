
package Worklet2;

import api.Cell;
import api.EvaluationContext;
import api.Spreadsheet;

import java.util.*;

import worklet1.ExpressionEvaluator;

/**
 * Worklet 2 standalone test runner.
 *
 * Covers:
 * 1. Dependency edge operations and defensive copies.
 * 2. Topological ordering.
 * 3. Circular-dependency prevention and detection.
 * 4. Recalculation of dependent cells.
 * 5. Formula-to-literal replacement.
 * 6. Formula error handling and recovery.
 * 7. Preservation of existing formulas after rejected updates.
 * 8. Division-by-zero handling in the expression evaluator.
 */
public class Worklet2Test {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        passed = 0;
        failed = 0;

        System.out.println("==================================================");
        System.out.println(" WORKLET 2 DEMONSTRATION & TEST SUITE");
        System.out.println("==================================================");

        // 1. Dependency graph operations
        test("Add dependency edges (A1 -> B1 -> C1)",
                Worklet2Test::testAddDependency);
        test("Remove dependency edges (A1 -> B1)",
                Worklet2Test::testRemoveDependency);
        test("Returned dependent set is a defensive copy",
                Worklet2Test::testDefensiveCopy);

        // 2. Topological ordering
        test("Topological sort returns valid calculation order",
                Worklet2Test::testTopologicalSortOrder);
        test("Subset topological sort on affected cells",
                Worklet2Test::testSubsetTopologicalSort);

        // 3. Circular dependency detection
        test("Self-dependency rejection (A1 -> A1)",
                Worklet2Test::testSelfDependency);
        test("Multi-cell circular dependency prevention",
                Worklet2Test::testMultiCellCycleDetection);
        test("CircularDependencyException on cyclic topological sort",
                Worklet2Test::testCircularDependencyExceptionThrown);
        test("Cycle prevention check (wouldCreateCycle)",
                Worklet2Test::testWouldCreateCycle);

        // 4. Recalculation and formula updates
        test("RecalculationEngine updates dependent cells",
                Worklet2Test::testRecalculationEngineFlow);
        test("Replacing a formula with a literal updates dependents",
                Worklet2Test::testFormulaToLiteralReplacement);
        test("Formula error handling and recovery",
                Worklet2Test::testFormulaErrorRecovery);
        test("Rejected circular formula update preserves old formula",
                Worklet2Test::testRejectedFormulaUpdatePreservesState);

        // 5. Division-by-zero handling
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

    // --- 1. Dependency graph operations ---

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

    // --- 2. Topological ordering ---

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

    // --- 3. Circular dependency detection ---

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
                "Rejected self-dependency must not leave a cycle"
        );
    }

    private static void testMultiCellCycleDetection() {
        DependencyGraph graph = new DependencyGraph();

        graph.addDependency("A1", "B1");
        graph.addDependency("B1", "C1");

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
        Map<String, Set<String>> cyclicGraph = new HashMap<>();

        // Construct a cycle directly to test GraphAlgorithms.
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

    // --- 4. Recalculation engine tests ---

    private static Spreadsheet createTestSpreadsheet() {
        return new Spreadsheet() {
            private final Map<String, String> cells = new HashMap<>();

            @Override
            public void setCell(String reference, String content) {
                cells.put(
                        reference.toUpperCase(Locale.ROOT),
                        content
                );
            }

            @Override
            public Object getValue(String reference) {
                String value = cells.get(
                        reference.toUpperCase(Locale.ROOT)
                );

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
    }

    private static void testRecalculationEngineFlow() {
        Spreadsheet sheet = createTestSpreadsheet();
        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(sheet);

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1+5");
        engine.setCell("C1", "=B1*2");

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

    /**
     * Replacing B1's formula with a literal should remove A1 -> B1
     * while retaining B1 -> C1 and recalculating C1.
     */
    private static void testFormulaToLiteralReplacement() {
        Spreadsheet sheet = createTestSpreadsheet();
        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(sheet);

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1+5");
        engine.setCell("C1", "=B1*2");

        assertEquals(
                15.0, toDouble(engine.getValue("B1")), 0.001,
                "B1 should initially evaluate to 15"
        );

        assertEquals(
                30.0, toDouble(engine.getValue("C1")), 0.001,
                "C1 should initially evaluate to 30"
        );

        engine.setCell("B1", "7");

        assertEquals(
                7.0, toDouble(engine.getValue("B1")), 0.001,
                "B1 should contain the new literal value"
        );

        assertEquals(
                14.0, toDouble(engine.getValue("C1")), 0.001,
                "C1 should recalculate using the new B1 value"
        );

        assertSetEquals(
                Collections.emptySet(),
                engine.getDependencyGraph().getDependencies("B1"),
                "B1 should no longer depend on A1"
        );

        assertSetEquals(
                Collections.emptySet(),
                engine.getDependencyGraph().getDependents("A1"),
                "A1 should no longer have B1 as a dependent"
        );

        assertSetEquals(
                setOf("C1"),
                engine.getDependencyGraph().getDependents("B1"),
                "C1 should remain dependent on B1"
        );

        assertTrue(
                !engine.getDependencyGraph().hasCycle(),
                "The graph should remain acyclic"
        );
    }

    /**
     * Checks that formula errors are reported and that replacing an
     * invalid formula clears the error and restores normal calculation.
     */
    private static void testFormulaErrorRecovery() {
        Spreadsheet sheet = createTestSpreadsheet();
        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(sheet);

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1/0");

        assertEquals(
                "#DIV/0!",
                engine.getDisplayValue("B1"),
                "Division-by-zero display error"
        );

        assertEquals(
                "#DIV/0!",
                engine.getError("B1"),
                "Division-by-zero error property"
        );

        // Changing the numerator alone must not clear the error.
        engine.setCell("A1", "20");

        assertEquals(
                "#DIV/0!",
                engine.getDisplayValue("B1"),
                "Error remains while divisor is zero"
        );

        // Replacing the invalid formula should recover normally.
        engine.setCell("B1", "=A1/2");

        assertEquals(
                10.0, toDouble(engine.getValue("B1")), 0.001,
                "Formula value after recovery"
        );

        assertEquals(
                "10.0",
                engine.getDisplayValue("B1"),
                "Formula display value after recovery"
        );

        assertEquals(
                null,
                engine.getError("B1"),
                "Error cleared after recovery"
        );
    }

    // --- 5. Circular formula update regression test ---

    private static void testRejectedFormulaUpdatePreservesState() {
        Spreadsheet sheet = createTestSpreadsheet();
        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(sheet);

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1+5");
        engine.setCell("C1", "=B1*2");

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

        assertEquals(
                15.0, toDouble(engine.getValue("B1")), 0.001,
                "B1's original formula must be preserved"
        );

        assertEquals(
                30.0, toDouble(engine.getValue("C1")), 0.001,
                "C1's value must remain correct"
        );

        assertSetEquals(
                setOf("A1"),
                engine.getDependencyGraph().getDependencies("B1"),
                "B1 must retain its original dependency on A1"
        );

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

    // --- 6. Division by zero in ExpressionEvaluator ---

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

    // --- Test harness utilities ---

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
                    message + " [expected=" + expected
                            + ", actual=" + actual + "]"
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
                    message + " [expected=" + expected
                            + ", actual=" + actual + "]"
            );
        }
    }

    private static void assertEquals(
            Object expected,
            Object actual,
            String message
    ) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(
                    message + " [expected=" + expected
                            + ", actual=" + actual + "]"
            );
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
