package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.*;

@AllArgsConstructor
public class DeserializationData {

    private Map<String, Object> data;
    @Getter private Configurer configurer;

    /**
     * @return immutable map of current deserialization data
     */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(this.data);
    }

    /**
     * Checks if value under specific key is available.
     *
     * @param key target key
     * @return true if key is present in deserialization data, otherwise false
     */
    public boolean containsKey(@NonNull String key) {
        return this.data.containsKey(key);
    }

    /**
     * Gets data under specific key without any transformations.
     * May differ slightly depending on the used Configurer backend.
     * <p>
     * Not recommended in the most cases. Provided without
     * any guarantees for the shape of returned data.
     *
     * @param key target key
     * @return value or null
     */
    public Object getRaw(@NonNull String key) {
        return this.data.get(key);
    }

    /**
     * Gets data under specific key applying type resolving a.k.a.
     * deserialization for the specified {@link GenericsDeclaration}.
     * <p>
     * See also more specific methods:
     * - {@link #get(String, Class)}
     * - {@link #getAsList(String, Class)}
     * - {@link #getAsMap(String, Class, Class)}
     *
     * @param key         target key
     * @param genericType target type for value
     * @return transformed value or null
     */
    public Object getDirect(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        Object object = this.data.get(key);
        return this.configurer.resolveType(object, GenericsDeclaration.of(object), genericType.getType(), genericType);
    }

    /**
     * Gets data under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param key       target key
     * @param valueType target type for value
     * @return transformed value or null
     */
    public <T> T get(@NonNull String key, @NonNull Class<T> valueType) {
        Object object = this.data.get(key);
        return this.configurer.resolveType(object, GenericsDeclaration.of(object), valueType, null);
    }

    /**
     * Gets collection under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     * <p>
     * Allows to specify extended generic type that can be used
     * to resolve more complex collections, e.g. {@code LinkedList<Map<String, SomeState>>}
     *
     * @param key                 target key
     * @param collectionValueType target type for collection
     * @param genericType         target type declaration for collection
     * @return transformed collection or null
     */
    @SuppressWarnings("unchecked")
    public <T> Collection<T> getAsCollection(@NonNull String key, @NonNull Class<T> collectionValueType, @NonNull GenericsDeclaration genericType) {
        if (!Collections.class.isAssignableFrom(genericType.getType())) {
            throw new IllegalArgumentException("genericType.type must be a superclass of Collection");
        }
        return (Collection<T>) this.getDirect(key, genericType);
    }

    /**
     * Gets list under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param key           target key
     * @param listValueType target type for list
     * @return transformed list or null
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getAsList(@NonNull String key, @NonNull Class<T> listValueType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(List.class, Collections.singletonList(listValueType));
        return (List<T>) this.getAsCollection(key, listValueType, genericType);
    }

    /**
     * Gets map under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param key          target key
     * @param mapKeyType   target type for map keys
     * @param mapValueType target type for map values
     * @return transformed map or null
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getAsMap(@NonNull String key, @NonNull Class<K> mapKeyType, @NonNull Class<V> mapValueType) {
        Object object = this.data.get(key);
        GenericsDeclaration genericType = GenericsDeclaration.of(Map.class, Arrays.asList(mapKeyType, mapValueType));
        return (Map<K, V>) this.configurer.resolveType(object, GenericsDeclaration.of(object), Map.class, genericType);
    }

    /**
     * Gets map under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     * <p>
     * Allows to specify extended generic type that can be used
     * to resolve more complex maps, e.g. {@code Map<String, Map<Integer, SomeState>>}
     *
     * @param key         target key
     * @param genericType target type declaration for map
     * @return transformed map or null
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getAsMap(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        if (!Map.class.isAssignableFrom(genericType.getType())) {
            throw new IllegalArgumentException("genericType.type must be a superclass of Map");
        }
        return (Map<K, V>) this.getDirect(key, genericType);
    }
}
