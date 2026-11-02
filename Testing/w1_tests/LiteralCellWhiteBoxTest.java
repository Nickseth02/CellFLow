
package Testing.w1_tests;

import worklet1.model.LiteralCell;

public class LiteralCellWhiteBoxTest {

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

    private static void checkThrows(String name, Runnable action) {
        try {
            action.run();
            check(name, false);
        } catch (IllegalArgumentException e) {
            check(name, true);
        }
    }

    public static void main(String[] args) {

        System.out.println("===== LITERAL CELL WHITE-BOX TESTING =====");

        // Numeric input branch
        LiteralCell number = LiteralCell.fromInput("A1", "25");
        check("Integer input", number.getValue().equals(25.0));

        // Decimal input branch
        LiteralCell decimal = LiteralCell.fromInput("A2", "12.5");
        check("Decimal input", decimal.getValue().equals(12.5));

        // Text input branch
        LiteralCell text = LiteralCell.fromInput("A3", "Hello");
        check("Text input", text.getValue().equals("Hello"));

        // Null input branch
        checkThrows("Null input rejected",
                () -> LiteralCell.fromInput("A4", null));

        // Empty input branch
        checkThrows("Empty input rejected",
                () -> LiteralCell.fromInput("A5", ""));

        // Formula input branch
        checkThrows("Formula input rejected",
                () -> LiteralCell.fromInput("A6", "=10+5"));

        // Non-finite input branch
        checkThrows("Infinity input rejected",
                () -> LiteralCell.fromInput("A7", "Infinity"));

        checkThrows("NaN input rejected",
                () -> LiteralCell.fromInput("A8", "NaN"));

        System.out.println("\n===== TEST SUMMARY =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }
}

