package eu.okaeri.configs.migrate.builtin;

import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.builtin.special.SimpleSequentialMigration;
import lombok.Getter;
import lombok.ToString;

@ToString
public class NamedMigration extends SimpleSequentialMigration {

    private @Getter final String name;

    public NamedMigration(String name, ConfigMigration... migrations) {
        super(migrations);
        this.name = name;
    }
}
