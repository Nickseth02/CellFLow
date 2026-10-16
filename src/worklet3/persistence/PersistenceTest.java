package worklet3.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standalone unit test for save/load format.
 * It does not require the team's Spreadsheet implementation.
 */
public class PersistenceTest {
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("cellflow-test-", ".sheet");

        try {
            Map<String, String> original = new LinkedHashMap<>();
            original.put("A1", "10");
            original.put("B1", "=A1*2");
            original.put("C1", "Hello World");
            original.put("D1", "First line\nSecond line");
            original.put("E1", "Text\twith tab");
            original.put("F1", "A backslash: \\");

            SheetSerializer serializer = new SheetSerializer();
            SheetLoader loader = new SheetLoader();

            serializer.save(original, file);
            Map<String, String> restored = loader.load(file);

            if (!original.equals(restored)) {
                throw new AssertionError(
                        "FAIL: Original and restored contents differ."
                                + "\nOriginal: " + original
                                + "\nRestored: " + restored);
            }

            if (!"=A1*2".equals(restored.get("B1"))) {
                throw new AssertionError("FAIL: Formula was changed.");
            }

            System.out.println("PASS: Cell contents preserved.");
            System.out.println("PASS: Formula text preserved.");
            System.out.println("PASS: Tabs, newlines, and backslashes preserved.");
            System.out.println("ALL TESTS PASSED!");

            // Confirm malformed files are rejected.
            Path invalid = Files.createTempFile("cellflow-invalid-", ".sheet");
            try {
                Files.writeString(invalid, "NOT_A_SHEET\nA1\t10\n");
                try {
                    loader.load(invalid);
                    throw new AssertionError("FAIL: Invalid header accepted.");
                } catch (java.io.IOException expected) {
                    System.out.println("PASS: Invalid file rejected.");
                }
            } finally {
                Files.deleteIfExists(invalid);
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
