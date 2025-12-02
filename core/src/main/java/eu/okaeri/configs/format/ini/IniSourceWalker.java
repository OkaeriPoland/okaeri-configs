package eu.okaeri.configs.format.ini;

import eu.okaeri.configs.format.SourceLocation;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Source walker for INI and Properties formats.
 * <p>
 * Handles both:
 * <ul>
 *   <li>Properties format: {@code database.port=5432}</li>
 *   <li>INI format: {@code [database]} section with {@code port=5432}</li>
 * </ul>
 */
public class IniSourceWalker implements SourceWalker {

    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\s*\\[(.+)]\\s*$");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\s*([^=;#]+?)\\s*=\\s*(.*)$");

    private final Map<ConfigPath, SourceLocation> pathToLocation = new LinkedHashMap<>();

    public IniSourceWalker(@NonNull String content) {
        this.parse(content);
    }

    public static IniSourceWalker of(@NonNull String content) {
        return new IniSourceWalker(content);
    }

    @Override
    public SourceLocation findPath(@NonNull ConfigPath path) {
        SourceLocation location = this.pathToLocation.get(path);
        if (location != null) {
            return location;
        }

        // Fall back to parent paths for indexed access
        // This handles cases like numbers[2] where we only have numbers=1,2,invalid,4
        ConfigPath searchPath = path;
        ConfigPath.PathNode lastNode = null;

        while ((location == null) && !searchPath.isEmpty()) {
            lastNode = searchPath.getLastNode();
            searchPath = searchPath.parent();
            location = this.pathToLocation.get(searchPath);
        }

        if ((location == null) || (lastNode == null)) {
            return null;
        }

        String value = location.getValue();
        if (value == null) {
            return location;
        }

        // For indexed access on comma-separated values, point to the specific element
        if (lastNode instanceof ConfigPath.IndexNode) {
            int index = ((ConfigPath.IndexNode) lastNode).getIndex();
            int[] range = findCommaSeparatedElement(value, index);
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
     * Finds the start offset and length of an element in a comma-separated string.
     */
    private static int[] findCommaSeparatedElement(String value, int index) {
        int currentIndex = 0;
        int start = 0;

        for (int i = 0; i <= value.length(); i++) {
            if ((i == value.length()) || (value.charAt(i) == ',')) {
                if (currentIndex == index) {
                    return new int[]{start, i - start};
                }
                currentIndex++;
                start = i + 1;
            }
        }
        return null;
    }

    private void parse(String content) {
        String[] lines = content.split("\n", -1);
        ConfigPath currentSection = ConfigPath.root();

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            int lineNumber = i + 1;
            String trimmed = rawLine.trim();

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) {
                continue;
            }

            // Section header: [section.name]
            Matcher sectionMatcher = SECTION_PATTERN.matcher(trimmed);
            if (sectionMatcher.matches()) {
                String sectionName = sectionMatcher.group(1).trim();
                // Parse section name - may contain dots like [database.settings]
                currentSection = parseDottedPath(sectionName);
                continue;
            }

            // Key=Value
            Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(rawLine);
            if (keyValueMatcher.matches()) {
                String key = keyValueMatcher.group(1).trim();
                String value = keyValueMatcher.group(2);

                // Build full path: section + key (key may also have dots in properties format)
                ConfigPath fullPath = appendDottedPath(currentSection, key);

                // Calculate columns
                int keyColumn = rawLine.indexOf(key);
                int equalsPos = rawLine.indexOf('=', keyColumn);
                int valueColumn = equalsPos + 1;

                // Skip whitespace after equals
                while ((valueColumn < rawLine.length()) && Character.isWhitespace(rawLine.charAt(valueColumn))) {
                    valueColumn++;
                }

                // If value is empty or only whitespace, valueColumn should be -1
                if (value.trim().isEmpty()) {
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
     * Parses a dotted path string into a ConfigPath.
     * E.g., "database.settings" becomes ConfigPath with properties ["database", "settings"]
     */
    private static ConfigPath parseDottedPath(String dottedPath) {
        if ((dottedPath == null) || dottedPath.isEmpty()) {
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
