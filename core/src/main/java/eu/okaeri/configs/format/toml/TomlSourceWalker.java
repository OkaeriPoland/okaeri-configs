package eu.okaeri.configs.format.toml;

import eu.okaeri.configs.format.SourceLocation;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Source walker for TOML format.
 * <p>
 * Handles TOML structure including:
 * <ul>
 *   <li>Key-value pairs: {@code key = value}</li>
 *   <li>Dotted keys: {@code a.b.c = value}</li>
 *   <li>Section headers: {@code [section]}</li>
 *   <li>Nested sections: {@code [section.subsection]}</li>
 *   <li>Array of tables: {@code [[section]]}</li>
 *   <li>Arrays with indexed access</li>
 * </ul>
 */
public class TomlSourceWalker implements SourceWalker {

    // Section header: [section] or [section.subsection]
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\s*\\[([^\\[\\]]+)]\\s*$");
    // Array of tables header: [[section]] or [[section.subsection]]
    private static final Pattern ARRAY_OF_TABLES_PATTERN = Pattern.compile("^\\s*\\[\\[([^\\[\\]]+)]]\\s*$");
    // Key-value pair: key = value or "quoted.key" = value
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\s*([^=]+?)\\s*=\\s*(.*)$");

    private final Map<ConfigPath, SourceLocation> pathToLocation = new LinkedHashMap<>();

    public TomlSourceWalker(@NonNull String content) {
        this.parse(content);
    }

    public static TomlSourceWalker of(@NonNull String content) {
        return new TomlSourceWalker(content);
    }

    @Override
    public SourceLocation findPath(@NonNull ConfigPath path) {
        SourceLocation location = this.pathToLocation.get(path);
        if (location != null) {
            return location;
        }

        // Fall back to parent paths for indexed access
        // This handles cases like stringList[2] where we only have stringList = ['a', 'b', 'invalid']
        ConfigPath searchPath = path;
        ConfigPath.PathNode lastNode = null;

        while (location == null && !searchPath.isEmpty()) {
            lastNode = searchPath.getLastNode();
            searchPath = searchPath.parent();
            location = this.pathToLocation.get(searchPath);
        }

        if (location == null || lastNode == null) {
            return null;
        }

        String value = location.getValue();
        if (value == null) {
            return location;
        }

        // For indexed access on array values, point to the specific element
        if (lastNode instanceof ConfigPath.IndexNode) {
            int index = ((ConfigPath.IndexNode) lastNode).getIndex();
            int[] range = findArrayElement(value, index);
            if (range != null) {
                return SourceLocation.builder()
                    .lineNumber(location.getLineNumber())
                    .rawLine(location.getRawLine())
                    .keyColumn(location.getKeyColumn())
                    .key(location.getKey())
                    .valueColumn(location.getValueColumn() + range[0])
                    .value(value.substring(range[0], range[0] + range[1]))
                    .build();
            }
        }

        return location;
    }

    /**
     * Finds the start offset and length of an element in a TOML array.
     * Handles: [1, 2, 3], ['a', 'b'], [{...}, {...}]
     */
    private static int[] findArrayElement(String value, int index) {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            return null;
        }

