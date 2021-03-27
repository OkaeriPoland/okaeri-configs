package eu.okaeri.configs.validator.jakartaee;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.configurer.WrappedConfigurer;
import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.schema.FieldDeclaration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class JakartaValidator extends WrappedConfigurer {

    public JakartaValidator(Configurer wrapped) {
        super(wrapped);
    }

    @Override
    public boolean isValid(FieldDeclaration declaration) {

        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();

        Object parentObject = declaration.getObject();
        String realFieldName = declaration.getField().getName();
        Set<ConstraintViolation<Object>> violations = validator.validateProperty(parentObject, realFieldName);

        if (!violations.isEmpty()) {
            String reason = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            throw new ValidationException(declaration.getName() + " is invalid: " + reason);
        }

        return super.isValid(declaration);
    }
}
