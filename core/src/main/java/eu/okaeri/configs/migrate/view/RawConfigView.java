package eu.okaeri.configs.migrate.view;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.TypedKeyReader;
import eu.okaeri.configs.serdes.TypedKeyWriter;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides raw key-value access to config data for migrations.
 * <p>
 * Supports dot-separated nested key paths (e.g., "section.subsection.key").
 * Implements both {@link TypedKeyReader} and {@link TypedKeyWriter} for
 * automatic type resolution and simplification.
 */
@AllArgsConstructor
@RequiredArgsConstructor
public class RawConfigView implements TypedKeyReader, TypedKeyWriter {

    private final OkaeriConfig config;
    private String nestedSeparator = "\\.";

    // ==================== INTERFACE REQUIREMENTS ====================

    @Override
    public Configurer getConfigurer() {
        return this.config.getConfigurer();
    }

    @Override
    public SerdesContext getReaderContext(@NonNull String key) {
        return SerdesContext.of(this.getConfigurer(), this.config.getContext(), null);
    }

    @Override
    public SerdesContext getWriterContext(@NonNull String key) {
        return SerdesContext.of(this.getConfigurer(), this.config.getContext(), null);
    }

    @Override
    public Object getRaw(@NonNull String key) {
        Map<String, Object> document = this.config.asMap(this.getConfigurer(), true);
        return this.valueExtract(document, key);
    }

    @Override
    public void setRaw(@NonNull String key, Object value) {
        Map<String, Object> document = this.config.asMap(this.getConfigurer(), true);
        this.valuePut(document, key, value);
        this.config.load(document);
    }

    @Override
    public Object getRawOrNull(@NonNull String key) {
        return this.getRaw(key);
    }

    // ==================== MIGRATION CONVENIENCE METHODS ====================

    /**
     * Gets the raw value at the specified key path (alias for {@link #getRaw}).
     *
     * @param key the dot-separated key path
     * @return the raw value, or null if not found
     */
    public Object get(@NonNull String key) {
        return this.getRaw(key);
    }

    /**
     * Checks if a key exists at the specified path.
     *
     * @param key the dot-separated key path
     * @return true if the key exists
     */
    public boolean exists(@NonNull String key) {
        Map<String, Object> document = this.config.asMap(this.getConfigurer(), true);
        return this.valueExists(document, key);
    }

    /**
     * Removes the value at the specified key path.
     *
     * @param key the dot-separated key path
     * @return the previous value, or null
     */
    public Object remove(@NonNull String key) {
        Map<String, Object> document = this.config.asMap(this.getConfigurer(), true);
        Object old = this.valueRemove(document, key);

        // top-level keys need to be removed from internalState as well
        if (key.split(this.nestedSeparator).length == 1) {
            Map<String, Object> internalState = this.config.getInternalState();
            if (internalState != null) {
                internalState.remove(key);
            }
        }

        this.config.load(document);
        return old;
    }

    // ==================== NESTED PATH HELPERS ====================

    protected boolean valueExists(Map<?, ?> document, String path) {
        String[] split = path.split(this.nestedSeparator);
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            if (i == (split.length - 1)) {
                return document.containsKey(part);
            }
            Object element = document.get(part);
            if (element instanceof Map) {
                document = (Map<?, ?>) element;
                continue;
            }
            return false;
        }
        return false;
    }

    protected Object valueExtract(Map<?, ?> document, String path) {
        String[] split = path.split(this.nestedSeparator);
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            Object element = document.get(part);
            if (i == (split.length - 1)) {
                return element;
            }
            if (element instanceof Map) {
                document = (Map<?, ?>) element;
                continue;
            }
            // can't traverse deeper - return null
            return null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Object valuePut(Map<?, ?> document, String path, Object value) {
        String[] split = path.split(this.nestedSeparator);
        Map<Object, Object> current = (Map<Object, Object>) document;
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            if (i == (split.length - 1)) {
                return current.put(part, value);
            }
            Object element = current.get(part);
            if (element instanceof Map) {
                current = (Map<Object, Object>) element;
                continue;
            }
            if (element != null) {
                String elementStr = element.getClass().getSimpleName();
                throw new IllegalArgumentException("Cannot insert '" + path + "': " +
                    "type conflict (ended at index " + i + " [" + part + ":" + elementStr + "])");
            }
            Map<Object, Object> map = new LinkedHashMap<>();
            current.put(part, map);
            current = map;
        }
        throw new IllegalArgumentException("Cannot put '" + path + "'");
    }

    @SuppressWarnings("unchecked")
    protected Object valueRemove(Map<?, ?> document, String path) {
        String[] split = path.split(this.nestedSeparator);
        Map<Object, Object> current = (Map<Object, Object>) document;
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            if (i == (split.length - 1)) {
                return current.remove(part);
            }
            Object element = current.get(part);
            if (element instanceof Map) {
                current = (Map<Object, Object>) element;
                continue;
            }
            return null;
        }
        return null;
    }
}
