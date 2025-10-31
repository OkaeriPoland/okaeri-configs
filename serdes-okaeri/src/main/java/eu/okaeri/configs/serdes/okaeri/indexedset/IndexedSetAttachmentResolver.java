package eu.okaeri.configs.serdes.okaeri.indexedset;

import eu.okaeri.configs.serdes.SerdesAnnotationResolver;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.Optional;

public class IndexedSetAttachmentResolver implements SerdesAnnotationResolver<IndexedSetSpec, IndexedSetSpecData> {

    @Override
    public Class<IndexedSetSpec> getAnnotationType() {
        return IndexedSetSpec.class;
    }

    @Override
    public Optional<IndexedSetSpecData> resolveAttachment(@NonNull Field field, @NonNull IndexedSetSpec annotation) {
        return Optional.of(IndexedSetSpecData.of(annotation.key()));
    }

    @Override
    public Optional<IndexedSetSpecData> resolveClassAttachment(@NonNull Class<?> clazz, @NonNull IndexedSetSpec annotation) {
        return Optional.of(IndexedSetSpecData.of(annotation.key()));
    }
}
