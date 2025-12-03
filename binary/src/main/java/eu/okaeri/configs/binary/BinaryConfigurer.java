package eu.okaeri.configs.binary;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
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

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("bin");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(inputStream)) {
            Object decoded = ois.readObject();
            if (decoded instanceof Map) {
                return (Map<String, Object>) decoded;
            } else {
                throw new IllegalStateException("Binary root element must be a map structure, got: " +
                    (decoded == null ? "null" : decoded.getClass().getName()));
            }
        }
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        try (ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(data);
        }
    }
}
