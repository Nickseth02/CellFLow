
import worklet1.model.SpreadsheetModel;
import worklet2.engine.RecalculationEngineImpl;

public class W1W2IntegrationTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String test, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + test);
            passed++;
        } else {
            System.out.println("[FAIL] " + test);
            failed++;
        }
    }

    private static boolean isValue(
            RecalculationEngineImpl engine,
            String cell,
            double expected) {

        Object actual = engine.getValue(cell);

        return actual instanceof Number &&
                Math.abs(((Number) actual).doubleValue()
                        - expected) < 0.0001;
    }

    public static void main(String[] args) {

        System.out.println("===== W1 + W2 INTEGRATION TEST =====");

        // W1 spreadsheet model connected to W2 engine
        SpreadsheetModel sheet = new SpreadsheetModel();
        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(sheet);

        // Test 1: Initial cell values
        engine.setCell("A1", "10");
        engine.setCell("A2", "20");

        check("W1 stores literal cell values",
                isValue(engine, "A1", 10) &&
                isValue(engine, "A2", 20));

        // Test 2: Create dependent formulas
        engine.setCell("A3", "=A1+A2");
        engine.setCell("A4", "=A3*2");

        check("W1 evaluates formulas through W2",
                isValue(engine, "A3", 30) &&
                isValue(engine, "A4", 60));

        // Test 3: Verify dependency relationships
        check("Dependency A1 -> A3 is registered",
                engine.getDependencyGraph()
                        .getDependents("A1").contains("A3"));

        check("Dependency A2 -> A3 is registered",
                engine.getDependencyGraph()
                        .getDependents("A2").contains("A3"));

        check("Dependency A3 -> A4 is registered",
                engine.getDependencyGraph()
                        .getDependents("A3").contains("A4"));

        // Test 4: Change source cell
        engine.setCell("A1", "50");

        check("A3 recalculates after A1 changes",
                isValue(engine, "A3", 70));

        check("A4 recalculates through dependency chain",
                isValue(engine, "A4", 140));

        // Test 5: Change another source cell
        engine.setCell("A2", "40");

        check("A3 updates after A2 changes",
                isValue(engine, "A3", 90));

        check("A4 updates after A2 changes",
                isValue(engine, "A4", 180));

        // Test summary
        System.out.println("\n===== TEST SUMMARY =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total: " + (passed + failed));

        if (failed == 0) {
            System.out.println(
                    "W1 + W2 INTEGRATION TEST PASSED");
        } else {
            System.out.println(
                    "W1 + W2 INTEGRATION TEST NEEDS ATTENTION");
        }
    }
}
