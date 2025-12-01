package eu.okaeri.configs.serdes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
    }

    /**
     * Represents a list/array index in the path.
     */
    @AllArgsConstructor
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
    }
}
