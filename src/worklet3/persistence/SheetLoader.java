package worklet3.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and validates a CellFlow .sheet file.
 *
 * This class returns cell contents rather than modifying a Spreadsheet
 * directly, so the UI/integration layer can validate the whole file first.
 */
public class SheetLoader {
    public Map<String, String> load(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IOException("Spreadsheet file not found: " + file);
        }

        Map<String, String> result = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!SheetSerializer.HEADER.equals(header)) {
                throw new IOException(
                        "Invalid file format. This is not a CellFlow sheet.");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty()) {
                    continue;
                }

                int separator = line.indexOf('\t');
                if (separator <= 0) {
                    throw new IOException(
                            "Invalid entry at line " + lineNumber);
                }

                String reference = line.substring(0, separator)
                        .toUpperCase(Locale.ROOT);
                if (!reference.matches("[A-Z]+[1-9][0-9]*")) {
                    throw new IOException(
                            "Invalid cell reference at line " + lineNumber);
                }
                if (result.containsKey(reference)) {
                    throw new IOException(
                            "Duplicate cell reference: " + reference);
                }

                String content = unescape(line.substring(separator + 1),
                        lineNumber);
                if (content.isEmpty()) {
                    throw new IOException(
                            "Empty cells should not be stored: " + reference);
                }

                result.put(reference, content);
            }
        }

        return result;
    }

    private String unescape(String value, int lineNumber) throws IOException {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                result.append(c);
                continue;
            }

            if (i + 1 >= value.length()) {
                throw new IOException(
                        "Incomplete escape sequence at line " + lineNumber);
            }

            char escaped = value.charAt(++i);
            switch (escaped) {
                case '\\': result.append('\\'); break;
                case 't': result.append('\t'); break;
                case 'r': result.append('\r'); break;
                case 'n': result.append('\n'); break;
                default:
                    throw new IOException(
                            "Invalid escape sequence at line " + lineNumber);
            }
        }

        return result.toString();
    }
}

