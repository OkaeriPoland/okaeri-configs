package eu.okaeri.configs.migrate.view;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.NonNull;

import java.util.Collection;
import java.util.Map;

/**
 * Provides raw key-value access to internalState config data for migrations.
 * <p>
 * Supports dot-separated nested key paths (e.g., "section.subsection.key").
 * <p>
 * All reads/writes operate directly on {@code config.getInternalState()}
 * without triggering deserialization. A single {@code config.update()} call
 * at the end of ALL migrations syncs internalState to Java fields.
 */
public class InternalStateView extends ConfigView {

    public InternalStateView(@NonNull OkaeriConfig config, String nestedSeparator) {
        super(config, nestedSeparator);
    }

    public InternalStateView(@NonNull OkaeriConfig config) {
        super(config);
    }

    // ==================== INTERFACE REQUIREMENTS ====================

    @Override
    public Object set(@NonNull String key, Object value, GenericsDeclaration genericType) {
        return this.setRaw(key, value);
    }

    @Override
    public Object getRaw(@NonNull String key) {
        return this.valueExtract(this.getInternalState(), key);
    }

    @Override
    public Object setRaw(@NonNull String key, Object value) {
        return this.valuePut(this.getInternalState(), key, value);
    }

    @Override
    public void setCollection(@NonNull String key, Collection<?> collection, @NonNull GenericsDeclaration genericType) {
        this.setRaw(key, collection);
    }

    @Override
    public void setMap(@NonNull String key, Map<?, ?> map, @NonNull GenericsDeclaration genericType) {
        this.setRaw(key, map);
    }

    // ==================== MIGRATION CONVENIENCE METHODS ====================

    public Map<String, Object> getInternalState() {
        return this.config.getInternalState();
    }

    /**
     * Checks if a key exists at the specified path.
     *
     * @param key the dot-separated key path
     * @return true if the key exists
     */
    @Override
    public boolean exists(@NonNull String key) {
        return this.valueExists(this.getInternalState(), key);
    }

    /**
     * Removes the value at the specified key path.
     *
     * @param key the dot-separated key path
     * @return the previous value, or null
     */
    @Override
    public Object remove(@NonNull String key) {
        return this.valueRemove(this.getInternalState(), key);
    }
}
