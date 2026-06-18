package eu.okaeri.configs.migrate.builtin.special;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.ConfigView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleNoopMigration implements ConfigMigration {

    private final boolean result;

    @Override
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull ConfigView view) {
        return this.result;
    }
}
