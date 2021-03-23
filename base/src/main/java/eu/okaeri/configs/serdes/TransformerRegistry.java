package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.impl.ObjectToStringTransformer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransformerRegistry {

    private final Map<GenericsPair, ObjectTransformer> transformerMap = new ConcurrentHashMap<>();
    private final Set<ObjectSerializer> serializerSet = ConcurrentHashMap.newKeySet();

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
        this.serializerSet.add(serializer);
    }

    public ObjectTransformer getTransformer(GenericsDeclaration from, GenericsDeclaration to) {
        GenericsPair pair = new GenericsPair(from, to);
        return this.transformerMap.get(pair);
    }

    public boolean canTransform(Class<?> from, Class<?> to) {
        return this.getTransformer(GenericsDeclaration.of(from), GenericsDeclaration.of(to)) != null;
    }

    @SuppressWarnings("unchecked")
    public ObjectSerializer getSerializer(Class<?> clazz) {
        return this.serializerSet.stream()
                .filter(serializer -> serializer.supports(clazz))
                .findFirst()
                .orElse(null);
    }
}
