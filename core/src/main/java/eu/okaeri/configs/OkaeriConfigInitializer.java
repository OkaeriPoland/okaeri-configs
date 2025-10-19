package eu.okaeri.configs;

import java.util.function.Consumer;

public interface OkaeriConfigInitializer extends Consumer<OkaeriConfig> {
    void apply(OkaeriConfig config) throws Exception;
}
