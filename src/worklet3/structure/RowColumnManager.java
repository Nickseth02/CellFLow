package worklet3.structure;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import worklet3.formatting.CellFormat;
import worklet3.formatting.CellFormatManager;

/**
 * Performs row and column insertions/deletions on a map of original cell
 * contents. Cell references use Excel-style, one-based A1 notation.
 *
 * The caller must pass the original contents (not calculated display values).
 * Formatting is shifted along with cells when a CellFormatManager is supplied.
 * This class does not directly modify the live Spreadsheet API or dependency
 * graph; the integration layer must apply the returned contents to the engine.
 */
public final class RowColumnManager {

    public Map<String, String> insertRow(
            Map<String, String> contents,
            CellFormatManager formats,
            int rowIndex) {
        validateIndex(rowIndex);
        return transform(contents, formats, true, rowIndex, true);
    }

    public Map<String, String> deleteRow(
            Map<String, String> contents,
            CellFormatManager formats,
            int rowIndex) {
        validateIndex(rowIndex);
        return transform(contents, formats, true, rowIndex, false);
    }

    public Map<String, String> insertColumn(
            Map<String, String> contents,
            CellFormatManager formats,
            int columnIndex) {
        validateIndex(columnIndex);
        return transform(contents, formats, false, columnIndex, true);
    }

    public Map<String, String> deleteColumn(
            Map<String, String> contents,
            CellFormatManager formats,
            int columnIndex) {
        validateIndex(columnIndex);
        return transform(contents, formats, false, columnIndex, false);
    }

    private Map<String, String> transform(
            Map<String, String> contents,
            CellFormatManager formats,
            boolean rowOperation,
            int index,
            boolean insertion) {

        Objects.requireNonNull(contents, "Cell contents cannot be null");

        Map<String, String> shiftedContents = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER);

        // First shift cell locations and update references inside formulas.
        for (Map.Entry<String, String> entry : contents.entrySet()) {
            String oldReference = normalizeReference(entry.getKey());
            int row = rowNumber(oldReference);
            int column = columnNumber(columnLetters(oldReference));

            int newRow = row;
            int newColumn = column;

            if (rowOperation) {
                if (insertion && row >= index) {
                    newRow++;
                } else if (!insertion && row == index) {
                    continue;
                } else if (!insertion && row > index) {
                    newRow--;
                }
            } else {
                if (insertion && column >= index) {
                    newColumn++;
                } else if (!insertion && column == index) {
                    continue;
                } else if (!insertion && column > index) {
                    newColumn--;
                }
            }

            String newReference = columnName(newColumn) + newRow;
            String content = entry.getValue();

            if (content != null && content.startsWith("=")) {
                if (rowOperation) {
                    content = insertion
                            ? ReferenceUpdater.afterRowInsertion(content, index)
                            : ReferenceUpdater.afterRowDeletion(content, index);
                } else {
                    content = insertion
                            ? ReferenceUpdater.afterColumnInsertion(content, index)
                            : ReferenceUpdater.afterColumnDeletion(content, index);
                }
            }

            if (shiftedContents.put(newReference, content) != null) {
                throw new IllegalArgumentException(
                        "Cell reference collision at " + newReference);
            }
        }

        if (formats != null) {
            Map<String, CellFormat> shiftedFormats = new LinkedHashMap<>();

            for (Map.Entry<String, CellFormat> entry
                    : formats.snapshot().entrySet()) {
                String oldReference = normalizeReference(entry.getKey());
                int row = rowNumber(oldReference);
                int column = columnNumber(columnLetters(oldReference));

                int newRow = row;
                int newColumn = column;

                if (rowOperation) {
                    if (insertion && row >= index) {
                        newRow++;
                    } else if (!insertion && row == index) {
                        continue;
                    } else if (!insertion && row > index) {
                        newRow--;
                    }
                } else {
                    if (insertion && column >= index) {
                        newColumn++;
                    } else if (!insertion && column == index) {
                        continue;
                    } else if (!insertion && column > index) {
                        newColumn--;
                    }
                }

                String newReference = columnName(newColumn) + newRow;
                shiftedFormats.put(newReference, entry.getValue());
            }

            formats.replaceAll(shiftedFormats);
        }

        return shiftedContents;
    }

    private static String normalizeReference(String reference) {
        if (reference == null) {
            throw new IllegalArgumentException("Cell reference cannot be null");
        }

        String normalized = reference.toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]+[1-9][0-9]*")) {
            throw new IllegalArgumentException(
                    "Invalid cell reference: " + reference);
        }
        return normalized;
    }

    private static String columnLetters(String reference) {
        int i = 0;
        while (i < reference.length()
                && Character.isLetter(reference.charAt(i))) {
            i++;
        }
        return reference.substring(0, i);
    }

    private static int rowNumber(String reference) {
        int i = 0;
        while (i < reference.length()
                && Character.isLetter(reference.charAt(i))) {
            i++;
        }
        return Integer.parseInt(reference.substring(i));
    }

    private static int columnNumber(String letters) {
        int result = 0;
        for (int i = 0; i < letters.length(); i++) {
            result = result * 26 + (letters.charAt(i) - 'A' + 1);
        }
        return result;
    }

    private static String columnName(int column) {
        StringBuilder result = new StringBuilder();
        while (column > 0) {
            column--;
            result.insert(0, (char) ('A' + column % 26));
            column /= 26;
        }
        return result.toString();
    }

    private static void validateIndex(int index) {
        if (index < 1) {
            throw new IllegalArgumentException(
                    "Row and column indexes must be 1 or greater.");
        }
    }
}
