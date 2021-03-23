package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.util.*;

public class SerializationData {

    private Map<String, Object> data = new LinkedHashMap<>();
    private final Configurer configurer;

    public SerializationData(Configurer configurer) {
        this.configurer = configurer;
    }

    public Map<String, Object> asMap() {
        return this.data;
    }

    public void add(String key, Object value) {
        value = this.configurer.simplify(value, null);
        this.data.put(key, value);
    }

    public <T> void add(String key, Object value, Class<T> clazz) {
        GenericsDeclaration genericType = GenericsDeclaration.of(clazz);
        Object object = this.configurer.simplify(value, genericType);
        this.data.put(key, object);
    }

    public <T> void addCollection(String key, Collection<?> collection, Class<T> collectionClazz) {
        GenericsDeclaration genericType = GenericsDeclaration.of(collection, Collections.singletonList(collectionClazz));
        Object object = this.configurer.simplifyCollection(collection, genericType);
        this.data.put(key, object);
    }

    @SuppressWarnings("unchecked")
    public <K, V> void addAsMap(String key, Map<K, V> map, Class<K> keyClazz, Class<V> valueClazz) {
        GenericsDeclaration genericType = GenericsDeclaration.of(map, Arrays.asList(keyClazz, valueClazz));
        Object object = this.configurer.simplifyMap((Map<Object, Object>) map, genericType);
        this.data.put(key, object);
    }

    public void addFormatted(String key, String format, Object value) {
        if (value == null) {
            this.data.put(key, null);
            return;
        }
        this.add(key, String.format(format, value));
    }
}
