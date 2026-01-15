package eu.okaeri.configs;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.serdes.ChainedPreProcessor;
import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.serdes.ValuePreProcessor;
import eu.okaeri.configs.serdes.standard.EnvironmentPlaceholderProcessor;
import eu.okaeri.configs.validator.ConfigValidator;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Fluent configuration helper for {@link OkaeriConfig}.
 * Provides a modern lambda-based API for configuring instances.
 * <p>
 * Example usage:
 * <pre>{@code
 * TestConfig config = ConfigManager.create(TestConfig.class, it -> {
 *     it.configure(opt -> {
 *         opt.configurer(new YamlBukkitConfigurer(), new SerdesBukkit());
 *         opt.bindFile(new File(this.getDataFolder(), "config.yml"));
 *         opt.removeOrphans(true);
 *     });
 *     it.saveDefaults();
 *     it.load(true);
 * });
 * }</pre>
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class OkaeriConfigOptions {

    private final OkaeriConfig config;

    /**
     * Sets the configuration file binding from a {@link File}.
     *
     * @param bindFile the file to bind
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions bindFile(@NonNull File bindFile) {
        this.config.setBindFile(bindFile.toPath());
        return this;
    }

    /**
     * Sets the configuration file binding from a {@link Path}.
     *
     * @param path the path to bind
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions bindFile(@NonNull Path path) {
        this.config.setBindFile(path);
        return this;
    }

    /**
     * Sets the configuration file binding from a pathname string.
     *
     * @param pathname the pathname to bind
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions bindFile(@NonNull String pathname) {
        this.config.setBindFile(Paths.get(pathname));
        return this;
    }

    /**
     * Replaces the current configurer with the provided one, preserving the existing
     * {@link eu.okaeri.configs.serdes.SerdesRegistry} if available.
     *
     * @param configurer the new configurer
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions configurer(@NonNull Configurer configurer) {
        if (this.config.getConfigurer() != null) {
            configurer.setRegistry(this.config.getConfigurer().getRegistry());
        }
        this.config.setConfigurer(configurer);
        return this;
    }

    /**
     * Replaces the current configurer with the provided one and registers the specified
     * serdes packs, preserving the existing {@link eu.okaeri.configs.serdes.SerdesRegistry}
     * if available.
     *
     * @param configurer the new configurer
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions configurer(@NonNull Configurer configurer, @NonNull OkaeriSerdes... serdes) {
        if (this.config.getConfigurer() != null) {
            configurer.setRegistry(this.config.getConfigurer().getRegistry());
        }
        this.config.setConfigurer(configurer);
        this.config.getConfigurer().getRegistry().add(serdes);
        return this;
    }

    /**
     * Registers one or more serdes components (transformers, serializers, packs, or annotation resolvers).
     * <p>
     * All serdes types implement {@link OkaeriSerdes}, allowing unified registration:
     * <pre>{@code
     * opt.serdes(
     *     new SerdesCommons(),           // pack
     *     new MyTransformer(),           // transformer
     *     new MySerializer(),            // serializer
     *     new MyAnnotationResolver()     // annotation resolver
     * );
     * }</pre>
     *
     * @param serdes the serdes components to register
     * @return this options for chaining
     * @throws IllegalStateException if configurer is null
     */
    public OkaeriConfigOptions serdes(@NonNull OkaeriSerdes... serdes) {
        if (this.config.getConfigurer() == null) {
            throw new IllegalStateException("configurer cannot be null");
        }
        this.config.getConfigurer().getRegistry().add(serdes);
        return this;
    }

    /**
     * Sets the logger for this configuration tree.
     *
     * @param logger the logger to use
     * @return this configurer for chaining
     * @throws IllegalStateException if context is not initialized (configurer not set)
     */
    public OkaeriConfigOptions logger(@NonNull Logger logger) {
        ConfigContext context = this.config.getContext();
        if (context == null) {
            throw new IllegalStateException("configurer must be set before setting logger");
        }
        context.setLogger(logger);
        return this;
    }

    /**
     * Sets whether to automatically remove orphaned (undeclared) keys.
     *
     * @param removeOrphans true to remove orphans, false otherwise
     * @return this configurer for chaining
     * @throws IllegalStateException if context is not initialized (configurer not set)
     */
    public OkaeriConfigOptions removeOrphans(boolean removeOrphans) {
        ConfigContext context = this.config.getContext();
        if (context == null) {
            throw new IllegalStateException("configurer must be set before setting removeOrphans");
        }
        context.setRemoveOrphans(removeOrphans);
        return this;
    }

    /**
     * Sets whether to include consecutive comments above the field in error messages.
     * When enabled, error messages will show all comment lines directly above the
     * field that caused the error, providing additional context.
     *
     * @param errorComments true to include comments in errors, false otherwise
     * @return this configurer for chaining
     * @throws IllegalStateException if context is not initialized (configurer not set)
     */
    public OkaeriConfigOptions errorComments(boolean errorComments) {
        ConfigContext context = this.config.getContext();
        if (context == null) {
            throw new IllegalStateException("configurer must be set before setting errorComments");
        }
        context.setErrorComments(errorComments);
        return this;
    }

    /**
     * Sets the validator for this configuration.
     * <p>
     * The validator is invoked during load and save operations to check field values.
     * Only one validator is supported. If you need multiple validators, create a
     * composite validator that delegates to multiple implementations.
     * <p>
     * Example usage:
     * <pre>{@code
     * config.configure(opt -> {
     *     opt.configurer(new YamlBukkitConfigurer());
     *     opt.validator(new OkaeriValidator());
     * });
     * }</pre>
     *
     * @param validator the validator to set
     * @return this configurer for chaining
     * @throws IllegalStateException if context is not initialized (configurer not set)
     */
    public OkaeriConfigOptions validator(@NonNull ConfigValidator validator) {
        ConfigContext context = this.config.getContext();
        if (context == null) {
            throw new IllegalStateException("configurer must be set before setting validator");
        }
        context.setValidator(validator);
        return this;
    }

    /**
     * Sets the value pre-processor for this configuration.
     * <p>
     * The pre-processor transforms raw values from the config file before type resolution.
     * This allows processing like environment variable placeholder resolution to happen
     * before values are converted to their target types.
     * <p>
     * Example usage:
     * <pre>{@code
     * config.configure(opt -> {
     *     opt.configurer(new YamlBukkitConfigurer());
     *     opt.valuePreProcessor(new EnvironmentPlaceholderProcessor());
     * });
     * }</pre>
     *
     * @param preProcessor the pre-processor to set
     * @return this options for chaining
     * @throws IllegalStateException if context is not initialized (configurer not set)
     * @see EnvironmentPlaceholderProcessor
     * @see #resolvePlaceholders()
     */
    public OkaeriConfigOptions valuePreProcessor(@NonNull ValuePreProcessor preProcessor) {
        ConfigContext context = this.config.getContext();
        if (context == null) {
            throw new IllegalStateException("configurer must be set before setting valuePreProcessor");
        }
        context.setValuePreProcessor(preProcessor);
        return this;
    }

    /**
     * Sets multiple value pre-processors for this configuration, chained in order.
     * <p>
     * Each processor receives the output of the previous one. This allows combining
     * built-in processors like {@link EnvironmentPlaceholderProcessor} with custom ones.
     * <p>
     * Example usage:
     * <pre>{@code
     * config.configure(opt -> {
     *     opt.configurer(new YamlBukkitConfigurer());
     *     opt.valuePreProcessor(
     *         new EnvironmentPlaceholderProcessor(),
     *         new MyCustomProcessor()
     *     );
     * });
     * }</pre>
     *
     * @param preProcessors the pre-processors to chain, in order
     * @return this options for chaining
     * @throws IllegalStateException if context is not initialized (configurer not set)
     * @throws IllegalArgumentException if no processors are provided
     * @see ChainedPreProcessor
     * @see #valuePreProcessor(ValuePreProcessor)
     */
    public OkaeriConfigOptions valuePreProcessor(@NonNull ValuePreProcessor... preProcessors) {
        if (preProcessors.length == 0) {
            throw new IllegalArgumentException("at least one pre-processor is required");
        }
        if (preProcessors.length == 1) {
            return this.valuePreProcessor(preProcessors[0]);
        }
        return this.valuePreProcessor(new ChainedPreProcessor(preProcessors));
    }

    /**
     * Enables environment variable placeholder resolution.
     * <p>
     * This is a convenience method that sets up {@link EnvironmentPlaceholderProcessor}
     * to resolve Spring Boot compatible placeholders:
     * <ul>
     *   <li>{@code ${VAR}} - resolves from environment</li>
     *   <li>{@code ${VAR:default}} - uses default value if variable is not set</li>
     *   <li>{@code $${VAR}} - escapes to literal {@code ${VAR}}</li>
     * </ul>
     * <p>
     * Example configuration file:
     * <pre>{@code
     * database:
     *   host: ${DB_HOST:localhost}
     *   port: ${DB_PORT:5432}
     *   password: ${DB_PASSWORD}
     * }</pre>
     * <p>
     * Example usage:
     * <pre>{@code
     * config.configure(opt -> {
     *     opt.configurer(new YamlBukkitConfigurer());
     *     opt.resolvePlaceholders();
     * });
     * }</pre>
     *
     * @return this options for chaining
     * @throws IllegalStateException if context is not initialized (configurer not set)
     * @see EnvironmentPlaceholderProcessor
     * @see #valuePreProcessor(ValuePreProcessor)
     */
    public OkaeriConfigOptions resolvePlaceholders() {
        return this.valuePreProcessor(new EnvironmentPlaceholderProcessor());
    }
}
