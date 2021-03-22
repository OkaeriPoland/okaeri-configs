package eu.okaeri.configs.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Headers.class)
public @interface Header {
    String[] value();
}
