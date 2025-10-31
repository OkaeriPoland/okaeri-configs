package eu.okaeri.configs.serdes;

import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

public interface SerdesAnnotationResolver<A extends Annotation, D extends SerdesContextAttachment> {

    Class<A> getAnnotationType();

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
