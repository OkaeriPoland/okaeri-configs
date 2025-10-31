package eu.okaeri.configs.serdes.okaeri.indexedset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the key field for IndexedSet serialization.
 * Can be applied at field level or class level (for all IndexedSet fields in the config).
 * Field-level annotations take precedence over class-level annotations.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * // Field-level usage
 * @IndexedSetSpec(key = "id")
 * private IndexedSet<Item> items;
 *
 * // Class-level usage (applies to all IndexedSet fields)
 * @IndexedSetSpec(key = "name")
 * public class MyConfig extends OkaeriConfig {
 *     private IndexedSet<Item> items1;  // Uses "name" from class
 *
 *     @IndexedSetSpec(key = "id")       // Override for specific field
 *     private IndexedSet<Item> items2;  // Uses "id"
 * }
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IndexedSetSpec {
    String key() default "key";
}
