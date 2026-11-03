

import worklet1.model.SpreadsheetModel;

public class Worklet1PerformanceTest {

    private static final int[] SIZES = {
        1000, 5000, 10000, 25000, 50000, 100000
    };

    public static void main(String[] args) {

        System.out.println("WORKLET 1 - SPREADSHEET ENGINE BENCHMARK");
        System.out.println("================================================");

        for (int size : SIZES) {

            SpreadsheetModel sheet = new SpreadsheetModel();

            Runtime runtime = Runtime.getRuntime();
            System.gc();

            long memoryBefore =
                runtime.totalMemory() - runtime.freeMemory();

            long start = System.nanoTime();

            // Insert actual spreadsheet cells
            for (int i = 1; i <= size; i++) {
                sheet.setCell("A" + i, String.valueOf(i));
            }

            long creationEnd = System.nanoTime();

            // Retrieve and verify actual spreadsheet cells
            boolean correct = true;

            for (int i = 1; i <= size; i++) {
                Object value = sheet.getValue("A" + i);

                if (!(value instanceof Number) ||
                    Math.abs(((Number) value).doubleValue() - i) > 0.0001) {
                    correct = false;
                    break;
                }
            }

            long retrievalEnd = System.nanoTime();

            long memoryAfter =
                runtime.totalMemory() - runtime.freeMemory();

            double creationTime =
                (creationEnd - start) / 1_000_000.0;

            double retrievalTime =
                (retrievalEnd - creationEnd) / 1_000_000.0;

            long memoryUsed =
                Math.max(0, memoryAfter - memoryBefore) / (1024 * 1024);

            System.out.println(
                "Cells: " + size +
                " | Creation: " + creationTime + " ms" +
                " | Retrieval: " + retrievalTime + " ms" +
                " | Memory: " + memoryUsed + " MB" +
                " | Correct: " + correct
            );
        }

        System.out.println("================================================");
        System.out.println("BENCHMARK COMPLETED");
    }
}

