package eu.okaeri.configs.yaml.bukkit.serdes.itemstack;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the serialization format for ItemStack fields.
 * Can be applied at field level or class level (for all ItemStack fields in the config).
 * Field-level annotations take precedence over class-level annotations.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * // Field-level usage
 * @ItemStackSpec(format = ItemStackFormat.NATURAL)
 * private ItemStack item1;
 *
 * @ItemStackSpec(format = ItemStackFormat.COMPACT)
 * private ItemStack item2;
 *
 * // Class-level usage (applies to all ItemStack fields)
 * @ItemStackSpec(format = ItemStackFormat.NATURAL)
 * public class MyConfig extends OkaeriConfig {
 *     private ItemStack item1;  // Uses NATURAL from class
 *
 *     @ItemStackSpec(format = ItemStackFormat.COMPACT)  // Override for specific field
 *     private ItemStack item2;  // Uses COMPACT
 * }
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ItemStackSpec {
    ItemStackFormat format() default ItemStackFormat.NATURAL;
}
