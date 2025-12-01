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
 *   <li>Simple lists via comma notation: {@code tags=a,b,c} (when line ≤80 chars)</li>
 *   <li>Complex lists via index notation: {@code items.0=first}, {@code items.1=second}</li>
 *   <li>Write-only comments using {@code #} prefix</li>
 *   <li>Zero external dependencies (uses java.util.Properties)</li>
 * </ul>
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
        Properties props = new Properties();
        props.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Convert to sorted map (Properties doesn't maintain order)
        Map<String, String> flat = new TreeMap<>();
        for (String key : props.stringPropertyNames()) {
            flat.put(key, props.getProperty(key));
        }

        this.map = this.unflatten(flat, declaration);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        StringBuilder sb = new StringBuilder();

        this.writeHeader(sb, declaration);
        this.writeProperties(sb, declaration);

        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ==================== Writing ====================

    private void writeHeader(StringBuilder sb, ConfigDeclaration declaration) {
        String[] header = declaration.getHeader();
        if ((header != null) && (header.length > 0)) {
            for (String line : header) {
                sb.append(this.commentPrefix).append(line).append("\n");
            }
            sb.append("\n");
        }
    }

    private void writeProperties(StringBuilder sb, ConfigDeclaration declaration) {
        Map<String, String> flat = this.flatten(this.map);
        Set<String> writtenCommentPaths = new HashSet<>();

        for (Map.Entry<String, String> entry : flat.entrySet()) {
            String key = entry.getKey();
            this.writeFieldComments(sb, key, declaration, writtenCommentPaths);
            sb.append(escapeKey(key)).append("=").append(escapeValue(entry.getValue())).append("\n");
        }
    }

    private void writeFieldComments(StringBuilder sb, String key, ConfigDeclaration declaration, Set<String> written) {
        String[] parts = key.split("\\.");
        ConfigDeclaration currentDecl = declaration;
        StringBuilder pathBuilder = new StringBuilder();

        for (String part : parts) {
            // Skip numeric indices
            if (isNumeric(part)) {
                pathBuilder.append((pathBuilder.length() > 0) ? "." : "").append(part);
                continue;
            }

            pathBuilder.append((pathBuilder.length() > 0) ? "." : "").append(part);
            String path = pathBuilder.toString();

            if (written.contains(path) || (currentDecl == null)) {
                currentDecl = this.getNestedDeclaration(currentDecl, part);
                continue;
            }

            Optional<FieldDeclaration> field = currentDecl.getField(part);
            if (field.isPresent()) {
                String[] comment = field.get().getComment();
                if (comment != null) {
                    for (String line : comment) {
                        sb.append(this.commentPrefix).append(line).append("\n");
                    }
                }
                written.add(path);
                currentDecl = this.getNestedDeclaration(currentDecl, part);
            } else {
                currentDecl = null;
            }
        }
    }

    private ConfigDeclaration getNestedDeclaration(ConfigDeclaration decl, String fieldName) {
        if (decl == null) {
            return null;
        }
        Optional<FieldDeclaration> field = decl.getField(fieldName);
        if (field.isPresent() && field.get().getType().isConfig()) {
            return ConfigDeclaration.of(field.get().getType().getType());
        }
        return null;
    }

    // ==================== Flattening (Object → Properties) ====================

    private Map<String, String> flatten(Map<?, ?> map) {
        Map<String, String> result = new LinkedHashMap<>();
        this.flattenMap("", map, result);
        return result;
    }

    private void flattenMap(String prefix, Map<?, ?> map, Map<String, String> result) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : (prefix + "." + entry.getKey());
            this.flattenValue(key, entry.getValue(), result);
        }
    }

    private void flattenValue(String key, Object value, Map<String, String> result) {
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

    private void flattenMapOrList(String key, Map<?, ?> map, Map<String, String> result) {
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

    private void flattenList(String key, List<?> list, Map<String, String> result) {
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
    private String tryFormatAsCommaList(String key, List<?> list) {
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
    private List<Object> tryConvertToList(Map<?, ?> map) {
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

    // ==================== Unflattening (Properties → Object) ====================

    private Map<String, Object> unflatten(Map<String, String> flat, ConfigDeclaration declaration) {
        // Build nested structure from dot notation
        Map<String, Object> root = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : flat.entrySet()) {
            this.setNestedValue(root, entry.getKey().split("\\."), entry.getValue());
        }
        // Convert map structures to lists where appropriate
        return this.convertStructures(root, declaration);
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> root, String[] path, String value) {
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
    private Map<String, Object> convertStructures(Map<String, Object> map, ConfigDeclaration declaration) {
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
    private Object convertValue(Object value, GenericsDeclaration type) {
        if (value instanceof String) {
            return this.convertStringValue((String) value, type);
        }
        if (value instanceof Map) {
            return this.convertMapValue((Map<String, Object>) value, type);
        }
        return value;
    }

    private Object convertStringValue(String value, GenericsDeclaration type) {
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
    private Object convertMapValue(Map<String, Object> map, GenericsDeclaration type) {
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

    private List<Object> convertMapToList(Map<String, Object> map, GenericsDeclaration elementType) {
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
    private Object convertListElement(Object value, GenericsDeclaration elementType,
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
    private List<Object> parseCommaList(String value) {
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

    private boolean looksLikeList(Map<String, Object> map) {
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

    private GenericsDeclaration getFieldType(ConfigDeclaration declaration, String key) {
        if (declaration == null) {
            return null;
        }
        Optional<FieldDeclaration> field = declaration.getField(key);
        return field.map(FieldDeclaration::getType).orElse(null);
    }

    // ==================== Utilities ====================

    private static boolean isNumeric(String str) {
        return parseNonNegativeInt(str) != null;
    }

    private static Integer parseNonNegativeInt(String str) {
        try {
            int value = Integer.parseInt(str);
            return (value >= 0) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean containsDelimiter(String value) {
        return (value.indexOf(',') >= 0) || (value.indexOf('\n') >= 0) || (value.indexOf('\r') >= 0);
    }

    private static boolean isCollectionType(GenericsDeclaration type) {
        return (type != null) && Collection.class.isAssignableFrom(type.getType());
    }

    private static boolean isMapType(GenericsDeclaration type) {
        return (type != null) && Map.class.isAssignableFrom(type.getType());
    }

    private static String escapeKey(String key) {
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c == '=') || (c == ':') || (c == ' ')) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String escapeValue(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        boolean leadingWhitespace = true;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    leadingWhitespace = false;
                    break;
                case '\n':
                    sb.append("\\n");
                    leadingWhitespace = false;
                    break;
                case '\r':
                    sb.append("\\r");
                    leadingWhitespace = false;
                    break;
                case '\t':
                    sb.append("\\t");
                    leadingWhitespace = false;
                    break;
                case ' ':
                    sb.append(leadingWhitespace ? "\\ " : " ");
                    break;
                default:
                    if (c > 0x7F) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else {
                        sb.append(c);
                    }
                    leadingWhitespace = false;
            }
        }
        return sb.toString();
    }
}
