package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class SerializationData {

    @Getter @NonNull private final Configurer configurer;
    @Getter @NonNull private final SerdesContext context;
    private Map<String, Object> data = new LinkedHashMap<>();

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

    /**
     * Adds value to the serialization data under specific key.
     *
     * @param key   target key
     * @param value target value
     */
    public void addRaw(@NonNull String key, Object value) {
        this.data.put(key, value);
    }

    /**
     * Adds value to the serialization data under specific key.
     * Provided value is simplified using attached Configurer.
     *
     * @param key   target key
     * @param value target value
     */
    public void add(@NonNull String key, Object value) {
        value = this.configurer.simplify(value, null, SerdesContext.of(this.configurer), true);
        this.data.put(key, value);
    }

    /**
     * Adds value to the serialization data under specific key.
     * Provided value is simplified using attached Configurer.
     * <p>
     * Allows to provide {@link GenericsDeclaration} for simplification process.
     * If possible, it is recommended to use one of generic methods:
     * - {@link #add(String, Object, Class)}
     * - {@link #addCollection(String, Collection, Class)}
     * - {@link #addAsMap(String, Map, Class, Class)}
     * There are also dedicated methods for complex collections/maps:
     * - {@link #addCollection(String, Collection, GenericsDeclaration)}
     * - {@link #addAsMap(String, Map, GenericsDeclaration)}
     *
     * @param key         target key
     * @param value       target value
     * @param genericType type declaration of value for simplification process
     */
    public void add(@NonNull String key, Object value, @NonNull GenericsDeclaration genericType) {
        value = this.configurer.simplify(value, genericType, SerdesContext.of(this.configurer), true);
        this.data.put(key, value);
    }

    /**
     * Adds value to the serialization data under specific key.
     * Provided value is simplified using attached Configurer.
     * <p>
     * This method allows to narrow target simplification type
     * and is recommended to be used with non-primitive classes.
     * <p>
     * Specifying target simplification type allows to make sure
     * correct serializer is used, e.g. interface type instead
     * of some implementation type that would otherwise inferred.
     *
     * @param key       target key
     * @param value     target value
     * @param valueType type of value for simplification process
     * @param <T>       type of value
     */
    public <T> void add(@NonNull String key, Object value, @NonNull Class<T> valueType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(valueType);
        this.add(key, value, genericType);
    }

    /**
     * Adds collection of values to the serialization data under specific key.
     * Provided collection of values is simplified using attached Configurer.
     * <p>
     * Allows to provide {@link GenericsDeclaration} for simplification process.
     * For simple collections it is advised to use {@link #addCollection(String, Collection, Class)}
     * <p>
     * This method is intended to be used when adding complex generic
     * types as for example {@code List<Map<String, SomeState>>}.
     *
     * @param key         target key
     * @param collection  target collection
     * @param genericType type declaration of value for simplification process
     */
    public void addCollection(@NonNull String key, Collection<?> collection, @NonNull GenericsDeclaration genericType) {
        Object object = this.configurer.simplifyCollection(collection, genericType, SerdesContext.of(this.configurer), true);
        this.data.put(key, object);
    }

    /**
     * Adds collection of values to the serialization data under specific key.
     * Provided collection of values is simplified using attached Configurer.
     * <p>
     * This method allows to narrow target simplification type
     * and is recommended to be used with collections.
     * <p>
     * Specifying target simplification type allows to make sure
     * correct serializer is used, e.g. interface type instead
     * of some implementation type that would otherwise inferred.
     *
     * @param key                 target key
     * @param collection          target collection
     * @param collectionValueType type of collection for simplification process
     * @param <T>                 type of collection values
     */
    public <T> void addCollection(@NonNull String key, Collection<?> collection, @NonNull Class<T> collectionValueType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(collection, Collections.singletonList(collectionValueType));
        this.addCollection(key, collection, genericType);
    }

    /**
     * Adds array of values to the serialization data under specific key.
     * Provided array of values is simplified using attached Configurer.
     * <p>
     * This method allows to narrow target simplification type
     * and is recommended to be used with arrays.
     * <p>
     * Specifying target simplification type allows to make sure
     * correct serializer is used, e.g. interface type instead
     * of some implementation type that would otherwise inferred.
     *
     * @param key            target key
     * @param array          target array
     * @param arrayValueType type of array for simplification process
     * @param <T>            type of array values
     */
    public <T> void addArray(@NonNull String key, T[] array, @NonNull Class<T> arrayValueType) {
        this.addCollection(key, (array == null) ? null : Arrays.asList(array), arrayValueType);
    }

    /**
     * Adds map to the serialization data under specific key.
     * Provided map is simplified using attached Configurer.
     * <p>
     * Allows to provide {@link GenericsDeclaration} for simplification process.
     * For simple collections it is advised to use {@link #addAsMap(String, Map, Class, Class)}
     * <p>
     * This method is intended to be used when adding complex generic
     * types as for example {@code Map<SomeType, Map<String, SomeState>>}.
     *
     * @param key         target key
     * @param map         target map
     * @param genericType type of map for simplification process
     */
    @SuppressWarnings("unchecked")
    public void addAsMap(@NonNull String key, Map<?, ?> map, @NonNull GenericsDeclaration genericType) {
        Object object = this.configurer.simplifyMap((Map<Object, Object>) map, genericType, SerdesContext.of(this.configurer), true);
        this.data.put(key, object);
    }

    /**
     * Adds map to the serialization data under specific key.
     * Provided map is simplified using attached Configurer.
     * <p>
     * This method allows to narrow target simplification type
     * and is recommended to be used with maps.
     * <p>
     * Specifying target simplification type allows to make sure
     * correct serializer is used, e.g. interface type instead
     * of some implementation type that would otherwise inferred.
     *
     * @param key          target key
     * @param map          target map
     * @param mapKeyType   type of key for simplification process
     * @param mapValueType type of value for simplification process
     * @param <K>          type of map keys
     * @param <V>          type of map values
     */
    @SuppressWarnings("unchecked")
    public <K, V> void addAsMap(@NonNull String key, Map<K, V> map, @NonNull Class<K> mapKeyType, @NonNull Class<V> mapValueType) {
        GenericsDeclaration genericType = GenericsDeclaration.of(map, Arrays.asList(mapKeyType, mapValueType));
        this.addAsMap(key, map, genericType);
    }

    /**
     * Adds numeric value to the serialization data under specific key.
     * Provided value is formatted using {@link String#format(String, Object...)}
     *
     * @param key    target key
     * @param format target format
     * @param value  target value
     */
    public void addFormatted(@NonNull String key, @NonNull String format, Object value) {
        if (value == null) {
            this.data.put(key, null);
            return;
        }
        this.add(key, String.format(format, value));
    }
}
