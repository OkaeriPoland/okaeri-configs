package eu.okaeri.configs.migrate.view;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.TypedKeyReader;
import eu.okaeri.configs.serdes.TypedKeyWriter;
import lombok.NonNull;

import java.util.Map;

/**
 * Provides raw key-value access to config data for migrations.
 * <p>
 * Supports dot-separated nested key paths (e.g., "section.subsection.key").
 * Implements both {@link TypedKeyReader} and {@link TypedKeyWriter} for
 * automatic type resolution and simplification.
 */
public class RawConfigView extends ConfigView {

    public RawConfigView(@NonNull OkaeriConfig config, String nestedSeparator) {
        super(config, nestedSeparator);
    }

    public RawConfigView(@NonNull OkaeriConfig config) {
        super(config);
    }

    // ==================== INTERFACE REQUIREMENTS ====================

    @Override
    public Object getRaw(@NonNull String key) {
        Map<String, Object> document = this.config.asMap();
        return this.valueExtract(document, key);
    }

    @Override
    public Object setRaw(@NonNull String key, Object value) {
        Map<String, Object> document = this.config.asMap();
        Object old = this.valuePut(document, key, value);
        this.config.load(document);
        return old;
    }

    // ==================== MIGRATION CONVENIENCE METHODS ====================

    /**
     * Checks if a key exists at the specified path.
     *
     * @param key the dot-separated key path
     * @return true if the key exists
     */
    @Override
    public boolean exists(@NonNull String key) {
        Map<String, Object> document = this.config.asMap();
        return this.valueExists(document, key);
    }

    /**
     * Removes the value at the specified key path.
     *
     * @param key the dot-separated key path
     * @return the previous value, or null
     */
    @Override
    public Object remove(@NonNull String key) {
        Map<String, Object> document = this.config.asMap();
        Object old = this.valueRemove(document, key);

        // top-level keys need to be removed from internalState as well
        if (key.split(this.nestedSeparator).length == 1) {
            Map<String, Object> internalState = this.getInternalState();
            if (internalState != null) {
                internalState.remove(key);
            }
        }

        this.config.load(document);
        return old;
    }
}
