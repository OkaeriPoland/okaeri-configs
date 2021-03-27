package eu.okaeri.configs.validator.jakartaee;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.configurer.WrappedConfigurer;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.schema.FieldDeclaration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

public class JakartaValidator extends WrappedConfigurer {

    private final Validator validator;

    public JakartaValidator(Configurer wrapped) {
        super(wrapped);
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isValid(FieldDeclaration declaration, Object value) {

        Class<Object> parent = (Class<Object>) declaration.getObject().getClass();
        String realFieldName = declaration.getField().getName();
        Set<ConstraintViolation<Object>> violations = this.validator.validateValue(parent, realFieldName, value);

        if (!violations.isEmpty()) {
            String reason = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            throw new ValidationException(declaration.getName() + " (" + value + ") is invalid: " + reason);
        }

        return super.isValid(declaration, value);
    }
}
