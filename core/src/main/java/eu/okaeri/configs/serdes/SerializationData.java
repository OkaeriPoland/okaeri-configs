package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data container for {@link ObjectSerializer#serialize} output.
 * <p>
 * Provides methods to add serialized values under keys. Values are automatically
 * simplified using the attached {@link Configurer}.
 * <p>
 * <b>Multi-key output:</b>
 * <pre>{@code
 * public void serialize(Location loc, SerializationData data, GenericsDeclaration generics) {
 *     data.set("world", loc.getWorld().getName());
 *     data.set("x", loc.getX());
 *     data.set("y", loc.getY());
 *     data.set("z", loc.getZ());
 * }
 * // Output: {world: "overworld", x: 100.5, y: 64.0, z: -200.5}
 * }</pre>
 * <p>
 * <b>Single value output:</b>
 * <pre>{@code
 * public void serialize(Duration duration, SerializationData data, GenericsDeclaration generics) {
 *     data.setValue(duration.toString());
 * }
 * // Output: "PT1H30M" (not a map)
 * }</pre>
 *
 * @see ObjectSerializer#serialize
 * @see DeserializationData
 */
@RequiredArgsConstructor
public class SerializationData implements TypedKeyWriter {

    @Getter @NonNull private final Configurer configurer;
    @Getter @NonNull private final SerdesContext context;
    private Map<String, Object> data = new LinkedHashMap<>();

    @Override
    public SerdesContext getWriterContext(@NonNull String key) {
        ConfigPath newPath = ObjectSerializer.VALUE.equals(key) ? this.context.getPath() : this.context.getPath().property(key);
        return SerdesContext.of(this.configurer, this.context.getConfigContext(), null).withPath(newPath);
    }

    /**
     * Removes all currently stored serialization data.
     */
    public void clear() {
        this.data.clear();
    }

    /**
     * @return unmodifiable map of current serialization data
     */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(this.data);
    }

    // ==================== CORE WRITE METHOD ====================

    @Override
    public void setRaw(@NonNull String key, Object value) {
        this.data.put(key, value);
    }

    // ==================== VALUE METHODS ====================

    /**
     * Replaces serialization result with the new raw value.
     *
     * @param value the new serialization value
     */
    public void setValueRaw(Object value) {
        this.clear();
        this.setRaw(ObjectSerializer.VALUE, value);
    }

    /**
     * Replaces serialization result with the new value (auto-simplified).
     *
     * @param value the new serialization value
     */
    public void setValue(Object value) {
        this.clear();
        this.set(ObjectSerializer.VALUE, value);
    }

    /**
     * Replaces serialization result with the new value using explicit type.
     *
     * @param value       target value
     * @param genericType type declaration for simplification
     */
    public void setValue(Object value, @NonNull GenericsDeclaration genericType) {
        this.clear();
        this.set(ObjectSerializer.VALUE, value, genericType);
    }

    /**
     * Replaces serialization result with the new value using explicit type.
     *
     * @param value     target value
     * @param valueType type for simplification
     * @param <T>       type of value
     */
    public <T> void setValue(Object value, @NonNull Class<T> valueType) {
        this.clear();
        this.set(ObjectSerializer.VALUE, value, valueType);
    }

    /**
     * Replaces serialization result with the new collection.
     *
     * @param collection  target collection
     * @param genericType type declaration for simplification
     */
    public void setValueCollection(Collection<?> collection, @NonNull GenericsDeclaration genericType) {
        this.clear();
        this.setCollection(ObjectSerializer.VALUE, collection, genericType);
    }

    /**
     * Replaces serialization result with the new collection.
     *
     * @param collection          target collection
     * @param collectionValueType element type for simplification
     * @param <T>                 type of collection values
     */
    public <T> void setValueCollection(Collection<?> collection, @NonNull Class<T> collectionValueType) {
        this.clear();
        this.setCollection(ObjectSerializer.VALUE, collection, collectionValueType);
    }

    /**
     * Replaces serialization result with the new array.
     *
     * @param array          target array
     * @param arrayValueType element type for simplification
     * @param <T>            type of array values
     */
    public <T> void setValueArray(T[] array, @NonNull Class<T> arrayValueType) {
        this.clear();
        this.setArray(ObjectSerializer.VALUE, array, arrayValueType);
    }

    // ==================== UTILITY ====================

    /**
     * Adds formatted numeric value.
     *
     * @param key    target key
     * @param format target format
     * @param value  target value
     */
    public void setFormatted(@NonNull String key, @NonNull String format, Object value) {
        if (value == null) {
            this.setRaw(key, null);
            return;
        }
        this.set(key, String.format(format, value));
    }

    // ==================== DEPRECATED ALIASES ====================

    /** @deprecated Use {@link #setRaw(String, Object)} */
    @Deprecated
    public void addRaw(@NonNull String key, Object value) {
        this.setRaw(key, value);
    }

    /** @deprecated Use {@link #set(String, Object)} */
    @Deprecated
    public void add(@NonNull String key, Object value) {
        this.set(key, value);
    }

    /** @deprecated Use {@link #set(String, Object, GenericsDeclaration)} */
    @Deprecated
    public void add(@NonNull String key, Object value, @NonNull GenericsDeclaration genericType) {
        this.set(key, value, genericType);
    }

    /** @deprecated Use {@link #set(String, Object, Class)} */
    @Deprecated
    public <T> void add(@NonNull String key, Object value, @NonNull Class<T> valueType) {
        this.set(key, value, valueType);
    }

    /** @deprecated Use {@link #setCollection(String, Collection, GenericsDeclaration)} */
    @Deprecated
    public void addCollection(@NonNull String key, Collection<?> collection, @NonNull GenericsDeclaration genericType) {
        this.setCollection(key, collection, genericType);
    }

    /** @deprecated Use {@link #setCollection(String, Collection, Class)} */
    @Deprecated
    public <T> void addCollection(@NonNull String key, Collection<?> collection, @NonNull Class<T> collectionValueType) {
        this.setCollection(key, collection, collectionValueType);
    }

    /** @deprecated Use {@link #setArray(String, Object[], Class)} */
    @Deprecated
    public <T> void addArray(@NonNull String key, T[] array, @NonNull Class<T> arrayValueType) {
        this.setArray(key, array, arrayValueType);
    }

    /** @deprecated Use {@link #setMap(String, Map, GenericsDeclaration)} */
    @Deprecated
    public void addAsMap(@NonNull String key, Map<?, ?> map, @NonNull GenericsDeclaration genericType) {
        this.setMap(key, map, genericType);
    }

    /** @deprecated Use {@link #setMap(String, Map, Class, Class)} */
    @Deprecated
    public <K, V> void addAsMap(@NonNull String key, Map<K, V> map, @NonNull Class<K> mapKeyType, @NonNull Class<V> mapValueType) {
        this.setMap(key, map, mapKeyType, mapValueType);
    }

    /** @deprecated Use {@link #setFormatted(String, String, Object)} */
    @Deprecated
    public void addFormatted(@NonNull String key, @NonNull String format, Object value) {
        this.setFormatted(key, format, value);
    }
}
