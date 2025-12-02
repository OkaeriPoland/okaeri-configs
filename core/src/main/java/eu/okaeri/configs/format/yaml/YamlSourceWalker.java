package eu.okaeri.configs.format.yaml;

import eu.okaeri.configs.format.SourceLocation;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
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
public class YamlSourceWalker implements SourceWalker {

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(\\s*)([^#:\\-][^:]*?):\\s*(.*)$");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^(\\s*)-\\s*(.*)$");
    private static final Set<String> MULTILINE_INDICATORS = new HashSet<>(Arrays.asList("|", "|-", ">", ">-"));

    @Getter
    private final List<SourceLocation> locations = new ArrayList<>();

    @Getter
    private final Map<ConfigPath, SourceLocation> pathToLocation = new LinkedHashMap<>();

    private final String content;

    public YamlSourceWalker(@NonNull String content) {
        this.content = content;
        this.parse();
    }

    public static YamlSourceWalker of(@NonNull String content) {
        return new YamlSourceWalker(content);
    }

    @Override
    public SourceLocation findPath(@NonNull ConfigPath path) {
        return this.pathToLocation.get(path);
    }

    /**
     * Inserts comments from the ConfigDeclaration into the YAML content.
     * Handles header comments, nested configs, lists, and maps.
     *
     * @param declaration   the config declaration with comment annotations
     * @param commentPrefix the prefix for comments (e.g., "# ")
     * @return the content with comments inserted
     */
    public String insertComments(@NonNull ConfigDeclaration declaration, @NonNull String commentPrefix) {
        // Track which path patterns have been commented (to avoid duplicates across list items)
        Set<String> commentedPatterns = new HashSet<>();

        // Build new content with comments
        StringBuilder result = new StringBuilder();

        // Prepend header comment if present
        String[] header = declaration.getHeader();
        if (header != null) {
            result.append(formatComment(commentPrefix, header));
        }

        for (int i = 0; i < this.locations.size(); i++) {
            SourceLocation location = this.locations.get(i);
            ConfigPath path = location.getConfigPath();

            // Only add comments for locations with paths and keys
            if ((path != null) && (location.getKey() != null)) {
                String pattern = path.toPattern(declaration);
                if (!commentedPatterns.contains(pattern)) {
                    String[] comment = resolveComment(path, declaration);
                    if (comment != null) {
                        String commentStr = formatComment(commentPrefix, comment);
                        result.append(indent(commentStr, location.getIndent()));
                    }
                    commentedPatterns.add(pattern);
                }
            }

            result.append(location.getRawLine());
            // Add newline after each line except trailing empty lines
            boolean isLastLine = (i == (this.locations.size() - 1));
            boolean isTrailingEmpty = isLastLine && location.getRawLine().isEmpty();
            if (!isTrailingEmpty) {
                result.append("\n");
            }
        }

        return result.toString();
    }


    /**
     * Resolves the comment for a given path using ConfigPath.resolveFieldDeclaration().
     */
    private static String[] resolveComment(ConfigPath path, ConfigDeclaration declaration) {
        if ((path == null) || path.isEmpty() || (declaration == null)) {
            return null;
        }
        Optional<FieldDeclaration> field = path.resolveFieldDeclaration(declaration);
        return field.map(FieldDeclaration::getComment).orElse(null);
    }

    private void parse() {
        String[] rawLines = this.content.split("\n", -1);

        // Track context: list of (path, indent) pairs
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
                if (!trimmed.isEmpty() && (indent <= multilineBaseIndent)) {
                    inMultiline = false;
                } else {
                    this.locations.add(SourceLocation.builder()
                        .lineNumber(lineNumber)
                        .indent(indent)
                        .rawLine(rawLine)
                        .build());
                    continue;
                }
            }

            // Blank lines
            if (trimmed.isEmpty()) {
                this.locations.add(SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .indent(0)
                    .rawLine(rawLine)
                    .build());
                continue;
            }

            // Comments
            if (trimmed.startsWith("#")) {
                this.locations.add(SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .indent(indent)
                    .rawLine(rawLine)
                    .build());
                continue;
            }

            // Check if this is a list item first (before popping stack)
            Matcher listMatcher = LIST_ITEM_PATTERN.matcher(rawLine);
            boolean isListItem = listMatcher.matches();

            // Pop path stack for entries at same or higher indent
            // For list items, only pop entries with HIGHER indent (keep parent at same indent)
            if (isListItem) {
                while (!pathStack.isEmpty() && (pathStack.get(pathStack.size() - 1).indent > indent)) {
                    pathStack.remove(pathStack.size() - 1);
                }
            } else {
                while (!pathStack.isEmpty() && (pathStack.get(pathStack.size() - 1).indent >= indent)) {
                    pathStack.remove(pathStack.size() - 1);
                }
            }
            // Clear list indices for HIGHER indents only
            listIndices.keySet().removeIf(ind -> ind > indent);

            if (isListItem) {
                int listIndent = listMatcher.group(1).length();
                String listContent = listMatcher.group(2);

                // Get or initialize list index for this indent
                int listIndex = listIndices.getOrDefault(listIndent, -1) + 1;
                listIndices.put(listIndent, listIndex);

                // For list items, find the parent list path (skip any indexed paths at same indent)
                ConfigPath parentPath = this.getListParentPath(pathStack, listIndent);
                ConfigPath indexedPath = parentPath.index(listIndex);

                // Check if list content is a key-value
                if (listContent.contains(":")) {
                    int colonPos = listContent.indexOf(':');
                    String key = listContent.substring(0, colonPos).trim();
                    String value = listContent.substring(colonPos + 1).trim();

                    ConfigPath fullPath = indexedPath.property(key);
                    int keyColumn = rawLine.indexOf(key, listIndent + 2);
                    int valueColumn = value.isEmpty() ? -1 : rawLine.lastIndexOf(value);

                    SourceLocation location = SourceLocation.builder()
                        .lineNumber(lineNumber)
                        .keyColumn(keyColumn)
                        .indent(listIndent)
                        .key(key)
                        .value(value.isEmpty() ? null : value)
                        .valueColumn(valueColumn)
                        .rawLine(rawLine)
                        .configPath(fullPath)
                        .build();
                    this.locations.add(location);
                    this.pathToLocation.put(fullPath, location);

                    // Push the indexed path for sibling keys in same list item
                    pathStack.add(new PathEntry(indexedPath, listIndent));

                    if (isMultilineIndicator(value)) {
                        inMultiline = true;
                        multilineBaseIndent = listIndent;
                    }
                } else {
                    // Simple list item
                    int valueColumn = listContent.isEmpty() ? -1 : rawLine.indexOf(listContent, listIndent + 2);

                    SourceLocation location = SourceLocation.builder()
                        .lineNumber(lineNumber)
                        .keyColumn(listIndent + 2)
                        .indent(listIndent)
                        .value(listContent.isEmpty() ? null : listContent)
                        .valueColumn(valueColumn)
                        .rawLine(rawLine)
                        .configPath(indexedPath)
                        .build();
                    this.locations.add(location);
                    this.pathToLocation.put(indexedPath, location);
                }
                continue;
            }

            // Try key-value or key-only
            Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(rawLine);
            if (keyValueMatcher.matches()) {
                String key = keyValueMatcher.group(2).trim();
                String value = keyValueMatcher.group(3).trim();

                ConfigPath parentPath = this.getParentPath(pathStack);
                ConfigPath fullPath = parentPath.property(key);

                int keyColumn = rawLine.indexOf(key);
                int valueColumn = value.isEmpty() ? -1 : rawLine.lastIndexOf(value);

                SourceLocation location = SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .keyColumn(keyColumn)
                    .indent(indent)
                    .key(key)
                    .value(value.isEmpty() ? null : value)
                    .valueColumn(valueColumn)
                    .rawLine(rawLine)
                    .configPath(fullPath)
                    .build();
                this.locations.add(location);
                this.pathToLocation.put(fullPath, location);

                // Push for nested content
                pathStack.add(new PathEntry(fullPath, indent));

                if (isMultilineIndicator(value)) {
                    inMultiline = true;
                    multilineBaseIndent = indent;
                }
                continue;
            }

            // Unrecognized - treat as content
            this.locations.add(SourceLocation.builder()
                .lineNumber(lineNumber)
                .indent(indent)
                .rawLine(rawLine)
                .build());
        }
    }

