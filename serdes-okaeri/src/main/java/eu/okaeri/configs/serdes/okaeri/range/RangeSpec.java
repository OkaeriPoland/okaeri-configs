package eu.okaeri.configs.serdes.okaeri.range;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the serialization format for Range fields.
 * Can be applied at field level or class level (for all Range fields in the config).
 * Field-level annotations take precedence over class-level annotations.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * // Field-level usage
 * @RangeSpec(format = RangeFormat.INLINE)
 * private IntRange damageRange = IntRange.of(10, 20);  // Serializes as "10-20"
 *
 * @RangeSpec(format = RangeFormat.SECTION)
 * private IntRange healthRange = IntRange.of(50, 100); // Serializes as {min: 50, max: 100}
 *
 * // Class-level usage (applies to all Range fields)
 * @RangeSpec(format = RangeFormat.INLINE)
 * public class MyConfig extends OkaeriConfig {
 *     private IntRange range1 = IntRange.of(1, 10);     // Uses INLINE from class
 *
 *     @RangeSpec(format = RangeFormat.SECTION)          // Override for specific field
 *     private IntRange range2 = IntRange.of(20, 30);    // Uses SECTION
 * }
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RangeSpec {

    /**
     * Serialization format for the range field.
     * Defaults to SECTION for better readability.
     */
    RangeFormat format() default RangeFormat.SECTION;
}
