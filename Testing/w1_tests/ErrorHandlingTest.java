
package Testing.w1_tests;

import worklet1.model.SpreadsheetModel;

public class ErrorHandlingTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name);
            failed++;
        }
    }

    public static void main(String[] args) {

        System.out.println("===== ERROR HANDLING TESTING =====");

        SpreadsheetModel sheet = new SpreadsheetModel();

        // Division by zero
        sheet.setCell("A1", "=10/0");
        String divisionError = sheet.getError("A1");

        check("Division by zero is detected",
                divisionError != null && !divisionError.isEmpty());

        // Invalid formula
        sheet.setCell("A2", "=10+");
        String formulaError = sheet.getError("A2");

        check("Invalid formula is detected",
                formulaError != null && !formulaError.isEmpty());

        System.out.println("\n===== TEST SUMMARY =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }
}

