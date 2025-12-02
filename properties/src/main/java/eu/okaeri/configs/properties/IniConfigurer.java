package eu.okaeri.configs.properties;

import eu.okaeri.configs.schema.ConfigDeclaration;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * INI configurer using section-based format with [section] headers.
 * <p>
 * Features:
 * <ul>
 *   <li>Section headers: {@code [database]}</li>
 *   <li>Key-value pairs within sections: {@code host=localhost}</li>
 *   <li>Nested sections via dot notation: {@code [database.primary]}</li>
 *   <li>Simple lists via comma notation: {@code tags=a,b,c}</li>
 *   <li>Complex lists via index notation: {@code 0=first}, {@code 1=second}</li>
 *   <li>Comments using {@code ;} or {@code #} prefix</li>
 *   <li>Zero external dependencies</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * ; Database configuration
 * [database]
 * host=localhost
 * port=5432
 *
 * [server]
 * host=0.0.0.0
 * port=8080
 * </pre>
 */
@Accessors(chain = true)
public class IniConfigurer extends FlatConfigurer {

    private @Setter int maxSectionDepth = 2;

    public IniConfigurer() {
        this.commentPrefix = "; ";
    }

    public IniConfigurer(@NonNull Map<String, Object> map) {
        super(map);
        this.commentPrefix = "; ";
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("ini", "cfg");
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Map<String, String> flat = new LinkedHashMap<>();

        String currentSection = "";
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                continue;
            }

            // Section header
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1).trim();
                continue;
            }

            // Key-value pair
            int eqIndex = line.indexOf('=');
            if (eqIndex > 0) {
                String key = line.substring(0, eqIndex).trim();
                String value = line.substring(eqIndex + 1).trim();

                // Remove surrounding quotes and unescape
                value = unescape(unquote(value));

                // Build full key with section prefix
                String fullKey = currentSection.isEmpty() ? key : (currentSection + "." + key);
                flat.put(fullKey, value);
            }
        }

        this.map = this.unflatten(flat, declaration);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        StringBuilder sb = new StringBuilder();

        this.writeHeader(sb, declaration);
        this.writeIni(sb, declaration);

        this.writeOutput(outputStream, sb);
    }

    // ==================== INI-Specific Writing ====================

    private void writeIni(StringBuilder sb, ConfigDeclaration declaration) {
        Map<String, String> flat = this.flatten(this.map);

        // Group entries by section
        Map<String, List<Map.Entry<String, String>>> sections = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : flat.entrySet()) {
            String fullKey = entry.getKey();
            String[] sectionAndKey = this.splitIntoSectionAndKey(fullKey, declaration);
            String section = sectionAndKey[0];
            String key = sectionAndKey[1];

            sections.computeIfAbsent(section, k -> new ArrayList<>())
                .add(new AbstractMap.SimpleEntry<>(key, entry.getValue()));
        }

        Set<String> writtenComments = new HashSet<>();

        // Write root entries first (empty section)
        List<Map.Entry<String, String>> rootEntries = sections.remove("");
        if (rootEntries != null) {
            for (Map.Entry<String, String> entry : rootEntries) {
                this.writeFieldComments(sb, entry.getKey(), declaration, writtenComments);
                sb.append(escapeKey(entry.getKey())).append("=").append(escapeValue(entry.getValue())).append("\n");
            }
        }

        // Write each section
        for (Map.Entry<String, List<Map.Entry<String, String>>> section : sections.entrySet()) {
            String sectionName = section.getKey();

            sb.append("\n");

            // Write comment for top-level field
            String topLevelField = sectionName.contains(".")
                ? sectionName.substring(0, sectionName.indexOf('.'))
                : sectionName;
            this.writeFieldComments(sb, topLevelField, declaration, writtenComments);

            sb.append("[").append(sectionName).append("]\n");

            for (Map.Entry<String, String> entry : section.getValue()) {
                String entryKey = entry.getKey();
                // Write comments using section-qualified key for proper deduplication
                String qualifiedKey = sectionName + "." + entryKey;
                this.writeFieldComments(sb, qualifiedKey, declaration, writtenComments);

                sb.append(escapeKey(entryKey)).append("=").append(escapeValue(entry.getValue())).append("\n");
            }
        }
    }

    /**
     * Splits a flat key into section path and remaining key.
     * Only creates sections for OkaeriConfig subclasses up to maxSectionDepth.
     */
    private String[] splitIntoSectionAndKey(String fullKey, ConfigDeclaration declaration) {
        String[] parts = fullKey.split("\\.");
        if ((parts.length == 1) || (declaration == null)) {
            return new String[]{"", fullKey};
        }

        int sectionDepth = declaration.findConfigDepth(parts, this.maxSectionDepth);
        if (sectionDepth == 0) {
            return new String[]{"", fullKey};
        }

        String sectionPath = String.join(".", Arrays.copyOfRange(parts, 0, sectionDepth));
        String remainingKey = String.join(".", Arrays.copyOfRange(parts, sectionDepth, parts.length));
        return new String[]{sectionPath, remainingKey};
    }

    // ==================== INI-Specific Utilities ====================

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if (((first == '"') && (last == '"')) || ((first == '\'') && (last == '\''))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String escapeKey(String key) {
        // INI keys typically don't need escaping, but we escape = and newlines
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c == '=') || (c == '\n') || (c == '\r')) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String escapeValue(String value) {
        // Check if quoting is needed
        boolean needsQuotes = value.isEmpty()
            || value.startsWith(" ")
            || value.endsWith(" ")
            || value.contains("\\")
            || value.contains(";")
            || value.contains("#")
            || value.contains("=")
            || value.contains("\n")
            || value.contains("\r")
            || value.contains("\t");

        if (needsQuotes) {
            // Escape backslashes and control chars
            String escaped = value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

            // Prefer single quotes if value contains double quotes but no single quotes
            boolean hasDouble = value.contains("\"");
            boolean hasSingle = value.contains("'");

            if (hasDouble && !hasSingle) {
                return "'" + escaped + "'";
            } else {
                // Use double quotes, escape internal double quotes
                return "\"" + escaped.replace("\"", "\\\"") + "\"";
            }
        }

        return value;
    }

    private static String unescape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c == '\\') && ((i + 1) < value.length())) {
                char next = value.charAt(i + 1);
                switch (next) {
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    default:
                        sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
