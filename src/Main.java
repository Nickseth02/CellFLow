
import worklet1.BasicCell;

public class Main {
    public static void main(String[] args) {
        BasicCell cell = new BasicCell("A1", 10, "10");

        System.out.println("Cell: " + cell.getReference());
        System.out.println("Value: " + cell.getValue());
        System.out.println("Display: " + cell.getDisplayValue());
    }
}
