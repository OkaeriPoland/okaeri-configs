package eu.okaeri.configs.validator;

import eu.okaeri.configs.schema.FieldDeclaration;
import lombok.NonNull;

/**
 * Interface for configuration field validation.
 * <p>
 * Validators check field values against constraints and throw
 * {@link eu.okaeri.configs.exception.ValidationException} on failure.
 * <p>
 * Usage with the new API:
 * <pre>{@code
 * TestConfig config = ConfigManager.create(TestConfig.class, it -> {
 *     it.configure(opt -> {
 *         opt.configurer(new YamlBukkitConfigurer(), new SerdesBukkit());
 *         opt.validator(new OkaeriValidator());  // Validator as option
 *         opt.errorComments(true);
 *     });
 * });
 * }</pre>
 *
 * @see eu.okaeri.configs.ConfigContext
 */
public interface ConfigValidator {

    /**
     * Validates a field value.
     *
     * @param declaration the field being validated
     * @param value the value to validate
     * @return true if valid
     * @throws eu.okaeri.configs.exception.ValidationException if validation fails
     */
    boolean isValid(@NonNull FieldDeclaration declaration, Object value);

    /**
     * Returns true if this validator validates on load (deserialization).
     * Default is true.
     *
     * @return true if validation should run during load
     */
    default boolean validateOnLoad() {
        return true;
    }

    /**
     * Returns true if this validator validates on save (serialization).
     * Default is true.
     *
     * @return true if validation should run during save
     */
    default boolean validateOnSave() {
        return true;
    }
}
