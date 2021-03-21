package eu.okaeri.configs.transformer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.transformer.impl.StringToDoubleTransformer;
import eu.okaeri.configs.transformer.impl.StringToFloatTransformer;
import eu.okaeri.configs.transformer.impl.StringToIntegerTransformer;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TransformerRegistry {

    private static final Map<GenericsPair, ObjectTransformer> TRANSFORMER_MAP = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ObjectSerializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    static {
        register(new StringToDoubleTransformer());
        register(new StringToFloatTransformer());
        register(new StringToIntegerTransformer());
    }

    public static void register(ObjectTransformer transformer) {
        TRANSFORMER_MAP.put(transformer.getPair(), transformer);
    }

    public static void register(ObjectSerializer serializer) {
        SERIALIZER_MAP.put(serializer.getType(), serializer);
    }

    public static ObjectTransformer getTransformer(GenericsDeclaration from, GenericsDeclaration to) {
        GenericsPair pair = new GenericsPair(from, to);
        return TRANSFORMER_MAP.get(pair);
    }

    public static ObjectSerializer getSerializer(Class<?> clazz) {
        return SERIALIZER_MAP.get(clazz);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <S, D> D transform(S object, Class<D> to) {

        if (object == null) {
            return null;
        }

        if (object.getClass() == to) {
            return (D) object;
        }

        try {
            return to.cast(object);
        }
        catch (ClassCastException exception) {

            ObjectTransformer transformer = getTransformer(new GenericsDeclaration(object.getClass()), new GenericsDeclaration(to));
            if (transformer == null) {
                throw new IllegalAccessException("no transformer for " + object.getClass() + " -> " + to + " pair");
            }

            return (D) transformer.transform(object);
        }
    }
}
