package eu.okaeri.configs;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.util.UnsafeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigManager {

    public static <T extends OkaeriConfig> T create(@NonNull Class<T> clazz) throws OkaeriException {

        T config;
        try {
            config = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException exception) {
            throw new OkaeriException("cannot create " + clazz.getSimpleName() + " instance: " +
                "make sure default constructor is available or if subconfig use new instead");
        }

        return initialize(config);
    }

    public static <T extends OkaeriConfig> T createUnsafe(@NonNull Class<T> clazz) throws OkaeriException {
        return initialize(UnsafeUtil.allocateInstance(clazz));
    }

    public static <T extends OkaeriConfig> T create(@NonNull Class<T> clazz, @NonNull OkaeriConfigInitializer initializer) throws OkaeriException {
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

    public static <T extends OkaeriConfig> T transformCopy(@NonNull OkaeriConfig config, @NonNull Class<T> into) throws OkaeriException {

        T copy = ConfigManager.createUnsafe(into);
        Configurer configurer = config.getConfigurer();

        copy.withConfigurer(configurer);
        ConfigDeclaration copyDeclaration = copy.getDeclaration();

        if (config.getBindFile() != null) {
            copy.withBindFile(config.getBindFile());
        }

        configurer.getAllKeys().stream()
            .map(copyDeclaration::getField)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(field -> {

                Object value = configurer.getValue(field.getName());
                GenericsDeclaration generics = GenericsDeclaration.of(value);

                if ((value != null) && ((field.getType().getType() != value.getClass()) || (!generics.isPrimitiveWrapper() && !generics.isPrimitive()))) {
                    value = configurer.resolveType(value, generics, field.getType().getType(), field.getType(), SerdesContext.of(configurer, field));
                }

                field.updateValue(value);
            });

        return copy;
    }

    public static <T extends OkaeriConfig> T deepCopy(@NonNull OkaeriConfig config, @NonNull Configurer newConfigurer, @NonNull Class<T> into) throws OkaeriException {
        T copy = ConfigManager.createUnsafe(into);
        copy.withConfigurer(newConfigurer, config.getConfigurer().getRegistry().allSerdes());
        copy.withBindFile(config.getBindFile());
        copy.load(config.saveToString());
        return copy;
    }

    public static <T extends OkaeriConfig> T initialize(@NonNull T config) {
        config.updateDeclaration();
        return config;
    }
}
