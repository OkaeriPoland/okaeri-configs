package eu.okaeri.configs.migrate.builtin.special;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.ConfigView;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Arrays;

@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class SimpleMultiMigration implements ConfigMigration {

    private final ConfigMigration[] migrations;
    private boolean requireAll = false;

    @Override
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull ConfigView view) {

        long performed = Arrays.stream(this.migrations)
            .filter(migration -> migration.migrate(config, view))
            .count();

        return this.requireAll
            ? (performed == this.migrations.length)
            : (performed > 0);
    }
}
