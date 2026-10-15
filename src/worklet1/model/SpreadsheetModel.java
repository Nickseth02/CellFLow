
package worklet1.model;

import api.Cell;
import api.Spreadsheet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory spreadsheet storage for Worklet 1.
 *
 * Handles cell storage and literal values.
 * Formula evaluation and dependency recalculation are delegated
 * to the corresponding components.
 */
public final class SpreadsheetModel implements Spreadsheet {

    private final Map<String, Cell> cells = new LinkedHashMap<>();

    /**
     * Sets a cell from its reference and original content.
     * An existing cell at the same reference is replaced.
     */
    @Override
    public synchronized void setCell(String reference, String content) {

        String normalizedReference = normalizeReference(reference);

        Objects.requireNonNull(content, "Cell content must not be null");

        Cell cell;

        if (content.isEmpty()) {
            cell = new EmptyCell(normalizedReference);
        } else {
            cell = LiteralCell.fromInput(normalizedReference, content);
        }

        cells.put(normalizedReference, cell);
    }

    /**
     * Retrieves a stored cell.
     * Returns null if the reference has not been stored.
     */
    @Override
    public synchronized Cell getCell(String reference) {
        return cells.get(normalizeReference(reference));
    }

    @Override
    public synchronized Object getValue(String reference) {
        Cell cell = getCell(reference);
        return cell == null ? null : cell.getValue();
    }

    @Override
    public synchronized String getDisplayValue(String reference) {
        Cell cell = getCell(reference);
        return cell == null ? "" : cell.getDisplayValue();
    }

    @Override
    public synchronized String getError(String reference) {
        Cell cell = getCell(reference);
        return cell == null ? null : cell.getError();
    }

    /**
     * Removes a cell from storage.
     * Removing an absent cell has no effect.
     */
    public synchronized void removeCell(String reference) {
        cells.remove(normalizeReference(reference));
    }

    /**
     * Returns a read-only snapshot of all explicitly stored cells.
     * The returned map is independent of subsequent storage changes.
     *
     * Added for Worklet 3 without changing the shared Spreadsheet API.
     */
    public synchronized Map<String, Cell> getAllCellContents() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(cells)
        );
    }

    /**
     * Returns the number of explicitly stored cells.
     */
    public synchronized int getStoredCellCount() {
        return cells.size();
    }

    /**
     * Normalizes A1-style references so that a1 and A1
     * refer to the same cell.
     */
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
