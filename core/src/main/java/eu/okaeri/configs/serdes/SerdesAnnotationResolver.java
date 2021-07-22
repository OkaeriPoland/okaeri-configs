package eu.okaeri.configs.serdes;

import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

public interface SerdesAnnotationResolver<A extends Annotation, D extends SerdesContextAttachment> {

    Class<A> getAnnotationType();

    Optional<D> resolveAttachment(@NonNull Field field, @NonNull A annotation);
}
