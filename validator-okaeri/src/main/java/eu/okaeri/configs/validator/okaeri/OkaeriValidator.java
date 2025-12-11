package eu.okaeri.configs.validator.okaeri;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.schema.FieldDeclaration;
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
     * Validates the configuration entity by iterating through declared fields.
     * Only validates fields present in ConfigDeclaration (excludes transient/@Exclude fields).
     * <p>
     * Note: okaeri-validator does not support cross-field validation, so we validate
     * each declared field individually.
     *
     * @param entity the configuration object to validate
     * @return true if valid
     * @throws ValidationException if validation fails
     * @throws IllegalArgumentException if entity is not an OkaeriConfig instance
     */
    @Override
    public boolean isValid(@NonNull Object entity) {

        if (!(entity instanceof OkaeriConfig)) {
            throw new IllegalArgumentException("OkaeriValidator can only validate OkaeriConfig instances, got: " + entity.getClass().getName());
        }

        OkaeriConfig config = (OkaeriConfig) entity;
        for (FieldDeclaration field : config.getDeclaration().getFields()) {
            this.validateField(entity, field);
        }

        return true;
    }

    private void validateField(Object entity, FieldDeclaration field) {

        Set<ConstraintViolation> violations = this.validator.validatePropertyValue(
            entity.getClass(),
            field.getField(),
            field.getValue()
        );

        if (violations.isEmpty()) {
            return;
        }

        String reason = violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));

        throw new ValidationException(field.getName() + " (" + field.getValue() + ") is invalid: " + reason);
    }
}
