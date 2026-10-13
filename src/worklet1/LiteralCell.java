
package worklet1;

/**
 * Represents a numeric or text literal cell.
 */
public final class LiteralCell extends BasicCell {

    private LiteralCell(
            String reference,
            String content,
            Object value,
            String displayValue) {

        super(reference, content, value, displayValue, null);
    }

    /**
     * Creates a literal cell from the original user input.
     *
     * Numeric input becomes a Double.
     * Other non-empty input is stored as text.
     */
    public static LiteralCell fromInput(
            String reference,
            String content) {

        if (content == null) {
            throw new IllegalArgumentException(
                    "Cell content must not be null");
        }

        if (content.isEmpty()) {
            throw new IllegalArgumentException(
                    "Use EmptyCell for empty content");
        }

        if (content.startsWith("=")) {
            throw new IllegalArgumentException(
                    "Formula input must be handled by the formula component");
        }

        String trimmed = content.trim();

        try {
            double number = Double.parseDouble(trimmed);

            if (!Double.isFinite(number)) {
                throw new NumberFormatException(
                        "Non-finite numbers are not supported");
            }

            return new LiteralCell(
                    reference,
                    content,
                    number,
                    content
            );

        } catch (NumberFormatException exception) {
            // Non-numeric input is treated as text.
        }

        return new LiteralCell(
                reference,
                content,
                content,
                content
        );
    }
}
