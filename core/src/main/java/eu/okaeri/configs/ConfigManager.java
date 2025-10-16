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

/**
 * Utility class for creating and managing OkaeriConfig instances.
 * Provides factory methods for config creation, copying, and initialization.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigManager {

    /**
     * Creates a new config instance using the default constructor.
     * Automatically calls {@link #initialize(OkaeriConfig)} to set up the declaration.
     *
     * @param clazz the config class to instantiate
     * @param <T> the config type
     * @return initialized config instance
     * @throws OkaeriException if the class cannot be instantiated (no default constructor or other instantiation error)
     */
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

    /**
     * Creates a new config instance using unsafe allocation (bypasses constructor).
     * Useful for internal operations where constructor invocation is not needed.
     * Automatically calls {@link #initialize(OkaeriConfig)} to set up the declaration.
     *
     * @param clazz the config class to instantiate
     * @param <T> the config type
     * @return initialized config instance
     * @throws OkaeriException if unsafe allocation fails
     */
    public static <T extends OkaeriConfig> T createUnsafe(@NonNull Class<T> clazz) throws OkaeriException {
        return initialize(UnsafeUtil.allocateInstance(clazz));
    }

    /**
     * Creates a new config instance and applies custom initialization logic.
     * The initializer is typically used to configure the instance (e.g., set configurer, bind file).
     *
     * @param clazz the config class to instantiate
     * @param initializer function to customize the config after creation
     * @param <T> the config type
     * @return initialized and customized config instance
     * @throws OkaeriException if creation or initialization fails
     */
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

    /**
     * Creates a copy of a config by transforming it to a target type.
     * <p>
     * Copies field values from the source config's configurer to matching fields in the target config.
     * Useful for converting between different config types with overlapping fields (e.g., Document wrapper pattern in okaeri-persistence).
     * The source and target share the same configurer instance.
     * <p>
     * Type transformations are applied automatically when field types don't match.
     *
     * @param config the source config to copy from
     * @param into the target config class
     * @param <T> the target config type
     * @return new config instance of target type with copied values
     * @throws OkaeriException if transformation fails
     */
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

    /**
     * Creates a deep copy of a config with a new configurer instance.
     * <p>
     * Serializes the source config to string and loads it into the target config with a fresh configurer.
     * Copies all custom serializers from the source configurer's registry to the new configurer.
     * <p>
     * Useful for creating independent copies with different backing storage while preserving all data.
     * Both configurers must support the same format (use same serialization format).
     *
     * @param config the source config to copy from
     * @param newConfigurer the configurer instance for the copy (must support same format as source)
     * @param into the target config class
     * @param <T> the target config type
     * @return new config instance with copied data and new configurer
     * @throws OkaeriException if serialization or deserialization fails
     */
    public static <T extends OkaeriConfig> T deepCopy(@NonNull OkaeriConfig config, @NonNull Configurer newConfigurer, @NonNull Class<T> into) throws OkaeriException {
        T copy = ConfigManager.createUnsafe(into);
        copy.withConfigurer(newConfigurer, config.getConfigurer().getRegistry().allSerdes());
        if (config.getBindFile() != null) {
            copy.withBindFile(config.getBindFile());
        }
        copy.load(config.saveToString());
        return copy;
    }

    /**
     * Initializes a config instance by generating its field declaration metadata.
     * Called automatically by {@link #create} methods.
     * <p>
     * Can be called explicitly if you manually instantiate a config class.
     *
     * @param config the config instance to initialize
     * @param <T> the config type
     * @return the same config instance (for chaining)
     */
    public static <T extends OkaeriConfig> T initialize(@NonNull T config) {
        config.updateDeclaration();
        return config;
    }
}
