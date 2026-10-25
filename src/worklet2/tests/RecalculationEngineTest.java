package worklet2.tests;

import worklet2.engine.RecalculationEngineImpl;
import api.Cell;
import api.Spreadsheet;

import java.util.HashMap;
import java.util.Map;

public class RecalculationEngineTest {

    public static void main(String[] args) {

        // Simple Spreadsheet implementation for testing
        Spreadsheet sheet = new Spreadsheet() {

            private final Map<String, String> cells =
                    new HashMap<>();

            @Override
            public void setCell(String reference, String content) {
                cells.put(
                        reference.toUpperCase(),
                        content
                );
            }

            @Override
            public Object getValue(String reference) {

                String value =
                        cells.get(reference.toUpperCase());

                if (value == null) {
                    return null;
                }

                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return value;
                }
            }

            @Override
            public String getDisplayValue(String reference) {

                Object value =
                        getValue(reference);

                return value == null
                        ? ""
                        : value.toString();
            }

            @Override
            public String getError(String reference) {
                return null;
            }

            @Override
            public Cell getCell(String reference) {
                return null;
            }
        };

        // Create recalculation engine
        RecalculationEngineImpl engine =
                new RecalculationEngineImpl(sheet);

        System.out.println(
                "=== Recalculation Engine Test ==="
        );

        // --------------------------------
        // STEP 1: Set initial values
        // --------------------------------

        System.out.println(
                "\nSetting A1 = 10"
        );

        engine.setCell("A1", "10");

        System.out.println(
                "Setting B1 = A1 + 5"
        );

        engine.setCell("B1", "=A1+5");

        System.out.println(
                "Setting C1 = B1 * 2"
        );

        engine.setCell("C1", "=B1*2");

        // --------------------------------
        // STEP 2: Check initial values
        // --------------------------------

        System.out.println(
                "\n=== Initial Values ==="
        );

        System.out.println(
                "A1 = "
                        + engine.getDisplayValue("A1")
        );

        System.out.println(
                "B1 = "
                        + engine.getDisplayValue("B1")
        );

        System.out.println(
                "C1 = "
                        + engine.getDisplayValue("C1")
        );

        // --------------------------------
        // STEP 3: Change A1
        // --------------------------------

        System.out.println(
                "\n=== Changing A1 ==="
        );

        System.out.println(
                "Changing A1 from 10 to 20..."
        );

        engine.setCell("A1", "20");

        // --------------------------------
        // STEP 4: Check recalculated values
        // --------------------------------

        System.out.println(
                "\n=== After Recalculation ==="
        );

        System.out.println(
                "A1 = "
                        + engine.getDisplayValue("A1")
        );

        System.out.println(
                "B1 = "
                        + engine.getDisplayValue("B1")
        );

        System.out.println(
                "C1 = "
                        + engine.getDisplayValue("C1")
        );
    }
}