        int currentIndex = 0;
        int depth = 0;
        int start = -1;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 1; i < value.length() - 1; i++) {
            char c = value.charAt(i);

            // Handle string boundaries
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringChar = c;
                if (start < 0 && currentIndex == index) {
                    start = i;
                }
            } else if (inString && c == stringChar && value.charAt(i - 1) != '\\') {
                inString = false;
                if (currentIndex == index) {
                    return new int[]{start, i - start + 1};
                }
            } else if (!inString) {
                // Handle nesting
                if (c == '[' || c == '{') {
                    if (depth == 0 && start < 0 && currentIndex == index) {
                        start = i;
                    }
                    depth++;
                } else if (c == ']' || c == '}') {
                    depth--;
                    if (depth == 0 && currentIndex == index) {
                        return new int[]{start, i - start + 1};
                    }
                } else if (c == ',' && depth == 0) {
                    if (currentIndex == index && start >= 0) {
                        // Find end of current element (trim trailing whitespace)
                        int end = i;
                        while (end > start && Character.isWhitespace(value.charAt(end - 1))) {
                            end--;
                        }
                        return new int[]{start, end - start};
                    }
                    currentIndex++;
                    start = -1;
                } else if (!Character.isWhitespace(c) && start < 0 && currentIndex == index) {
                    start = i;
                }
            }
        }

        // Handle last element
        if (currentIndex == index && start >= 0) {
            int end = value.length() - 1;
            while (end > start && Character.isWhitespace(value.charAt(end - 1))) {
                end--;
            }
            return new int[]{start, end - start};
        }

        return null;
    }

    private void parse(String content) {
        String[] lines = content.split("\n", -1);
        ConfigPath currentSection = ConfigPath.root();

        // Track array indices for array of tables (e.g., [[servers]] -> servers[0], servers[1], ...)
        Map<String, Integer> arrayTableIndices = new LinkedHashMap<>();

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            int lineNumber = i + 1;
            String trimmed = rawLine.trim();

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // Array of tables header: [[section]] or [[section.subsection]]
            // Must check BEFORE regular section pattern since [[x]] also matches [x] in some regex impls
            Matcher arrayTableMatcher = ARRAY_OF_TABLES_PATTERN.matcher(trimmed);
            if (arrayTableMatcher.matches()) {
                String sectionName = arrayTableMatcher.group(1).trim();
                // Increment the index for this array table path
                int index = arrayTableIndices.getOrDefault(sectionName, -1) + 1;
                arrayTableIndices.put(sectionName, index);
                // Build path with index: [[servers]] -> servers[0], servers[1], etc.
                currentSection = parseDottedPath(sectionName).index(index);
                continue;
            }

            // Section header: [section] or [section.subsection]
            Matcher sectionMatcher = SECTION_PATTERN.matcher(trimmed);
            if (sectionMatcher.matches()) {
                String sectionName = sectionMatcher.group(1).trim();
                currentSection = parseDottedPath(sectionName);
                continue;
            }

            // Key = Value
            Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(rawLine);
            if (keyValueMatcher.matches()) {
                String key = keyValueMatcher.group(1).trim();
                String value = keyValueMatcher.group(2).trim();

                // Handle quoted keys
                if ((key.startsWith("\"") && key.endsWith("\"")) ||
                    (key.startsWith("'") && key.endsWith("'"))) {
                    key = key.substring(1, key.length() - 1);
                }

                // Build full path: section + key (key may also have dots)
                ConfigPath fullPath = appendDottedPath(currentSection, key);

                // Calculate columns
                int keyColumn = rawLine.indexOf(key);
                int equalsPos = findEqualsIndex(rawLine);
                int valueColumn = equalsPos + 1;

                // Skip whitespace after equals
                while (valueColumn < rawLine.length() && Character.isWhitespace(rawLine.charAt(valueColumn))) {
                    valueColumn++;
                }

                // If value is empty, valueColumn should be -1
                if (value.isEmpty()) {
                    valueColumn = -1;
                    value = null;
                }

                SourceLocation location = SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .rawLine(rawLine)
                    .keyColumn(keyColumn)
                    .key(key)
                    .valueColumn(valueColumn)
                    .value(value)
                    .build();

                this.pathToLocation.put(fullPath, location);
            }
        }
    }

    /**
     * Finds the equals sign index, respecting quoted strings.
     */
    private static int findEqualsIndex(String line) {
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inString) {
                if (c == stringChar && (i == 0 || line.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inString = true;
                    stringChar = c;
                } else if (c == '=') {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Parses a dotted path string into a ConfigPath.
     * E.g., "database.settings" becomes ConfigPath with properties ["database", "settings"]
     */
    private static ConfigPath parseDottedPath(String dottedPath) {
        if (dottedPath == null || dottedPath.isEmpty()) {
            return ConfigPath.root();
        }
        String[] parts = dottedPath.split("\\.");
        ConfigPath path = ConfigPath.root();
        for (String part : parts) {
            if (!part.isEmpty()) {
                path = path.property(part);
            }
        }
        return path;
    }

    /**
     * Appends a dotted key to an existing path.
     */
    private static ConfigPath appendDottedPath(ConfigPath base, String dottedKey) {
        String[] parts = dottedKey.split("\\.");
        ConfigPath path = base;
        for (String part : parts) {
            if (!part.isEmpty()) {
                path = path.property(part);
            }
        }
        return path;
    }
}
