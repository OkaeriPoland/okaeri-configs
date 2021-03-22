package eu.okaeri.configs.serdes.transformer;

import eu.okaeri.configs.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.serdes.impl.*;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransformerRegistry {

    private final Map<GenericsPair, ObjectTransformer> transformerMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, ObjectSerializer> serializerMap = new ConcurrentHashMap<>();

    public void register(ObjectTransformer transformer) {
        this.transformerMap.put(transformer.getPair(), transformer);
    }

    public void register(OkaeriSerdesPack serdesPack) {
        serdesPack.register(this);
    }

    @SuppressWarnings("unchecked")
    public void register(TwoSideObjectTransformer transformer) {
        this.register(new ObjectTransformer() {
            @Override
            public GenericsPair getPair() {
                return transformer.getPair();
            }

            @Override
            public Object transform(Object data) {
                return transformer.leftToRight(data);
            }
        });
        this.register(new ObjectTransformer() {
            @Override
            public GenericsPair getPair() {
                return transformer.getPair().reverse();
            }

            @Override
            public Object transform(Object data) {
                return transformer.rightToLeft(data);
            }
        });
    }

    public void registerWithReversedToString(ObjectTransformer transformer) {
        this.transformerMap.put(transformer.getPair(), transformer);
        this.transformerMap.put(transformer.getPair().reverse(), new ObjectToStringTransformer());
    }

    public void register(ObjectSerializer serializer) {
        this.serializerMap.put(serializer.getType(), serializer);
    }

    public ObjectTransformer getTransformer(GenericsDeclaration from, GenericsDeclaration to) {
        GenericsPair pair = new GenericsPair(from, to);
        return this.transformerMap.get(pair);
    }

    public boolean canTransform(Class<?> from, Class<?> to) {
        return this.getTransformer(new GenericsDeclaration(from), new GenericsDeclaration(to)) != null;
    }

    public ObjectSerializer getSerializer(Class<?> clazz) {
        return this.serializerMap.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public SerializationData serializeOrNull(Object object, Configurer configurer) {

        ObjectSerializer serializer = this.getSerializer(object.getClass());
        if (serializer == null) {
            return null;
        }

        SerializationData data = new SerializationData(configurer);
        serializer.serialize(object, data);

        return data;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <S, D> D transform(S object, Class<D> to) {

        if (object == null) {
            return null;
        }

        try {
            return to.cast(object);
        }
        catch (ClassCastException exception) {

            ObjectTransformer transformer = this.getTransformer(new GenericsDeclaration(object.getClass()), new GenericsDeclaration(to));
            if (transformer == null) {
                throw new IllegalAccessException("no transformer for " + object.getClass() + " -> " + to + " pair");
            }

            return (D) transformer.transform(object);
        }
    }
}
