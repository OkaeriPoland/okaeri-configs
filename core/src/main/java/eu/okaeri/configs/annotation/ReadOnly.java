package eu.okaeri.configs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as read-only, preserving its original loaded value during save operations.
 * <p>
 * Fields annotated with @ReadOnly can be loaded from input and modified programmatically,
 * but when saved, the original loaded value is persisted instead of any programmatic changes.
 * This allows the POJO to be modified at runtime while ensuring the configuration file
 * maintains its original value.
 * <p>
 * Behavior:
 * - Load: Field is loaded normally from configuration
 * - Runtime: Field can be modified in code like any other field
 * - Save: Original loaded value is saved, ignoring any runtime modifications
 * <p>
 * Useful for:
 * - Configuration values that should not be modified by the application
 * - Fields that need runtime modification but should preserve file values
 * - Test configurations where programmatic changes should not persist
 * <p>
 * Example:
 * <pre>
 * public class AppConfig extends OkaeriConfig {
 *     private String normalField = "can be changed";
 *
 *     {@literal @}ReadOnly
 *     private String configVersion = "1.0";  // Preserves original value on save
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {
}
