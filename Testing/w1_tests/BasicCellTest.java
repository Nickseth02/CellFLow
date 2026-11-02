
package Testing.w1_tests;

import worklet1.model.BasicCell;
import worklet1.model.LiteralCell;

public class BasicCellTest {

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

    public static void main(String[] args) {

        System.out.println("===== BASIC CELL TESTING =====");

        // Create a literal cell
        BasicCell cell = LiteralCell.fromInput("A1", "10");

        check("Cell creation", cell != null);
        check("Cell reference", "A1".equals(cell.getReference()));
        check("Original content", "10".equals(cell.getContent()));
        check("Display value", "10".equals(cell.getDisplayValue()));
        check("Evaluated value", Double.valueOf(10.0).equals(cell.getValue()));
        check("No cell error", cell.getError() == null);
        check("No formula dependencies", cell.getDependencies().isEmpty());

        // Test a text cell
        BasicCell textCell = LiteralCell.fromInput("B1", "Hello");

        check("Text cell creation", textCell != null);
        check("Text cell display", "Hello".equals(textCell.getDisplayValue()));
        check("Text cell value", "Hello".equals(textCell.getValue()));

        System.out.println("\n===== TEST SUMMARY =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }
}
