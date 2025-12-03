package eu.okaeri.configs;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
     * @param serdesPacks the serdes packs to register
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions configurer(@NonNull Configurer configurer, @NonNull OkaeriSerdesPack... serdesPacks) {
        if (this.config.getConfigurer() != null) {
            configurer.setRegistry(this.config.getConfigurer().getRegistry());
        }
        this.config.setConfigurer(configurer);
        Arrays.stream(serdesPacks).forEach(this.config.getConfigurer()::register);
        return this;
    }

    /**
     * Registers a serdes pack in the current configurer.
     *
     * @param serdesPack the serdes pack to register
     * @return this configurer for chaining
     * @throws IllegalStateException if configurer is null
     */
    public OkaeriConfigOptions serdes(@NonNull OkaeriSerdesPack serdesPack) {
        if (this.config.getConfigurer() == null) {
            throw new IllegalStateException("configurer cannot be null");
        }
        this.config.getConfigurer().register(serdesPack);
        return this;
    }

    /**
     * Registers multiple serdes packs in the current configurer.
     *
     * @param serdesPacks the serdes packs to register
     * @return this configurer for chaining
     * @throws IllegalStateException if configurer is null
     */
    public OkaeriConfigOptions serdes(@NonNull OkaeriSerdesPack... serdesPacks) {
        if (this.config.getConfigurer() == null) {
            throw new IllegalStateException("configurer cannot be null");
        }
        Arrays.stream(serdesPacks).forEach(this.config.getConfigurer()::register);
        return this;
    }

    /**
     * Sets the logger for this configuration.
     *
     * @param logger the logger to use
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions logger(@NonNull Logger logger) {
        this.config.setLogger(logger);
        return this;
    }

    /**
     * Sets whether to automatically remove orphaned (undeclared) keys.
     *
     * @param removeOrphans true to remove orphans, false otherwise
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions removeOrphans(boolean removeOrphans) {
        this.config.setRemoveOrphans(removeOrphans);
        return this;
    }

    /**
     * Sets whether to include consecutive comments above the field in error messages.
     * When enabled, error messages will show all comment lines directly above the
     * field that caused the error, providing additional context.
     *
     * @param errorComments true to include comments in errors, false otherwise
     * @return this configurer for chaining
     */
    public OkaeriConfigOptions errorComments(boolean errorComments) {
        this.config.setErrorComments(errorComments);
        return this;
    }
}
