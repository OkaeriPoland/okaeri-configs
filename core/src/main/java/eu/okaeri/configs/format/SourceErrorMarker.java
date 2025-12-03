package eu.okaeri.configs.format;

import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Builder;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Formats Rust-style error markers for any source content.
 * <p>
 * Example usage:
 * <pre>{@code
 * String marker = SourceErrorMarker.builder()
 *     .walker(walker)
 *     .path(path)
 *     .sourceFile("config.yml")
 *     .hint("Expected integer")
 *     .rawContent(content)
 *     .includeCommentsAbove(true)
 *     .commentChecker(configurer::isCommentLine)
 *     .build()
 *     .format();
 * }</pre>
 * <p>
 * Example output:
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
@Builder
public final class SourceErrorMarker {

    /**
     * Maximum number of lines to show before using ellipsis for multiline content.
     */
    private static final int MAX_MULTILINE_DISPLAY = 5;

    // Required
    private final @NonNull SourceWalker walker;
    private final @NonNull ConfigPath path;

    // Optional with defaults
    private final String sourceFile;
    private final String hint;
    private final @Builder.Default int valueOffset = -1;
    private final @Builder.Default int valueLength = 1;
    private final String rawContent;
    private final int contextLinesBefore;
    private final int contextLinesAfter;
    private final boolean includeCommentsAbove;
    private final Predicate<String> commentChecker;

    /**
     * Formats the error marker.
     *
     * @return formatted marker string, or empty string if path not found
     */
    public String format() {

        SourceLocation location = this.walker.findPath(this.path);
        if (location == null) {
            return "";
        }

        return this.formatLocation(location);
    }

    private String formatLocation(SourceLocation location) {
        // Handle multiline block scalars specially
        if (location.isMultiline() && (this.rawContent != null)) {
            return this.formatMultilineLocation(location);
        }

        StringBuilder sb = new StringBuilder();

        int lineNum = location.getLineNumber();
        String rawLine = location.getRawLine();

        // Parse raw content into lines for context
        String[] allLines = null;
        if (this.rawContent != null) {
            allLines = this.rawContent.split("\n", -1);
        }

        // Calculate context lines, expanding for comments if enabled
        int ctxBefore = this.contextLinesBefore;
        if (this.includeCommentsAbove && (allLines != null) && (this.commentChecker != null)) {
            int commentCount = countConsecutiveCommentsAbove(allLines, lineNum - 1, this.commentChecker);
            if (commentCount > ctxBefore) {
                ctxBefore = commentCount;
            }
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
                if (((first == '"') && (last == '"')) || ((first == '\'') && (last == '\''))) {
                    quoteOffset = 1;
                    quoteTrim = 2;
                }
            }

            if (this.valueOffset >= 0) {
                // Point to specific range within the value
                int adjustedOffset = this.valueOffset + quoteOffset;
                if (adjustedOffset < value.length()) {
                    underlineStart += adjustedOffset;
                    int maxLength = value.length() - adjustedOffset - ((quoteTrim > 0) ? 1 : 0);
                    underlineLength = Math.min(Math.max(1, this.valueLength), maxLength);
                } else {
                    underlineLength = Math.max(1, value.length() - quoteTrim);
                }
            } else {
                underlineStart += quoteOffset;
                underlineLength = Math.max(1, value.length() - quoteTrim);
            }
        } else if ((location.getKeyColumn() >= 0) && (location.getKey() != null)) {
            underlineStart = location.getKeyColumn();
            underlineLength = location.getKey().length();
        } else {
            underlineStart = 0;
            underlineLength = Math.max(1, rawLine.trim().length());
        }

        // Calculate range of lines to show
        int firstLine = Math.max(1, lineNum - ctxBefore);
        int lastLine = (allLines != null) ? Math.min(allLines.length, lineNum + this.contextLinesAfter) : lineNum;

        int lineNumWidth = String.valueOf(lastLine).length();
        String padding = repeat(' ', lineNumWidth);

