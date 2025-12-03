package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data container for {@link ObjectSerializer#deserialize} input.
 * <p>
 * Provides methods to read values from keys with automatic type resolution
 * using the attached {@link Configurer}.
 * <p>
 * <b>Multi-key input:</b>
 * <pre>{@code
 * public Location deserialize(DeserializationData data, GenericsDeclaration generics) {
 *     return new Location(
 *         Bukkit.getWorld(data.get("world", String.class)),
 *         data.get("x", Double.class),
 *         data.get("y", Double.class),
 *         data.get("z", Double.class)
 *     );
 * }
 * }</pre>
 * <p>
 * <b>Single value input:</b>
 * <pre>{@code
 * public Duration deserialize(DeserializationData data, GenericsDeclaration generics) {
 *     if (data.isValue()) {
 *         return Duration.parse(data.getValue(String.class));
 *     }
 *     // fallback to multi-key format
 * }
 * }</pre>
 *
 * @see ObjectSerializer#deserialize
 * @see SerializationData
 */
@AllArgsConstructor
public class DeserializationData implements TypedKeyReader {

    @NonNull private Map<String, Object> data;
    @Getter @NonNull private Configurer configurer;
    @Getter @NonNull private SerdesContext context;

    @Override
    public SerdesContext getReaderContext(@NonNull String key) {
        ConfigPath newPath = ObjectSerializer.VALUE.equals(key) ? this.context.getPath() : this.context.getPath().property(key);
        return SerdesContext.of(this.configurer, this.context.getConfigContext(), null).withPath(newPath);
    }

    /**
     * @return unmodifiable map of current deserialization data
     */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(this.data);
    }

    /**
     * Checks if value object is available. Magic value
     * represents whole deserialization object when
     * attempting to deserialize non-map object.
     *
     * @return state of value object presence
     */
    public boolean isValue() {
        return this.containsKey(ObjectSerializer.VALUE);
    }

    /**
     * Checks if value under specific key is available.
     *
     * @param key target key
     * @return true if key is present in deserialization data
     */
    public boolean containsKey(@NonNull String key) {
        return this.data.containsKey(key);
    }

    // ==================== CORE READ METHOD ====================

    @Override
    public Object getRaw(@NonNull String key) {
        if (!this.isValue() && ObjectSerializer.VALUE.equals(key)) {
            return this.asMap();
        }
        return this.data.get(key);
    }

    // ==================== VALUE METHODS (delegate to get with VALUE key) ====================

    /**
     * Gets raw value object without transformation.
     *
     * @return value or null
     */
    public Object getValueRaw() {
        return this.getRaw(ObjectSerializer.VALUE);
    }

    /**
     * Gets value object with type resolution.
     *
     * @param genericType target type declaration
     * @param <T>         type of transformed value
     * @return transformed value or null
     */
    public <T> T getValueDirect(@NonNull GenericsDeclaration genericType) {
        return this.get(ObjectSerializer.VALUE, genericType);
    }

    /**
     * Gets value object with type resolution.
     *
     * @param valueType target class
     * @param <T>       type of transformed value
     * @return transformed value or null
     */
    public <T> T getValue(@NonNull Class<T> valueType) {
        return this.get(ObjectSerializer.VALUE, valueType);
    }

    /**
     * Gets value as list.
     *
     * @param listValueType target element type
     * @param <T>           target element type
     * @return transformed list or null
     */
    public <T> List<T> getValueAsList(@NonNull Class<T> listValueType) {
        return this.getAsList(ObjectSerializer.VALUE, listValueType);
    }

    /**
     * Gets value as set.
     *
     * @param setValueType target element type
     * @param <T>          target element type
     * @return transformed set or null
     */
    public <T> Set<T> getValueAsSet(@NonNull Class<T> setValueType) {
        return this.getAsSet(ObjectSerializer.VALUE, setValueType);
    }

    /**
     * Gets value as collection with full type declaration.
     *
     * @param genericType target collection type declaration
     * @param <T>         element type
     * @return transformed collection or null
     */
    public <T> Collection<T> getValueAsCollection(@NonNull GenericsDeclaration genericType) {
        return this.getAsCollection(ObjectSerializer.VALUE, genericType);
    }

    // ==================== LEGACY ALIASES ====================

    /**
     * @deprecated Use {@link #get(String, GenericsDeclaration)} instead.
     */
    @Deprecated
    public <T> T getDirect(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        return this.get(key, genericType);
    }
}
