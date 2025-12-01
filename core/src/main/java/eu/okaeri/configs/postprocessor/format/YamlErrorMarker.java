package eu.okaeri.configs.postprocessor.format;

import lombok.NonNull;

import java.util.Arrays;

/**
 * Formats Rust-style error markers for YAML content.
 * <p>
 * Example output:
 * <pre>
 *  --> config.yml:5:10
 *    |
 *  5 |     port: abc
 *    |           ^^^ expected integer
 * </pre>
 */
public class YamlErrorMarker {

    /**
     * Formats a Rust-style error marker for a path in the given YAML content.
     *
     * @param yamlContent the YAML content
     * @param path        the path to mark (e.g., "database.port")
     * @param sourceFile  optional source file name for the header
     * @return formatted marker string, or empty string if path not found
     */
    public static String format(@NonNull String yamlContent, @NonNull String path, String sourceFile) {
        return format(yamlContent, path, sourceFile, null);
    }

    /**
     * Formats a Rust-style error marker for a path in the given YAML content.
     *
     * @param yamlContent the YAML content
     * @param path        the path to mark (e.g., "database.port")
     * @param sourceFile  optional source file name for the header
     * @param hint        optional hint message to show after the carets
     * @return formatted marker string, or empty string if path not found
     */
    public static String format(@NonNull String yamlContent, @NonNull String path, String sourceFile, String hint) {
        return format(yamlContent, path, sourceFile, hint, -1);
    }

    /**
     * Formats a Rust-style error marker for a path in the given YAML content.
     *
     * @param yamlContent the YAML content
     * @param path        the path to mark (e.g., "database.port")
     * @param sourceFile  optional source file name for the header
     * @param hint        optional hint message to show after the carets
     * @param valueOffset offset within the value to point to (-1 to underline whole value)
     * @return formatted marker string, or empty string if path not found
     */
    public static String format(@NonNull String yamlContent, @NonNull String path, String sourceFile, String hint, int valueOffset) {
        return format(yamlContent, path, sourceFile, hint, valueOffset, 1);
    }

    /**
     * Formats a Rust-style error marker for a path in the given YAML content.
     *
     * @param yamlContent the YAML content
     * @param path        the path to mark (e.g., "database.port")
     * @param sourceFile  optional source file name for the header
     * @param hint        optional hint message to show after the carets
     * @param valueOffset offset within the value to point to (-1 to underline whole value)
     * @param valueLength length of the range to underline (1 for single character)
     * @return formatted marker string, or empty string if path not found
     */
    public static String format(@NonNull String yamlContent, @NonNull String path, String sourceFile, String hint, int valueOffset, int valueLength) {
        YamlWalker walker = YamlWalker.of(yamlContent);
        YamlLine line = walker.findLine(path);

        if (line == null) {
            return "";
        }

        return formatLine(line, sourceFile, hint, valueOffset, valueLength);
    }

    /**
     * Formats a Rust-style error marker for the given YamlLine.
     *
     * @param line       the parsed YAML line
     * @param sourceFile optional source file name for the header
     * @return formatted marker string
     */
    public static String formatLine(@NonNull YamlLine line, String sourceFile) {
        return formatLine(line, sourceFile, null);
    }

    /**
     * Formats a Rust-style error marker for the given YamlLine.
     *
     * @param line       the parsed YAML line
     * @param sourceFile optional source file name for the header
     * @param hint       optional hint message to show after the carets
     * @return formatted marker string
     */
    public static String formatLine(@NonNull YamlLine line, String sourceFile, String hint) {
        return formatLine(line, sourceFile, hint, -1);
    }

    /**
     * Formats a Rust-style error marker for the given YamlLine.
     *
     * @param line        the parsed YAML line
     * @param sourceFile  optional source file name for the header
     * @param hint        optional hint message to show after the carets
     * @param valueOffset offset within the value to point to (-1 to underline whole value)
     * @return formatted marker string
     */
    public static String formatLine(@NonNull YamlLine line, String sourceFile, String hint, int valueOffset) {
        return formatLine(line, sourceFile, hint, valueOffset, 1);
    }

    /**
     * Formats a Rust-style error marker for the given YamlLine.
     *
     * @param line        the parsed YAML line
     * @param sourceFile  optional source file name for the header
     * @param hint        optional hint message to show after the carets
     * @param valueOffset offset within the value to point to (-1 to underline whole value)
     * @param valueLength length of the range to underline (1 for single character)
     * @return formatted marker string
     */
    public static String formatLine(@NonNull YamlLine line, String sourceFile, String hint, int valueOffset, int valueLength) {
        StringBuilder sb = new StringBuilder();

        int lineNum = line.getLineNumber();
        String rawLine = line.getRawLine();

        // Determine what to underline: value if present, otherwise key
        int underlineStart;
        int underlineLength;

        if ((line.getValueColumn() >= 0) && (line.getValue() != null)) {
            // Underline the value (or specific range within it)
            underlineStart = line.getValueColumn();
            String value = line.getValue();

            if (valueOffset >= 0) {
                // Point to specific range within the value
                // Account for quotes: offset is relative to unquoted content
                int adjustedOffset = valueOffset;
                if (value.startsWith("\"") || value.startsWith("'")) {
                    adjustedOffset += 1;
                }
                if (adjustedOffset < value.length()) {
                    underlineStart += adjustedOffset;
                    // Use provided length, but don't exceed value bounds
                    int maxLength = value.length() - adjustedOffset;
                    underlineLength = Math.min(Math.max(1, valueLength), maxLength);
                } else {
                    underlineLength = value.length();
                }
            } else {
                underlineLength = value.length();
            }
        } else if ((line.getColumn() >= 0) && (line.getKey() != null)) {
            // Underline the key
            underlineStart = line.getColumn();
            underlineLength = line.getKey().length();
        } else {
            // Fallback: underline from indent
            underlineStart = line.getIndent();
            underlineLength = Math.max(1, rawLine.trim().length());
        }

        // Line number width for alignment
        int lineNumWidth = String.valueOf(lineNum).length();
        String lineNumStr = String.valueOf(lineNum);
        String padding = repeat(' ', lineNumWidth);

        // Header: --> file:line:column
        sb.append(" --> ");
        if ((sourceFile != null) && !sourceFile.isEmpty()) {
            sb.append(sourceFile).append(":");
        }
        sb.append(lineNum).append(":").append(underlineStart + 1);
        sb.append("\n");

        // Empty line with pipe
        sb.append(padding).append(" |\n");

        // The source line
        sb.append(lineNumStr).append(" | ").append(rawLine).append("\n");

        // The marker line with carets and optional hint
        sb.append(padding).append(" | ");
        sb.append(repeat(' ', underlineStart));
        sb.append(repeat('^', Math.max(1, underlineLength)));

        if ((hint != null) && !hint.isEmpty()) {
            sb.append(" ").append(hint);
        }

        return sb.toString();
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