        // Header
        sb.append(" --> ");
        if ((this.sourceFile != null) && !this.sourceFile.isEmpty()) {
            sb.append(this.sourceFile).append(":");
        }
        sb.append(lineNum).append(":").append(underlineStart + 1);
        sb.append("\n");

        sb.append(padding).append(" |\n");

        // Context lines before
        if (allLines != null) {
            for (int i = firstLine; i < lineNum; i++) {
                String contextLine = ((i - 1) < allLines.length) ? allLines[i - 1] : "";
                sb.append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(contextLine).append("\n");
            }
        }

        // Error line
        sb.append(padLeft(String.valueOf(lineNum), lineNumWidth)).append(" | ").append(rawLine).append("\n");

        // Marker line
        sb.append(padding).append(" | ");
        sb.append(repeat(' ', underlineStart));
        sb.append(repeat('^', Math.max(1, underlineLength)));

        if ((this.hint != null) && !this.hint.isEmpty()) {
            sb.append(" ").append(this.hint);
        }

        // Context lines after
        if (allLines != null) {
            for (int i = lineNum + 1; i <= lastLine; i++) {
                String contextLine = ((i - 1) < allLines.length) ? allLines[i - 1] : "";
                sb.append("\n").append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(contextLine);
            }
        }

        return sb.toString();
    }

    private String formatMultilineLocation(SourceLocation location) {
        StringBuilder sb = new StringBuilder();

        int keyLineNum = location.getLineNumber();
        String keyRawLine = location.getRawLine();
        int contentStartLineNum = location.getContentLineNumber();
        int contentEndLineNum = location.getContentEndLineNumber();
        int contentColumn = location.getContentColumn();

        if (contentStartLineNum <= 0) {
            contentStartLineNum = keyLineNum;
            contentEndLineNum = keyLineNum;
            contentColumn = (location.getValueColumn() >= 0) ? location.getValueColumn() : 0;
        }

        String[] allLines = this.rawContent.split("\n", -1);

        // Count comments above key line
        int commentsAboveCount = 0;
        if (this.includeCommentsAbove && (this.commentChecker != null)) {
            commentsAboveCount = countConsecutiveCommentsAbove(allLines, keyLineNum - 1, this.commentChecker);
        }

        // Build multiline content and track line boundaries
        List<LineInfo> lineInfos = new ArrayList<>();
        int charOffset = 0;

        for (int i = contentStartLineNum; i <= contentEndLineNum; i++) {
            if ((i - 1) < allLines.length) {
                String line = allLines[i - 1];
                String trimmedLine = line.substring(Math.min(contentColumn, line.length()));
                lineInfos.add(new LineInfo(i, charOffset, trimmedLine, line));
                charOffset += trimmedLine.length();
                if (i < contentEndLineNum) {
                    charOffset++; // for newline
                }
            }
        }

        // Determine error position(s)
        int errorStartLine = contentStartLineNum;
        int errorStartCol = contentColumn;
        int errorEndLine = contentStartLineNum;
        int errorEndCol = contentColumn;
        int underlineLength = 1;

        if (this.valueOffset >= 0) {
            OffsetPosition startPos = translateOffset(lineInfos, this.valueOffset, contentColumn);
            errorStartLine = startPos.lineNum;
            errorStartCol = startPos.column;

            int endOffset = (this.valueOffset + Math.max(1, this.valueLength)) - 1;
            OffsetPosition endPos = translateOffset(lineInfos, endOffset, contentColumn);
            errorEndLine = endPos.lineNum;
            errorEndCol = endPos.column;

            if (errorStartLine == errorEndLine) {
                underlineLength = Math.max(1, (errorEndCol - errorStartCol) + 1);
            }
        } else {
            if (!lineInfos.isEmpty()) {
                LineInfo lastLine = lineInfos.get(lineInfos.size() - 1);
                errorStartLine = lastLine.lineNum;
                errorEndLine = lastLine.lineNum;
                errorStartCol = contentColumn;
                errorEndCol = (contentColumn + lastLine.trimmedContent.length()) - 1;
                underlineLength = Math.max(1, lastLine.trimmedContent.length());
            }
        }

        // Determine display range
        int displayStartLine = keyLineNum - commentsAboveCount;
        int displayEndLine = Math.max(errorEndLine, contentEndLineNum);
        while ((displayEndLine > errorEndLine) && ((displayEndLine - 1) < allLines.length)) {
            String line = allLines[displayEndLine - 1];
            if (line.trim().isEmpty()) {
                displayEndLine--;
            } else {
                break;
            }
        }

        int lineNumWidth = String.valueOf(displayEndLine).length();
        String padding = repeat(' ', lineNumWidth);

        // Header
        sb.append(" --> ");
        if ((this.sourceFile != null) && !this.sourceFile.isEmpty()) {
            sb.append(this.sourceFile).append(":");
        }
        sb.append(errorStartLine).append(":").append(errorStartCol + 1);
        sb.append("\n");

        sb.append(padding).append(" |\n");

        int totalLines = (displayEndLine - displayStartLine) + 1;
        boolean hasCustomContext = (this.contextLinesBefore > 0) || (this.contextLinesAfter > 0);

        if ((totalLines <= MAX_MULTILINE_DISPLAY) && !hasCustomContext) {
            // Show all lines
            for (int i = displayStartLine; i <= displayEndLine; i++) {
                String line = ((i - 1) < allLines.length) ? allLines[i - 1] : "";
                sb.append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(line).append("\n");

                if ((i >= errorStartLine) && (i <= errorEndLine)) {
                    sb.append(padding).append(" | ");

                    int markerStart;
                    int markerLength;
                    boolean isLastErrorLine = (i == errorEndLine);

                    if ((i == errorStartLine) && (i == errorEndLine)) {
                        markerStart = errorStartCol;
                        markerLength = underlineLength;
                    } else if (i == errorStartLine) {
                        markerStart = errorStartCol;
                        markerLength = line.length() - errorStartCol;
                    } else if (i == errorEndLine) {
                        markerStart = contentColumn;
                        markerLength = (errorEndCol - contentColumn) + 1;
                    } else {
                        markerStart = contentColumn;
                        markerLength = line.length() - contentColumn;
                    }

                    sb.append(repeat(' ', markerStart));
                    sb.append(repeat('^', Math.max(1, markerLength)));

                    if (isLastErrorLine && (this.hint != null) && !this.hint.isEmpty()) {
                        sb.append(" ").append(this.hint);
                    }
                    sb.append("\n");
                }
            }
            if ((sb.length() > 0) && (sb.charAt(sb.length() - 1) == '\n')) {
                sb.setLength(sb.length() - 1);
            }
        } else {
            // Ellipsis mode
            int ctxBefore = (this.contextLinesBefore > 0) ? this.contextLinesBefore : 2;
            int ctxAfter = (this.contextLinesAfter > 0) ? this.contextLinesAfter : 2;

            // Show comments above key line
            for (int i = displayStartLine; i < keyLineNum; i++) {
                String line = ((i - 1) < allLines.length) ? allLines[i - 1] : "";
                sb.append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(line).append("\n");
            }

            // Show key line
            sb.append(padLeft(String.valueOf(keyLineNum), lineNumWidth)).append(" | ").append(keyRawLine).append("\n");

            int lastLineAfterKey = Math.min(keyLineNum + ctxBefore, errorStartLine - 1);
            int firstLineBeforeError = Math.max(errorStartLine - ctxAfter, keyLineNum + 1);

            for (int i = keyLineNum + 1; i <= lastLineAfterKey; i++) {
                String line = ((i - 1) < allLines.length) ? allLines[i - 1] : "";
                sb.append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(line).append("\n");
            }

            int skippedLines = firstLineBeforeError - lastLineAfterKey - 1;
            if (skippedLines > 0) {
                sb.append(padding).append(" | ... (").append(skippedLines).append(" more line").append((skippedLines > 1) ? "s" : "").append(")\n");
            }

            for (int i = Math.max(firstLineBeforeError, lastLineAfterKey + 1); i < errorStartLine; i++) {
                String line = ((i - 1) < allLines.length) ? allLines[i - 1] : "";
                sb.append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(line).append("\n");
            }

            // Show error lines
            for (int i = errorStartLine; i <= errorEndLine; i++) {
                String line = ((i - 1) < allLines.length) ? allLines[i - 1] : "";
                sb.append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(line).append("\n");

                sb.append(padding).append(" | ");

                int markerStart;
                int markerLength;
                boolean isLastErrorLine = (i == errorEndLine);

                if ((i == errorStartLine) && (i == errorEndLine)) {
                    markerStart = errorStartCol;
                    markerLength = underlineLength;
                } else if (i == errorStartLine) {
                    markerStart = errorStartCol;
                    markerLength = line.length() - errorStartCol;
                } else if (i == errorEndLine) {
                    markerStart = contentColumn;
                    markerLength = (errorEndCol - contentColumn) + 1;
                } else {
                    markerStart = contentColumn;
                    markerLength = line.length() - contentColumn;
                }

                sb.append(repeat(' ', markerStart));
                sb.append(repeat('^', Math.max(1, markerLength)));

                if (isLastErrorLine && (this.hint != null) && !this.hint.isEmpty()) {
                    sb.append(" ").append(this.hint);
                }

                if (i < errorEndLine) {
                    sb.append("\n");
                }
            }

            // Show remaining lines after error
            if (errorEndLine < contentEndLineNum) {
                int remaining = contentEndLineNum - errorEndLine;
                if (remaining <= 2) {
                    for (int i = errorEndLine + 1; i <= contentEndLineNum; i++) {
                        String line = ((i - 1) < allLines.length) ? allLines[i - 1] : "";
                        sb.append("\n").append(padLeft(String.valueOf(i), lineNumWidth)).append(" | ").append(line);
                    }
                } else {
                    sb.append("\n").append(padding).append(" | ... (").append(remaining).append(" more line").append((remaining > 1) ? "s" : "").append(")");
                }
            }
        }

        return sb.toString();
    }

    private static OffsetPosition translateOffset(List<LineInfo> lineInfos, int offset, int baseColumn) {
        int currentOffset = 0;
        for (LineInfo info : lineInfos) {
            int lineLength = info.trimmedContent.length();
            if ((currentOffset + lineLength) > offset) {
                int columnInTrimmed = offset - currentOffset;
                return new OffsetPosition(info.lineNum, baseColumn + columnInTrimmed);
            }
            currentOffset += lineLength + 1;
        }
        if (!lineInfos.isEmpty()) {
            LineInfo last = lineInfos.get(lineInfos.size() - 1);
            int lastCharCol = baseColumn + Math.max(0, last.trimmedContent.length() - 1);
            return new OffsetPosition(last.lineNum, lastCharCol);
        }
        return new OffsetPosition(1, baseColumn);
    }

    private static int countConsecutiveCommentsAbove(String[] lines, int lineIndex, Predicate<String> commentChecker) {
        int count = 0;
        for (int i = lineIndex - 1; i >= 0; i--) {
            if (commentChecker.test(lines[i])) {
                count++;
            } else {
                break;
            }
        }
        return count;
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

    private static class LineInfo {
        final int lineNum;
        final int startOffset;
        final String trimmedContent;
        final String rawLine;

        LineInfo(int lineNum, int startOffset, String trimmedContent, String rawLine) {
            this.lineNum = lineNum;
            this.startOffset = startOffset;
            this.trimmedContent = trimmedContent;
            this.rawLine = rawLine;
        }
    }

    private static class OffsetPosition {
        final int lineNum;
        final int column;

        OffsetPosition(int lineNum, int column) {
            this.lineNum = lineNum;
            this.column = column;
        }
    }
}
