package eu.okaeri.configs.toml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.format.toml.TomlSourceWalker;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.ConfigPath;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * TOML configurer using Jackson's TOML dataformat for parsing,
 * with post-processing for proper TOML section formatting.
 * <p>
 * Features:
 * <ul>
 *   <li>Full TOML 1.0 parsing via Jackson</li>
 *   <li>Proper nested table sections at any depth</li>
 *   <li>Header and field comments support</li>
 *   <li>Preserves field declaration order</li>
 * </ul>
 */
@Accessors(chain = true)
public class TomlJacksonConfigurer extends Configurer {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<LinkedHashMap<String, Object>>() {
    };
    private static final String NULL_MARKER = "__null__";

    private Map<String, Object> map = new LinkedHashMap<>();
    private @Setter TomlMapper mapper;
    private @Setter int maxSectionDepth = 2;

    public TomlJacksonConfigurer() {
        this.mapper = createDefaultMapper();
    }

    public TomlJacksonConfigurer(@NonNull TomlMapper mapper) {
        this.mapper = mapper;
    }

    public TomlJacksonConfigurer(@NonNull Map<String, Object> map) {
        this();
        this.map = map;
    }

    private static TomlMapper createDefaultMapper() {
        return TomlMapper.builder().build();
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("toml");
    }

    @Override
    public SourceWalker createSourceWalker() {
        String raw = this.getRawContent();
        return (raw == null) ? null : TomlSourceWalker.of(raw);
    }

    @Override
    public boolean isCommentLine(String line) {
        return line.trim().startsWith("#");
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) {
        if (value == null) {
            return null;
        }

        // long/Long values outside double precision range must be converted to String
        // (TOML libraries may use double internally, which can only represent integers up to 2^53 precisely)
        GenericsDeclaration genericsDeclaration = GenericsDeclaration.of(value);
        if ((genericsDeclaration.getType() == long.class) || (genericsDeclaration.getType() == Long.class)) {
            long longValue = (Long) value;
            long maxSafeInteger = 1L << 53; // 9007199254740992
            if ((longValue > maxSafeInteger) || (longValue < -maxSafeInteger)) {
                return super.simplify(value, genericType, serdesContext, false);
            }
        }

        return super.simplify(value, genericType, serdesContext, conservative);
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
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

    // ==================== Loading ====================

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        this.map = this.mapper.readValue(inputStream, MAP_TYPE);
        if (this.map == null) {
            this.map = new LinkedHashMap<>();
        }
        // Convert __null__ markers back to null
        this.normalizeNullMarkers(this.map);
    }

