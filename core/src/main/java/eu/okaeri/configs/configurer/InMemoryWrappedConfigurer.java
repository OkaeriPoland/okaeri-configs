package eu.okaeri.configs.configurer;

import eu.okaeri.configs.schema.GenericsDeclaration;

import java.util.Map;

public class InMemoryWrappedConfigurer extends WrappedConfigurer {

    private final Map<String, Object> map;

    public InMemoryWrappedConfigurer(Configurer configurer, Map<String, Object> map) {
        super(configurer);
        this.map = map;
    }

    @Override
    public boolean keyExists(String key) {
        return this.map.containsKey(key);
    }

    @Override
    public Object getValue(String key) {
        return this.map.get(key);
    }

    @Override
    public <T> T getValue(String key, Class<T> clazz, GenericsDeclaration genericType) {
        Object value = this.getValue(key);
        if (value == null) return null;
        return this.resolveType(value, GenericsDeclaration.of(value), clazz, genericType);
    }

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type) {
        this.map.put(key, value);
    }
}
