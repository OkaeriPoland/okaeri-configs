package eu.okaeri.configs.serdes;

import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Resolves field or class annotations into {@link SerdesContextAttachment} data.
 * <p>
 * Allows customizing serialization/deserialization behavior per-field using annotations.
 * The resolved attachment is available via {@link SerdesContext#getAttachment(Class)}.
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * // 1. Define annotation
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target({ElementType.FIELD, ElementType.TYPE})
 * public @interface DurationSpec {
 *     ChronoUnit fallbackUnit() default ChronoUnit.SECONDS;
 * }
 *
 * // 2. Define attachment data
 * @Data
 * public class DurationSpecData implements SerdesContextAttachment {
 *     private final ChronoUnit fallbackUnit;
 * }
 *
 * // 3. Implement resolver
 * public class DurationResolver implements SerdesAnnotationResolver<DurationSpec, DurationSpecData> {
 *     @Override
 *     public Class<DurationSpec> getAnnotationType() {
 *         return DurationSpec.class;
 *     }
 *
 *     @Override
 *     public Optional<DurationSpecData> resolveAttachment(Field field, DurationSpec annotation) {
 *         return Optional.of(new DurationSpecData(annotation.fallbackUnit()));
 *     }
 * }
 *
 * // 4. Use in config
 * public class MyConfig extends OkaeriConfig {
 *     @DurationSpec(fallbackUnit = ChronoUnit.MINUTES)
 *     private Duration timeout = Duration.ofMinutes(5);
 * }
 * }</pre>
 *
 * @param <A> the annotation type to resolve
 * @param <D> the attachment data type to produce
 * @see SerdesContextAttachment
 * @see SerdesContext#getAttachment(Class)
 */
public interface SerdesAnnotationResolver<A extends Annotation, D extends SerdesContextAttachment> extends OkaeriSerdes {

    @Override
    default void register(@NonNull SerdesRegistry registry) {
        registry.register(this);
    }

    /**
     * Returns the annotation type this resolver handles.
     * Used as the registry key for lookup.
     *
     * @return the annotation class
     */
    Class<A> getAnnotationType();

    /**
     * Resolves attachment data from a field-level annotation.
     *
     * @param field the annotated field
     * @param annotation the annotation instance
     * @return optional attachment data, or empty to skip
     */
    Optional<D> resolveAttachment(@NonNull Field field, @NonNull A annotation);

    /**
     * Resolves attachment from class-level annotation.
     * This method is called as a fallback when a field doesn't have the annotation.
     * Default implementation returns empty, making this feature opt-in for resolvers.
     *
     * @param clazz the class declaring the field
     * @param annotation the annotation instance from the class
     * @return optional attachment data, or empty if class-level resolution is not supported
     */
    default Optional<D> resolveClassAttachment(@NonNull Class<?> clazz, @NonNull A annotation) {
        return Optional.empty();
    }
}
