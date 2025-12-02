package eu.okaeri.configs.binary;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Binary configurer using Java's built-in ObjectOutputStream/ObjectInputStream.
 * <p>
 * This produces binary files containing serialized Java objects.
 * Values are simplified to basic JDK types (Map, List, String, primitives, wrappers)
 * before serialization, same as other configurers.
 * </p>
 */
public class BinaryConfigurer extends Configurer {

    private Map<String, Object> map = new LinkedHashMap<>();

    public BinaryConfigurer() {
    }

    public BinaryConfigurer(@NonNull Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("bin");
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.map.put(key, simplified);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.map.put(key, value);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {
        return this.map.remove(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.map.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(inputStream)) {
            Object decoded = ois.readObject();
            if (decoded instanceof Map) {
                this.map = (Map<String, Object>) decoded;
            } else {
                throw new IllegalStateException("Binary root element must be a map structure, got: " +
                    (decoded == null ? "null" : decoded.getClass().getName()));
            }
        }
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        try (ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(this.map);
        }
    }
}
