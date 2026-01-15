package eu.okaeri.configs.serdes;

import lombok.NonNull;

/**
 * Processes raw configuration values before type resolution.
 * <p>
 * Implementations can transform string values loaded from config files
 * before they are converted to their target types. Common use cases include:
 * <ul>
 *   <li>Environment variable placeholder resolution (${VAR})</li>
 *   <li>Value encryption/decryption</li>
 *   <li>String interpolation</li>
 *   <li>Custom placeholder formats</li>
 * </ul>
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * ConfigManager.create(MyConfig.class, it -> {
 *     it.configure(opt -> {
 *         opt.configurer(new YamlBukkitConfigurer());
 *         opt.resolvePlaceholders();  // enables environment placeholder resolution
 *     });
 *     it.load();
 * });
 * }</pre>
 * <p>
 * The return type {@link PreProcessResult} allows processors to indicate:
 * <ul>
 *   <li>{@link PreProcessResult#noop()} - no change, use original value</li>
 *   <li>{@link PreProcessResult#runtimeOnly(Object)} - changed, but preserve original for saving</li>
 *   <li>{@link PreProcessResult#transformed(Object)} - changed, write new value to file</li>
 * </ul>
 *
 * @see PreProcessResult
 * @see eu.okaeri.configs.serdes.standard.EnvironmentPlaceholderProcessor
 */
@FunctionalInterface
public interface ValuePreProcessor {

    /**
     * Process a raw value before type resolution.
     * <p>
     * This method is called for each field value during configuration loading,
     * before the value is converted to the target field type.
     * <p>
     * Return {@link PreProcessResult#noop()} when no processing is needed.
     * This allows efficient handling of values that don't require transformation.
     *
     * @param value   the raw value from config file (usually String)
     * @param context context with field info, path, and configurer
     * @return result indicating whether and how the value was modified
     */
    PreProcessResult process(Object value, @NonNull SerdesContext context);
}
