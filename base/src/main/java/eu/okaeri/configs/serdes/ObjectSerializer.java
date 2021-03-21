package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;

public interface ObjectSerializer<T> {

    Class<? super T> getType();

    void serialize(T object, SerializationData data);

    T deserialize(DeserializationData data, GenericsDeclaration generics);
}
