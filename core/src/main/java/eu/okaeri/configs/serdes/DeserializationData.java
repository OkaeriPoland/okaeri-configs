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
     * @return unmodifiable map of current deserialization data
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
     * This one is hacky, see also more specific methods:
     * - {@link #get(String, Class)}
     * - {@link #getAsList(String, Class)}
     * - {@link #getAsMap(String, Class, Class)}
     *
     * @param key         target key
     * @param genericType target type for value
     * @param <T>         type of transformed value
     * @return transformed value or null
     */
    @SuppressWarnings("unchecked")
    public <T> T getDirect(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        Object object = this.data.get(key);
        return (T) this.configurer.resolveType(object, null, genericType.getType(), genericType);
    }

    /**
     * Gets data under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param key       target key
     * @param valueType target class for value
     * @param <T>       type of transformed value
     * @return transformed value or null
     */
    public <T> T get(@NonNull String key, @NonNull Class<T> valueType) {
        Object object = this.data.get(key);
        return this.configurer.resolveType(object, null, valueType, null);
    }

    /**
     * Gets collection under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     * <p>
     * Allows to specify extended generic type that can be used
     * to resolve more complex collections, e.g. {@code LinkedList<Map<String, SomeState>>}
     *
     * @param key         target key
     * @param genericType target type declaration for collection
     * @param <T>         type of collection
     * @return transformed collection or null
     */
    public <T> Collection<T> getAsCollection(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        if (!Collections.class.isAssignableFrom(genericType.getType())) {
            throw new IllegalArgumentException("genericType.type must be a superclass of Collection");
        }
        return this.getDirect(key, genericType);
    }

    /**
     * Gets list under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param key           target key
     * @param listValueType target type for list
     * @param <T>           target type for list
     * @return transformed list or null
     */
    public <T> List<T> getAsList(@NonNull String key, @NonNull Class<T> listValueType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(List.class, Collections.singletonList(listValueType));
        return (List<T>) this.getAsCollection(key, genericType);
    }

    /**
     * Gets map under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param key          target key
     * @param mapKeyType   target type for map keys
     * @param mapValueType target type for map values
     * @param <K>          type of map keys
     * @param <V>          type of map values
     * @return transformed map or null
     */
    public <K, V> Map<K, V> getAsMap(@NonNull String key, @NonNull Class<K> mapKeyType, @NonNull Class<V> mapValueType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(Map.class, Arrays.asList(mapKeyType, mapValueType));
        return this.getDirect(key, genericType);
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
     * @param <K>         type of map keys
     * @param <V>         type of map values
     * @return transformed map or null
     */
    public <K, V> Map<K, V> getAsMap(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        if (!Map.class.isAssignableFrom(genericType.getType())) {
            throw new IllegalArgumentException("genericType.type must be a superclass of Map");
        }
        return this.getDirect(key, genericType);
    }
}