    @SuppressWarnings("unchecked")
    private void normalizeNullMarkers(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();

            if (NULL_MARKER.equals(value)) {
                entry.setValue(null);
            } else if (value instanceof Map) {
                this.normalizeNullMarkers((Map<String, Object>) value);
            } else if (value instanceof List) {
                this.normalizeNullMarkersInList((List<Object>) value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void normalizeNullMarkersInList(List<Object> list) {
        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            if (NULL_MARKER.equals(value)) {
                list.set(i, null);
            } else if (value instanceof Map) {
                this.normalizeNullMarkers((Map<String, Object>) value);
            } else if (value instanceof List) {
                this.normalizeNullMarkersInList((List<Object>) value);
            }
        }
    }

    // ==================== Writing ====================

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        StringBuilder sb = new StringBuilder();

        // Write header comments
        this.writeHeader(sb, declaration);

        // Convert null values to __null__ markers before serialization
        Map<Object, Object> normalized = this.convertNullsToMarkers(this.map);

        // Get flat TOML from Jackson
        String flatToml = this.mapper.writeValueAsString(normalized);

        // Post-process into sections
        String sectioned = this.convertToSections(flatToml, declaration);
        sb.append(sectioned);

        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> convertNullsToMarkers(Map<?, ?> map) {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(entry.getKey(), this.convertNullValue(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object convertNullValue(Object value) {
        if (value == null) {
            return NULL_MARKER;
        }
        if (value instanceof Map) {
            return this.convertNullsToMarkers((Map<?, ?>) value);
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(this.convertNullValue(item));
            }
            return result;
        }
        return value;
    }

    private void writeHeader(StringBuilder sb, ConfigDeclaration declaration) {
        String[] header = declaration.getHeader();
        if ((header != null) && (header.length > 0)) {
            for (String line : header) {
                sb.append("# ").append(line).append("\n");
            }
            sb.append("\n");
        }
    }

    /**
     * Converts flat dotted TOML keys into proper nested sections.
     * Only creates sections for OkaeriConfig subclasses, not plain Maps.
     */
    private String convertToSections(String flatToml, ConfigDeclaration declaration) {
        // Group lines by their section path (empty string = root)
        Map<String, List<String>> sections = new LinkedHashMap<>();

        for (String line : flatToml.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int eqIndex = findEqualsIndex(trimmed);
            if (eqIndex < 0) {
                sections.computeIfAbsent("", key -> new ArrayList<>()).add(line);
                continue;
            }

            String fullKey = trimmed.substring(0, eqIndex).trim();
            String value = trimmed.substring(eqIndex + 1).trim();

            // Find the deepest section path for this key (only for OkaeriConfig fields)
            String[] pathInfo = this.findSectionPath(fullKey, declaration);
            String sectionPath = pathInfo[0];
            String remainingKey = pathInfo[1];

            sections.computeIfAbsent(sectionPath, key -> new ArrayList<>())
                .add(remainingKey + " = " + value);
        }

        // Build output with comments using ConfigPath for resolution
        StringBuilder result = new StringBuilder();
        Set<String> commentedPatterns = new HashSet<>();

        // Root lines (empty section path)
        List<String> rootLines = sections.getOrDefault("", Collections.emptyList());
        for (String line : rootLines) {
            String key = extractKey(line);
            if (key != null) {
                this.writeCommentsForKey(result, key, declaration, commentedPatterns);
            }
            result.append(line).append("\n");
        }

        // Nested sections
        for (Map.Entry<String, List<String>> section : sections.entrySet()) {
            String sectionPath = section.getKey();
            if (sectionPath.isEmpty()) {
                continue; // Already handled root
            }

            List<String> lines = section.getValue();
            result.append("\n");

            // Add comment for section itself
            this.writeCommentsForKey(result, sectionPath, declaration, commentedPatterns);
            result.append("[").append(sectionPath).append("]\n");

            for (String line : lines) {
                String key = extractKey(line);
                if (key != null) {
                    String fullKey = sectionPath + "." + key;
                    this.writeCommentsForKey(result, fullKey, declaration, commentedPatterns);
                }
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Writes comments for a dotted key path using ConfigPath for resolution.
     * Uses pattern-based deduplication (list.*.field, map.*.field).
     */
    private void writeCommentsForKey(StringBuilder sb, String key, ConfigDeclaration declaration, Set<String> commentedPatterns) {
        ConfigPath path = ConfigPath.parseFlat(key, declaration);
        List<ConfigPath.PathNode> nodes = path.getNodes();

        for (int i = 0; i < nodes.size(); i++) {
            // Skip indices/keys - they don't have field comments
            if (!(nodes.get(i) instanceof ConfigPath.PropertyNode)) {
                continue;
            }

            ConfigPath partialPath = path.subPath(i);
            String pattern = partialPath.toPattern();
            if (commentedPatterns.contains(pattern)) {
                continue;
            }

            Optional<FieldDeclaration> field = partialPath.resolveFieldDeclaration(declaration);
            if (field.isPresent()) {
                String[] comment = field.get().getComment();
                if (comment != null) {
                    for (String line : comment) {
                        if (line.isEmpty()) {
                            // @Comment("") -> empty line (no # at all)
                            sb.append("\n");
                        } else if (line.trim().isEmpty()) {
                            // @Comment(" ") -> "#" (just the hash)
                            sb.append("#\n");
                        } else {
                            // Normal comment text
                            sb.append("# ").append(line).append("\n");
                        }
                    }
                }
                commentedPatterns.add(pattern);
            }
        }
    }

    /**
     * Finds the section path for a dotted key using ConfigDeclaration.
     * Only creates sections for OkaeriConfig subclasses, not plain Maps.
     * Respects maxSectionDepth limit.
     */
    private String[] findSectionPath(String dottedKey, ConfigDeclaration declaration) {
        String[] parts = dottedKey.split("\\.");
        if ((parts.length == 1) || (declaration == null)) {
            return new String[]{"", dottedKey};
        }

        int sectionDepth = declaration.findConfigDepth(parts, this.maxSectionDepth);
        if (sectionDepth == 0) {
            return new String[]{"", dottedKey};
        }

        String sectionPath = String.join(".", Arrays.copyOfRange(parts, 0, sectionDepth));
        String remainingKey = String.join(".", Arrays.copyOfRange(parts, sectionDepth, parts.length));
        return new String[]{sectionPath, remainingKey};
    }

    private static int findEqualsIndex(String line) {
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inString) {
                if ((c == stringChar) && (line.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            } else {
                if ((c == '"') || (c == '\'')) {
                    inString = true;
                    stringChar = c;
                } else if (c == '=') {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String extractKey(String line) {
        int eqIndex = findEqualsIndex(line);
        if (eqIndex > 0) {
            return line.substring(0, eqIndex).trim();
        }
        return null;
    }
}
