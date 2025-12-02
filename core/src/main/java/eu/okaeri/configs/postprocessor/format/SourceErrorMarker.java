package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.serdes.ConfigPath;
import lombok.NonNull;

import java.util.Arrays;

/**
 * Formats Rust-style error markers for any source content.
 * <p>
 * Example output:
 * <pre>
 *  --> config.yml:5:10
 *    |
 *  5 |     port: abc
 *    |           ^^^ expected integer
 * </pre>
 */
public final class SourceErrorMarker {

    /**
     * Formats a Rust-style error marker using a SourceWalker to locate the path.
     *
     * @param walker      the source walker for the format
     * @param path        the config path to mark
     * @param sourceFile  optional source file name for the header
     * @param hint        optional hint message to show after the carets
     * @param valueOffset offset within the value to point to (-1 to underline whole value)
     * @param valueLength length of the range to underline (1 for single character)
     * @return formatted marker string, or empty string if path not found
     */
    public static String format(SourceWalker walker, @NonNull ConfigPath path, String sourceFile, String hint, int valueOffset, int valueLength) {
        if (walker == null) {
            return "";
        }

        SourceLocation location = walker.findPath(path);
        if (location == null) {
            return "";
        }

        return formatLocation(location, sourceFile, hint, valueOffset, valueLength);
    }

    /**
     * Formats a Rust-style error marker using a SourceWalker to locate the path.
     */
    public static String format(SourceWalker walker, @NonNull ConfigPath path, String sourceFile, String hint, int valueOffset) {
        return format(walker, path, sourceFile, hint, valueOffset, 1);
    }

    /**
     * Formats a Rust-style error marker using a SourceWalker to locate the path.
     */
    public static String format(SourceWalker walker, @NonNull ConfigPath path, String sourceFile, String hint) {
        return format(walker, path, sourceFile, hint, -1, 1);
    }

    /**
     * Formats a Rust-style error marker using a SourceWalker to locate the path.
     */
    public static String format(SourceWalker walker, @NonNull ConfigPath path, String sourceFile) {
        return format(walker, path, sourceFile, null, -1, 1);
    }

    /**
     * Formats a Rust-style error marker for the given SourceLocation.
     *
     * @param location    the source location
     * @param sourceFile  optional source file name for the header
     * @param hint        optional hint message to show after the carets
     * @param valueOffset offset within the value to point to (-1 to underline whole value)
     * @param valueLength length of the range to underline (1 for single character)
     * @return formatted marker string
     */
    public static String formatLocation(@NonNull SourceLocation location, String sourceFile, String hint, int valueOffset, int valueLength) {
        StringBuilder sb = new StringBuilder();

        int lineNum = location.getLineNumber();
        String rawLine = location.getRawLine();

        // Determine what to underline: value if present, otherwise key
        int underlineStart;
        int underlineLength;

        if ((location.getValueColumn() >= 0) && (location.getValue() != null)) {
            // Underline the value (or specific range within it)
            underlineStart = location.getValueColumn();
            String value = location.getValue();

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
        } else if ((location.getKeyColumn() >= 0) && (location.getKey() != null)) {
            // Underline the key
            underlineStart = location.getKeyColumn();
            underlineLength = location.getKey().length();
        } else {
            // Fallback: underline whole line content
            underlineStart = 0;
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
        if (count <= 0) return "";
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
