package eu.okaeri.configs.validator.okaeri;

import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.validator.ConfigValidator;
import eu.okaeri.validator.ConstraintViolation;
import eu.okaeri.validator.policy.NullPolicy;
import lombok.NonNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * ConfigValidator implementation using okaeri-validator.
 * <p>
 * Validates the entire entity using okaeri-validator's constraint annotations.
 * <p>
 * Usage:
 * <pre>{@code
 * TestConfig config = ConfigManager.create(TestConfig.class, it -> {
 *     it.configure(opt -> {
 *         opt.configurer(new YamlBukkitConfigurer());
 *         opt.validator(new OkaeriValidator());
 *     });
 * });
 * }</pre>
 */
public class OkaeriValidator implements ConfigValidator {

    private final eu.okaeri.validator.OkaeriValidator validator;

    public OkaeriValidator() {
        this(false);
    }

    public OkaeriValidator(boolean defaultNotNull) {
        this.validator = eu.okaeri.validator.OkaeriValidator.of(
            defaultNotNull ? NullPolicy.NOT_NULL : NullPolicy.NULLABLE
        );
    }

    /**
     * Validates the entire configuration entity.
     *
     * @param entity the configuration object to validate
     * @return true if valid
     * @throws ValidationException if validation fails
     */
    @Override
    public boolean isValid(@NonNull Object entity) {

        Set<ConstraintViolation> violations = this.validator.validate(entity);
        if (violations.isEmpty()) {
            return true;
        }

        String reason = violations.stream()
            .map(violation -> violation.getField() + " (" + violation.getType() + ") " + violation.getMessage())
            .collect(Collectors.joining(", "));

        throw new ValidationException(entity.getClass().getSimpleName() + " is invalid: " + reason);
    }
}
