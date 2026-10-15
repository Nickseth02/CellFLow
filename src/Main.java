
import worklet1.LiteralCell;
import worklet1.EmptyCell;
import worklet1.BasicCell;

public class Main {
    public static void main(String[] args) {

        // Create a numeric literal cell
        BasicCell cell = LiteralCell.fromInput("A1", "10");

        System.out.println("Cell: " + cell.getReference());
        System.out.println("Original Content: " + cell.getContent());
        System.out.println("Value: " + cell.getValue());
        System.out.println("Display: " + cell.getDisplayValue());

        System.out.println();

        // Create an empty cell
        BasicCell emptyCell = new EmptyCell("B1");

        System.out.println("Cell: " + emptyCell.getReference());
        System.out.println("Value: " + emptyCell.getValue());
        System.out.println("Display: '" + emptyCell.getDisplayValue() + "'");
    }
}
