package eu.okaeri.configs.migrate.builtin.action;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.ConfigView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public class SimpleDeleteMigration implements ConfigMigration {

    private final String key;

    @Override
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull ConfigView view) {

        if (!view.exists(this.key)) {
            return false;
        }

        view.remove(this.key);
        return true;
    }
}
