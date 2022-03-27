package eu.okaeri.configs.migrate;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.NonNull;

@FunctionalInterface
public interface ConfigMigration {

    boolean migrate(@NonNull OkaeriConfig config, @NonNull RawConfigView view);
}
