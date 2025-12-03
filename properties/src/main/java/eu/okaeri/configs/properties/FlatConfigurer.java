package eu.okaeri.configs.properties;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Abstract base class for flat key-value configuration formats.
 * Provides common flatten/unflatten logic for formats like Properties and INI.
 * <p>
 * Subclasses implement format-specific parsing and serialization:
 * <ul>
 *   <li>Properties: dot notation (database.host=localhost)</li>
 *   <li>INI: section notation ([database] + host=localhost)</li>
 * </ul>
 */
@Accessors(chain = true)
public abstract class FlatConfigurer extends Configurer {

    protected static final String NULL_MARKER = "__null__";
    protected static final int DEFAULT_SIMPLE_LIST_MAX_LINE_LENGTH = 80;

    protected @Setter String commentPrefix = "# ";
    protected @Setter int simpleListMaxLineLength = DEFAULT_SIMPLE_LIST_MAX_LINE_LENGTH;

    protected FlatConfigurer() {
    }

    @Override
    public boolean isCommentLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith(";") || trimmed.startsWith("#");
    }

    // ==================== Header Writing ====================

    protected void writeHeader(StringBuilder sb, ConfigDeclaration declaration) {
        String[] header = declaration.getHeader();
        if ((header != null) && (header.length > 0)) {
            for (String line : header) {
                sb.append(this.commentPrefix).append(line).append("\n");
            }
            sb.append("\n");
        }
    }

    protected void writeOutput(OutputStream outputStream, StringBuilder sb) throws Exception {
        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ==================== Flattening (Object → Flat Map) ====================

    protected Map<String, String> flatten(Map<?, ?> map) {
        Map<String, String> result = new LinkedHashMap<>();
        this.flattenMap("", map, result);
        return result;
    }

    protected void flattenMap(String prefix, Map<?, ?> map, Map<String, String> result) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : (prefix + "." + entry.getKey());
            this.flattenValue(key, entry.getValue(), result);
        }
    }

    protected void flattenValue(String key, Object value, Map<String, String> result) {
        if (value == null) {
            result.put(key, NULL_MARKER);
        } else if (value instanceof Map) {
            this.flattenMapOrList(key, (Map<?, ?>) value, result);
        } else if (value instanceof List) {
            this.flattenList(key, (List<?>) value, result);
        } else {
            result.put(key, String.valueOf(value));
        }
    }

    protected void flattenMapOrList(String key, Map<?, ?> map, Map<String, String> result) {
        if (map.isEmpty()) {
            result.put(key, "");
            return;
        }
        // Maps with sequential integer keys represent lists
        List<Object> asList = this.tryConvertToList(map);
        if (asList != null) {
            this.flattenList(key, asList, result);
        } else {
            this.flattenMap(key, map, result);
        }
    }

    protected void flattenList(String key, List<?> list, Map<String, String> result) {
        if (list.isEmpty()) {
            result.put(key, "");
            return;
        }
        // Try compact comma format first
        String commaFormat = this.tryFormatAsCommaList(key, list);
        if (commaFormat != null) {
            result.put(key, commaFormat);
            return;
        }
        // Fall back to index notation
        for (int i = 0; i < list.size(); i++) {
            this.flattenValue(key + "." + i, list.get(i), result);
        }
    }

    /**
     * Tries to format a list as comma-separated values.
     * Returns null if not suitable (contains commas/newlines, exceeds line length, or has complex values).
     */
    protected String tryFormatAsCommaList(String key, List<?> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if ((item instanceof Map) || (item instanceof List)) {
                return null;
            }
            String value = (item == null) ? NULL_MARKER : String.valueOf(item);
            if (containsDelimiter(value)) {
                return null;
            }
            if (i > 0) {
                sb.append(",");
            }
            sb.append(value);
        }
        // Check line length: key=value
        if ((key.length() + 1 + sb.length()) > this.simpleListMaxLineLength) {
            return null;
        }
        return sb.toString();
    }

    /**
     * Converts a map with sequential integer keys (0, 1, 2, ...) to a list.
     * Returns null if the map doesn't represent a list.
     */
    protected List<Object> tryConvertToList(Map<?, ?> map) {
        if (map.isEmpty()) {
            return null;
        }
        // Parse and validate indices
        TreeMap<Integer, Object> indexed = new TreeMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Integer index = parseNonNegativeInt(String.valueOf(entry.getKey()));
            if (index == null) {
                return null;
            }
            indexed.put(index, entry.getValue());
        }
        // Check sequential from 0
        int expected = 0;
        for (int index : indexed.keySet()) {
            if (index != expected++) {
                return null;
            }
        }
        return new ArrayList<>(indexed.values());
    }

    // ==================== Unflattening (Flat Map → Object) ====================

    protected Map<String, Object> unflatten(Map<String, String> flat, ConfigDeclaration declaration) {
        // Build nested structure from dot notation
        Map<String, Object> root = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : flat.entrySet()) {
            this.setNestedValue(root, entry.getKey().split("\\."), entry.getValue());
        }
        // Convert map structures to lists where appropriate
        return this.convertStructures(root, declaration);
    }

    @SuppressWarnings("unchecked")
    protected void setNestedValue(Map<String, Object> root, String[] path, String value) {
        Map<String, Object> current = root;
        for (int i = 0; i < (path.length - 1); i++) {
            Object existing = current.get(path[i]);
            if (existing instanceof Map) {
                current = (Map<String, Object>) existing;
            } else {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(path[i], newMap);
                current = newMap;
            }
        }
        String leafKey = path[path.length - 1];
        current.put(leafKey, NULL_MARKER.equals(value) ? null : value);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> convertStructures(Map<String, Object> map, ConfigDeclaration declaration) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            GenericsDeclaration fieldType = this.getFieldType(declaration, key);

            result.put(key, this.convertValue(value, fieldType));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected Object convertValue(Object value, GenericsDeclaration type) {
        if (value instanceof String) {
            return this.convertStringValue((String) value, type);
        }
        if (value instanceof Map) {
            return this.convertMapValue((Map<String, Object>) value, type);
        }
        return value;
    }

    protected Object convertStringValue(String value, GenericsDeclaration type) {
        // Empty string → empty collection or map
        if (value.isEmpty()) {
            if (isCollectionType(type)) {
                return new ArrayList<>();
            }
            if (isMapType(type)) {
                return new LinkedHashMap<>();
            }
        }
        // Comma-separated list for collection types
        if (isCollectionType(type)) {
            return this.parseCommaList(value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    protected Object convertMapValue(Map<String, Object> map, GenericsDeclaration type) {
        boolean shouldBeList = isCollectionType(type);
        boolean looksLikeList = this.looksLikeList(map);

        if (shouldBeList || looksLikeList) {
            GenericsDeclaration elementType = (type != null) ? type.getSubtypeAtOrNull(0) : null;
            return this.convertMapToList(map, elementType);
        }
        // Regular nested map/subconfig
        ConfigDeclaration nestedDecl = ((type != null) && type.isConfig())
            ? ConfigDeclaration.of(type.getType())
            : null;
        return this.convertStructures(map, nestedDecl);
    }

    protected List<Object> convertMapToList(Map<String, Object> map, GenericsDeclaration elementType) {
        // Sort by numeric key
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        sortedKeys.sort(Comparator.comparingInt(Integer::parseInt));

        GenericsDeclaration nestedType = (elementType != null) ? elementType.getSubtypeAtOrNull(0) : null;
        ConfigDeclaration elementDecl = ((elementType != null) && elementType.isConfig())
            ? ConfigDeclaration.of(elementType.getType())
            : null;

        List<Object> list = new ArrayList<>();
        for (String key : sortedKeys) {
            Object value = map.get(key);
            list.add(this.convertListElement(value, elementType, nestedType, elementDecl));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    protected Object convertListElement(Object value, GenericsDeclaration elementType,
                                        GenericsDeclaration nestedType, ConfigDeclaration elementDecl) {
        if (value instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) value;
            if (this.looksLikeList(nested)) {
                return this.convertMapToList(nested, nestedType);
            }
            return this.convertStructures(nested, elementDecl);
        }
        if ((value instanceof String) && isCollectionType(elementType)) {
            String strValue = (String) value;
            return strValue.isEmpty() ? new ArrayList<>() : this.parseCommaList(strValue);
        }
        return value;
    }

    /**
     * Parses a comma-separated string into a list, handling __null__ markers.
     */
    protected List<Object> parseCommaList(String value) {
        if (value.indexOf(',') < 0) {
            // Single element
            return new ArrayList<>(Collections.singletonList(NULL_MARKER.equals(value) ? null : value));
        }
        String[] parts = value.split(",", -1);
        List<Object> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            list.add(NULL_MARKER.equals(part) ? null : part);
        }
        return list;
    }

    protected boolean looksLikeList(Map<String, Object> map) {
        if (map.isEmpty()) {
            return false;
        }
        int expected = 0;
        TreeSet<Integer> indices = new TreeSet<>();
        for (String key : map.keySet()) {
            Integer index = parseNonNegativeInt(key);
            if (index == null) {
                return false;
            }
            indices.add(index);
        }
        for (int index : indices) {
            if (index != expected++) {
                return false;
            }
        }
        return true;
    }

    protected GenericsDeclaration getFieldType(ConfigDeclaration declaration, String key) {
        if (declaration == null) {
            return null;
        }
        Optional<FieldDeclaration> field = declaration.getField(key);
        return field.map(FieldDeclaration::getType).orElse(null);
    }

    // ==================== Comment Writing ====================

    /**
     * Writes comments for a dotted key path, handling list/map element types.
     * Uses pattern-based deduplication (list.*.field) so comments appear only on first occurrence.
     */
    protected void writeFieldComments(StringBuilder sb, String key, ConfigDeclaration declaration, Set<String> written) {
        ConfigPath path = ConfigPath.parseFlat(key, declaration);
        List<ConfigPath.PathNode> nodes = path.getNodes();

        for (int i = 0; i < nodes.size(); i++) {
            // Skip indices/keys - they don't have field comments
            if (!(nodes.get(i) instanceof ConfigPath.PropertyNode)) {
                continue;
            }

            ConfigPath partialPath = path.subPath(i);
            String pattern = partialPath.toPattern();
            if (written.contains(pattern)) {
                continue;
            }

            Optional<FieldDeclaration> field = partialPath.resolveFieldDeclaration(declaration);
            if (field.isPresent()) {
                String[] comment = field.get().getComment();
                if (comment != null) {
                    String prefixTrimmed = this.commentPrefix.trim();
                    for (String line : comment) {
                        if (line.isEmpty()) {
                            // @Comment("") -> empty line (no # at all)
                            sb.append("\n");
                        } else if (line.trim().isEmpty()) {
                            // @Comment(" ") -> "#" (just the hash)
                            sb.append(prefixTrimmed).append("\n");
                        } else {
                            // Normal comment text
                            sb.append(this.commentPrefix).append(line).append("\n");
                        }
                    }
                }
                written.add(pattern);
            }
        }
    }

    // ==================== Utilities ====================

    protected static boolean isNumeric(String str) {
        return parseNonNegativeInt(str) != null;
    }

    protected static Integer parseNonNegativeInt(String str) {
        try {
            int value = Integer.parseInt(str);
            return (value >= 0) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static boolean containsDelimiter(String value) {
        return (value.indexOf(',') >= 0) || (value.indexOf('\n') >= 0) || (value.indexOf('\r') >= 0);
    }

    protected static boolean isCollectionType(GenericsDeclaration type) {
        return (type != null) && Collection.class.isAssignableFrom(type.getType());
    }

    protected static boolean isMapType(GenericsDeclaration type) {
        return (type != null) && Map.class.isAssignableFrom(type.getType());
    }
}
