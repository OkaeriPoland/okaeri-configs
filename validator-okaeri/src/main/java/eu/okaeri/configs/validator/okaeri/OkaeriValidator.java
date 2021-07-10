package eu.okaeri.configs.validator.okaeri;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.configurer.WrappedConfigurer;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.validator.ConstraintViolation;
import eu.okaeri.validator.policy.NullPolicy;
import lombok.NonNull;

import java.util.Set;
import java.util.stream.Collectors;

public class OkaeriValidator extends WrappedConfigurer {

    private final boolean defaultNotNull;

    public OkaeriValidator(@NonNull Configurer wrapped) {
        this(wrapped, false);
    }

    public OkaeriValidator(@NonNull Configurer wrapped, boolean defaultNotNull) {
        super(wrapped);
        this.defaultNotNull = defaultNotNull;
    }

    @Override
    public boolean isValid(@NonNull FieldDeclaration declaration, Object value) {

        Class<?> parent = declaration.getObject().getClass();
        String realFieldName = declaration.getField().getName();

        NullPolicy nullPolicy = this.defaultNotNull ? NullPolicy.NOT_NULL : NullPolicy.NULLABLE;
        eu.okaeri.validator.OkaeriValidator validator = eu.okaeri.validator.OkaeriValidator.of(parent, nullPolicy);
        Set<ConstraintViolation> violations = validator.validateValue(realFieldName, value);

        if (!violations.isEmpty()) {
            String reason = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            throw new ValidationException(declaration.getName() + " (" + value + ") is invalid: " + reason);
        }

        return super.isValid(declaration, value);
    }
}
