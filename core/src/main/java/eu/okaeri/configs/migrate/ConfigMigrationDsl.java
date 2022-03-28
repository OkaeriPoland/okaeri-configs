package eu.okaeri.configs.migrate;

import eu.okaeri.configs.migrate.builtin.action.*;
import eu.okaeri.configs.migrate.builtin.special.*;
import lombok.NonNull;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface ConfigMigrationDsl {

    static ConfigMigration copy(@NonNull String fromKey, @NonNull String toKey) {
        return new SimpleCopyMigration(fromKey, toKey);
    }

    static ConfigMigration delete(@NonNull String key) {
        return new SimpleDeleteMigration(key);
    }

    static ConfigMigration move(@NonNull String fromKey, @NonNull String toKey) {
        return new SimpleMoveMigration(fromKey, toKey);
    }

    static ConfigMigration supply(@NonNull String key, @NonNull Supplier supplier) {
        return new SimpleSupplyMigration(key, supplier);
    }

    static ConfigMigration update(@NonNull String key, @NonNull Function<Object, Object> function) {
        return new SimpleUpdateMigration(key, function);
    }


    static ConfigMigration when(@NonNull ConfigMigration when, @NonNull ConfigMigration migrationTrue, @NonNull ConfigMigration migrationFalse) {
        return new SimpleConditionalMigration(when, migrationTrue, migrationFalse);
    }

    static ConfigMigration when(@NonNull ConfigMigration when, @NonNull ConfigMigration migrationTrue) {
        return new SimpleConditionalMigration(when, migrationTrue, noop(false));
    }

    static ConfigMigration exists(@NonNull String key) {
        return new SimpleExistsMigration(key);
    }

    static ConfigMigration multi(@NonNull ConfigMigration... migrations) {
        return new SimpleMultiMigration(migrations);
    }

    static ConfigMigration any(@NonNull ConfigMigration... migrations) {
        return new SimpleMultiMigration(migrations);
    }

    static ConfigMigration all(@NonNull ConfigMigration... migrations) {
        return new SimpleMultiMigration(migrations, true);
    }

    static ConfigMigration noop(boolean result) {
        return new SimpleNoopMigration(result);
    }

    static ConfigMigration not(@NonNull ConfigMigration migration) {
        return new SimpleNotMigration(migration);
    }

    static <T> ConfigMigration match(@NonNull String key, @NonNull Predicate<T> predicate) {
        return new SimplePredicateMigration<>(key, predicate);
    }
}
