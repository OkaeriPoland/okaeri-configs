package eu.okaeri.configs.serdes.commons.duration;

import eu.okaeri.configs.serdes.SerdesAnnotationResolver;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.Optional;

public class DurationAttachmentResolver implements SerdesAnnotationResolver<DurationSpec, DurationSpecData> {

    @Override
    public Class<DurationSpec> getAnnotationType() {
        return DurationSpec.class;
    }

    @Override
    public Optional<DurationSpecData> resolveAttachment(@NonNull Field field, @NonNull DurationSpec annotation) {
        return Optional.of(DurationSpecData.of(annotation.fallbackUnit(), annotation.format()));
    }
}
