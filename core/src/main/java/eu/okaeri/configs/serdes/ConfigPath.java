package eu.okaeri.configs.serdes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a path to a configuration value, supporting nested properties,
 * list indices, and map keys. Inspired by JSR-380 (Bean Validation) property paths.
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code database.host} - nested property</li>
 *   <li>{@code servers[0].name} - list index</li>
 *   <li>{@code settings["api-key"]} - map key (string)</li>
 *   <li>{@code limits[0]["daily"]} - combined</li>
 * </ul>
 * <p>
 * Instances are immutable - each navigation method returns a new ConfigPath.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class ConfigPath implements SerdesContextAttachment {

    public static final ConfigPath ROOT = new ConfigPath(Collections.emptyList());

    private final List<PathNode> nodes;

    /**
     * Creates an empty root path.
     *
     * @return empty path
     */
    public static ConfigPath root() {
        return ROOT;
    }

    /**
     * Creates a path starting with a property name.
     *
     * @param name the property name
     * @return new path with single property node
     */
    public static ConfigPath of(@NonNull String name) {
        return ROOT.property(name);
    }

    /**
     * Parses a path string into a ConfigPath.
     * Supports properties, indices, and quoted keys.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "database.host"} → property("database").property("host")</li>
     *   <li>{@code "servers[0].name"} → property("servers").index(0).property("name")</li>
     *   <li>{@code "settings[\"api-key\"]"} → property("settings").key("api-key")</li>
     * </ul>
     *
     * @param path the path string to parse
     * @return parsed ConfigPath
     */
    public static ConfigPath parse(@NonNull String path) {
        if (path.isEmpty()) {
            return ROOT;
        }

        ConfigPath result = ROOT;
        int i = 0;
        int len = path.length();

        while (i < len) {
            // Skip leading dot
            if (path.charAt(i) == '.') {
                i++;
                continue;
            }

            // Index or key: [...]
            if (path.charAt(i) == '[') {
                int closeIndex = path.indexOf(']', i);
                if (closeIndex == -1) {
                    throw new IllegalArgumentException("Unclosed bracket in path: " + path);
                }

                String content = path.substring(i + 1, closeIndex);

                // Quoted string key: ["key"] or ['key']
                if ((content.startsWith("\"") && content.endsWith("\"")) ||
                    (content.startsWith("'") && content.endsWith("'"))) {
                    String key = content.substring(1, content.length() - 1);
                    result = result.key(key);
                } else {
                    // Numeric index
                    try {
                        int index = Integer.parseInt(content);
                        result = result.index(index);
                    } catch (NumberFormatException e) {
                        // Treat as unquoted key
                        result = result.key(content);
                    }
                }
                i = closeIndex + 1;
            } else {
                // Property name: read until . or [ or end
                int end = i;
                while (end < len && path.charAt(end) != '.' && path.charAt(end) != '[') {
                    end++;
                }
                String propName = path.substring(i, end);
                if (!propName.isEmpty()) {
                    result = result.property(propName);
                }
                i = end;
            }
        }

        return result;
    }

    /**
     * Appends a property name to this path.
     *
     * @param name the property name
     * @return new path with property appended
     */
    public ConfigPath property(@NonNull String name) {
        List<PathNode> newNodes = new ArrayList<>(this.nodes);
        newNodes.add(new PropertyNode(name));
        return new ConfigPath(newNodes);
    }

    /**
     * Appends a list/array index to this path.
     *
     * @param index the index (0-based)
     * @return new path with index appended
     */
    public ConfigPath index(int index) {
        List<PathNode> newNodes = new ArrayList<>(this.nodes);
        newNodes.add(new IndexNode(index));
        return new ConfigPath(newNodes);
    }

    /**
     * Appends a map key to this path.
     *
     * @param key the map key
     * @return new path with key appended
     */
    public ConfigPath key(@NonNull Object key) {
        List<PathNode> newNodes = new ArrayList<>(this.nodes);
        newNodes.add(new KeyNode(key));
        return new ConfigPath(newNodes);
    }

    /**
     * Returns the number of nodes in this path.
     *
     * @return node count
     */
    public int size() {
        return this.nodes.size();
    }

    /**
     * Checks if this path is empty (root).
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    /**
     * Returns an unmodifiable view of the path nodes.
     *
     * @return list of nodes
     */
    public List<PathNode> getNodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    /**
     * Returns the last node in this path, or null if empty.
     *
     * @return last node or null
     */
    public PathNode getLastNode() {
        return this.nodes.isEmpty() ? null : this.nodes.get(this.nodes.size() - 1);
    }

    /**
     * Returns the parent path (path without the last node).
     *
     * @return parent path, or ROOT if this is already root or single-node
     */
    public ConfigPath parent() {
        if (this.nodes.size() <= 1) {
            return ROOT;
        }
        return new ConfigPath(new ArrayList<>(this.nodes.subList(0, this.nodes.size() - 1)));
    }

    @Override
    public String toString() {
        if (this.nodes.isEmpty()) {
            return "<root>";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.nodes.size(); i++) {
            PathNode node = this.nodes.get(i);

            if (node instanceof PropertyNode) {
                if (i > 0) {
                    sb.append(".");
                }
                sb.append(((PropertyNode) node).getName());
            } else if (node instanceof IndexNode) {
                sb.append("[").append(((IndexNode) node).getIndex()).append("]");
            } else if (node instanceof KeyNode) {
                Object key = ((KeyNode) node).getKey();
                sb.append("[");
                if (key instanceof String) {
                    sb.append("\"").append(escapeString((String) key)).append("\"");
                } else {
                    sb.append(key);
                }
                sb.append("]");
            }
        }
        return sb.toString();
    }

    private static String escapeString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ==================== Path Node Types ====================

    /**
     * Base interface for path nodes.
     */
    public interface PathNode {
    }

    /**
     * Represents a property/field name in the path.
     */
    @AllArgsConstructor
    public static class PropertyNode implements PathNode {
        private final String name;

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // PropertyNode("x") equals KeyNode("x")
            if (o instanceof KeyNode) {
                Object key = ((KeyNode) o).getKey();
                return (key instanceof String) && this.name.equals(key);
            }
            if (!(o instanceof PropertyNode)) return false;
            return this.name.equals(((PropertyNode) o).name);
        }

        @Override
        public int hashCode() {
            // Hash based on name only - matches KeyNode(String) hashCode
            return this.name.hashCode();
        }
    }

    /**
     * Represents a list/array index in the path.
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class IndexNode implements PathNode {
        private final int index;

        public int getIndex() {
            return this.index;
        }

        @Override
        public String toString() {
            return "[" + this.index + "]";
        }
    }

    /**
     * Represents a map key in the path.
     */
    @AllArgsConstructor
    public static class KeyNode implements PathNode {
        private final Object key;

        public Object getKey() {
            return this.key;
        }

        @Override
        public String toString() {
            if (this.key instanceof String) {
                return "[\"" + this.key + "\"]";
            }
            return "[" + this.key + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // KeyNode("x") equals PropertyNode("x")
            if ((this.key instanceof String) && (o instanceof PropertyNode)) {
                return this.key.equals(((PropertyNode) o).getName());
            }
            if (!(o instanceof KeyNode)) return false;
            return this.key.equals(((KeyNode) o).key);
        }

        @Override
        public int hashCode() {
            // For String keys, hash the string directly - matches PropertyNode hashCode
            return this.key.hashCode();
        }
    }
}
