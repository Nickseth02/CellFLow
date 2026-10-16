
package worklet1;

import api.EvaluationContext;
import java.util.HashMap;
import java.util.Map;

public class EvaluatorTest {

    public static void main(String[] args) {

        Map<String, Object> cells = new HashMap<>();
        cells.put("A1", 10);
        cells.put("A2", 20);
        cells.put("A3", 30);
        cells.put("B1", 5);

        EvaluationContext context = cellReference ->
                cells.get(cellReference.toUpperCase());

        test("Addition", "=10+5", context, 15.0);
        test("Operator precedence", "=10+5*2", context, 20.0);
        test("Parentheses", "=(10+5)*2", context, 30.0);
        test("Exponentiation", "=2^3", context, 8.0);
        test("Cell references", "=A1+B1", context, 15.0);
        test("SUM", "=SUM(A1:A3)", context, 60.0);
        test("AVG", "=AVG(A1:A3)", context, 20.0);
        test("MIN", "=MIN(A1:A3)", context, 10.0);
        test("MAX", "=MAX(A1:A3)", context, 30.0);

    }

    private static void test(
            String name,
            String formula,
            EvaluationContext context,
            double expected) {

        try {
            Object actual = ExpressionEvaluator.evaluate(formula, context);
            boolean passed =
                    Math.abs(((Number) actual).doubleValue() - expected) < 0.000001;

            System.out.println(
                    (passed ? "PASS" : "FAIL") + " | " + name
                    + " | Expected: " + expected
                    + " | Actual: " + actual);

        } catch (Exception e) {
            System.out.println(
                    "ERROR | " + name + " | " + e.getMessage());
        }
    }
}
