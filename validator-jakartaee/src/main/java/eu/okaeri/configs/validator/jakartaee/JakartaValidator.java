package eu.okaeri.configs.validator.jakartaee;

import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.schema.FieldDeclaration;
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

    @Override
    @SuppressWarnings("unchecked")
    public boolean isValid(@NonNull FieldDeclaration declaration, Object value) {
        Class<Object> parent = (Class<Object>) declaration.getObject().getClass();
        String realFieldName = declaration.getField().getName();
        Set<ConstraintViolation<Object>> violations = this.validator.validateValue(parent, realFieldName, value);

        if (!violations.isEmpty()) {
            String reason = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
            throw new ValidationException(declaration.getName() + " (" + value + ") is invalid: " + reason);
        }

        return true;
    }
}
