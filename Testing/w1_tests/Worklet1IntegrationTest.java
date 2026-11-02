
package Testing.w1_tests;

import worklet1.model.SpreadsheetModel;
import worklet1.commands.SetCellCommand;
import worklet1.commands.UndoRedoManager;

public class Worklet1IntegrationTest {

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

    private static boolean isEqual(Object actual, double expected) {
        return actual instanceof Number &&
                Math.abs(((Number) actual).doubleValue() - expected) < 0.0001;
    }

    public static void main(String[] args) {

        System.out.println("===== WORKLET 1 INTEGRATION TESTING =====");

        SpreadsheetModel sheet = new SpreadsheetModel();
        UndoRedoManager history = new UndoRedoManager();

        // 1. Enter cell values
        sheet.setCell("A1", "10");
        sheet.setCell("B1", "20");

        check("Cell input integration",
                isEqual(sheet.getValue("A1"), 10) &&
                isEqual(sheet.getValue("B1"), 20));

        // 2. Formula evaluation
        sheet.setCell("C1", "=A1+B1");

        check("Formula calculation integration",
                isEqual(sheet.getValue("C1"), 30));

        // 3. Execute a command to update A1
        SetCellCommand command =
                new SetCellCommand(sheet, "A1", "50");

        history.execute(command);

        check("Command execution integration",
                isEqual(sheet.getValue("A1"), 50));

        // 4. Verify formula recalculation
        check("Formula recalculation after update",
                isEqual(sheet.getValue("C1"), 70));

        // 5. Undo
        history.undo();

        check("Undo restores original cell value",
                isEqual(sheet.getValue("A1"), 10));

        check("Undo restores formula result",
                isEqual(sheet.getValue("C1"), 30));

        // 6. Redo
        history.redo();

        check("Redo reapplies cell update",
                isEqual(sheet.getValue("A1"), 50));

        check("Redo updates formula result",
                isEqual(sheet.getValue("C1"), 70));

        System.out.println("\n===== TEST SUMMARY =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }
}
