package eu.okaeri.configs.xml;

import eu.okaeri.configs.postprocessor.format.SourceLocation;
import eu.okaeri.configs.postprocessor.format.SourceWalker;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.NonNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Source walker for XML format.
 * <p>
 * Handles simple XML structure like:
 * <pre>{@code
 * <config>
 *   <database>
 *     <port>5432</port>
 *   </database>
 * </config>
 * }</pre>
 */
public class XmlSourceWalker implements SourceWalker {

    // Match opening tag: <name> or <name attr="...">
    private static final Pattern OPEN_TAG = Pattern.compile("<([a-zA-Z_][a-zA-Z0-9_\\-]*)(?:\\s[^>]*)?>(.*)");
    // Match closing tag: </name>
    private static final Pattern CLOSE_TAG = Pattern.compile("</([a-zA-Z_][a-zA-Z0-9_\\-]*)>");
    // Match self-closing tag: <name/>
    private static final Pattern SELF_CLOSING_TAG = Pattern.compile("<([a-zA-Z_][a-zA-Z0-9_\\-]*)/>");
    // Match inline element: <name>value</name>
    private static final Pattern INLINE_ELEMENT = Pattern.compile("<([a-zA-Z_][a-zA-Z0-9_\\-]*)(?:\\s[^>]*)?>([^<]*)</\\1>");
    // Match entry with key attribute: <entry key="...">value</entry>
    private static final Pattern ENTRY_WITH_KEY = Pattern.compile("<entry\\s+key=\"([^\"]+)\"[^>]*>([^<]*)</entry>");
    // Match item element: <item>value</item>
    private static final Pattern ITEM_ELEMENT = Pattern.compile("<item[^>]*>([^<]*)</item>");

    private final Map<ConfigPath, SourceLocation> pathToLocation = new LinkedHashMap<>();

    public XmlSourceWalker(@NonNull String content) {
        this.parse(content);
    }

    public static XmlSourceWalker of(@NonNull String content) {
        return new XmlSourceWalker(content);
    }

    @Override
    public SourceLocation findPath(@NonNull ConfigPath path) {
        return this.pathToLocation.get(path);
    }

    private void parse(String content) {
        String[] lines = content.split("\n", -1);
        Deque<ConfigPath> pathStack = new ArrayDeque<>();
        pathStack.push(ConfigPath.root());
        Map<ConfigPath, Integer> listIndices = new HashMap<>();

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            int lineNumber = i + 1;
            String trimmed = rawLine.trim();

            // Skip XML declaration, comments, empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("<?") || trimmed.startsWith("<!--")) {
                continue;
            }

            // Skip root config element
            if ("<config>".equals(trimmed) || "</config>".equals(trimmed)) {
                continue;
            }

            ConfigPath parentPath = pathStack.peek();

            // Check for entry with key attribute: <entry key="name">value</entry>
            Matcher entryMatcher = ENTRY_WITH_KEY.matcher(trimmed);
            if (entryMatcher.find()) {
                String key = entryMatcher.group(1);
                String value = entryMatcher.group(2);
                ConfigPath fullPath = parentPath.property(key);

                int valueStart = rawLine.indexOf(">") + 1;

                this.pathToLocation.put(fullPath, SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .rawLine(rawLine)
                    .keyColumn(rawLine.indexOf("key=\"") + 5)
                    .key(key)
                    .valueColumn(valueStart)
                    .value(value)
                    .build());
                continue;
            }

            // Check for item element (list item): <item>value</item>
            Matcher itemMatcher = ITEM_ELEMENT.matcher(trimmed);
            if (itemMatcher.find()) {
                int index = listIndices.getOrDefault(parentPath, 0);
                listIndices.put(parentPath, index + 1);

                String value = itemMatcher.group(1);
                ConfigPath fullPath = parentPath.index(index);

                int valueStart = rawLine.indexOf(">") + 1;

                this.pathToLocation.put(fullPath, SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .rawLine(rawLine)
                    .keyColumn(rawLine.indexOf("<item"))
                    .key("item")
                    .valueColumn(valueStart)
                    .value(value)
                    .build());
                continue;
            }

            // Check for inline element: <name>value</name>
            Matcher inlineMatcher = INLINE_ELEMENT.matcher(trimmed);
            if (inlineMatcher.find()) {
                String tagName = inlineMatcher.group(1);
                String value = inlineMatcher.group(2);
                ConfigPath fullPath = parentPath.property(tagName);

                int tagStart = rawLine.indexOf("<" + tagName);
                int valueStart = rawLine.indexOf(">", tagStart) + 1;

                this.pathToLocation.put(fullPath, SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .rawLine(rawLine)
                    .keyColumn(tagStart + 1) // +1 to skip '<'
                    .key(tagName)
                    .valueColumn(valueStart)
                    .value(value)
                    .build());
                continue;
            }

            // Check for self-closing tag: <name/> (represents null)
            Matcher selfClosingMatcher = SELF_CLOSING_TAG.matcher(trimmed);
            if (selfClosingMatcher.find()) {
                String tagName = selfClosingMatcher.group(1);
                if (!"null".equals(tagName)) {
                    ConfigPath fullPath = parentPath.property(tagName);
                    int tagStart = rawLine.indexOf("<" + tagName);

                    this.pathToLocation.put(fullPath, SourceLocation.builder()
                        .lineNumber(lineNumber)
                        .rawLine(rawLine)
                        .keyColumn(tagStart + 1)
                        .key(tagName)
                        .valueColumn(-1)
                        .value(null)
                        .build());
                }
                continue;
            }

            // Check for closing tag: </name>
            Matcher closeMatcher = CLOSE_TAG.matcher(trimmed);
            if (closeMatcher.find()) {
                String tagName = closeMatcher.group(1);
                // Pop if this closes the current container
                if (pathStack.size() > 1) {
                    ConfigPath current = pathStack.peek();
                    ConfigPath.PathNode lastNode = current.getLastNode();
                    if ((lastNode instanceof ConfigPath.PropertyNode) &&
                        tagName.equals(((ConfigPath.PropertyNode) lastNode).getName())) {
                        pathStack.pop();
                        // Reset list index when exiting a container
                        listIndices.remove(current);
                    }
                }
                continue;
            }

            // Check for opening tag: <name> (container element)
            Matcher openMatcher = OPEN_TAG.matcher(trimmed);
            if (openMatcher.find()) {
                String tagName = openMatcher.group(1);
                String remainder = openMatcher.group(2);

                // If there's no content after the tag, it's a container
                if (remainder.trim().isEmpty() || !remainder.contains("</")) {
                    pathStack.push(parentPath.property(tagName));
                }
            }
        }
    }
}
