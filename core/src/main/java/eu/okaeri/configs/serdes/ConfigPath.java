package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.*;

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
     * Parses a flat dot-notation path into a ConfigPath, using schema to distinguish
     * between indices, map keys, and property names.
     * <p>
     * Examples (with appropriate schema):
     * <ul>
     *   <li>{@code "database.host"} → property("database").property("host")</li>
     *   <li>{@code "servers.0.name"} → property("servers").index(0).property("name")</li>
     *   <li>{@code "settings.main.timeout"} → property("settings").key("main").property("timeout")</li>
     * </ul>
     *
     * @param path            the flat dot-notation path to parse
     * @param rootDeclaration the root config declaration for schema-aware parsing
     * @return parsed ConfigPath
     */
    public static ConfigPath parseFlat(@NonNull String path, @NonNull ConfigDeclaration rootDeclaration) {
        if (path.isEmpty()) {
            return ROOT;
        }

        String[] parts = path.split("\\.");
        ConfigPath result = ROOT;
        ConfigDeclaration currentDecl = rootDeclaration;
        GenericsDeclaration pendingElementType = null;
        boolean pendingMapKey = false;

        for (String part : parts) {
            // Handle numeric indices (list elements) or map keys
            if (pendingMapKey || (pendingElementType != null)) {
                Integer index = parseIndex(part);
                if (index != null) {
                    result = result.index(index);
                } else {
                    result = result.key(part);
                }
                // Advance to element's config declaration if it's a config type
                if (pendingElementType.isConfig()) {
                    currentDecl = ConfigDeclaration.of(pendingElementType.getType());
                }
                pendingElementType = null;
                pendingMapKey = false;
                continue;
            }

            // Regular property
            result = result.property(part);

            if (currentDecl == null) {
                continue;
            }

            Optional<FieldDeclaration> field = currentDecl.getField(part);
            if (!field.isPresent()) {
                currentDecl = null;
                continue;
            }

            GenericsDeclaration fieldType = field.get().getType();
            if (fieldType.isConfig()) {
                currentDecl = ConfigDeclaration.of(fieldType.getType());
            } else if (isCollectionLike(fieldType)) {
                pendingElementType = fieldType.getSubtypeAtOrNull(0);
                currentDecl = null;
            } else if (isMapLike(fieldType)) {
                GenericsDeclaration valueType = fieldType.getSubtypeAtOrNull(1);
                if ((valueType != null) && valueType.isConfig()) {
                    pendingElementType = valueType;
                    pendingMapKey = true;
                }
                currentDecl = null;
            } else {
                currentDecl = null;
            }
        }

        return result;
    }

    private static Integer parseIndex(String s) {
        try {
            int value = Integer.parseInt(s);
            return (value >= 0) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isCollectionLike(GenericsDeclaration type) {
        return (type != null) && Collection.class.isAssignableFrom(type.getType());
    }

    private static boolean isMapLike(GenericsDeclaration type) {
        return (type != null) && Map.class.isAssignableFrom(type.getType());
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
                while ((end < len) && (path.charAt(end) != '.') && (path.charAt(end) != '[')) {
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

    /**
     * Returns a sub-path from root up to and including the node at endIndex.
     * <p>
     * Example with path {@code servers[0].name} (3 nodes):
     * <ul>
     *   <li>{@code subPath(0)} → "servers"</li>
     *   <li>{@code subPath(1)} → "servers[0]"</li>
     *   <li>{@code subPath(2)} → "servers[0].name"</li>
     * </ul>
     * Example with path {@code settings["main"].timeout} (3 nodes):
     * <ul>
     *   <li>{@code subPath(0)} → "settings"</li>
     *   <li>{@code subPath(1)} → "settings["main"]"</li>
     *   <li>{@code subPath(2)} → "settings["main"].timeout"</li>
     * </ul>
     *
     * @param endIndex the last node index to include (0-based, inclusive)
     * @return sub-path containing nodes [0, endIndex]
     * @throws IndexOutOfBoundsException if endIndex is out of range
     */
    public ConfigPath subPath(int endIndex) {
        if ((endIndex < 0) || (endIndex >= this.nodes.size())) {
            throw new IndexOutOfBoundsException("endIndex: " + endIndex + ", size: " + this.nodes.size());
        }
        return new ConfigPath(new ArrayList<>(this.nodes.subList(0, endIndex + 1)));
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

    // ==================== Declaration Resolution ====================

    /**
     * Resolves this path to a FieldDeclaration, walking through nested configs,
     * list elements, and map values as needed.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code database.host} - returns FieldDeclaration for 'host' in Database config</li>
     *   <li>{@code servers[0].name} - returns FieldDeclaration for 'name' in list element config</li>
     *   <li>{@code settings["main"].timeout} - returns FieldDeclaration for 'timeout' in map value config</li>
     * </ul>
     *
     * @param rootDeclaration the root config declaration to start resolution from
     * @return the field declaration if path resolves successfully, empty otherwise
     */
    public Optional<FieldDeclaration> resolveFieldDeclaration(@NonNull ConfigDeclaration rootDeclaration) {
        ConfigDeclaration currentDecl = rootDeclaration;
        FieldDeclaration lastField = null;

        for (PathNode node : this.nodes) {
            if (node instanceof PropertyNode) {
                String name = ((PropertyNode) node).getName();

                // If currentDecl is null but lastField was a Map, treat this as map key access
                if ((currentDecl == null) && (lastField != null) && isMapLike(lastField.getType())) {
                    GenericsDeclaration valueType = lastField.getType().getSubtypeAtOrNull(1);
                    currentDecl = ((valueType != null) && valueType.isConfig())
                        ? ConfigDeclaration.of(valueType.getType())
                        : null;
                    lastField = null;
                    continue;
                }

                if (currentDecl == null) {
                    return Optional.empty();
                }

                Optional<FieldDeclaration> field = currentDecl.getField(name);
                if (!field.isPresent()) {
                    return Optional.empty();
                }
                lastField = field.get();
                currentDecl = resolveNextDeclaration(lastField.getType());
            } else if (node instanceof IndexNode) {
                // Inside a list - lastField should be List<T>, resolve T's declaration
                if (lastField == null) {
                    return Optional.empty();
                }
                GenericsDeclaration elementType = lastField.getType().getSubtypeAtOrNull(0);
                currentDecl = ((elementType != null) && elementType.isConfig())
                    ? ConfigDeclaration.of(elementType.getType())
                    : null;
                lastField = null; // Reset - we're now inside the element
            } else if (node instanceof KeyNode) {
                // Inside a map - lastField should be Map<K,V>, resolve V's declaration
                if (lastField == null) {
                    return Optional.empty();
                }
                GenericsDeclaration valueType = lastField.getType().getSubtypeAtOrNull(1);
                currentDecl = ((valueType != null) && valueType.isConfig())
                    ? ConfigDeclaration.of(valueType.getType())
                    : null;
                lastField = null; // Reset - we're now inside the value
            }
        }

        return Optional.ofNullable(lastField);
    }

    private static ConfigDeclaration resolveNextDeclaration(GenericsDeclaration type) {
        if (type == null) {
            return null;
        }
        if (type.isConfig()) {
            return ConfigDeclaration.of(type.getType());
        }
        // For List/Map, we need index/key node to resolve further
        return null;
    }

    /**
     * Converts this path to a pattern string for deduplication purposes.
     * Indices and keys are replaced with wildcards (*).
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code servers[0].name} → "servers.*.name"</li>
     *   <li>{@code settings["main"].timeout} → "settings.*.timeout"</li>
     *   <li>{@code data[0]["key"].value} → "data.*.*.value"</li>
     * </ul>
     *
     * @return pattern string with wildcards for indices/keys
     */
    public String toPattern() {
        StringBuilder sb = new StringBuilder();
        for (PathNode node : this.nodes) {
            if (sb.length() > 0) {
                sb.append(".");
            }
            if (node instanceof PropertyNode) {
                sb.append(((PropertyNode) node).getName());
            } else if ((node instanceof IndexNode) || (node instanceof KeyNode)) {
                sb.append("*");
            }
        }
        return sb.toString();
    }

    /**
     * Converts this path to a pattern string using declaration for context.
     * Detects PropertyNodes that are actually map keys (when following a Map field).
     * <p>
     * This is useful when paths are built without schema awareness (e.g., YAML parsing)
     * and PropertyNodes may represent map keys rather than field names.
     * <p>
     * Difference from {@link #toPattern()}:
     * <ul>
     *   <li>{@code toPattern()} treats all PropertyNodes as field names</li>
     *   <li>{@code toPattern(declaration)} uses schema to detect map keys</li>
     * </ul>
     * <p>
     * Example with {@code Map<String, SubConfig> settings} field:
     * <pre>
     * // Path built from YAML: settings.main.timeout (all PropertyNodes)
     * path.toPattern()            → "settings.main.timeout"
     * path.toPattern(declaration) → "settings.*.timeout"
     * </pre>
     * The declaration-aware version recognizes "main" as a map key (not a field),
     * converting it to wildcard for proper deduplication across map entries.
     *
     * @param declaration the root config declaration for context
     * @return pattern string with wildcards for indices/keys/map-key-properties
     */
    public String toPattern(@NonNull ConfigDeclaration declaration) {
        StringBuilder sb = new StringBuilder();
        ConfigDeclaration currentDecl = declaration;
        FieldDeclaration lastField = null;

        for (PathNode node : this.nodes) {
            if (node instanceof PropertyNode) {
                String name = ((PropertyNode) node).getName();

                // Check if this PropertyNode is actually a map key
                if ((currentDecl == null) && (lastField != null) && isMapLike(lastField.getType())) {
                    sb.append((sb.length() > 0) ? "." : "").append("*");
                    GenericsDeclaration valueType = lastField.getType().getSubtypeAtOrNull(1);
                    currentDecl = ((valueType != null) && valueType.isConfig())
                        ? ConfigDeclaration.of(valueType.getType())
                        : null;
                    lastField = null;
                    continue;
                }

                sb.append((sb.length() > 0) ? "." : "").append(name);

                // Update declaration context
                if (currentDecl != null) {
                    Optional<FieldDeclaration> field = currentDecl.getField(name);
                    if (field.isPresent()) {
                        lastField = field.get();
                        GenericsDeclaration fieldType = lastField.getType();
                        currentDecl = fieldType.isConfig() ? ConfigDeclaration.of(fieldType.getType()) : null;
                    } else {
                        currentDecl = null;
                        lastField = null;
                    }
                }
            } else if (node instanceof IndexNode) {
                sb.append((sb.length() > 0) ? "." : "").append("*");
                if (lastField != null) {
                    GenericsDeclaration elementType = lastField.getType().getSubtypeAtOrNull(0);
                    currentDecl = ((elementType != null) && elementType.isConfig())
                        ? ConfigDeclaration.of(elementType.getType())
                        : null;
                    lastField = null;
                }
            } else if (node instanceof KeyNode) {
                sb.append((sb.length() > 0) ? "." : "").append("*");
                if (lastField != null) {
                    GenericsDeclaration valueType = lastField.getType().getSubtypeAtOrNull(1);
                    currentDecl = ((valueType != null) && valueType.isConfig())
                        ? ConfigDeclaration.of(valueType.getType())
                        : null;
                    lastField = null;
                }
            }
        }
        return sb.toString();
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
            return (o instanceof PropertyNode) && this.name.equals(((PropertyNode) o).name);
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
            return (o instanceof KeyNode) && this.key.equals(((KeyNode) o).key);
        }

        @Override
        public int hashCode() {
            // For String keys, hash the string directly - matches PropertyNode hashCode
            return this.key.hashCode();
        }
    }
}
