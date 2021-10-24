
package eu.okaeri.configs.yaml.bukkit.serdes.itemstack;

import eu.okaeri.configs.serdes.SerdesAnnotationResolver;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.Optional;

public class ItemStackAttachmentResolver implements SerdesAnnotationResolver<ItemStackSpec, ItemStackSpecData> {

    @Override
    public Class<ItemStackSpec> getAnnotationType() {
        return ItemStackSpec.class;
    }

    @Override
    public Optional<ItemStackSpecData> resolveAttachment(@NonNull Field field, @NonNull ItemStackSpec annotation) {
        return Optional.of(ItemStackSpecData.of(annotation.format()));
    }
}
