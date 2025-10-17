package eu.okaeri.configs.annotation;

import eu.okaeri.configs.serdes.ObjectSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a custom serializer for this field, overriding the global registry.
 * <p>
 * The specified serializer must:
 * <ul>
 *   <li>Have a public no-args constructor</li>
 *   <li>Support the field's type (validated at initialization)</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * public class MyConfig extends OkaeriConfig {
 *     // Uses default serializer from registry
 *     private ItemStack normalItem;
 *
 *     // Uses CraftItemStackSerializer for this specific field
 *     {@literal @}Serdes(serializer = CraftItemStackSerializer.class)
 *     private ItemStack customItem;
 * }
 * </pre>
 * <p>
 * Serializer instances are cached per field declaration (not created on every access).
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Serdes {
    /**
     * The serializer class to use for this field.
     * Must have a public no-args constructor.
     *
     * @return the serializer class
     */
    Class<? extends ObjectSerializer<?>> serializer();
}
