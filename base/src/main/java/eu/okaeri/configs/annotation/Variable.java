package eu.okaeri.configs.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Variable {
    String value();
    VariableMode mode() default VariableMode.RUNTIME;
}
