package eu.okaeri.configs;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ConfigManager {

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T extends OkaeriConfig> T create(Class<? extends OkaeriConfig> clazz) {

        OkaeriConfig config;
        try {
            config = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException exception) {
            Class<?> unsafeClazz = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClazz.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            Object unsafeInstance = theUnsafeField.get(null);
            Method allocateInstance = unsafeClazz.getDeclaredMethod("allocateInstance", Class.class);
            config = ((OkaeriConfig) allocateInstance.invoke(unsafeInstance, clazz));
        }

        return (T) initialize(config);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T extends OkaeriConfig> T create(Class<? extends OkaeriConfig> clazz, OkaeriConfigInitializer initializer) {
        OkaeriConfig config = create(clazz);
        initializer.apply(config);
        return (T) config;
    }

    @SneakyThrows
    public static <T extends OkaeriConfig> T initialize(T config) {
        config.updateDeclaration();
        return config;
    }
}
