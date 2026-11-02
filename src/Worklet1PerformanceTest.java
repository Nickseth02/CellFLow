
public class Worklet1PerformanceTest {

    private static final int[] SIZES = {
        1000, 5000, 10000,
        25000, 50000, 100000
    };

    public static void main(String[] args) {

        System.out.println("WORKLET 1 PERFORMANCE TEST");
        System.out.println("--------------------------------");

        for (int size : SIZES) {

            // Warm-up / preparation
            System.gc();

            long start = System.nanoTime();

            // Replace this section with your
            // actual Worklet 1 cell creation API.
            Object[] cells = new Object[size];

            for (int i = 0; i < size; i++) {
                cells[i] = i + 1;
            }

            long end = System.nanoTime();

            // Verify all values
            boolean correct = true;

            for (int i = 0; i < size; i++) {
                if (!cells[i].equals(i + 1)) {
                    correct = false;
                    break;
                }
            }

            double timeMs =
                (end - start) / 1_000_000.0;

            Runtime runtime =
                Runtime.getRuntime();

            long memoryMB =
                (runtime.totalMemory()
                - runtime.freeMemory())
                / (1024 * 1024);

            System.out.println(
                "Cells: " + size
                + " | Time: " + timeMs + " ms"
                + " | Memory: " + memoryMB + " MB"
                + " | Correct: " + correct
            );
        }
    }
}
