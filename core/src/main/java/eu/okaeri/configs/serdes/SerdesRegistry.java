package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Central registry for serialization/deserialization components.
 * <p>
 * Stores and manages three types of serdes components:
 * <ul>
 *   <li>{@link ObjectTransformer} - converts between two types (e.g., String → Integer)</li>
 *   <li>{@link ObjectSerializer} - serializes complex objects to/from maps</li>
 *   <li>{@link SerdesAnnotationResolver} - resolves field annotations to context attachments</li>
 * </ul>
 * <p>
 * <b>Registration behavior:</b>
 * <ul>
 *   <li>Transformers: exact type pair matching, last registered wins for same pair</li>
 *   <li>Serializers: last registered wins (reverse iteration during lookup)</li>
 *   <li>Annotation resolvers: one per annotation type, last registered wins</li>
 * </ul>
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * registry.add(
 *     new StringToIntegerTransformer(),
 *     new MyCustomSerializer(),
 *     new SerdesCommons()
 * );
 * }</pre>
 *
 * @see OkaeriSerdes
 * @see ObjectTransformer
 * @see ObjectSerializer
 * @see SerdesAnnotationResolver
 */
public class SerdesRegistry {

    private final Map<Class<? extends Annotation>, SerdesAnnotationResolver<Annotation, SerdesContextAttachment>> annotationResolverMap = new ConcurrentHashMap<>();
    private final List<ObjectSerializer> serializerList = new CopyOnWriteArrayList<>();
    private final Map<GenericsPair, ObjectTransformer> transformerMap = new ConcurrentHashMap<>();

    /**
     * Registers one or more serdes components.
     * <p>
     * This is the unified API for registering any type that implements {@link OkaeriSerdes}:
     * transformers, serializers, packs, or annotation resolvers.
     * <p>
     * Each component's {@link OkaeriSerdes#register(SerdesRegistry)} method is called,
     * which delegates to the appropriate type-specific registration method.
     *
     * @param serdes the serdes components to register
     */
    public void add(@NonNull OkaeriSerdes... serdes) {
        for (OkaeriSerdes pack : serdes) {
            pack.register(this);
        }
    }

    /**
     * Registers a unidirectional transformer.
     * <p>
     * Transformers convert from one type to another (e.g., String → Integer).
     * If a transformer for the same type pair already exists, it is replaced.
     *
     * @param transformer the transformer to register
     */
    public void register(@NonNull ObjectTransformer transformer) {
        this.transformerMap.put(transformer.getPair(), transformer);
    }

    /**
     * Registers a serdes component by calling its self-registration method.
     *
     * @param serdesPack the serdes component to register
     */
    public void register(@NonNull OkaeriSerdes serdesPack) {
        serdesPack.register(this);
    }

