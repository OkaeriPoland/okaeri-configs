package eu.okaeri.configs.migrate.builtin.special;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.function.Predicate;

@ToString
@RequiredArgsConstructor
public class SimplePredicateMigration<T> implements ConfigMigration {

    private final String key;
    private final Predicate<T> predicate;

    @Override
    @SuppressWarnings("unchecked")
    public boolean migrate(@NonNull OkaeriConfig config, @NonNull RawConfigView view) {

        if (!view.exists(this.key)) {
            return false;
        }

        T value = (T) view.get(this.key);
        return this.predicate.test(value);
    }
}
