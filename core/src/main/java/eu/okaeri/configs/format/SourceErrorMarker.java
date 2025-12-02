package eu.okaeri.configs.format;

import eu.okaeri.configs.serdes.ConfigPath;
import lombok.NonNull;

import java.util.Arrays;

/**
 * Formats Rust-style error markers for any source content.
 * <p>
 * Example output with context lines:
 * <pre>
 *  --> config.yml:5:10
 *    |
 *  3 |     other: value
 *  4 |     more: stuff
 *  5 |     port: abc
 *    |           ^^^ expected integer
 *  6 |     next: line
 * </pre>
 */
public final class SourceErrorMarker {

    /**
     * Formats a Rust-style error marker using a SourceWalker to locate the path.
     *
     * @param walker             the source walker for the format
     * @param path               the config path to mark
     * @param sourceFile         optional source file name for the header
     * @param hint               optional hint message to show after the carets
     * @param valueOffset        offset within the value to point to (-1 to underline whole value)
     * @param valueLength        length of the range to underline (1 for single character)
     * @param rawContent         optional raw content for context lines
     * @param contextLinesBefore number of lines to show before the error line
     * @param contextLinesAfter  number of lines to show after the error line
     * @return formatted marker string, or empty string if path not found
     */
    public static String format(SourceWalker walker, @NonNull ConfigPath path, String sourceFile, String hint,
                                int valueOffset, int valueLength, String rawContent, int contextLinesBefore, int contextLinesAfter) {
        if (walker == null) {
            return "";
        }

        SourceLocation location = walker.findPath(path);
        if (location == null) {
            return "";
        }

        return formatLocation(location, sourceFile, hint, valueOffset, valueLength, rawContent, contextLinesBefore, contextLinesAfter);
    }

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
        return format(walker, path, sourceFile, hint, valueOffset, valueLength, null, 0, 0);
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
     * Formats a Rust-style error marker for the given SourceLocation with context lines.
     *
     * @param location           the source location
     * @param sourceFile         optional source file name for the header
     * @param hint               optional hint message to show after the carets
     * @param valueOffset        offset within the value to point to (-1 to underline whole value)
     * @param valueLength        length of the range to underline (1 for single character)
     * @param rawContent         optional raw content for context lines
     * @param contextLinesBefore number of lines to show before the error line
     * @param contextLinesAfter  number of lines to show after the error line
     * @return formatted marker string
     */
    public static String formatLocation(@NonNull SourceLocation location, String sourceFile, String hint,
                                        int valueOffset, int valueLength, String rawContent, int contextLinesBefore, int contextLinesAfter) {
        StringBuilder sb = new StringBuilder();

        int lineNum = location.getLineNumber();
        String rawLine = location.getRawLine();

        // Parse raw content into lines for context
        String[] allLines = null;
        if ((rawContent != null) && ((contextLinesBefore > 0) || (contextLinesAfter > 0))) {
            allLines = rawContent.split("\n", -1);
        }

        // Determine what to underline: value if present, otherwise key
        int underlineStart;
        int underlineLength;

        if ((location.getValueColumn() >= 0) && (location.getValue() != null)) {
            // Underline the value (or specific range within it)
            underlineStart = location.getValueColumn();
            String value = location.getValue();

            // Strip quotes from underline position - highlight content inside quotes
            int quoteOffset = 0;
            int quoteTrim = 0;
            if (value.length() >= 2) {
                char first = value.charAt(0);
                char last = value.charAt(value.length() - 1);
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    quoteOffset = 1;
                    quoteTrim = 2;
                }
            }

            if (valueOffset >= 0) {
                // Point to specific range within the value
                // Offset is relative to unquoted content
                int adjustedOffset = valueOffset + quoteOffset;
                if (adjustedOffset < value.length()) {
                    underlineStart += adjustedOffset;
                    // Use provided length, but don't exceed value bounds
                    int maxLength = value.length() - adjustedOffset - (quoteTrim > 0 ? 1 : 0);
                    underlineLength = Math.min(Math.max(1, valueLength), maxLength);
                } else {
                    underlineLength = Math.max(1, value.length() - quoteTrim);
                }
            } else {
                // Underline content inside quotes
                underlineStart += quoteOffset;
                underlineLength = Math.max(1, value.length() - quoteTrim);
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

        // Calculate range of lines to show
        int firstLine = Math.max(1, lineNum - contextLinesBefore);
        int lastLine = (allLines != null) ? Math.min(allLines.length, lineNum + contextLinesAfter) : lineNum;

        // Line number width for alignment (use max line number for consistent width)
        int lineNumWidth = String.valueOf(lastLine).length();
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

        // Context lines before
        if (allLines != null) {
            for (int i = firstLine; i < lineNum; i++) {
                String contextLine = (i - 1 < allLines.length) ? allLines[i - 1] : "";
                sb.append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(contextLine).append("\n");
            }
        }

        // The error line
        sb.append(padLeft(String.valueOf(lineNum), lineNumWidth)).append(" | ").append(rawLine).append("\n");

        // The marker line with carets and optional hint
        sb.append(padding).append(" | ");
        sb.append(repeat(' ', underlineStart));
        sb.append(repeat('^', Math.max(1, underlineLength)));

        if ((hint != null) && !hint.isEmpty()) {
            sb.append(" ").append(hint);
        }

        // Context lines after
        if (allLines != null) {
            for (int i = lineNum + 1; i <= lastLine; i++) {
                String contextLine = (i - 1 < allLines.length) ? allLines[i - 1] : "";
                sb.append("\n").append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(contextLine);
            }
        }

        return sb.toString();
    }

    /**
     * Formats a Rust-style error marker for the given SourceLocation (no context lines).
     *
     * @param location    the source location
     * @param sourceFile  optional source file name for the header
     * @param hint        optional hint message to show after the carets
     * @param valueOffset offset within the value to point to (-1 to underline whole value)
     * @param valueLength length of the range to underline (1 for single character)
     * @return formatted marker string
     */
    public static String formatLocation(@NonNull SourceLocation location, String sourceFile, String hint, int valueOffset, int valueLength) {
        return formatLocation(location, sourceFile, hint, valueOffset, valueLength, null, 0, 0);
    }

    private static String repeat(char c, int count) {
        if (count <= 0) return "";
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static String padLeft(String str, int length) {
        if (str.length() >= length) return str;
        return repeat(' ', length - str.length()) + str;
    }
}
