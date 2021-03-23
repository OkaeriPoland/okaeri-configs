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
        return this.configurer.resolveType(object, GenericsDeclaration.single(object), clazz, null);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAsList(String key, Class<T> listType) {
        GenericsDeclaration genericType = new GenericsDeclaration(List.class, Collections.singletonList(new GenericsDeclaration(listType)));
        Object object = this.data.get(key);
        return this.configurer.resolveType(object, GenericsDeclaration.single(object), List.class, genericType);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getAsMap(String key, Class<K> keyClazz, Class<V> valueClazz) {
        GenericsDeclaration genericType = new GenericsDeclaration(Map.class, Arrays.asList(new GenericsDeclaration(keyClazz), new GenericsDeclaration(valueClazz)));
        Object object = this.data.get(key);
        return (Map<K, V>) this.configurer.resolveType(object, GenericsDeclaration.single(object), Map.class, genericType);
    }
}
