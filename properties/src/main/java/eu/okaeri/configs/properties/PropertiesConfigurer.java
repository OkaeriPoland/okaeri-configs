package eu.okaeri.configs.properties;

import eu.okaeri.configs.schema.ConfigDeclaration;
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
 *   <li>Header and field comments using {@code #} prefix</li>
 *   <li>Zero external dependencies (uses java.util.Properties)</li>
 * </ul>
 */
@Accessors(chain = true)
public class PropertiesConfigurer extends FlatConfigurer {

    /**
     * When true, escapes non-ASCII characters as \\uXXXX sequences.
     * Set to true for legacy tools expecting ISO-8859-1 encoded properties.
     */
    private @Setter boolean escapeUnicode = false;

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("properties");
    }

    @Override
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        OrderedProperties props = new OrderedProperties();
        props.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        Map<String, String> flat = new LinkedHashMap<>();
        for (String key : props.orderedKeys()) {
            flat.put(key, props.getProperty(key));
        }

        return this.unflatten(flat, declaration);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        StringBuilder sb = new StringBuilder();

        this.writeHeader(sb, declaration);
        this.writeProperties(sb, data, declaration);

        this.writeOutput(outputStream, sb);
    }

    // ==================== Properties-Specific Writing ====================

    private void writeProperties(StringBuilder sb, Map<String, Object> data, ConfigDeclaration declaration) {
        Map<String, String> flat = this.flatten(data);
        Set<String> writtenCommentPaths = new HashSet<>();

        for (Map.Entry<String, String> entry : flat.entrySet()) {
            String key = entry.getKey();
            this.writeFieldComments(sb, key, declaration, writtenCommentPaths);
            sb.append(escapeKey(key)).append("=").append(this.escapeValue(entry.getValue())).append("\n");
        }
    }

    // ==================== Properties-Specific Escaping ====================

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

    private String escapeValue(String value) {
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
                    if (this.escapeUnicode && (c > 0x7F)) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else {
                        sb.append(c);
                    }
                    leadingWhitespace = false;
            }
        }
        return sb.toString();
    }

    // ==================== Inner Classes ====================

    /**
     * Properties subclass that preserves insertion order.
     * Standard Properties uses Hashtable internally which has undefined iteration order.
     */
    private static class OrderedProperties extends Properties {
        private static final long serialVersionUID = 1L;
        private List<String> orderedKeys = new ArrayList<>();

        @Override
        public synchronized Object put(Object key, Object value) {
            String keyStr = String.valueOf(key);
            if (!this.containsKey(keyStr)) {
                this.orderedKeys.add(keyStr);
            }
            return super.put(key, value);
        }

        public List<String> orderedKeys() {
            return this.orderedKeys;
        }

        @Override
        public synchronized Object clone() {
            OrderedProperties clone = (OrderedProperties) super.clone();
            clone.orderedKeys = new ArrayList<>(this.orderedKeys);
            return clone;
        }
    }
}
