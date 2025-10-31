package eu.okaeri.configs.serdes.commons.duration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * Specifies the serialization format and fallback unit for Duration fields.
 * Can be applied at field level or class level (for all Duration fields in the config).
 * Field-level annotations take precedence over class-level annotations.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * // Field-level usage
 * @DurationSpec(format = DurationFormat.SIMPLIFIED, fallbackUnit = ChronoUnit.SECONDS)
 * private Duration timeout;
 *
 * // Class-level usage (applies to all Duration fields)
 * @DurationSpec(format = DurationFormat.ISO, fallbackUnit = ChronoUnit.MILLIS)
 * public class MyConfig extends OkaeriConfig {
 *     private Duration duration1;  // Uses ISO and MILLIS from class
 *
 *     @DurationSpec(format = DurationFormat.SIMPLIFIED, fallbackUnit = ChronoUnit.MILLIS)
 *     private Duration duration2;  // Field-level completely overrides class-level
 * }
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DurationSpec {

    ChronoUnit fallbackUnit() default ChronoUnit.SECONDS;

    DurationFormat format() default DurationFormat.SIMPLIFIED;
}
