package worklet2.engine.error;

import java.util.Objects;

/**
 * Represents an error associated with a Worklet 2 cell.
 */
public final class CellError {

    private final CellErrorType type;
    private final String cellReference;
    private final String message;
    private final String sourceCellReference;

    public CellError(
            CellErrorType type,
            String cellReference,
            String message,
            String sourceCellReference) {

        this.type = Objects.requireNonNull(type, "Error type must not be null");
        this.cellReference = Objects.requireNonNull(
                cellReference, "Cell reference must not be null");
        this.message = Objects.requireNonNull(
                message, "Error message must not be null");
        this.sourceCellReference = sourceCellReference;
    }

    public CellErrorType getType() {
        return type;
    }

    public String getCellReference() {
        return cellReference;
    }

    public String getMessage() {
        return message;
    }

    public String getSourceCellReference() {
        return sourceCellReference;
    }

    public String getDisplayCode() {
        return type.getDisplayCode();
    }

    public boolean isPropagated() {
        return sourceCellReference != null;
    }
}