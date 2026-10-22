package worklet3.formatting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stores and manages formatting by cell reference.
 *
 * Cell values, formulas, and dependency information remain in the existing
 * spreadsheet engine. This class manages appearance only.
 */
public final class CellFormatManager {

    private static final Pattern REFERENCE =
            Pattern.compile("[A-Z]+[1-9][0-9]*");

    private final Map<String, CellFormat> formats =
            new LinkedHashMap<>();

    /** Returns a cell's format, or the default format if none is saved. */
    public synchronized CellFormat getFormat(String reference) {
        String key = normalize(reference);
        CellFormat format = formats.get(key);
        return format == null ? new CellFormat() : format;
    }

    /** Saves a format. Default formats are removed to save memory. */
    public synchronized void setFormat(
            String reference, CellFormat format) {

        String key = normalize(reference);
        Objects.requireNonNull(format, "Format cannot be null");

        if (format.isDefault()) {
            formats.remove(key);
        } else {
            formats.put(key, format);
        }
    }

    public synchronized void clearFormat(String reference) {
        formats.remove(normalize(reference));
    }

    public synchronized void clearAllFormats() {
        formats.clear();
    }

    public synchronized void setBold(String reference, boolean bold) {
        setFormat(reference, getFormat(reference).withBold(bold));
    }

    public synchronized void setItalic(String reference, boolean italic) {
        setFormat(reference, getFormat(reference).withItalic(italic));
    }

    public synchronized void setAlignment(
            String reference, CellFormat.Alignment alignment) {
        setFormat(reference, getFormat(reference).withAlignment(alignment));
    }

    public synchronized void setNumberFormat(
            String reference, CellFormat.NumberFormat numberFormat) {
        setFormat(reference,
                getFormat(reference).withNumberFormat(numberFormat));
    }

    /**
     * Returns an independent, read-only copy of all non-default formats.
     */
    public synchronized Map<String, CellFormat> snapshot() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(formats));
    }

    /**
     * Replaces all formats, useful when structural operations move cells.
     * Validation completes before the existing state is changed.
     */
    public synchronized void replaceAll(
            Map<String, CellFormat> updatedFormats) {

        Objects.requireNonNull(
                updatedFormats, "Updated formats cannot be null");

        Map<String, CellFormat> validated = new LinkedHashMap<>();

        for (Map.Entry<String, CellFormat> entry
                : updatedFormats.entrySet()) {

            String key = normalize(entry.getKey());
            CellFormat format = Objects.requireNonNull(
                    entry.getValue(), "Format cannot be null");

            if (validated.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Duplicate cell reference: " + key);
            }

            if (!format.isDefault()) {
                validated.put(key, format);
            }
        }

        formats.clear();
        formats.putAll(validated);
    }

    private static String normalize(String reference) {
        if (reference == null) {
            throw new IllegalArgumentException(
                    "Cell reference is required");
        }

        String key = reference.toUpperCase(Locale.ROOT);

        if (!REFERENCE.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Invalid cell reference: " + reference);
        }

        return key;
    }
}
