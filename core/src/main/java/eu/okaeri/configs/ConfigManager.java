package eu.okaeri.configs;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ConfigManager {

    @SneakyThrows
    public static <T extends OkaeriConfig> T create(Class<T> clazz) {

        T config;
        try {
            config = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException exception) {
            Class<?> unsafeClazz = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClazz.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            Object unsafeInstance = theUnsafeField.get(null);
            Method allocateInstance = unsafeClazz.getDeclaredMethod("allocateInstance", Class.class);
            //noinspection unchecked
            config = (T) allocateInstance.invoke(unsafeInstance, clazz);
        }

        return initialize(config);
    }

    @SneakyThrows
    public static <T extends OkaeriConfig> T create(Class<T> clazz, OkaeriConfigInitializer initializer) {
        T config = create(clazz);
        initializer.apply(config);
        return config;
    }

    @SneakyThrows
    public static <T extends OkaeriConfig> T initialize(T config) {
        config.updateDeclaration();
        return config;
    }
}
