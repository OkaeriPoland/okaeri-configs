package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.standard.ObjectToStringTransformer;
import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SerdesRegistry {

    private final Map<Class<? extends Annotation>, SerdesAnnotationResolver<Annotation, SerdesContextAttachment>> annotationResolverMap = new ConcurrentHashMap<>();
    private final Set<ObjectSerializer> serializerSet = ConcurrentHashMap.newKeySet();
    private final Map<GenericsPair, ObjectTransformer> transformerMap = new ConcurrentHashMap<>();

    public void register(@NonNull ObjectTransformer transformer) {
        this.transformerMap.put(transformer.getPair(), transformer);
    }

    public void register(@NonNull OkaeriSerdesPack serdesPack) {
        serdesPack.register(this);
    }

    /**
     * @deprecated Use {@link #register(BidirectionalTransformer)}
     */
    @Deprecated
    public <L, R> void register(@NonNull TwoSideObjectTransformer<L, R> transformer) {
        this.register(((BidirectionalTransformer<L, R>) transformer));
    }

    public <L, R> void register(@NonNull BidirectionalTransformer<L, R> transformer) {
        this.register(new ObjectTransformer<L, R>() {
            @Override
            public GenericsPair<L, R> getPair() {
                return transformer.getPair();
            }

            @Override
            public R transform(@NonNull L data, @NonNull SerdesContext serdesContext) {
                return transformer.leftToRight(data, serdesContext);
            }
        });
        this.register(new ObjectTransformer<R, L>() {
            @Override
            public GenericsPair<R, L> getPair() {
                return transformer.getPair().reverse();
            }

            @Override
            public L transform(@NonNull R data, @NonNull SerdesContext serdesContext) {
                return transformer.rightToLeft(data, serdesContext);
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

    @SuppressWarnings("unchecked")
    public void registerExclusive(@NonNull Class<?> type, @NonNull ObjectSerializer serializer) {
        this.serializerSet.removeIf(ser -> ser.supports(type));
        this.serializerSet.add(serializer);
    }

    public ObjectTransformer getTransformer(@NonNull GenericsDeclaration from, @NonNull GenericsDeclaration to) {
        GenericsPair pair = new GenericsPair(from, to);
        return this.transformerMap.get(pair);
    }

    public List<ObjectTransformer> getTransformersFrom(@NonNull GenericsDeclaration from) {
        return this.transformerMap.entrySet().stream()
                .filter(entry -> from.equals(entry.getKey().getFrom()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public List<ObjectTransformer> getTransformersTo(@NonNull GenericsDeclaration to) {
        return this.transformerMap.entrySet().stream()
                .filter(entry -> to.equals(entry.getKey().getTo()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
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

    @SuppressWarnings("unchecked")
    public void register(@NonNull SerdesAnnotationResolver<? extends Annotation, ? extends SerdesContextAttachment> annotationResolver) {
        this.annotationResolverMap.put(annotationResolver.getAnnotationType(), (SerdesAnnotationResolver<Annotation, SerdesContextAttachment>) annotationResolver);
    }

    public SerdesAnnotationResolver<Annotation, SerdesContextAttachment> getAnnotationResolver(@NonNull Class<? extends Annotation> annotationType) {
        return this.annotationResolverMap.get(annotationType);
    }

    public SerdesAnnotationResolver<Annotation, SerdesContextAttachment> getAnnotationResolver(@NonNull Annotation annotation) {
        return this.annotationResolverMap.get(annotation.annotationType());
    }

    public OkaeriSerdesPack allSerdes() {
        return registry -> {
            this.transformerMap.values().forEach(registry::register);
            this.serializerSet.forEach(registry::register);
            this.annotationResolverMap.values().forEach(registry::register);
        };
    }
}
