package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.NonNull;

import java.util.*;

public class SerializationData {

    private Map<String, Object> data = new LinkedHashMap<>();
    private final Configurer configurer;

    public SerializationData(@NonNull Configurer configurer) {
        this.configurer = configurer;
    }

    /**
     * @return immutable map of current serialization data
     */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(this.data);
    }

    /**
     * Adds value to the serialization data under specific key.
     * Provided value is simplified using attached Configurer.
     *
     * @param key   target key
     * @param value target value
     */
    public void add(@NonNull String key, Object value) {
        value = this.configurer.simplify(value, null, true);
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
     * <p>
     * This method is intended to use when adding complex generic
     * types as for example {@code Map<SomeType, Map<String, SomeState>>}.
     *
     * @param key         target key
     * @param value       target value
     * @param genericType type declaration of value for simplification process
     */
    public void add(@NonNull String key, Object value, @NonNull GenericsDeclaration genericType) {
        value = this.configurer.simplify(value, genericType, true);
        this.data.put(key, value);
    }

    public <T> void add(@NonNull String key, Object value, @NonNull Class<T> clazz) {
        GenericsDeclaration genericType = GenericsDeclaration.of(clazz);
        Object object = this.configurer.simplify(value, genericType, true);
        this.data.put(key, object);
    }

    public <T> void addCollection(@NonNull String key, Collection<?> collection, @NonNull Class<T> collectionClazz) {
        GenericsDeclaration genericType = GenericsDeclaration.of(collection, Collections.singletonList(collectionClazz));
        Object object = this.configurer.simplifyCollection(collection, genericType, true);
        this.data.put(key, object);
    }

    @SuppressWarnings("unchecked")
    public <K, V> void addAsMap(@NonNull String key, Map<K, V> map, @NonNull Class<K> keyClazz, @NonNull Class<V> valueClazz) {
        GenericsDeclaration genericType = GenericsDeclaration.of(map, Arrays.asList(keyClazz, valueClazz));
        Object object = this.configurer.simplifyMap((Map<Object, Object>) map, genericType, true);
        this.data.put(key, object);
    }

    public void addFormatted(@NonNull String key, @NonNull String format, Object value) {
        if (value == null) {
            this.data.put(key, null);
            return;
        }
        this.add(key, String.format(format, value));
    }
}
