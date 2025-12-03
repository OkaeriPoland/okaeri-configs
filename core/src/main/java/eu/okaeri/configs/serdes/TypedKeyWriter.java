package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Interface for writing typed values by key with automatic simplification.
 * <p>
 * Implementations must provide {@link #setRaw(String, Object)}, {@link #getConfigurer()},
 * and {@link #getWriterContext(String)}. All other methods have default implementations.
 *
 * @see SerializationData
 * @see eu.okaeri.configs.migrate.view.RawConfigView
 */
public interface TypedKeyWriter {

    /**
     * @return the configurer used for simplification
     */
    Configurer getConfigurer();

    /**
     * Gets the serialization context for a specific key.
     *
     * @param key the key being written to
     * @return the context for simplification
     */
    SerdesContext getWriterContext(@NonNull String key);

    /**
     * Sets a raw value at the specified key without any transformation.
     *
     * @param key the key
     * @param value the raw value
     */
    void setRaw(@NonNull String key, Object value);

    /**
     * Gets the raw value at the specified key (for returning old value on set).
     * Implementations that don't track previous values can return null.
     * This method should not throw if the key doesn't exist.
     *
     * @param key the key
     * @return the raw value, or null if not found or not supported
     */
    default Object getRawOrNull(@NonNull String key) {
        return null;
    }

    /**
     * Sets a value at the specified key with automatic simplification.
     *
     * @param key the key
     * @param value the value (will be auto-simplified)
     * @return the previous raw value, or null
     */
    default Object set(@NonNull String key, Object value) {
        Object old = this.getRawOrNull(key);
        value = this.getConfigurer().simplify(value, null, this.getWriterContext(key), true);
        this.setRaw(key, value);
        return old;
    }

    /**
     * Sets a value at the specified key with explicit type declaration.
     *
     * @param key the key
     * @param value the value
     * @param genericType the type declaration for simplification
     * @return the previous raw value, or null
     */
    default Object set(@NonNull String key, Object value, @NonNull GenericsDeclaration genericType) {
        Object old = this.getRawOrNull(key);
        value = this.getConfigurer().simplify(value, genericType, this.getWriterContext(key), true);
        this.setRaw(key, value);
        return old;
    }

    /**
     * Sets a value at the specified key with explicit type.
     *
     * @param key the key
     * @param value the value
     * @param type the type for simplification
     * @param <T> the value type
     * @return the previous raw value, or null
     */
    default <T> Object set(@NonNull String key, Object value, @NonNull Class<T> type) {
        return this.set(key, value, GenericsDeclaration.of(type));
    }

    /**
     * Sets a collection at the specified key with full type declaration.
     *
     * @param key the key
     * @param collection the collection
     * @param genericType the collection type declaration
     */
    default void setCollection(@NonNull String key, Collection<?> collection, @NonNull GenericsDeclaration genericType) {
        if (collection == null) {
            this.setRaw(key, null);
            return;
        }
        Object object = this.getConfigurer().simplifyCollection(collection, genericType, this.getWriterContext(key), true);
        this.setRaw(key, object);
    }

    /**
     * Sets a collection at the specified key with element type resolution.
     *
     * @param key the key
     * @param collection the collection
     * @param elementType the element type for simplification
     * @param <T> the element type
     */
    default <T> void setCollection(@NonNull String key, Collection<?> collection, @NonNull Class<T> elementType) {
        if (collection == null) {
            this.setRaw(key, null);
            return;
        }
        GenericsDeclaration genericType = GenericsDeclaration.of(collection, Collections.singletonList(elementType));
        this.setCollection(key, collection, genericType);
    }

    /**
     * Sets an array at the specified key with element type resolution.
     *
     * @param key the key
     * @param array the array
     * @param elementType the element type for simplification
     * @param <T> the element type
     */
    default <T> void setArray(@NonNull String key, T[] array, @NonNull Class<T> elementType) {
        if (array == null) {
            this.setRaw(key, null);
            return;
        }
        this.setCollection(key, Arrays.asList(array), elementType);
    }

    /**
     * Sets a map at the specified key with full type declaration.
     *
     * @param key the key
     * @param map the map
     * @param genericType the map type declaration
     */
    @SuppressWarnings("unchecked")
    default void setMap(@NonNull String key, Map<?, ?> map, @NonNull GenericsDeclaration genericType) {
        if (map == null) {
            this.setRaw(key, null);
            return;
        }
        Object object = this.getConfigurer().simplifyMap((Map<Object, Object>) map, genericType, this.getWriterContext(key), true);
        this.setRaw(key, object);
    }

    /**
     * Sets a map at the specified key with key and value type resolution.
     *
     * @param key the key
     * @param map the map
     * @param keyType the map key type
     * @param valueType the map value type
     * @param <K> the key type
     * @param <V> the value type
     */
    default <K, V> void setMap(@NonNull String key, Map<K, V> map, @NonNull Class<K> keyType, @NonNull Class<V> valueType) {
        if (map == null) {
            this.setRaw(key, null);
            return;
        }
        GenericsDeclaration genericType = GenericsDeclaration.of(map, Arrays.asList(keyType, valueType));
        this.setMap(key, map, genericType);
    }
}
