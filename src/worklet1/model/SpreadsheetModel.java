
package worklet1.model;

import api.Cell;
import api.Spreadsheet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import worklet1.formula.FormulaCell;

public final class SpreadsheetModel implements Spreadsheet {

    private final Map<String, Cell> cells = new LinkedHashMap<>();

    @Override
    public synchronized void setCell(String reference, String content) {
        String normalizedReference = normalizeReference(reference);

        Objects.requireNonNull(content, "Cell content must not be null");

        Cell cell;

        if (content.isEmpty()) {
            cell = new EmptyCell(normalizedReference);
        } else if (content.startsWith("=")) {
            cell = new FormulaCell(
                    normalizedReference,
                    content.substring(1)
            );
        } else {
            cell = LiteralCell.fromInput(normalizedReference, content);
        }

        cells.put(normalizedReference, cell);
    }

    @Override
    public synchronized Cell getCell(String reference) {
        return cells.get(normalizeReference(reference));
    }

    @Override
    public synchronized Object getValue(String reference) {
        String normalizedReference = normalizeReference(reference);
        Cell cell = cells.get(normalizedReference);

        if (cell == null) {
            return null;
        }

        if (cell instanceof FormulaCell) {
            cell.evaluate(this::getNumericOrStoredValue);
        }

        return cell.getValue();
    }

    private Object getNumericOrStoredValue(String reference) {
        String normalizedReference = normalizeReference(reference);
        Cell cell = cells.get(normalizedReference);

        if (cell == null) {
            return null;
        }

        if (cell instanceof FormulaCell) {
            return getValue(normalizedReference);
        }

        return cell.getValue();
    }

    @Override
    public synchronized String getDisplayValue(String reference) {
        String normalizedReference = normalizeReference(reference);
        Cell cell = cells.get(normalizedReference);

        if (cell == null) {
            return "";
        }

        if (cell instanceof FormulaCell) {
            getValue(normalizedReference);
        }

        return cell.getDisplayValue();
    }

    @Override
    public synchronized String getError(String reference) {
        Cell cell = getCell(reference);
        return cell == null ? null : cell.getError();
    }

    public synchronized void removeCell(String reference) {
        cells.remove(normalizeReference(reference));
    }

    public synchronized Map<String, Cell> getAllCellContents() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(cells)
        );
    }

    public synchronized int getStoredCellCount() {
        return cells.size();
    }

    private String normalizeReference(String reference) {
        Objects.requireNonNull(
                reference,
                "Cell reference must not be null"
        );

        String normalized = reference.toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]+[1-9][0-9]*")) {
            throw new IllegalArgumentException(
                    "Invalid cell reference: " + reference
            );
        }

        return normalized;
    }
}
