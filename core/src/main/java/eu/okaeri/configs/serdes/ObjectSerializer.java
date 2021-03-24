package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;

public interface ObjectSerializer<T> {

    boolean supports(Class<? super T> type);

    void serialize(T object, SerializationData data);

    T deserialize(DeserializationData data, GenericsDeclaration generics);
}
