package worklet3.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Saves original cell contents (including formulas) to a CellFlow .sheet file.
 */
public class SheetSerializer {
    static final String HEADER = "CELLFLOW_SHEET_V1";

    public void save(Map<String, String> contents, Path file)
            throws IOException {
        if (contents == null || file == null) {
            throw new IllegalArgumentException(
                    "Contents and file path are required.");
        }

        Map<String, String> sorted =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, String> entry : contents.entrySet()) {
            String reference = entry.getKey();
            String content = entry.getValue();

            if (reference == null
                    || !reference.matches("[A-Za-z]+[1-9][0-9]*")) {
                throw new IOException("Invalid cell reference: " + reference);
            }
            if (content == null || content.isEmpty()) {
                continue;
            }

            String key = reference.toUpperCase(java.util.Locale.ROOT);
            if (sorted.containsKey(key)) {
                throw new IOException("Duplicate cell reference: " + key);
            }
            sorted.put(key, content);
        }

        Path absoluteFile = file.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                absoluteFile, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                writer.write(entry.getKey());
                writer.write('\t');
                writer.write(escape(entry.getValue()));
                writer.newLine();
            }
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\t", "\\t")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n");
    }
}
