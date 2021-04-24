package eu.okaeri.configs;

import eu.okaeri.configs.exception.OkaeriException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ConfigManager {

    public static <T extends OkaeriConfig> T create(Class<T> clazz) throws OkaeriException {

        T config;
        try {
            config = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException exception) {
            try {
                //noinspection unchecked
                config = (T) allocateInstance(clazz);
            } catch (Exception exception1) {
                throw new OkaeriException("failed to create " + clazz + " instance, neither default constructor available, nor unsafe succeeded");
            }
        }

        return initialize(config);
    }

    public static <T extends OkaeriConfig> T create(Class<T> clazz, OkaeriConfigInitializer initializer) throws OkaeriException {
        T config = create(clazz);
        try {
            initializer.apply(config);
        } catch (Exception exception) {
            if (config.getConfigurer() != null) {
                throw new OkaeriException("failed to initialize " + clazz.getName() + " [" + config.getConfigurer().getClass() + "]", exception);
            }
            throw new OkaeriException("failed to initialize " + clazz.getName(), exception);
        }
        return config;
    }

    public static <T extends OkaeriConfig> T copy(OkaeriConfig config, Class<T> into) throws OkaeriException {
        T copy = ConfigManager.create(into);
        copy.withConfigurer(config.getConfigurer(), config.getConfigurer().getRegistry().allSerdes());
        copy.withBindFile(config.getBindFile());
        config.getConfigurer().getAllKeys().forEach(key -> copy.set(key, config.get(key)));
        return copy;
    }

    public static <T extends OkaeriConfig> T initialize(T config) {
        config.updateDeclaration();
        return config;
    }

    private static Object allocateInstance(Class<?> clazz) throws Exception {
        Class<?> unsafeClazz = Class.forName("sun.misc.Unsafe");
        Field theUnsafeField = unsafeClazz.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        Object unsafeInstance = theUnsafeField.get(null);
        Method allocateInstance = unsafeClazz.getDeclaredMethod("allocateInstance", Class.class);
        return allocateInstance.invoke(unsafeInstance, clazz);
    }
}
