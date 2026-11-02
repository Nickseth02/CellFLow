
package Testing.w1_tests;

import worklet1.model.SpreadsheetModel;

public class EvaluatorTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + testName);
            passed++;
        } else {
            System.out.println("[FAIL] " + testName);
            failed++;
        }
    }

    private static boolean approximatelyEqual(Object actual, double expected) {
        if (!(actual instanceof Number)) {
            return false;
        }

        return Math.abs(((Number) actual).doubleValue() - expected) < 0.0001;
    }

    public static void main(String[] args) {

        System.out.println("===== FORMULA EVALUATOR TESTING =====");

        SpreadsheetModel sheet = new SpreadsheetModel();

        // Store input values
        sheet.setCell("A1", "10");
        sheet.setCell("B1", "5");

        // Arithmetic formula tests
        sheet.setCell("C1", "=10+5");
        check("Addition", approximatelyEqual(sheet.getValue("C1"), 15));

        sheet.setCell("C2", "=10-5");
        check("Subtraction", approximatelyEqual(sheet.getValue("C2"), 5));

        sheet.setCell("C3", "=10*5");
        check("Multiplication", approximatelyEqual(sheet.getValue("C3"), 50));

        sheet.setCell("C4", "=10/5");
        check("Division", approximatelyEqual(sheet.getValue("C4"), 2));

        // Operator precedence
        sheet.setCell("C5", "=10+5*2");
        check("Operator precedence", approximatelyEqual(sheet.getValue("C5"), 20));

        // Bracket evaluation
        sheet.setCell("C6", "=(10+5)*2");
        check("Bracket evaluation", approximatelyEqual(sheet.getValue("C6"), 30));

        // Cell reference evaluation
        sheet.setCell("C7", "=A1+B1");
        check("Cell reference formula", approximatelyEqual(sheet.getValue("C7"), 15));

        System.out.println("\n===== TEST SUMMARY =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }
}
