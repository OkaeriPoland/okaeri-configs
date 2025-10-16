package eu.okaeri.configs.serdes.serializable;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.lang.reflect.Method;

public class ConfigSerializableSerializer implements ObjectSerializer<ConfigSerializable> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return ConfigSerializable.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull ConfigSerializable object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        object.serialize(data, generics);
    }

    @Override
    @SneakyThrows
    public ConfigSerializable deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        Method deserializeMethod;
        try {
            deserializeMethod = generics.getType().getMethod("deserialize", DeserializationData.class, GenericsDeclaration.class);
        } catch (NoSuchMethodException exception) {
            throw new RuntimeException("public static " + generics.getType().getSimpleName() +
                " deserialize(DeserializationData, GenericsDeclaration)" +
                " method missing in ConfigSerializable " + generics.getType(), exception);
        }

        return (ConfigSerializable) deserializeMethod.invoke(null, data, generics);
    }
}
