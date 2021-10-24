package eu.okaeri.configs.yaml.bukkit.serdes.itemstack;

import eu.okaeri.configs.serdes.SerdesContextAttachment;
import lombok.Value;

@Value(staticConstructor = "of")
public class ItemStackSpecData implements SerdesContextAttachment {
    private final ItemStackFormat format;
}
