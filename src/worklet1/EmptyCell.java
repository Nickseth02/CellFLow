
package worklet1;

/**
 * Represents an explicitly stored empty cell.
 */
public final class EmptyCell extends BasicCell {

    public EmptyCell(String reference) {
        super(
                reference,
                "",
                null,
                "",
                null
        );
    }
}
