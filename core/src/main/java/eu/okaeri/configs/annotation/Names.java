package eu.okaeri.configs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation was intended mainly for legacy compatibility use; however, it never worked 100% as one might expect.
 * If you are implementing a POJO of an existing config please specify each key using {@link CustomKey} to avoid surprises.
 * <p>
 * I, the author, believe the keys in the config files should match the field names whenever possible.
 * Reject the snake-case, embrace the true identity of your fields. Enjoy straightforward configs!
 * <p>
 * Note: Due to compatibility reasons, the bugged behavior of SNAKE_CASE and HYPHEN_CASE will never get fixed.
 * <p>
 * Example bugs with anything other than {@link NameStrategy#IDENTITY}:
 * - myVectorY -> my-vectory
 * - myServiceAPI -> my-service-a-pi
 *
 * @deprecated Legacy compatibility use annotation with bugged implementation. It's also regex-based, do I need to say more?
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Names {

    NameStrategy strategy();

    NameModifier modifier() default NameModifier.NONE;
}
