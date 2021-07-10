package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.standard.ObjectToStringTransformer;
import lombok.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransformerRegistry {

    private final Map<GenericsPair, ObjectTransformer> transformerMap = new ConcurrentHashMap<>();
    private final Set<ObjectSerializer> serializerSet = ConcurrentHashMap.newKeySet();

    public void register(@NonNull ObjectTransformer transformer) {
        this.transformerMap.put(transformer.getPair(), transformer);
    }

    public void register(@NonNull OkaeriSerdesPack serdesPack) {
        serdesPack.register(this);
    }

    @SuppressWarnings("unchecked")
    public void register(@NonNull TwoSideObjectTransformer transformer) {
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

    public void registerWithReversedToString(@NonNull ObjectTransformer transformer) {
        this.transformerMap.put(transformer.getPair(), transformer);
        this.transformerMap.put(transformer.getPair().reverse(), new ObjectToStringTransformer());
    }

    public void register(@NonNull ObjectSerializer serializer) {
        this.serializerSet.add(serializer);
    }

    public ObjectTransformer getTransformer(@NonNull GenericsDeclaration from, @NonNull GenericsDeclaration to) {
        GenericsPair pair = new GenericsPair(from, to);
        return this.transformerMap.get(pair);
    }

    public boolean canTransform(@NonNull GenericsDeclaration from, @NonNull GenericsDeclaration to) {
        return this.getTransformer(from, to) != null;
    }

    @SuppressWarnings("unchecked")
    public ObjectSerializer getSerializer(@NonNull Class<?> clazz) {
        return this.serializerSet.stream()
                .filter(serializer -> serializer.supports(clazz))
                .findFirst()
                .orElse(null);
    }

    public OkaeriSerdesPack allSerdes() {
        return registry -> {
            this.transformerMap.values().forEach(registry::register);
            this.serializerSet.forEach(registry::register);
        };
    }
}
