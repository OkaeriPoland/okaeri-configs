package eu.okaeri.configs.format.xml;

import eu.okaeri.configs.format.SourceLocation;
import eu.okaeri.configs.format.SourceWalker;
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
    private final String rootElement;

    public XmlSourceWalker(@NonNull String content, @NonNull String rootElement) {
        this.rootElement = rootElement;
        this.parse(content);
    }

    public static XmlSourceWalker of(@NonNull String content, @NonNull String rootElement) {
        return new XmlSourceWalker(content, rootElement);
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

            // Skip empty lines
            if (rawLine.trim().isEmpty()) {
                continue;
            }

            // Process all elements in this line (handles inline/minified XML)
            this.parseLine(rawLine, lineNumber, pathStack, listIndices);
        }
    }

    private void parseLine(String rawLine, int lineNumber, Deque<ConfigPath> pathStack, Map<ConfigPath, Integer> listIndices) {
        int pos = 0;

        while (pos < rawLine.length()) {
            // Find next '<'
            int tagStart = rawLine.indexOf('<', pos);
            if (tagStart < 0) {
                break;
            }

            // Skip XML declaration: <?...?>
            if (rawLine.startsWith("<?", tagStart)) {
                int declEnd = rawLine.indexOf("?>", tagStart);
                pos = (declEnd >= 0) ? (declEnd + 2) : rawLine.length();
                continue;
            }

            // Skip comment: <!--...-->
            if (rawLine.startsWith("<!--", tagStart)) {
                int commentEnd = rawLine.indexOf("-->", tagStart);
                pos = (commentEnd >= 0) ? (commentEnd + 3) : rawLine.length();
                continue;
            }

            ConfigPath parentPath = pathStack.peek();
            String remaining = rawLine.substring(tagStart);

            // Check for closing tag: </name>
            Matcher closeMatcher = CLOSE_TAG.matcher(remaining);
            if (closeMatcher.lookingAt()) {
                String tagName = closeMatcher.group(1);
                if (!this.rootElement.equals(tagName) && (pathStack.size() > 1)) {
                    ConfigPath current = pathStack.peek();
                    ConfigPath.PathNode lastNode = current.getLastNode();
                    if ((lastNode instanceof ConfigPath.PropertyNode) &&
                        tagName.equals(((ConfigPath.PropertyNode) lastNode).getName())) {
                        pathStack.pop();
                        listIndices.remove(current);
                    }
                }
                pos = tagStart + closeMatcher.end();
                continue;
            }

            // Check for self-closing tag: <name/>
            Matcher selfClosingMatcher = SELF_CLOSING_TAG.matcher(remaining);
            if (selfClosingMatcher.lookingAt()) {
                String tagName = selfClosingMatcher.group(1);
                if (!"null".equals(tagName) && !this.rootElement.equals(tagName)) {
                    ConfigPath fullPath = parentPath.property(tagName);
                    this.pathToLocation.put(fullPath, SourceLocation.builder()
                        .lineNumber(lineNumber)
                        .rawLine(rawLine)
                        .keyColumn(tagStart + 1)
                        .key(tagName)
                        .valueColumn(-1)
                        .value(null)
                        .build());
                }
                pos = tagStart + selfClosingMatcher.end();
                continue;
            }

            // Check for entry with key attribute: <entry key="...">value</entry>
            Matcher entryMatcher = ENTRY_WITH_KEY.matcher(remaining);
            if (entryMatcher.lookingAt()) {
                String key = entryMatcher.group(1);
                String value = entryMatcher.group(2);
                ConfigPath fullPath = parentPath.property(key);

                int keyAttrPos = remaining.indexOf("key=\"") + 5;
                int valueStart = remaining.indexOf(">") + 1;

                this.pathToLocation.put(fullPath, SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .rawLine(rawLine)
                    .keyColumn(tagStart + keyAttrPos)
                    .key(key)
                    .valueColumn(tagStart + valueStart)
                    .value(value)
                    .build());
                pos = tagStart + entryMatcher.end();
                continue;
            }

            // Check for item element: <item>value</item>
            Matcher itemMatcher = ITEM_ELEMENT.matcher(remaining);
            if (itemMatcher.lookingAt()) {
                int index = listIndices.getOrDefault(parentPath, 0);
                listIndices.put(parentPath, index + 1);

                String value = itemMatcher.group(1);
                ConfigPath fullPath = parentPath.index(index);
                int valueStart = remaining.indexOf(">") + 1;

                this.pathToLocation.put(fullPath, SourceLocation.builder()
                    .lineNumber(lineNumber)
                    .rawLine(rawLine)
                    .keyColumn(tagStart)
                    .key("item")
                    .valueColumn(tagStart + valueStart)
                    .value(value)
                    .build());
                pos = tagStart + itemMatcher.end();
                continue;
            }

            // Check for inline element: <name>value</name>
            Matcher inlineMatcher = INLINE_ELEMENT.matcher(remaining);
            if (inlineMatcher.lookingAt()) {
                String tagName = inlineMatcher.group(1);
                String value = inlineMatcher.group(2);

                if (!this.rootElement.equals(tagName)) {
                    ConfigPath fullPath = parentPath.property(tagName);
                    int valueStart = remaining.indexOf(">") + 1;

                    this.pathToLocation.put(fullPath, SourceLocation.builder()
                        .lineNumber(lineNumber)
                        .rawLine(rawLine)
                        .keyColumn(tagStart + 1)
                        .key(tagName)
                        .valueColumn(tagStart + valueStart)
                        .value(value)
                        .build());
                }
                pos = tagStart + inlineMatcher.end();
                continue;
            }

            // Check for opening tag: <name> (container element)
            Matcher openMatcher = OPEN_TAG.matcher(remaining);
            if (openMatcher.lookingAt()) {
                String tagName = openMatcher.group(1);
                if (!this.rootElement.equals(tagName)) {
                    pathStack.push(parentPath.property(tagName));
                }
                // Only advance past the opening tag itself, not captured content
                int closeAngle = remaining.indexOf('>');
                pos = tagStart + closeAngle + 1;
                continue;
            }

            // No match, move past this character
            pos = tagStart + 1;
        }
    }
}
