package eu.okaeri.configs.migrate.builtin.special;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public class SimpleConditionalMigration implements ConfigMigration {

    private final ConfigMigration when;
    private final ConfigMigration migrationTrue;
    private final ConfigMigration migrationFalse;

    @Override
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull RawConfigView view) {

        if (this.when.migrate(config, view)) {
            return this.migrationTrue.migrate(config, view);
        }

        return this.migrationFalse.migrate(config, view);
    }
}
