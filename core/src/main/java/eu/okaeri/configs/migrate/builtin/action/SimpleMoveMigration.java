package eu.okaeri.configs.migrate.builtin.action;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.function.Function;

@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class SimpleMoveMigration implements ConfigMigration {

    private final String fromKey;
    private final String toKey;
    private Function<Object, Object> updateFunction;

    @Override
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull RawConfigView view) {

        if (!view.exists(this.fromKey)) {
            return false;
        }

        Object targetValue = view.remove(this.fromKey);
        if (this.updateFunction == null) {
            view.set(this.toKey, targetValue);
            return true;
        }

        Object updatedValue = this.updateFunction.apply(targetValue);
        view.set(this.toKey, updatedValue);
        return true;
    }
}
