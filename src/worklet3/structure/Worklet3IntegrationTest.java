package worklet3.structure;

import java.util.LinkedHashMap;
import java.util.Map;
import worklet3.formatting.CellFormatManager;

/**
 * Small dependency-free integration test for the row/column data operations.
 * Run with: java -cp out worklet3.structure.Worklet3IntegrationTest
 */
public final class Worklet3IntegrationTest {

    private Worklet3IntegrationTest() {
    }

    public static void main(String[] args) {
        testInsertRowMovesCellsAndReferences();
        testDeleteColumnRemovesCellsAndUpdatesReferences();
        testFormattingMovesWithCells();
        testReferencesInsideQuotedTextStayUnchanged();
        testInvalidIndexIsRejected();

        System.out.println("All 5 Worklet 3 structure tests passed.");
    }

    private static void testInsertRowMovesCellsAndReferences() {
        Map<String, String> cells = new LinkedHashMap<>();
        cells.put("A1", "10");
        cells.put("A2", "20");
        cells.put("B2", "=A2+5");

        RowColumnManager manager = new RowColumnManager();
        Map<String, String> updated = manager.insertRow(cells, null, 2);

        assertEquals("10", updated.get("A1"), "A1 stays before insertion");
        assertEquals("20", updated.get("A3"), "A2 moves to A3");
        assertEquals("=A3+5", updated.get("B3"), "formula and cell move");
    }

    private static void testDeleteColumnRemovesCellsAndUpdatesReferences() {
        Map<String, String> cells = new LinkedHashMap<>();
        cells.put("A1", "1");
        cells.put("B1", "2");
        cells.put("C1", "=B1+A1");

        Map<String, String> updated =
                new RowColumnManager().deleteColumn(cells, null, 2);

        assertTrue("=#REF!+A1".equals(updated.get("B1")),
        "cell from column C shifts to B and its deleted reference becomes #REF!");

assertTrue(!updated.containsKey("C1"),
        "old column C reference is removed after shifting");
        assertEquals("= #REF!+A1".replace(" ", ""), updated.get("B1"),
                "reference to deleted column becomes #REF!");
    }

    private static void testFormattingMovesWithCells() {
        Map<String, String> cells = new LinkedHashMap<>();
        cells.put("A2", "Hello");

        CellFormatManager formats = new CellFormatManager();
        formats.setBold("A2", true);

        Map<String, String> updated =
                new RowColumnManager().insertRow(cells, formats, 2);

        assertEquals("Hello", updated.get("A3"), "content shifts down");
        assertTrue(formats.getFormat("A3").isBold(),
                "format shifts with content");
        assertTrue(!formats.getFormat("A2").isBold(),
                "inserted row has default formatting");
    }

    private static void testReferencesInsideQuotedTextStayUnchanged() {
        String formula = "=\"A1\"&B1";
        String updated = ReferenceUpdater.afterRowInsertion(formula, 1);
        assertEquals("=\"A1\"&B2", updated,
                "A1 inside a quoted string must not be rewritten");
    }

    private static void testInvalidIndexIsRejected() {
        boolean rejected = false;
        try {
            new RowColumnManager().insertRow(new LinkedHashMap<>(), null, 0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue(rejected, "row zero must be rejected");
    }

    private static void assertEquals(
            Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(
                    message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
