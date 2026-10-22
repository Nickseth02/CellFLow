package worklet3.formatting;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Stores the visual formatting of a spreadsheet cell.
 *
 * Formatting is independent of the cell's content and formula.
 * This class never changes the value stored by the spreadsheet engine.
 */
public final class CellFormat {

    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum NumberFormat {
        GENERAL,
        NUMBER,
        CURRENCY,
        PERCENT,
        DATE
    }

    private final boolean bold;
    private final boolean italic;
    private final Alignment alignment;
    private final NumberFormat numberFormat;

    /** Creates a default, unformatted cell. */
    public CellFormat() {
        this(false, false, Alignment.LEFT, NumberFormat.GENERAL);
    }

    public CellFormat(
            boolean bold,
            boolean italic,
            Alignment alignment,
            NumberFormat numberFormat) {

        this.bold = bold;
        this.italic = italic;
        this.alignment = Objects.requireNonNull(
                alignment, "Alignment cannot be null");
        this.numberFormat = Objects.requireNonNull(
                numberFormat, "Number format cannot be null");
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public NumberFormat getNumberFormat() {
        return numberFormat;
    }

    public CellFormat withBold(boolean value) {
        return new CellFormat(value, italic, alignment, numberFormat);
    }

    public CellFormat withItalic(boolean value) {
        return new CellFormat(bold, value, alignment, numberFormat);
    }

    public CellFormat withAlignment(Alignment value) {
        return new CellFormat(bold, italic, value, numberFormat);
    }

    public CellFormat withNumberFormat(NumberFormat value) {
        return new CellFormat(bold, italic, alignment, value);
    }

    /**
     * Returns a formatted display string without modifying the cell value.
     * Text and error strings are returned unchanged.
     * DATE formats Date objects or numeric epoch milliseconds.
     */
    public String formatDisplayValue(Object value) {
        if (value == null) {
            return "";
        }

        if (numberFormat == NumberFormat.DATE) {
            Date date;
            if (value instanceof Date) {
                date = (Date) value;
            } else if (value instanceof Number) {
                date = new Date(((Number) value).longValue());
            } else {
                return value.toString();
            }

            return new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.ROOT).format(date);
        }

        if (!(value instanceof Number)) {
            return value.toString();
        }

        Number number = (Number) value;

        switch (numberFormat) {
            case NUMBER:
                return new DecimalFormat("#,##0.##").format(number);

            case CURRENCY:
                return java.text.NumberFormat
                        .getCurrencyInstance(Locale.US)
                        .format(number);

            case PERCENT:
                return java.text.NumberFormat
                        .getPercentInstance(Locale.US)
                        .format(number);

            case GENERAL:
            default:
                return number.toString();
        }
    }

    public boolean isDefault() {
        return !bold
                && !italic
                && alignment == Alignment.LEFT
                && numberFormat == NumberFormat.GENERAL;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CellFormat)) {
            return false;
        }

        CellFormat format = (CellFormat) other;
        return bold == format.bold
                && italic == format.italic
                && alignment == format.alignment
                && numberFormat == format.numberFormat;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, italic, alignment, numberFormat);
    }
}
