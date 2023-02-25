package eu.okaeri.configs.annotation;

import eu.okaeri.configs.OkaeriConfig;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Includes.class)
public @interface Include {
    Class<? extends OkaeriConfig> value();
}