    private ConfigPath getParentPath(List<PathEntry> pathStack) {
        if (pathStack.isEmpty()) {
            return ConfigPath.root();
        }
        return pathStack.get(pathStack.size() - 1).path;
    }

    /**
     * Finds the parent path for a list item, skipping indexed paths at the same indent level.
     * This ensures consecutive list items share the same parent (e.g., items[0] and items[1] both have parent "items").
     */
    private ConfigPath getListParentPath(List<PathEntry> pathStack, int listIndent) {
        if (pathStack.isEmpty()) {
            return ConfigPath.root();
        }
        // Walk backwards through the stack to find the first non-indexed path at this indent level
        for (int i = pathStack.size() - 1; i >= 0; i--) {
            PathEntry entry = pathStack.get(i);
            if (entry.indent == listIndent) {
                // Check if this path ends with an index node (meaning it's from a previous list item)
                List<ConfigPath.PathNode> nodes = entry.path.getNodes();
                if (!nodes.isEmpty() && (nodes.get(nodes.size() - 1) instanceof ConfigPath.IndexNode)) {
                    // Skip this indexed path, continue looking
                    continue;
                }
            }
            return entry.path;
        }
        return ConfigPath.root();
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

    private static String formatComment(String prefix, String[] lines) {
        if (lines == null) return "";
        if (prefix == null) prefix = "";

        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            if (line.isEmpty()) {
                result.append("\n");
            } else if (line.startsWith(prefix.trim())) {
                result.append(line).append("\n");
            } else {
                result.append(prefix).append(line).append("\n");
            }
        }
        return result.toString();
    }

    private static String indent(String text, int spaces) {
        if (spaces <= 0) return text;

        StringBuilder indentStr = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            indentStr.append(" ");
        }
        String indent = indentStr.toString();

        StringBuilder result = new StringBuilder();
        for (String line : text.split("\n")) {
            result.append(indent).append(line).append("\n");
        }
        return result.toString();
    }

    private static class PathEntry {
        final ConfigPath path;
        final int indent;

        PathEntry(ConfigPath path, int indent) {
            this.path = path;
            this.indent = indent;
        }
    }
}
