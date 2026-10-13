
package worklet1;

import api.Cell;
import api.EvaluationContext;

import java.util.Set;

/**
 * Base class for spreadsheet cells.
 * Stores the original content and the current evaluated state.
 */
public abstract class BasicCell implements Cell {

    private final String reference;
    private final String content;
    private final Object value;
    private final String displayValue;
    private final String error;

    protected BasicCell(
            String reference,
            String content,
            Object value,
            String displayValue,
            String error) {

        this.reference = reference;
        this.content = content;
        this.value = value;
        this.displayValue = displayValue;
        this.error = error;
    }

    @Override
    public final String getReference() {
        return reference;
    }

    /**
     * Returns the evaluated value, not the original input.
     */
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getDisplayValue() {
        return displayValue;
    }

    @Override
    public String getError() {
        return error;
    }

    /**
     * Literal and empty cells have no formula dependencies.
     */
    @Override
    public Set<String> getDependencies() {
        return Set.of();
    }

    /**
     * Literal and empty cells are already evaluated.
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        return value;
    }

    /**
     * Returns the exact content entered by the user.
     * This is an additional method, not part of the shared Cell API.
     */
    public final String getContent() {
        return content;
    }
}
