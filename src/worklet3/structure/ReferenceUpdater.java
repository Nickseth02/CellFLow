package worklet3.structure;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates A1-style cell references in formula text after a row or column
 * is inserted or deleted. The input formula is returned unchanged when it
 * does not contain a reference affected by the operation.
 *
 * This class handles ordinary A1 references and optional $ markers. It does
 * not attempt to parse the complete formula language.
 */
public final class ReferenceUpdater {

    private static final Pattern REFERENCE = Pattern.compile(
            "(?<![A-Za-z0-9_])"
            + "(\\$?)([A-Za-z]+)(\\$?)([1-9][0-9]*)"
            + "(?![A-Za-z0-9_])");

    private ReferenceUpdater() {
        // Utility class.
    }

    public static String afterRowInsertion(
            String formula, int insertedRow) {
        validateIndex(insertedRow);
        return rewrite(formula, true, insertedRow, true);
    }

    public static String afterRowDeletion(
            String formula, int deletedRow) {
        validateIndex(deletedRow);
        return rewrite(formula, true, deletedRow, false);
    }

    public static String afterColumnInsertion(
            String formula, int insertedColumn) {
        validateIndex(insertedColumn);
        return rewrite(formula, false, insertedColumn, true);
    }

    public static String afterColumnDeletion(
            String formula, int deletedColumn) {
        validateIndex(deletedColumn);
        return rewrite(formula, false, deletedColumn, false);
    }

    private static String rewrite(
            String formula,
            boolean rowOperation,
            int index,
            boolean insertion) {

        if (formula == null || formula.isEmpty()) {
            return formula;
        }

        StringBuilder result = new StringBuilder();
        StringBuilder segment = new StringBuilder();
        boolean inString = false;

        // Do not alter A1-like text inside double-quoted formula strings.
        for (int i = 0; i < formula.length(); i++) {
            char c = formula.charAt(i);

            if (c == '"') {
                appendRewrittenSegment(result, segment, rowOperation,
                        index, insertion);
                result.append(c);
                inString = !inString;
            } else if (inString) {
                result.append(c);
            } else {
                segment.append(c);
            }
        }

        appendRewrittenSegment(result, segment, rowOperation, index, insertion);
        return result.toString();
    }

    private static void appendRewrittenSegment(
            StringBuilder result,
            StringBuilder segment,
            boolean rowOperation,
            int index,
            boolean insertion) {

        if (segment.length() == 0) {
            return;
        }

        Matcher matcher = REFERENCE.matcher(segment);
        StringBuffer updated = new StringBuffer();

        while (matcher.find()) {
            String columnAbsolute = matcher.group(1);
            String columnLetters = matcher.group(2);
            String rowAbsolute = matcher.group(3);
            int row;

            try {
                row = Integer.parseInt(matcher.group(4));
            } catch (NumberFormatException exception) {
                matcher.appendReplacement(updated,
                        Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            int column = columnNumber(columnLetters);
            int newRow = row;
            int newColumn = column;
            boolean deletedReference = false;

            if (rowOperation) {
                if (insertion && row >= index) {
                    newRow = row + 1;
                } else if (!insertion && row == index) {
                    deletedReference = true;
                } else if (!insertion && row > index) {
                    newRow = row - 1;
                }
            } else {
                if (insertion && column >= index) {
                    newColumn = column + 1;
                } else if (!insertion && column == index) {
                    deletedReference = true;
                } else if (!insertion && column > index) {
                    newColumn = column - 1;
                }
            }

            String replacement;
            if (deletedReference) {
                replacement = "#REF!";
            } else {
                replacement = columnAbsolute
                        + columnName(newColumn)
                        + rowAbsolute
                        + newRow;
            }

            matcher.appendReplacement(
                    updated, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(updated);
        result.append(updated);
        segment.setLength(0);
    }

    private static int columnNumber(String letters) {
        int result = 0;
        String normalized = letters.toUpperCase(Locale.ROOT);

        for (int i = 0; i < normalized.length(); i++) {
            result = result * 26 + (normalized.charAt(i) - 'A' + 1);
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
