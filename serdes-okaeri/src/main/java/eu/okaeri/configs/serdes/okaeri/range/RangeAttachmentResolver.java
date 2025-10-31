package eu.okaeri.configs.serdes.okaeri.range;

import eu.okaeri.configs.serdes.SerdesAnnotationResolver;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Resolves {@link RangeSpec} annotations into {@link RangeSpecData} context attachments.
 * This allows Range serializers to adapt their behavior based on per-field annotations.
 */
public class RangeAttachmentResolver implements SerdesAnnotationResolver<RangeSpec, RangeSpecData> {

    @Override
    public Class<RangeSpec> getAnnotationType() {
        return RangeSpec.class;
    }

    @Override
    public Optional<RangeSpecData> resolveAttachment(@NonNull Field field, @NonNull RangeSpec annotation) {
        return Optional.of(RangeSpecData.of(annotation.format()));
    }
}
