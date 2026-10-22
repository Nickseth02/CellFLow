
package worklet1;

import worklet1.model.SpreadsheetModel;
import api.Cell;

public class CellDemo {

    public static void main(String[] args) {
        System.out.println("=== Cell Module Test ===");

        SpreadsheetModel model = new SpreadsheetModel();

        // Test 1: Store a literal value
        model.setCell("A1", "25");
        System.out.println("Test 1 - Set A1 = 25");
        System.out.println("Value: " + model.getValue("A1"));
        System.out.println("Display: " + model.getDisplayValue("A1"));

        // Test 2: Retrieve the cell object
        Cell cell = model.getCell("A1");
        System.out.println("\nTest 2 - Retrieve cell");
        System.out.println("Reference: " + cell.getReference());
        System.out.println("Value: " + cell.getValue());

        // Test 3: Store text
        model.setCell("B1", "Hello");
        System.out.println("\nTest 3 - Store text");
        System.out.println("Display: " + model.getDisplayValue("B1"));

        // Test 4: Empty cell
        model.setCell("C1", "");
        System.out.println("\nTest 4 - Empty cell");
        System.out.println("Display: '" + model.getDisplayValue("C1") + "'");

        System.out.println("\n=== Tests Completed ===");
    }
}
