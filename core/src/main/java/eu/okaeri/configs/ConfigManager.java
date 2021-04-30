package eu.okaeri.configs;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigManager {

    public static <T extends OkaeriConfig> T create(Class<T> clazz) throws OkaeriException {

        T config;
        try {
            config = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException exception) {
            throw new OkaeriException("cannot create " + clazz.getSimpleName() + " instance: " +
                    "make sure default constructor is available or if subconfig use new instead");
        }

        return initialize(config);
    }

    public static <T extends OkaeriConfig> T createUnsafe(Class<T> clazz) throws OkaeriException {

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

    public static <T extends OkaeriConfig> T transformCopy(OkaeriConfig config, Class<T> into) throws OkaeriException {

        T copy = ConfigManager.createUnsafe(into);
        Configurer configurer = config.getConfigurer();

        copy.withConfigurer(configurer);
        copy.withBindFile(config.getBindFile());
        ConfigDeclaration copyDeclaration = copy.getDeclaration();

        configurer.getAllKeys().stream()
                .map(copyDeclaration::getField)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(field -> {

                    Object value = configurer.getValue(field.getName());
                    GenericsDeclaration generics = GenericsDeclaration.of(value);

                    if ((value != null) && ((field.getType().getType() != value.getClass()) || (!generics.isPrimitiveWrapper() && !generics.isPrimitive()))) {
                        value = configurer.resolveType(value, generics, field.getType().getType(), field.getType());
                    }

                    field.updateValue(value);
                });

        return copy;
    }

    public static <T extends OkaeriConfig> T deepCopy(OkaeriConfig config, Configurer newConfigurer, Class<T> into) throws OkaeriException {
        T copy = ConfigManager.createUnsafe(into);
        copy.withConfigurer(newConfigurer, config.getConfigurer().getRegistry().allSerdes());
        copy.withBindFile(config.getBindFile());
        copy.load(config.saveToString());
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
