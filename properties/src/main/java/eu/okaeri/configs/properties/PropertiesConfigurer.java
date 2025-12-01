package eu.okaeri.configs.properties;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Properties configurer using Java's built-in Properties format with dot notation for nesting.
 * <p>
 * Features:
 * <ul>
 *   <li>Nested structures via dot notation: {@code database.host=localhost}</li>
 *   <li>Lists via index notation: {@code items.0=first}, {@code items.1=second}</li>
 *   <li>Write-only comments using {@code #} prefix</li>
 *   <li>Zero external dependencies (uses java.util.Properties)</li>
 * </ul>
 * <p>
 * Example output:
 * <pre>
 * # Application settings
 * name=MyApp
 * database.host=localhost
 * database.port=5432
 * features.0=logging
 * features.1=metrics
 * </pre>
 */
@Accessors(chain = true)
public class PropertiesConfigurer extends Configurer {

    private static final String NULL_MARKER = "__null__";
    private static final int DEFAULT_SIMPLE_LIST_MAX_LINE_LENGTH = 80;

    private Map<String, Object> map = new LinkedHashMap<>();

    private @Setter String commentPrefix = "# ";
    private @Setter int simpleListMaxLineLength = DEFAULT_SIMPLE_LIST_MAX_LINE_LENGTH;

    public PropertiesConfigurer() {
    }

    public PropertiesConfigurer(@NonNull Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("properties");
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), false);
        this.map.put(key, simplified);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.map.put(key, value);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {
        return this.map.remove(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.map.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        // Use Java's Properties to handle parsing
        Properties props = new Properties();
        props.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Convert to sorted map (Properties doesn't maintain order)
        Map<String, String> flat = new LinkedHashMap<>();
        props.stringPropertyNames().stream()
            .sorted()
            .forEach(key -> flat.put(key, props.getProperty(key)));

        // Unflatten with declaration for empty collection detection
        this.map = this.unflatten(flat, declaration);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        StringBuilder sb = new StringBuilder();

        // Write header comments
        String[] header = declaration.getHeader();
        if (header != null) {
            for (String line : header) {
                sb.append(this.commentPrefix).append(line).append("\n");
            }
            if (header.length > 0) {
                sb.append("\n");
            }
        }

        // Flatten map and write with comments
        Map<String, String> flat = new LinkedHashMap<>();
        this.flatten("", this.map, flat);

        // Write properties with field comments
        Set<String> writtenComments = new HashSet<>();
        for (Map.Entry<String, String> entry : flat.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Write field comments for any level in the hierarchy
            this.writeComments(sb, key, declaration, writtenComments);

            sb.append(this.escapeKey(key)).append("=").append(this.escapeValue(value)).append("\n");
        }

        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writeComments(StringBuilder sb, String key, ConfigDeclaration declaration, Set<String> writtenComments) {
        String[] parts = key.split("\\.");
        ConfigDeclaration currentDecl = declaration;
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // Skip numeric indices (list elements)
            if (this.isNumeric(part)) {
                if (pathBuilder.length() > 0) {
                    pathBuilder.append(".");
                }
                pathBuilder.append(part);
                continue;
            }

            if (pathBuilder.length() > 0) {
                pathBuilder.append(".");
            }
            pathBuilder.append(part);
            String currentPath = pathBuilder.toString();

            // Only write comment once per path, and only on first occurrence
            if (writtenComments.contains(currentPath)) {
                // Navigate to nested declaration for next iteration
                if (currentDecl != null) {
                    Optional<FieldDeclaration> field = currentDecl.getField(part);
                    if (field.isPresent() && field.get().getType().isConfig()) {
                        currentDecl = ConfigDeclaration.of(field.get().getType().getType());
                    } else {
                        currentDecl = null;
                    }
                }
                continue;
            }

            if (currentDecl != null) {
                Optional<FieldDeclaration> field = currentDecl.getField(part);
                if (field.isPresent()) {
                    String[] comment = field.get().getComment();
                    if (comment != null) {
                        for (String line : comment) {
                            sb.append(this.commentPrefix).append(line).append("\n");
                        }
                    }
                    writtenComments.add(currentPath);

                    // Navigate to nested declaration
                    if (field.get().getType().isConfig()) {
                        currentDecl = ConfigDeclaration.of(field.get().getType().getType());
                    } else {
                        currentDecl = null;
                    }
                } else {
                    currentDecl = null;
                }
            }
        }
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void flatten(String prefix, Map<?, ?> map, Map<String, String> result) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix.isEmpty()
                ? String.valueOf(entry.getKey())
                : (prefix + "." + entry.getKey());
            Object value = entry.getValue();

            if (value == null) {
                result.put(key, NULL_MARKER);
                continue;
            } else if (value instanceof Map) {
                Map<?, ?> nested = (Map<?, ?>) value;
                if (nested.isEmpty()) {
                    // Empty value signals empty collection (type determined by ConfigDeclaration on load)
                    result.put(key, "");
                    continue;
                }
                // Check if this map represents a list (has sequential integer keys)
                // Lists are often simplified to Maps with numeric keys
                if (this.isListLikeMap(nested)) {
                    String simpleList = this.tryFlattenListLikeMap(key, nested);
                    if (simpleList != null) {
                        result.put(key, simpleList);
                        continue;
                    }
                }
                this.flatten(key, nested, result);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (list.isEmpty()) {
                    // Empty value signals empty collection (type determined by ConfigDeclaration on load)
                    result.put(key, "");
                    continue;
                }
                // Try simple comma-separated format for lists of simple values
                String simpleList = this.tryFlattenSimpleList(key, list);
                if (simpleList != null) {
                    result.put(key, simpleList);
                    continue;
                }
                // Fall back to index notation for complex lists
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item == null) {
                        result.put(key + "." + i, NULL_MARKER);
                    } else if (item instanceof Map) {
                        this.flatten(key + "." + i, (Map<?, ?>) item, result);
                    } else if (item instanceof List) {
                        // Handle nested lists (e.g., List<List<String>>)
                        this.flattenList(key + "." + i, (List<?>) item, result);
                    } else {
                        result.put(key + "." + i, String.valueOf(item));
                    }
                }
            } else {
                result.put(key, String.valueOf(value));
            }
        }
    }

    private void flattenList(String prefix, List<?> list, Map<String, String> result) {
        if (list.isEmpty()) {
            result.put(prefix, "");
            return;
        }
        // Try simple comma-separated format first
        String simpleList = this.tryFlattenSimpleList(prefix, list);
        if (simpleList != null) {
            result.put(prefix, simpleList);
            return;
        }
        // Fall back to index notation
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            String key = prefix + "." + i;
            if (item == null) {
                result.put(key, NULL_MARKER);
            } else if (item instanceof Map) {
                this.flatten(key, (Map<?, ?>) item, result);
            } else if (item instanceof List) {
                this.flattenList(key, (List<?>) item, result);
            } else {
                result.put(key, String.valueOf(item));
            }
        }
    }

    /**
     * Tries to flatten a list as comma-separated values.
     * Returns null if the list is not suitable for simple format.
     */
    private String tryFlattenSimpleList(String key, List<?> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            // Not simple: Map, List (nested structures)
            if ((item instanceof Map) || (item instanceof List)) {
                return null;
            }
            // Null uses __null__ marker
            String value = (item == null) ? NULL_MARKER : String.valueOf(item);
            // Not simple: contains comma or newline
            if ((value.indexOf(',') >= 0) || (value.indexOf('\n') >= 0) || (value.indexOf('\r') >= 0)) {
                return null;
            }
            if (i > 0) {
                sb.append(",");
            }
            sb.append(value);
        }
        // Check total line length: key + "=" + value
        int lineLength = key.length() + 1 + sb.length();
        if (lineLength > this.simpleListMaxLineLength) {
            return null;
        }
        return sb.toString();
    }

    /**
     * Checks if a map has sequential integer keys starting from 0 (i.e., represents a list).
     * Works with Map<?, ?> where keys might be String or Integer.
     */
    private boolean isListLikeMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return false;
        }

        Set<Integer> indices = new TreeSet<>();
        for (Object key : map.keySet()) {
            try {
                int index = Integer.parseInt(String.valueOf(key));
                if (index < 0) {
                    return false;
                }
                indices.add(index);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Check for sequential indices starting from 0
        int expected = 0;
        for (int index : indices) {
            if (index != expected) {
                return false;
            }
            expected++;
        }

        return true;
    }

    /**
     * Tries to flatten a list-like map (with sequential integer keys) as comma-separated values.
     * Returns null if the map is not suitable for simple format.
     */
    private String tryFlattenListLikeMap(String key, Map<?, ?> map) {
        // Sort entries by key numerically (keys might be String or Integer)
        List<Map.Entry<?, ?>> sortedEntries = new ArrayList<>(map.entrySet());
        sortedEntries.sort(Comparator.comparingInt(e -> Integer.parseInt(String.valueOf(e.getKey()))));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sortedEntries.size(); i++) {
            Object item = sortedEntries.get(i).getValue();
            // Not simple: Map, List (nested structures)
            if ((item instanceof Map) || (item instanceof List)) {
                return null;
            }
            // Null uses __null__ marker
            String value = (item == null) ? NULL_MARKER : String.valueOf(item);
            // Not simple: contains comma or newline
            if ((value.indexOf(',') >= 0) || (value.indexOf('\n') >= 0) || (value.indexOf('\r') >= 0)) {
                return null;
            }
            if (i > 0) {
                sb.append(",");
            }
            sb.append(value);
        }
        // Check total line length: key + "=" + value
        int lineLength = key.length() + 1 + sb.length();
        if (lineLength > this.simpleListMaxLineLength) {
            return null;
        }
        return sb.toString();
    }

    private String escapeKey(String key) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c == '=') || (c == ':') || (c == ' ')) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String escapeValue(String value) {
        StringBuilder sb = new StringBuilder();
        boolean leadingWhitespace = true;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
                leadingWhitespace = false;
            } else if (c == '\n') {
                sb.append("\\n");
                leadingWhitespace = false;
            } else if (c == '\r') {
                sb.append("\\r");
                leadingWhitespace = false;
            } else if (c == '\t') {
                sb.append("\\t");
                leadingWhitespace = false;
            } else if ((c == ' ') && leadingWhitespace) {
                // Escape leading spaces to preserve them
                sb.append("\\ ");
            } else if (c > 0x7F) {
                // Escape non-ASCII as unicode
                sb.append(String.format("\\u%04X", (int) c));
                leadingWhitespace = false;
            } else {
                sb.append(c);
                if (c != ' ') {
                    leadingWhitespace = false;
                }
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unflatten(Map<String, String> flat, ConfigDeclaration declaration) {
        Map<String, Object> root = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : flat.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String[] parts = key.split("\\.");
            Map<String, Object> current = root;

            for (int i = 0; i < (parts.length - 1); i++) {
                String part = parts[i];
                Object existing = current.get(part);

                if (existing == null) {
                    Map<String, Object> newMap = new LinkedHashMap<>();
                    current.put(part, newMap);
                    current = newMap;
                } else if (existing instanceof Map) {
                    current = (Map<String, Object>) existing;
                } else {
                    Map<String, Object> newMap = new LinkedHashMap<>();
                    current.put(part, newMap);
                    current = newMap;
                }
            }

            String leafKey = parts[parts.length - 1];
            current.put(leafKey, NULL_MARKER.equals(value) ? null : value);
        }

        // Convert maps with sequential integer keys to lists, using declaration for guidance
        return this.convertMapsToLists(root, declaration);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertMapsToLists(Map<String, Object> map, ConfigDeclaration declaration) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Get field type from declaration
            GenericsDeclaration fieldType = null;
            if (declaration != null) {
                Optional<FieldDeclaration> field = declaration.getField(key);
                if (field.isPresent()) {
                    fieldType = field.get().getType();
                }
            }

            if ((value instanceof String) && (fieldType != null) && Collection.class.isAssignableFrom(fieldType.getType())) {
                String strValue = (String) value;
                if (strValue.isEmpty()) {
                    // Empty string value for collection fields means empty collection
                    result.put(key, new ArrayList<>());
                } else if (strValue.indexOf(',') >= 0) {
                    // Comma-separated simple list - convert __null__ markers back to null
                    String[] parts = strValue.split(",");
                    List<Object> list = new ArrayList<>();
                    for (String part : parts) {
                        list.add(NULL_MARKER.equals(part) ? null : part);
                    }
                    result.put(key, list);
                } else {
                    // Single-element list (no comma) - convert __null__ marker back to null
                    Object element = NULL_MARKER.equals(strValue) ? null : strValue;
                    result.put(key, new ArrayList<>(Collections.singletonList(element)));
                }
                continue;
            }

            if ("".equals(value) && (fieldType != null) && Map.class.isAssignableFrom(fieldType.getType())) {
                // Empty string value for map fields means empty map
                result.put(key, new LinkedHashMap<>());
                continue;
            }

            if (value instanceof Map) {
                Map<String, Object> nested = (Map<String, Object>) value;

                // Check if this should be a list based on declaration or structure
                boolean shouldBeList = (fieldType != null) && Collection.class.isAssignableFrom(fieldType.getType());
                boolean looksLikeList = this.isListLike(nested);

                if (shouldBeList || looksLikeList) {
                    GenericsDeclaration elementType = (fieldType != null) ? fieldType.getSubtypeAtOrNull(0) : null;
                    result.put(key, this.convertToList(nested, elementType));
                } else {
                    // Get nested declaration for subconfigs
                    ConfigDeclaration nestedDeclaration = ((fieldType != null) && fieldType.isConfig())
                        ? ConfigDeclaration.of(fieldType.getType())
                        : null;
                    result.put(key, this.convertMapsToLists(nested, nestedDeclaration));
                }
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    private boolean isListLike(Map<String, Object> map) {
        if (map.isEmpty()) {
            return false;
        }

        Set<Integer> indices = new TreeSet<>();
        for (String key : map.keySet()) {
            try {
                int index = Integer.parseInt(key);
                if (index < 0) {
                    return false;
                }
                indices.add(index);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Check for sequential indices starting from 0
        int expected = 0;
        for (int index : indices) {
            if (index != expected) {
                return false;
            }
            expected++;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private List<Object> convertToList(Map<String, Object> map, GenericsDeclaration elementType) {
        List<Object> list = new ArrayList<>();
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        sortedKeys.sort(Comparator.comparingInt(Integer::parseInt));

        // Get nested element type for List<List<...>> cases
        GenericsDeclaration nestedElementType = (elementType != null) ? elementType.getSubtypeAtOrNull(0) : null;
        ConfigDeclaration elementDeclaration = ((elementType != null) && elementType.isConfig())
            ? ConfigDeclaration.of(elementType.getType())
            : null;

        for (String key : sortedKeys) {
            Object value = map.get(key);
            if (value instanceof Map) {
                Map<String, Object> nested = (Map<String, Object>) value;
                if (this.isListLike(nested)) {
                    list.add(this.convertToList(nested, nestedElementType));
                } else {
                    list.add(this.convertMapsToLists(nested, elementDeclaration));
                }
            } else if ((value instanceof String) && (elementType != null) && Collection.class.isAssignableFrom(elementType.getType())) {
                // Handle comma-separated string that should become a nested list
                String strValue = (String) value;
                if (strValue.isEmpty()) {
                    list.add(new ArrayList<>());
                } else if (strValue.indexOf(',') >= 0) {
                    String[] parts = strValue.split(",");
                    List<Object> nestedList = new ArrayList<>();
                    for (String part : parts) {
                        nestedList.add(NULL_MARKER.equals(part) ? null : part);
                    }
                    list.add(nestedList);
                } else {
                    Object element = NULL_MARKER.equals(strValue) ? null : strValue;
                    list.add(new ArrayList<>(Collections.singletonList(element)));
                }
            } else {
                list.add(value);
            }
        }

        return list;
    }
}
