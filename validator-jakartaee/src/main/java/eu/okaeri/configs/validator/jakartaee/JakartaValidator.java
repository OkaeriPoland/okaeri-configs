package eu.okaeri.configs.validator.jakartaee;

import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.validator.ConfigValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.NonNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * ConfigValidator implementation using Jakarta Bean Validation.
 * <p>
 * Validates the entire configuration entity, supporting both field-level constraints
 * and class-level constraints (e.g., {@code @PasswordMatches}, {@code @ValidDateRange}).
 * <p>
 * Usage:
 * <pre>{@code
 * TestConfig config = ConfigManager.create(TestConfig.class, it -> {
 *     it.configure(opt -> {
 *         opt.configurer(new YamlBukkitConfigurer());
 *         opt.validator(new JakartaValidator());
 *     });
 * });
 * }</pre>
 */
public class JakartaValidator implements ConfigValidator {

    private final Validator validator;

    public JakartaValidator() {
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * Validates the entire configuration entity.
     * <p>
     * This method validates all field constraints and class-level constraints
     * (e.g., JSR-380 cross-field validation like {@code @PasswordMatches}).
     *
     * @param entity the configuration object to validate
     * @return true if valid
     * @throws ValidationException if validation fails
     */
    @Override
    public boolean isValid(@NonNull Object entity) {

        Set<ConstraintViolation<Object>> violations = this.validator.validate(entity);
        if (violations.isEmpty()) {
            return true;
        }

        String reason = violations.stream()
            .map(violation -> {
                String propertyPath = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                Object invalidValue = violation.getInvalidValue();
                if (propertyPath.isEmpty()) {
                    // Class-level constraint
                    return message;
                } else {
                    // Field-level constraint
                    return propertyPath + " (" + invalidValue + ") " + message;
                }
            })
            .collect(Collectors.joining(", "));

        throw new ValidationException(entity.getClass().getSimpleName() + " is invalid: " + reason);
    }
}
