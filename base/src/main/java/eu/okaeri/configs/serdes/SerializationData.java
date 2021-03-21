package eu.okaeri.configs.serdes;

import java.util.LinkedHashMap;
import java.util.Map;

public class SerializationData {

    private Map<String, Object> data = new LinkedHashMap<>();

    public Map<String, Object> asMap() {
        return this.data;
    }

    public void add(String key, Object value) {
        this.data.put(key, value);
    }

    public void addFormatted(String key, String format, Object value) {
        if (value == null) {
            this.data.put(key, null);
            return;
        }
        this.data.put(key, String.format(format, value));
    }
}
