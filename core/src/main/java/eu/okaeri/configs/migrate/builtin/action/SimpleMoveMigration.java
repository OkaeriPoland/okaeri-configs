package eu.okaeri.configs.migrate.builtin.action;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public class SimpleMoveMigration implements ConfigMigration {

    private final String fromKey;
    private final String toKey;

    @Override
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull RawConfigView view) {

        if (!view.exists(this.fromKey)) {
            return false;
        }

        Object targetValue = view.remove(this.fromKey);
        Object oldValue = view.set(this.toKey, targetValue);

        return true;
    }
}
