package eu.okaeri.configs;

/**
 * Functional interface for initializing OkaeriConfig instances.
 * Supports lambda expressions with checked exception handling.
 */
@FunctionalInterface
public interface OkaeriConfigInitializer {

    /**
     * Accepts and initializes the config instance.
     *
     * @param config the config to initialize
     * @throws Exception if initialization fails
     */
    void apply(OkaeriConfig config) throws Exception;
}
