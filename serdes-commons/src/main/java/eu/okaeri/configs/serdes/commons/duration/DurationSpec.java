package eu.okaeri.configs.serdes.commons.duration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DurationSpec {

    ChronoUnit fallbackUnit() default ChronoUnit.SECONDS;

    DurationFormat format() default DurationFormat.SIMPLIFIED;
}
