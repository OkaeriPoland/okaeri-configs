package eu.okaeri.configs.serdes;

import eu.okaeri.configs.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class DeserializationData {

    private Map<String, Object> data;
    private Configurer configurer;

    public Map<String, Object> asMap() {
        return this.data;
    }

    public boolean containsKey(String key) {
        return this.data.containsKey(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        Object object = this.data.get(key);
        return this.configurer.resolveType(object, GenericsDeclaration.of(object), clazz, null);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAsList(String key, Class<T> listType) {
        Object object = this.data.get(key);
        GenericsDeclaration genericType = GenericsDeclaration.of(List.class, Collections.singletonList(listType));
        return this.configurer.resolveType(object, GenericsDeclaration.of(object), List.class, genericType);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getAsMap(String key, Class<K> keyClazz, Class<V> valueClazz) {
        Object object = this.data.get(key);
        GenericsDeclaration genericType = GenericsDeclaration.of(Map.class, Arrays.asList(keyClazz, valueClazz));
        return (Map<K, V>) this.configurer.resolveType(object, GenericsDeclaration.of(object), Map.class, genericType);
    }
}
