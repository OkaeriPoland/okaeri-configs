package eu.okaeri.configs.migrate.builtin;

import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.builtin.special.SimpleMultiMigration;
import lombok.Getter;
import lombok.ToString;

@ToString
public class NamedMigration extends SimpleMultiMigration {

    private @Getter final String description;

    public NamedMigration(String description, ConfigMigration... migrations) {
        super(migrations);
        this.description = description;
    }
}
