package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Getter;
import lombok.NonNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walks through YAML content tracking paths and positions.
 * Supports nested objects, lists, and maps for both comment insertion
 * and error position lookup.
 */
public class YamlWalker {

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(\\s*)([^#:\\-][^:]*?):\\s*(.*)$");
    private static final Pattern KEY_ONLY_PATTERN = Pattern.compile("^(\\s*)([^#:\\-][^:]*?):\\s*$");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^(\\s*)-\\s*(.*)$");
    private static final Set<String> MULTILINE_INDICATORS = new HashSet<>(Arrays.asList("|", "|-", ">", ">-"));

    @Getter
    private final List<YamlLine> lines = new ArrayList<>();

    @Getter
    private final Map<String, YamlLine> pathToLine = new LinkedHashMap<>();

    private final String content;

    public YamlWalker(@NonNull String content) {
        this.content = content;
        this.parse();
    }

    public static YamlWalker of(@NonNull String content) {
        return new YamlWalker(content);
    }

    /**
     * Find the line for a given ConfigPath.
     */
    public YamlLine findLine(@NonNull ConfigPath path) {
        return this.pathToLine.get(path.toString());
    }

    /**
     * Find the line for a given path string.
     */
    public YamlLine findLine(@NonNull String path) {
        return this.pathToLine.get(path);
    }

    private void parse() {
        String[] rawLines = this.content.split("\n", -1);

        // Track context: list of (path, indent) pairs
        // When indent decreases, we pop entries with higher or equal indent
        List<PathEntry> pathStack = new ArrayList<>();

        // Track list indices at each indent level
        Map<Integer, Integer> listIndices = new HashMap<>();

        boolean inMultiline = false;
        int multilineBaseIndent = 0;

        for (int i = 0; i < rawLines.length; i++) {
            String rawLine = rawLines[i];
            int lineNumber = i + 1;
            int indent = countIndent(rawLine);
            String trimmed = rawLine.trim();

            // Handle multiline content
            if (inMultiline) {
                if (!trimmed.isEmpty() && indent <= multilineBaseIndent) {
                    inMultiline = false;
                } else {
                    this.lines.add(YamlLine.builder()
                        .lineNumber(lineNumber)
                        .column(indent)
                        .indent(indent)
                        .type(YamlLineType.MULTILINE_CONTENT)
                        .rawLine(rawLine)
                        .build());
                    continue;
                }
            }

            // Blank lines
            if (trimmed.isEmpty()) {
                this.lines.add(YamlLine.builder()
                    .lineNumber(lineNumber)
                    .column(0)
                    .indent(0)
                    .type(YamlLineType.BLANK)
                    .rawLine(rawLine)
                    .build());
                continue;
            }

            // Comments
            if (trimmed.startsWith("#")) {
                this.lines.add(YamlLine.builder()
                    .lineNumber(lineNumber)
                    .column(indent)
                    .indent(indent)
                    .type(YamlLineType.COMMENT)
                    .rawLine(rawLine)
                    .build());
                continue;
            }

            // Pop path stack for entries at same or higher indent
            while (!pathStack.isEmpty() && pathStack.get(pathStack.size() - 1).indent >= indent) {
                pathStack.remove(pathStack.size() - 1);
            }
            // Clear list indices for HIGHER indents only (not same level - that's a sibling in same list)
            listIndices.keySet().removeIf(ind -> ind > indent);

            // Check if this is a list item
            Matcher listMatcher = LIST_ITEM_PATTERN.matcher(rawLine);
            if (listMatcher.matches()) {
                int listIndent = listMatcher.group(1).length();
                String listContent = listMatcher.group(2);

                // Get or initialize list index for this indent
                int listIndex = listIndices.getOrDefault(listIndent, -1) + 1;
                listIndices.put(listIndent, listIndex);

                // Build parent path
                String parentPath = buildPath(pathStack);
                String indexedPath = parentPath.isEmpty()
                    ? "[" + listIndex + "]"
                    : parentPath + "[" + listIndex + "]";

                // Check if list content is a key-value
                if (listContent.contains(":")) {
                    int colonPos = listContent.indexOf(':');
                    String key = listContent.substring(0, colonPos).trim();
                    String value = listContent.substring(colonPos + 1).trim();

                    String fullPath = indexedPath + "." + key;
                    int keyColumn = rawLine.indexOf(key, listIndent + 2);
                    int valueColumn = value.isEmpty() ? -1 : rawLine.lastIndexOf(value);

                    YamlLineType type = value.isEmpty() ? YamlLineType.LIST_ITEM_KEY_ONLY : YamlLineType.LIST_ITEM_KEY_VALUE;
                    YamlLine line = YamlLine.builder()
                        .lineNumber(lineNumber)
                        .column(keyColumn)
                        .indent(listIndent)
                        .type(type)
                        .key(key)
                        .value(value.isEmpty() ? null : value)
                        .valueColumn(valueColumn)
                        .rawLine(rawLine)
                        .path(fullPath)
                        .listIndex(listIndex)
                        .build();
                    this.lines.add(line);
                    this.pathToLine.put(fullPath, line);

                    // Push the indexed path (not the key path) for sibling keys in same list item
                    // Use listIndent so siblings like "port:" at same effective indent can find parent
                    pathStack.add(new PathEntry(indexedPath, listIndent));

                    if (isMultilineIndicator(value)) {
                        inMultiline = true;
                        multilineBaseIndent = listIndent;
                    }
                } else {
                    // Simple list item
                    int valueColumn = listContent.isEmpty() ? -1 : rawLine.indexOf(listContent, listIndent + 2);

                    YamlLine line = YamlLine.builder()
                        .lineNumber(lineNumber)
                        .column(listIndent + 2)
                        .indent(listIndent)
                        .type(YamlLineType.LIST_ITEM)
                        .value(listContent.isEmpty() ? null : listContent)
                        .valueColumn(valueColumn)
                        .rawLine(rawLine)
                        .path(indexedPath)
                        .listIndex(listIndex)
                        .build();
                    this.lines.add(line);
                    this.pathToLine.put(indexedPath, line);
                }
                continue;
            }

            // Try key-value or key-only
            Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(rawLine);
            if (keyValueMatcher.matches()) {
                String key = keyValueMatcher.group(2).trim();
                String value = keyValueMatcher.group(3).trim();

                String parentPath = buildPath(pathStack);
                String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;

                int keyColumn = rawLine.indexOf(key);
                int valueColumn = value.isEmpty() ? -1 : rawLine.lastIndexOf(value);

                YamlLineType type = value.isEmpty() ? YamlLineType.KEY_ONLY : YamlLineType.KEY_VALUE;
                YamlLine line = YamlLine.builder()
                    .lineNumber(lineNumber)
                    .column(keyColumn)
                    .indent(indent)
                    .type(type)
                    .key(key)
                    .value(value.isEmpty() ? null : value)
                    .valueColumn(valueColumn)
                    .rawLine(rawLine)
                    .path(fullPath)
                    .build();
                this.lines.add(line);
                this.pathToLine.put(fullPath, line);

                // Push for nested content
                pathStack.add(new PathEntry(fullPath, indent));

                if (isMultilineIndicator(value)) {
                    inMultiline = true;
                    multilineBaseIndent = indent;
                }
                continue;
            }

            // Unrecognized - treat as content
            this.lines.add(YamlLine.builder()
                .lineNumber(lineNumber)
                .column(indent)
                .indent(indent)
                .type(YamlLineType.MULTILINE_CONTENT)
                .rawLine(rawLine)
                .build());
        }
    }

    private String buildPath(List<PathEntry> pathStack) {
        if (pathStack.isEmpty()) {
            return "";
        }
        return pathStack.get(pathStack.size() - 1).path;
    }

    private static int countIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 2;
            else break;
        }
        return count;
    }

    private static boolean isMultilineIndicator(String value) {
        return MULTILINE_INDICATORS.contains(value);
    }

    private static class PathEntry {
        final String path;
        final int indent;

        PathEntry(String path, int indent) {
            this.path = path;
            this.indent = indent;
        }
    }
}
