package eu.okaeri.configs.migrate.builtin.action;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.function.Supplier;

@ToString
@RequiredArgsConstructor
public class SimpleSupplyMigration implements ConfigMigration {

    private final String key;
    private final Supplier supplier;

    @Override
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull RawConfigView view) {

        if (view.exists(this.key)) {
            return false;
        }

        view.set(this.key, this.supplier.get());
        return true;
    }
}
