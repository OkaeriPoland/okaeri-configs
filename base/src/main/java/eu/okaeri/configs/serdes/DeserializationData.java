package eu.okaeri.configs.serdes;

import eu.okaeri.configs.transformer.TransformerRegistry;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class DeserializationData {

    private Map<String, Object> data;

    public Map<String, Object> asMap() {
        return this.data;
    }

    public <T> T get(String key, Class<T> clazz) {
        return TransformerRegistry.transform(this.data.get(key), clazz);
    }
}