    /**
     * Registers a bidirectional transformer.
     * <p>
     * Creates two unidirectional transformers: one for each direction (L → R and R → L).
     *
     * @param transformer the bidirectional transformer to register
     * @param <L> the left type
     * @param <R> the right type
     */
    public <L, R> void register(@NonNull BidirectionalTransformer<L, R> transformer) {
        Class<?> originalClass = transformer.getClass();
        this.register(new ObjectTransformer<L, R>() {
            @Override
            public GenericsPair<L, R> getPair() {
                return transformer.getPair();
            }

            @Override
            public R transform(@NonNull L data, @NonNull SerdesContext serdesContext) {
                return transformer.leftToRight(data, serdesContext);
            }

            @Override
            public Class<?> getOriginalClass() {
                return originalClass;
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

            @Override
            public Class<?> getOriginalClass() {
                return originalClass;
            }
        });
    }

    /**
     * Registers a transformer with an automatic reverse transformer using {@code toString()}.
     * <p>
     * Registers the forward transformer (e.g., String → Integer) and creates a reverse
     * transformer (e.g., Integer → String) that uses {@link Object#toString()}.
     *
     * @param transformer the forward transformer to register
     */
    public void registerWithReversedToString(@NonNull ObjectTransformer transformer) {
        this.transformerMap.put(transformer.getPair(), transformer);

        GenericsPair reversePair = transformer.getPair().reverse();
        ObjectTransformer reverseTransformer = new ObjectTransformer() {
            @Override
            public GenericsPair getPair() {
                return reversePair;
            }

            @Override
            public Object transform(@NonNull Object data, @NonNull SerdesContext serdesContext) {
                return data.toString();
            }
        };

        this.transformerMap.put(reversePair, reverseTransformer);
    }

    /**
     * Registers an object serializer.
     * <p>
     * Serializers are checked in reverse order during lookup, so the last registered
     * serializer that supports a type wins.
     *
     * @param serializer the serializer to register
     */
    public void register(@NonNull ObjectSerializer serializer) {
        this.serializerList.add(serializer);
    }

    /**
     * Registers an object serializer with highest priority.
     * <p>
     * The serializer is inserted at the beginning of the list, but since lookup
     * uses reverse iteration, this actually gives it the lowest priority.
     *
     * @param serializer the serializer to register
     */
    public void registerFirst(@NonNull ObjectSerializer serializer) {
        this.serializerList.add(0, serializer);
    }

    /**
     * Registers a serializer exclusively for a type, removing all existing serializers
     * that support that type.
     *
     * @param type the type to exclusively handle
     * @param serializer the serializer to register
     */
    @SuppressWarnings("unchecked")
    public void registerExclusive(@NonNull Class<?> type, @NonNull ObjectSerializer serializer) {
        this.serializerList.removeIf(ser -> ser.supports(type));
        this.serializerList.add(serializer);
    }

    /**
     * Gets a transformer for the exact type pair.
     *
     * @param from the source type
     * @param to the target type
     * @return the transformer, or null if none registered for this pair
     */
    public ObjectTransformer getTransformer(@NonNull GenericsDeclaration from, @NonNull GenericsDeclaration to) {
        GenericsPair pair = new GenericsPair(from, to);
        return this.transformerMap.get(pair);
    }

    /**
     * Gets all transformers that convert from the specified type.
     *
     * @param from the source type
     * @return list of transformers (may be empty)
     */
    public List<ObjectTransformer> getTransformersFrom(@NonNull GenericsDeclaration from) {
        return this.transformerMap.entrySet().stream()
            .filter(entry -> from.equals(entry.getKey().getFrom()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Gets all transformers that convert to the specified type.
     *
     * @param to the target type
     * @return list of transformers (may be empty)
     */
    public List<ObjectTransformer> getTransformersTo(@NonNull GenericsDeclaration to) {
        return this.transformerMap.entrySet().stream()
            .filter(entry -> to.equals(entry.getKey().getTo()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Checks if a direct transformation is possible between two types.
     *
     * @param from the source type
     * @param to the target type
     * @return true if a transformer exists for this pair
     */
    public boolean canTransform(@NonNull GenericsDeclaration from, @NonNull GenericsDeclaration to) {
        return this.getTransformer(from, to) != null;
    }

    /**
     * Gets a serializer that supports the specified class.
     * <p>
     * Uses reverse iteration, so the last registered serializer that supports
     * the type is returned.
     *
     * @param clazz the class to serialize
     * @return the serializer, or null if none supports this class
     */
    @SuppressWarnings("unchecked")
    public ObjectSerializer getSerializer(@NonNull Class<?> clazz) {
        for (int i = this.serializerList.size() - 1; i >= 0; i--) {
            ObjectSerializer serializer = this.serializerList.get(i);
            if (serializer.supports(clazz)) {
                return serializer;
            }
        }
        return null;
    }

    /**
     * Registers an annotation resolver.
     * <p>
     * Only one resolver per annotation type is supported. If a resolver for the
     * same annotation type already exists, it is replaced.
     *
     * @param annotationResolver the resolver to register
     */
    @SuppressWarnings("unchecked")
    public void register(@NonNull SerdesAnnotationResolver<? extends Annotation, ? extends SerdesContextAttachment> annotationResolver) {
        this.annotationResolverMap.put(annotationResolver.getAnnotationType(), (SerdesAnnotationResolver<Annotation, SerdesContextAttachment>) annotationResolver);
    }

    /**
     * Gets the annotation resolver for the specified annotation type.
     *
     * @param annotationType the annotation class
     * @return the resolver, or null if none registered
     */
    public SerdesAnnotationResolver<Annotation, SerdesContextAttachment> getAnnotationResolver(@NonNull Class<? extends Annotation> annotationType) {
        return this.annotationResolverMap.get(annotationType);
    }

    /**
     * Gets the annotation resolver for the specified annotation instance.
     *
     * @param annotation the annotation instance
     * @return the resolver, or null if none registered
     */
    public SerdesAnnotationResolver<Annotation, SerdesContextAttachment> getAnnotationResolver(@NonNull Annotation annotation) {
        return this.annotationResolverMap.get(annotation.annotationType());
    }

    /**
     * Creates a serdes pack containing all registered components.
     * <p>
     * Useful for copying all serdes from one registry to another.
     *
     * @return a pack that registers all current transformers, serializers, and resolvers
     */
    public OkaeriSerdes allSerdes() {
        return registry -> {
            this.transformerMap.values().forEach(registry::register);
            this.serializerList.forEach(registry::register);
            this.annotationResolverMap.values().forEach(registry::register);
        };
    }
}
