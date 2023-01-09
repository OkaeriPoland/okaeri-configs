package eu.okaeri.configs.serdes.serializable;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

/**
 * Allows to implement class local serdes
 * with no need to register it in any way.
 * <p>
 * Requires static method for deserialization:
 * <pre>{@code public static MyType deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics)}</pre>
 */
public interface ConfigSerializable {
    void serialize(@NonNull SerializationData data, @NonNull GenericsDeclaration generics);
}
