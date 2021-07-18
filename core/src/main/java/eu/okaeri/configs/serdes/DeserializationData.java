package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class DeserializationData {

    private Map<String, Object> data;
    @Getter private Configurer configurer;

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(this.data);
    }

    public boolean containsKey(@NonNull String key) {
        return this.data.containsKey(key);
    }

    public Object getRaw(@NonNull String key) {
        return this.data.get(key);
    }

    public Object getDirect(@NonNull String key, @NonNull GenericsDeclaration genericType) {
        Object object = this.data.get(key);
        return this.configurer.resolveType(object, GenericsDeclaration.of(object), genericType.getType(), genericType);
    }

    public <T> T get(@NonNull String key, @NonNull Class<T> clazz) {
        Object object = this.data.get(key);
        return this.configurer.resolveType(object, GenericsDeclaration.of(object), clazz, null);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAsList(@NonNull String key, @NonNull Class<T> listType) {
        Object object = this.data.get(key);
        GenericsDeclaration genericType = GenericsDeclaration.of(List.class, Collections.singletonList(listType));
        return this.configurer.resolveType(object, GenericsDeclaration.of(object), List.class, genericType);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getAsMap(@NonNull String key, @NonNull Class<K> keyClazz, @NonNull Class<V> valueClazz) {
        Object object = this.data.get(key);
        GenericsDeclaration genericType = GenericsDeclaration.of(Map.class, Arrays.asList(keyClazz, valueClazz));
        return (Map<K, V>) this.configurer.resolveType(object, GenericsDeclaration.of(object), Map.class, genericType);
    }
}
