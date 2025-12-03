package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.NonNull;

import java.util.*;

/**
 * Interface for reading typed values by key with automatic type resolution.
 * <p>
 * Implementations must provide {@link #getRaw(String)}, {@link #getConfigurer()},
 * and {@link #getReaderContext(String)}. All other methods have default implementations.
 *
 * @see DeserializationData
 * @see eu.okaeri.configs.migrate.view.RawConfigView
 */
public interface TypedKeyReader {

    /**
     * @return the configurer used for type resolution
     */
    Configurer getConfigurer();

    /**
     * Gets the deserialization context for a specific key.
     *
     * @param key the key being read from
     * @return the context for type resolution
     */
    SerdesContext getReaderContext(@NonNull String key);

    /**
     * Gets the raw value at the specified key without any type transformation.
     *
     * @param key the key
     * @return the raw value, or null if not found
     */
    Object getRaw(@NonNull String key);

    /**
     * Gets the raw value at the specified key, or returns the default if not found.
     *
     * @param key the key
     * @param defaultValue the default value to return if key not found or value is null
     * @return the raw value, or defaultValue if not found
     */
    default Object getRawOr(@NonNull String key, Object defaultValue) {
        Object value = this.getRaw(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets the value at the specified key, resolved using a full {@link GenericsDeclaration}.
     * <p>
     * Use this for complex generic types that cannot be expressed with a simple class.
     *
     * @param key the key
     * @param genericType the target type declaration
     * @param <T> the target type
     * @return the resolved value, or null if not found
     */
    @SuppressWarnings("unchecked")
    default <T> T get(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        Object object = this.getRaw(key);
        return (T) this.getConfigurer().resolveType(object, null, genericType.getType(), genericType, this.getReaderContext(key));
    }

    /**
     * Gets the value at the specified key, resolved to the target type.
     *
     * @param key the key
     * @param type the target type class
     * @param <T> the target type
     * @return the resolved value, or null if not found
     */
    default <T> T get(@NonNull String key, @NonNull Class<T> type) {
        return this.get(key, GenericsDeclaration.of(type));
    }

    /**
     * Gets the value at the specified key, or returns the default if not found.
     *
     * @param key the key
     * @param type the target type class
     * @param defaultValue the default value to return if key not found or value is null
     * @param <T> the target type
     * @return the resolved value, or defaultValue if not found
     */
    default <T> T getOr(@NonNull String key, @NonNull Class<T> type, T defaultValue) {
        T value = this.get(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a list at the specified key with element type resolution.
     *
     * @param key the key
     * @param elementType the list element type
     * @param <T> the element type
     * @return the resolved list, or null if not found
     */
    default <T> List<T> getAsList(@NonNull String key, @NonNull Class<T> elementType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(List.class, Collections.singletonList(elementType));
        return this.get(key, genericType);
    }

    /**
     * Gets a set at the specified key with element type resolution.
     *
     * @param key the key
     * @param elementType the set element type
     * @param <T> the element type
     * @return the resolved set, or null if not found
     */
    default <T> Set<T> getAsSet(@NonNull String key, @NonNull Class<T> elementType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(Set.class, Collections.singletonList(elementType));
        return this.get(key, genericType);
    }

    /**
     * Gets a collection at the specified key using a full {@link GenericsDeclaration}.
     * <p>
     * Use this for complex collection types like {@code LinkedList<Map<String, Duration>>}.
     *
     * @param key the key
     * @param genericType the target collection type declaration
     * @param <T> the element type
     * @return the resolved collection, or null if not found
     * @throws IllegalArgumentException if genericType is not a Collection
     */
    default <T> Collection<T> getAsCollection(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        if (!Collection.class.isAssignableFrom(genericType.getType())) {
            throw new IllegalArgumentException("genericType.type must be a superclass of Collection: " + genericType);
        }
        return this.get(key, genericType);
    }

    /**
     * Gets a map at the specified key with key and value type resolution.
     *
     * @param key the key
     * @param keyType the map key type
     * @param valueType the map value type
     * @param <K> the key type
     * @param <V> the value type
     * @return the resolved map, or null if not found
     */
    default <K, V> Map<K, V> getAsMap(@NonNull String key, @NonNull Class<K> keyType, @NonNull Class<V> valueType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(Map.class, Arrays.asList(keyType, valueType));
        return this.get(key, genericType);
    }

    /**
     * Gets a map at the specified key using a full {@link GenericsDeclaration}.
     * <p>
     * Use this for complex map types like {@code Map<String, List<Duration>>}.
     *
     * @param key the key
     * @param genericType the target map type declaration
     * @param <K> the key type
     * @param <V> the value type
     * @return the resolved map, or null if not found
     * @throws IllegalArgumentException if genericType is not a Map
     */
    default <K, V> Map<K, V> getAsMap(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        if (!Map.class.isAssignableFrom(genericType.getType())) {
            throw new IllegalArgumentException("genericType.type must be a superclass of Map: " + genericType);
        }
        return this.get(key, genericType);
    }
}
