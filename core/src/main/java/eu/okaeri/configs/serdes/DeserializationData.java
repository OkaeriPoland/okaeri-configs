package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.*;

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
public class DeserializationData {

    @NonNull private Map<String, Object> data;
    @Getter @NonNull private Configurer configurer;
    @Getter @NonNull private SerdesContext context;

    /**
     * Creates a context for nested resolution with path but WITHOUT field.
     * This prevents reusing custom serializers recursively.
     */
    private SerdesContext contextForKey(@NonNull String key) {
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
     * <p>
     * Use this method when in need to produce multiple
     * types of the output from a single serializer.
     *
     * @return state of value object presence
     */
    public boolean isValue() {
        return this.containsKey(ObjectSerializer.VALUE);
    }

    /**
     * Gets data under value object key without any transformations.
     * May differ slightly depending on the used Configurer backend.
     * <p>
     * Not recommended in the most cases. Provided without
     * any guarantees for the shape of returned data.
     *
     * @return value or null
     */
    public Object getValueRaw() {
        return this.getRaw(ObjectSerializer.VALUE);
    }

    /**
     * Gets data under value object key applying type resolving a.k.a.
     * deserialization for the specified {@link GenericsDeclaration}.
     * <p>
     * This one is hacky, see also:
     * - {@link #getValue(Class)}
     *
     * @param genericType target type for value
     * @param <T>         type of transformed value
     * @return transformed value or null
     */
    public <T> T getValueDirect(@NonNull GenericsDeclaration genericType) {
        return this.getDirect(ObjectSerializer.VALUE, genericType);
    }

    /**
     * Gets data under value object key applying type resolving
     * a.k.a. deserialization for the specified class.
     * <p>
     * Use this method when in need to produce multiple
     * types of the output from a single serializer.
     *
     * @param valueType target class for value
     * @param <T>       type of transformed value
     * @return transformed value or null
     */
    public <T> T getValue(@NonNull Class<T> valueType) {
        return this.get(ObjectSerializer.VALUE, valueType);
    }

    /**
     * Gets collection under value object key applying type resolving a.k.a.
     * deserialization for the specified class.
     * <p>
     * Allows to specify extended generic type that can be used
     * to resolve more complex collections, e.g. {@code LinkedList<Map<String, SomeState>>}
     *
     * @param genericType target type declaration for collection
     * @param <T>         type of collection
     * @return transformed collection or null
     */
    public <T> Collection<T> getValueAsCollection(@NonNull GenericsDeclaration genericType) {
        return this.getAsCollection(ObjectSerializer.VALUE, genericType);
    }

    /**
     * Gets list under value object key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param listValueType target type for list
     * @param <T>           target type for list
     * @return transformed list or null
     */
    public <T> List<T> getValueAsList(@NonNull Class<T> listValueType) {
        return this.getAsList(ObjectSerializer.VALUE, listValueType);
    }

    /**
     * Gets set under value object key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param setValueType target type for set
     * @param <T>           target type for set
     * @return transformed set or null
     */
    public <T> Set<T> getValueAsSet(@NonNull Class<T> setValueType) {
        return this.getAsSet(ObjectSerializer.VALUE, setValueType);
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
        if (!this.isValue() && ObjectSerializer.VALUE.equals(key)) {
            return this.asMap();
        }
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
        Object object = this.getRaw(key);
        return (T) this.configurer.resolveType(object, null, genericType.getType(), genericType, this.contextForKey(key));
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
        Object object = this.getRaw(key);
        return this.configurer.resolveType(object, null, valueType, null, this.contextForKey(key));
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
        if (!Collection.class.isAssignableFrom(genericType.getType())) {
            throw new IllegalArgumentException("genericType.type must be a superclass of Collection: " + genericType);
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
     * Gets set under specific key applying type resolving a.k.a.
     * deserialization for the specified class.
     *
     * @param key           target key
     * @param setValueType target type for set
     * @param <T>           target type for set
     * @return transformed set or null
     */
    public <T> Set<T> getAsSet(@NonNull String key, @NonNull Class<T> setValueType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(Set.class, Collections.singletonList(setValueType));
        return (Set<T>) this.getAsCollection(key, genericType);
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
            throw new IllegalArgumentException("genericType.type must be a superclass of Map: " + genericType);
        }
        return this.getDirect(key, genericType);
    }
}
