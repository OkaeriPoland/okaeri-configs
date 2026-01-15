package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.exception.ValueIndexedException;
import eu.okaeri.configs.serdes.PreProcessResult;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.ValuePreProcessor;
import lombok.NonNull;

/**
 * Resolves environment variable placeholders in configuration values.
 * <p>
 * Supports Spring Boot compatible placeholder syntax:
 * <ul>
 *   <li>{@code ${VAR}} - resolves from environment</li>
 *   <li>{@code ${VAR:default}} - uses default value if variable is not set</li>
 *   <li>{@code $${VAR}} - escapes to literal {@code ${VAR}} (not resolved)</li>
 * </ul>
 * <p>
 * Resolution priority (same as {@code @Variable}):
 * <ol>
 *   <li>JVM System Property ({@code -DVAR=value})</li>
 *   <li>Environment Variable</li>
 *   <li>Default value (if specified)</li>
 * </ol>
 * <p>
 * <b>Example configuration:</b>
 * <pre>{@code
 * database:
 *   host: ${DB_HOST:localhost}
 *   port: ${DB_PORT:5432}
 *   password: ${DB_PASSWORD}
 * }</pre>
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * ConfigManager.create(MyConfig.class, it -> {
 *     it.configure(opt -> {
 *         opt.configurer(new YamlBukkitConfigurer());
 *         opt.resolvePlaceholders();
 *     });
 *     it.load();
 * });
 * }</pre>
 *
 * @see ValuePreProcessor
 */
public class EnvironmentPlaceholderProcessor implements ValuePreProcessor {

    @Override
    public PreProcessResult process(Object value, @NonNull SerdesContext context) {
        if (!(value instanceof String)) {
            return PreProcessResult.noop();
        }

        String str = (String) value;
        StringBuilder result = new StringBuilder();
        int len = str.length();
        int i = 0;
        boolean modified = false;

        while (i < len) {
            char c = str.charAt(i);

            // Check for escape: $${
            if ((c == '$') && ((i + 1) < len) && (str.charAt(i + 1) == '$') && ((i + 2) < len) && (str.charAt(i + 2) == '{')) {
                // $${VAR} → ${VAR} (literal, not resolved)
                result.append("${");
                i += 3; // skip $${
                modified = true;
                continue;
            }

            // Check for placeholder start: ${
            if ((c == '$') && ((i + 1) < len) && (str.charAt(i + 1) == '{')) {

                // Find closing }
                int placeholderStart = i;
                int braceStart = i + 2;
                int braceEnd = str.indexOf('}', braceStart);

                if (braceEnd == -1) {
                    // No closing brace, treat as literal
                    result.append(c);
                    i++;
                    continue;
                }

                // Parse variable name and optional default value
                String content = str.substring(braceStart, braceEnd);
                String varName;
                String defaultVal = null;

                int colonIdx = content.indexOf(':');
                if (colonIdx != -1) {
                    varName = content.substring(0, colonIdx);
                    defaultVal = content.substring(colonIdx + 1);
                } else {
                    varName = content;
                }

                // Resolve: system property > env var > default
                String resolved = System.getProperty(varName);
                if (resolved == null) {
                    resolved = System.getenv(varName);
                }
                if (resolved == null) {
                    resolved = defaultVal;
                }

                if (resolved == null) {
                    throw new ValueIndexedException(
                        "Unresolved property or env",
                        placeholderStart,
                        (braceEnd - placeholderStart) + 1
                    );
                }

                result.append(resolved);
                i = braceEnd + 1;
                modified = true;
            } else {
                result.append(c);
                i++;
            }
        }

        if (!modified) {
            return PreProcessResult.noop();
        }

        // Runtime-only: preserve original placeholder string for saving
        return PreProcessResult.runtimeOnly(result.toString());
    }
}
