package eu.okaeri.configs.migrate.builtin.special;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.ConfigView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public class SimpleNotMigration implements ConfigMigration {

    private final ConfigMigration migration;

    @Override
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull ConfigView view) {
        return !this.migration.migrate(config, view);
    }
}
