package eu.okaeri.configs.serdes.okaeri.range;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the serialization format for Range fields.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @RangeSpec(format = RangeFormat.INLINE)
 * private IntRange damageRange = IntRange.of(10, 20);  // Serializes as "10-20"
 *
 * @RangeSpec(format = RangeFormat.SECTION)
 * private IntRange healthRange = IntRange.of(50, 100); // Serializes as {min: 50, max: 100}
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RangeSpec {

    /**
     * Serialization format for the range field.
     * Defaults to SECTION for better readability.
     */
    RangeFormat format() default RangeFormat.SECTION;
}
