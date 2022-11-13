package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.yaml.bukkit.serdes.transformer.experimental.StringBase64ItemStackTransformer;

/**
 * @deprecated Prefer {@link eu.okaeri.configs.yaml.bukkit.serdes.serializer.ItemStackSerializer}
 * initialized in failsafe mode using constructor param ({@code new ItemStackSerializer(true)}).
 *
 * Example override:
 * {@code registry.register(new SerdesBukkit());}
 * {@code registry.registerExclusive(ItemStack.class, new ItemStackSerializer(true));}
 *
 * Alternatively use {@link StringBase64ItemStackTransformer} directly instead.
 */
@Deprecated
public class ItemStackTransformer extends StringBase64ItemStackTransformer {
}
