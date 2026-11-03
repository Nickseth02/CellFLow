package worklet2.engine.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages formula errors and error propagation in Worklet 2.
 */
public class ErrorHandler {

    private final Map<String, CellError> errors = new HashMap<>();

    /**
     * Records an error produced during formula evaluation.
     */
    public void recordError(
            String cellReference,
            CellErrorType type,
            String message) {

        Objects.requireNonNull(cellReference);
        Objects.requireNonNull(type);
        Objects.requireNonNull(message);

        errors.put(
                cellReference,
                new CellError(type, cellReference, message, null)
        );
    }

    /**
     * Propagates an existing dependency error to another cell.
     */
    public void propagateError(
            String cellReference,
            CellError sourceError) {

        Objects.requireNonNull(cellReference);
        Objects.requireNonNull(sourceError);

        errors.put(
                cellReference,
                new CellError(
                        sourceError.getType(),
                        cellReference,
                        "Error propagated from " +
                                sourceError.getCellReference(),
                        sourceError.getCellReference()
                )
        );
    }

    /**
     * Removes an error after successful recalculation.
     */
    public void clearError(String cellReference) {
        errors.remove(cellReference);
    }

    /**
     * Returns the error associated with a cell.
     */
    public CellError getError(String cellReference) {
        return errors.get(cellReference);
    }

    /**
     * Returns the display code of a cell error.
     */
    public String getErrorCode(String cellReference) {
        CellError error = errors.get(cellReference);

        return error == null ? null : error.getDisplayCode();
    }

    /**
     * Finds the first dependency that currently has an error.
     */
    public CellError findDependencyError(Set<String> dependencies) {

        for (String dependency : dependencies) {
            CellError error = errors.get(dependency);

            if (error != null) {
                return error;
            }
        }

        return null;
    }

    /**
     * Returns an unmodifiable view of all recorded errors.
     */
    public Map<String, CellError> getAllErrors() {
        return Collections.unmodifiableMap(errors);
    }

    /**
     * Clears all recorded errors.
     */
    public void clearAllErrors() {
        errors.clear();
    }
}