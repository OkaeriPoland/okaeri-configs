package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.serdes.ConfigPath;
import lombok.NonNull;

/**
 * Interface for format-specific source walkers that locate paths in raw content.
 * Implementations parse format-specific syntax to find where a config path
 * is located in the source for error reporting.
 */
public interface SourceWalker {

    /**
     * Finds the source location for the given config path.
     *
     * @param path the config path
     * @return the source location, or null if path not found
     */
    SourceLocation findPath(@NonNull ConfigPath path);
}
