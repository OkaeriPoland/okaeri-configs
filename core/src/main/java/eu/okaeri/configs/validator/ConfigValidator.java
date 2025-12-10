package eu.okaeri.configs.validator;

import lombok.NonNull;

/**
 * Interface for configuration entity validation.
 * <p>
 * Validators check configuration entities against constraints, throwing
 * {@link eu.okaeri.configs.exception.ValidationException} on failure.
 * <p>
 * Supports both field-level and cross-field validation:
 * <ul>
 *     <li><b>Field-level validation</b>: Validators like {@code OkaeriValidator} iterate fields internally</li>
 *     <li><b>Entity-level validation</b>: Validators like {@code JakartaValidator} validate the whole entity,
 *         enabling cross-field constraints (e.g., JSR-380 class-level constraints like {@code @PasswordMatches})</li>
 * </ul>
 * <p>
 * Usage with the new API:
 * <pre>{@code
 * TestConfig config = ConfigManager.create(TestConfig.class, it -> {
 *     it.configure(opt -> {
 *         opt.configurer(new YamlBukkitConfigurer(), new SerdesBukkit());
 *         opt.validator(new JakartaValidator());  // Supports cross-field validation
 *         opt.errorComments(true);
 *     });
 * });
 * }</pre>
 *
 * @see eu.okaeri.configs.ConfigContext
 */
public interface ConfigValidator {

    /**
     * Validates the entire configuration entity.
     * <p>
     * Implementations can validate field-level constraints (by iterating fields internally)
     * and/or entity-level constraints (cross-field validation like {@code @PasswordMatches}).
     *
     * @param entity the configuration object to validate
     * @return true if valid
     * @throws eu.okaeri.configs.exception.ValidationException if validation fails
     */
    boolean isValid(@NonNull Object entity);

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
