package eu.okaeri.configs.validator.okaeri;

import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.validator.ConfigValidator;
import eu.okaeri.validator.ConstraintViolation;
import eu.okaeri.validator.policy.NullPolicy;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ConfigValidator implementation using okaeri-validator.
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

    @Override
    public boolean isValid(@NonNull FieldDeclaration declaration, Object value) {
        Class<?> parent = declaration.getObject().getClass();
        Field field = declaration.getField();
        Set<ConstraintViolation> violations = this.validator.validatePropertyValue(parent, field, value);

        if (!violations.isEmpty()) {
            String reason = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
            throw new ValidationException(declaration.getName() + " (" + value + ") is invalid: " + reason);
        }

        return true;
    }
}
