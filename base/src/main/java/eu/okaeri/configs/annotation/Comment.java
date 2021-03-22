package eu.okaeri.configs.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Comments.class)
public @interface Comment {
    String[] value();
}
