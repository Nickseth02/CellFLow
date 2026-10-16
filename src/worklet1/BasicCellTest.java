
package worklet1;

import api.EvaluationContext;

public class BasicCellTest {

    public static void main(String[] args) {
        EvaluationContext context = reference -> {
            switch (reference) {
                case "A1": return 10.0;
                case "A2": return 5.0;
                default: throw new IllegalArgumentException(
                    "Unknown cell: " + reference
                );
            }
        };

        BasicCell cell = new BasicCell("B1", null, "=A1+A2");

        Object result = cell.evaluate(context);

        if (result instanceof Number
                && Math.abs(((Number) result).doubleValue() - 15.0) < 0.000001
                && cell.getError() == null) {
            System.out.println("PASS | BasicCell formula evaluation");
        } else {
            System.out.println("FAIL | Result: " + result
                    + " | Error: " + cell.getError());
        }

        System.out.println("Display value: " + cell.getDisplayValue());
    }
}
