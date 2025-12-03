package eu.okaeri.configs;

import eu.okaeri.configs.exception.ValidationException;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.validator.ConfigValidator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.logging.Logger;

/**
 * Shared context for a configuration tree.
 * <p>
 * The root config creates a ConfigContext, and all nested configs
 * share the same context. This allows sharing settings like validators
 * across the entire config hierarchy.
 * <p>
 * Example:
 * <pre>{@code
 * RootConfig (context created here)
 *   └── NestedConfig (shares context)
 *         └── DeeplyNestedConfig (shares same context)
 * }</pre>
 */
public class ConfigContext {

    /**
     * The root OkaeriConfig that owns this context.
     */
    @Getter
    private final OkaeriConfig rootConfig;

    /**
     * Raw content from the source file for error reporting.
     * Set during load() to enable source-level error markers.
     */
    @Getter
    @Setter
    private String rawContent;

    /**
     * Whether to include comments above fields in error messages.
     */
    @Getter
    @Setter
    private boolean errorComments = false;

    /**
     * Whether to remove orphaned keys (keys in file but not in config class).
     */
    @Getter
    @Setter
    private boolean removeOrphans = false;

    /**
     * Single validator for field values.
     * If you need multiple validators, create a composite validator.
     */
    @Getter
    @Setter
    private ConfigValidator validator;

    /**
     * Logger for the configuration tree.
     * Shared across root and all nested configs.
     */
    @Getter
    @Setter
    private Logger logger;

    /**
     * Creates a new context for the given root config.
     *
     * @param rootConfig the root configuration that owns this context
     */
    public ConfigContext(@NonNull OkaeriConfig rootConfig) {
        this.rootConfig = rootConfig;
        this.logger = Logger.getLogger(rootConfig.getClass().getSimpleName());
    }

    /**
     * Returns true if a validator is registered.
     *
     * @return true if validator exists
     */
    public boolean hasValidator() {
        return this.validator != null;
    }

    /**
     * Validates a field value using the registered validator.
     *
     * @param declaration the field being validated
     * @param value the value to validate
     * @param isLoad true if this is during load, false if during save
     * @throws ValidationException if validator fails
     */
    public void validate(@NonNull FieldDeclaration declaration, Object value, boolean isLoad) {
        if (this.validator == null) {
            return;
        }

        // Check if validator should run for this operation
        if (isLoad && !this.validator.validateOnLoad()) {
            return;
        }
        if (!isLoad && !this.validator.validateOnSave()) {
            return;
        }

        // Run validation - may throw ValidationException
        if (!this.validator.isValid(declaration, value)) {
            throw new ValidationException(
                this.validator.getClass().getSimpleName() + " marked " + declaration.getName() +
                " as invalid without throwing an exception"
            );
        }
    }
}
