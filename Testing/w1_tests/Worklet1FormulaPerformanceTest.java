
import worklet1.model.SpreadsheetModel;

public class Worklet1FormulaPerformanceTest {

    private static final int[] SIZES = {
        1000, 5000, 10000, 25000, 50000, 100000
    };

    private static final int REPEATS = 3;

    public static void main(String[] args) {

        System.out.println("WORKLET 1 - FORMULA PERFORMANCE TEST");
        System.out.println("==============================================");

        // Warm-up run
        runTest(1000);

        for (int size : SIZES) {

            double totalCreation = 0;
            double totalEvaluation = 0;
            boolean allCorrect = true;

            for (int run = 1; run <= REPEATS; run++) {

                double[] result = runTest(size);

                totalCreation += result[0];
                totalEvaluation += result[1];

                if (result[2] != 1) {
                    allCorrect = false;
                }
            }

            System.out.println(
                "Cells: " + size +
                " | Avg Formula Creation: "
                + (totalCreation / REPEATS) + " ms" +
                " | Avg Evaluation/Retrieval: "
                + (totalEvaluation / REPEATS) + " ms" +
                " | Correct: " + allCorrect
            );
        }

        System.out.println("==============================================");
        System.out.println("FORMULA BENCHMARK COMPLETED");
    }

    private static double[] runTest(int size) {

        SpreadsheetModel sheet = new SpreadsheetModel();

        // Insert source values
        for (int i = 1; i <= size; i++) {
            sheet.setCell("A" + i, String.valueOf(i));
        }

        // Create formula cells
        long start = System.nanoTime();

        for (int i = 1; i <= size; i++) {
            sheet.setCell("B" + i, "=A" + i + "*2");
        }

        long creationEnd = System.nanoTime();

        // Retrieve and verify formula results
        boolean correct = true;

        for (int i = 1; i <= size; i++) {

            Object value = sheet.getValue("B" + i);

            if (!(value instanceof Number) ||
                Math.abs(((Number) value).doubleValue() - (i * 2)) > 0.0001) {

                correct = false;
                break;
            }
        }

        long evaluationEnd = System.nanoTime();

        double creationTime =
            (creationEnd - start) / 1_000_000.0;

        double evaluationTime =
            (evaluationEnd - creationEnd) / 1_000_000.0;

        return new double[] {
            creationTime,
            evaluationTime,
            correct ? 1 : 0
        };
    }
}
