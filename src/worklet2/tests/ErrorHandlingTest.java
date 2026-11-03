
package worklet2.tests;

import worklet2.engine.RecalculationEngineImpl;
import api.Cell;
import api.Spreadsheet;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Dedicated tests for Worklet 2 error handling.
 */
public class ErrorHandlingTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println(" WORKLET 2 ERROR HANDLING TESTS");
        System.out.println("======================================");

        test("Direct division-by-zero detection",
                ErrorHandlingTest::testDivisionByZero);

        test("Error propagation through dependent cells",
                ErrorHandlingTest::testErrorPropagation);

        test("Error recovery after formula correction",
                ErrorHandlingTest::testErrorRecovery);

        test("Error clearing after formula-to-literal replacement",
                ErrorHandlingTest::testLiteralReplacement);

        test("Invalid value detection and propagation",
                ErrorHandlingTest::testInvalidValue);

        System.out.println("\n--------------------------------------");
        System.out.println("PASSED: " + passed);
        System.out.println("FAILED: " + failed);
        System.out.println("--------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static Spreadsheet createTestSpreadsheet() {

        return new Spreadsheet() {

            private final Map<String, String> cells = new HashMap<>();

            @Override
            public void setCell(String reference, String content) {
                cells.put(reference.toUpperCase(Locale.ROOT), content);
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

    private static void testDivisionByZero() {

        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(createTestSpreadsheet());

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1/0");

        assertEquals("#DIV/0!", engine.getError("B1"));
        assertEquals("#DIV/0!", engine.getDisplayValue("B1"));
    }

    private static void testErrorPropagation() {

        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(createTestSpreadsheet());

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1/0");
        engine.setCell("C1", "=B1+5");

        assertEquals("#DIV/0!", engine.getError("B1"));
        assertEquals("#DIV/0!", engine.getError("C1"));

        assertEquals("#DIV/0!", engine.getDisplayValue("C1"));

        if (!engine.getErrorHandler().getError("C1").isPropagated()) {
            throw new AssertionError("C1 error should be propagated");
        }
    }

    private static void testErrorRecovery() {

        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(createTestSpreadsheet());

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1/0");
        engine.setCell("C1", "=B1+5");

        engine.setCell("B1", "=A1/2");

        assertEquals(null, engine.getError("B1"));
        assertEquals(null, engine.getError("C1"));

        assertDoubleEquals(5.0, engine.getValue("B1"));
        assertDoubleEquals(10.0, engine.getValue("C1"));
    }

    private static void testLiteralReplacement() {

        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(createTestSpreadsheet());

        engine.setCell("A1", "10");
        engine.setCell("B1", "=A1/0");
        engine.setCell("C1", "=B1+5");

        engine.setCell("B1", "7");

        assertEquals(null, engine.getError("B1"));
        assertEquals(null, engine.getError("C1"));

        assertDoubleEquals(7.0, engine.getValue("B1"));
        assertDoubleEquals(12.0, engine.getValue("C1"));
    }

    private static void testInvalidValue() {

        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(createTestSpreadsheet());

        engine.setCell("A1", "hello");
        engine.setCell("B1", "=A1+5");
        engine.setCell("C1", "=B1*2");

        assertEquals("#VALUE!", engine.getError("B1"));
        assertEquals("#VALUE!", engine.getError("C1"));
        assertEquals("#VALUE!", engine.getDisplayValue("C1"));
    }

    private static void test(
            String name,
            Runnable testCase) {

        try {
            testCase.run();
            passed++;
            System.out.println("PASS | " + name);

        } catch (Throwable error) {
            failed++;
            System.out.println("FAIL | " + name);
            System.out.println("       " + error.getMessage());
        }
    }

    private static void assertEquals(
            Object expected,
            Object actual) {

        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(
                    "Expected: " + expected + ", Actual: " + actual
            );
        }
    }

    private static void assertDoubleEquals(
            double expected,
            Object actual) {

        if (!(actual instanceof Number)
                || Math.abs(expected - ((Number) actual).doubleValue()) > 0.001) {

            throw new AssertionError(
                    "Expected: " + expected + ", Actual: " + actual
            );
        }
    }
}
