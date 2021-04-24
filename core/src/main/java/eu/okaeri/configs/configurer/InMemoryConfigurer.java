package eu.okaeri.configs.configurer;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class InMemoryConfigurer extends Configurer {

    private Map<String, Object> map = new LinkedHashMap<>();

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        this.map.put(key, value);
    }

    @Override
    public Object getValue(String key) {
        return this.map.get(key);
    }

    @Override
    public boolean keyExists(String key) {
        return this.map.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    @Override
    public void load(InputStream inputStream, ConfigDeclaration declaration) throws Exception {
    }

    @Override
    public void write(OutputStream outputStream, ConfigDeclaration declaration) throws Exception {
    }
}
