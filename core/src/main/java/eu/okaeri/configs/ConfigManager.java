package eu.okaeri.configs;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.util.UnsafeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for creating and managing OkaeriConfig instances.
 * Provides factory methods for config creation, copying, and initialization.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigManager {

    /**
     * Creates a new config instance using the default constructor.
     *
     * @param clazz the config class to instantiate
     * @param <T>   the config type
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
     * <p>
     * <b>Note:</b> This method does NOT automatically initialize the declaration.
     * If you need the declaration initialized (e.g., for @ReadOnly to work correctly),
     * call {@link #initialize(OkaeriConfig)} after creation.
     * <p>
     * Useful for internal operations where constructor invocation is not needed.
     *
     * @param clazz the config class to instantiate
     * @param <T>   the config type
     * @return uninitialized config instance
     * @throws OkaeriException if unsafe allocation fails
     */
    public static <T extends OkaeriConfig> T createUnsafe(@NonNull Class<T> clazz) throws OkaeriException {
        return UnsafeUtil.allocateInstance(clazz);
    }

    /**
     * Creates a new config instance and applies custom initialization logic.
     * The initializer is typically used to configure the instance (e.g., set configurer, bind file).
     *
     * @param clazz       the config class to instantiate
     * @param initializer function to customize the config after creation
     * @param <T>         the config type
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
     * Copies field values from the source config to matching fields in the target config.
     * Values are read from source fields first (capturing programmatic changes), with fallback
     * to configurer data (preserving orphans for wrapper pattern support).
     * <p>
     * Useful for converting between different config types with overlapping fields (e.g., Document wrapper pattern in okaeri-persistence).
     * The source and target share the same configurer instance.
     * <p>
     * Type transformations are applied automatically when field types don't match.
     *
     * @param config the source config to copy from
     * @param into   the target config class
     * @param <T>    the target config type
     * @return new config instance of target type with copied values
     * @throws OkaeriException if transformation fails
     */
    public static <T extends OkaeriConfig> T transformCopy(@NonNull OkaeriConfig config, @NonNull Class<T> into) throws OkaeriException {

        T copy = initialize(ConfigManager.createUnsafe(into));
        Configurer configurer = config.getConfigurer();
        copy.setConfigurer(configurer);

        ConfigDeclaration copyDeclaration = copy.getDeclaration();
        ConfigDeclaration sourceDeclaration = config.getDeclaration();

        if (config.getBindFile() != null) {
            copy.setBindFile(config.getBindFile());
        }

        // Process each field in target declaration
        Map<String, Object> sourceState = config.getInternalState();

        copyDeclaration.getFields().forEach(targetField -> {
            String fieldName = targetField.getName();
            Object value;
            GenericsDeclaration generics;

            // Try to read from source field first (captures programmatic changes)
            Optional<FieldDeclaration> sourceField = sourceDeclaration.getField(fieldName);
            if (sourceField.isPresent()) {
                value = sourceField.get().getValue();

                // If value is OkaeriConfig or needs serialization, simplify it first
                // This ensures nested objects are properly serialized before transformation
                if ((value != null) && ((value instanceof OkaeriConfig) || !value.getClass().isPrimitive())) {
                    value = configurer.simplify(value, sourceField.get().getType(), SerdesContext.of(configurer, sourceField.get()), false);
                }

                generics = GenericsDeclaration.of(value);
            }
            // Fallback to internalState (preserves orphans)
            else if (sourceState != null && sourceState.containsKey(fieldName)) {
                value = sourceState.get(fieldName);
                generics = GenericsDeclaration.of(value);
            }
            // Field doesn't exist in source
            else {
                return; // Skip this field
            }

            // Apply type transformation if needed
            if ((value != null) && ((targetField.getType().getType() != value.getClass()) || (!generics.isPrimitiveWrapper() && !generics.isPrimitive()))) {
                value = configurer.resolveType(value, generics, targetField.getType().getType(), targetField.getType(), SerdesContext.of(configurer, targetField));
            }

            targetField.updateValue(value);
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
     * @param config        the source config to copy from
     * @param newConfigurer the configurer instance for the copy (must support same format as source)
     * @param into          the target config class
     * @param <T>           the target config type
     * @return new config instance with copied data and new configurer
     * @throws OkaeriException if serialization or deserialization fails
     */
    public static <T extends OkaeriConfig> T deepCopy(@NonNull OkaeriConfig config, @NonNull Configurer newConfigurer, @NonNull Class<T> into) throws OkaeriException {

        T copy = initialize(ConfigManager.createUnsafe(into));
        newConfigurer.register(config.getConfigurer().getRegistry().allSerdes());
        copy.setConfigurer(newConfigurer);

        if (config.getBindFile() != null) {
            copy.setBindFile(config.getBindFile());
        }

        // special handling for InMemoryConfigurer - copy internalState directly
        if (config.getConfigurer() instanceof InMemoryConfigurer) {
            // Build internalState from fields
            Map<String, Object> state = new LinkedHashMap<>();
            for (FieldDeclaration field : config.getDeclaration().getFields()) {
                Object simplified = newConfigurer.simplifyField(field.getValue(), field.getType(), field);
                state.put(field.getName(), simplified);
            }
            copy.setInternalState(state);
            copy.update();
            return copy;
        }

        copy.load(config.saveToString());
        return copy;
    }

    /**
     * Initializes a config instance by eagerly loading its declaration.
     * <p>
     * This ensures that field starting values (used by @ReadOnly) are captured
     * at the correct time - immediately after construction, before any modifications.
     * <p>
     * Called automatically by all {@link #create} methods.
     * Can be called explicitly if you manually instantiate a config class.
     *
     * @param config the config instance to initialize
     * @param <T>    the config type
     * @return the same config instance (for chaining)
     */
    public static <T extends OkaeriConfig> T initialize(@NonNull T config) {
        config.getDeclaration();
        return config;
    }
}
