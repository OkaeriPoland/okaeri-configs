package eu.okaeri.configs.util;

import eu.okaeri.configs.exception.OkaeriException;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @deprecated Internal use only. May and will change without warning.
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public final class UnsafeUtil {

    public static <T> T allocateInstance(@NonNull Class<T> clazz) throws OkaeriException {

        T instance;
        try {
            instance = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException exception) {
            try {
                //noinspection unchecked
                instance = (T) UnsafeUtil.allocateInstanceUnsafe(clazz);
            } catch (Exception exception1) {
                throw new OkaeriException("failed to create " + clazz + " instance, neither default constructor available, nor unsafe succeeded");
            }
        }

        return instance;
    }

    private static Object allocateInstanceUnsafe(@NonNull Class<?> clazz) throws Exception {
        Class<?> unsafeClazz = Class.forName("sun.misc.Unsafe");
        Field theUnsafeField = unsafeClazz.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        Object unsafeInstance = theUnsafeField.get(null);
        Method allocateInstance = unsafeClazz.getDeclaredMethod("allocateInstance", Class.class);
        return allocateInstance.invoke(unsafeInstance, clazz);
    }
}
