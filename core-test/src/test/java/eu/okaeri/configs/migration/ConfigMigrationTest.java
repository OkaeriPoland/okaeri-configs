package eu.okaeri.configs.migration;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for ConfigMigration interface - basic migration implementation patterns.
 */
class ConfigMigrationTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String oldField = "old value";
        private String newField = "default";
        private int version = 1;
    }

    @Test
    void testSimpleMigration_ReturnsTrue_WhenPerformed() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        ConfigMigration migration = (cfg, v) -> {
            v.set("version", 2);
            return true;
        };

        // When
        boolean result = migration.migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(config.getVersion()).isEqualTo(2);
    }

    @Test
    void testSimpleMigration_ReturnsFalse_WhenSkipped() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        ConfigMigration migration = (cfg, v) -> {
            // Migration decides to skip
            return false;
        };

        // When
        boolean result = migration.migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testMigration_AccessesConfig_Successfully() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        ConfigMigration migration = (cfg, v) -> {
            // Access config directly
            ((TestConfig) cfg).setNewField("migrated");
            return true;
        };

        // When
        boolean result = migration.migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(config.getNewField()).isEqualTo("migrated");
    }

    @Test
    void testMigration_AccessesRawConfigView_Successfully() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // Add a dynamic key to test removal behavior
        view.set("dynamicOldField", "old value");

        ConfigMigration migration = (cfg, v) -> {
            // Access via RawConfigView
            if (v.exists("dynamicOldField")) {
                Object value = v.get("dynamicOldField");
                v.set("newField", value);
                v.remove("dynamicOldField");
                return true;
            }
            return false;
        };

        // When
        boolean result = migration.migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("newField")).isEqualTo("old value");
        assertThat(view.exists("dynamicOldField")).isFalse();
    }

    @Test
    void testNamedMigration_WithName_ExecutesSuccessfully() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        ConfigMigration baseMigration = (cfg, v) -> {
            v.set("version", 2);
            return true;
        };

        ConfigMigration namedMigration = new NamedMigration("UpdateVersion", baseMigration);

        // When
        boolean result = namedMigration.migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(config.getVersion()).isEqualTo(2);
    }

    @Test
    void testNamedMigration_ToString_ContainsName() {
        // Given
        ConfigMigration baseMigration = (cfg, v) -> true;
        ConfigMigration namedMigration = new NamedMigration("MyMigration", baseMigration);

        // When
        String toString = namedMigration.toString();

        // Then
        assertThat(toString).contains("MyMigration");
    }

    @Test
    void testMultipleMigrations_InSequence_AllExecute() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        ConfigMigration migration1 = (cfg, v) -> {
            v.set("version", 2);
            return true;
        };

        ConfigMigration migration2 = (cfg, v) -> {
            v.set("newField", "updated");
            return true;
        };

        // When
        boolean result1 = migration1.migrate(config, view);
        boolean result2 = migration2.migrate(config, view);

        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        assertThat(config.getVersion()).isEqualTo(2);
        assertThat(config.getNewField()).isEqualTo("updated");
    }

    @Test
    void testMigration_ModifiesNestedStructure_Successfully() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // Setup nested structure
        view.set("section.subsection.value", "initial");

        ConfigMigration migration = (cfg, v) -> {
            if (v.exists("section.subsection.value")) {
                v.set("section.subsection.value", "migrated");
                return true;
            }
            return false;
        };

        // When
        boolean result = migration.migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("section.subsection.value")).isEqualTo("migrated");
    }

    @Test
    void testMigration_ConditionalExecution_WorksCorrectly() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        ConfigMigration migration = (cfg, v) -> {
            // Only migrate if version is 1
            Integer version = (Integer) v.get("version");
            if ((version != null) && (version == 1)) {
                v.set("version", 2);
                v.set("newField", "auto-migrated");
                return true;
            }
            return false;
        };

        // When
        boolean result = migration.migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(config.getVersion()).isEqualTo(2);
        assertThat(config.getNewField()).isEqualTo("auto-migrated");
    }

    @Test
    void testMigration_SkipsWhenConditionNotMet() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        config.setVersion(3); // Already migrated
        RawConfigView view = new RawConfigView(config);

        ConfigMigration migration = (cfg, v) -> {
            // Only migrate if version is 1
            Integer version = (Integer) v.get("version");
            if ((version != null) && (version == 1)) {
                v.set("version", 2);
                return true;
            }
            return false;
        };

        // When
        boolean result = migration.migrate(config, view);

        // Then
        assertThat(result).isFalse();
        assertThat(config.getVersion()).isEqualTo(3); // Unchanged
    }

    @Test
    void testMigration_WithTypeTransformation_WorksCorrectly() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // Setup old format (string version)
        view.set("versionString", "2");

        ConfigMigration migration = (cfg, v) -> {
            if (v.exists("versionString")) {
                String versionStr = (String) v.get("versionString");
                int versionInt = Integer.parseInt(versionStr);
                v.set("version", versionInt);
                v.remove("versionString");
                return true;
            }
            return false;
        };

        // When
        boolean result = migration.migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(config.getVersion()).isEqualTo(2);
        assertThat(view.exists("versionString")).isFalse();
    }

    @Test
    void testMigration_HandlesExceptions_Gracefully() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        ConfigMigration migration = (cfg, v) -> {
            try {
                // Attempt risky operation
                Object value = v.get("nonexistent");
                if (value != null) {
                    v.set("newField", value.toString());
                }
                return false; // Nothing to migrate
            } catch (Exception e) {
                return false; // Handle gracefully
            }
        };

        // When/then - should not throw
        assertThatCode(() -> migration.migrate(config, view))
            .doesNotThrowAnyException();
    }
}
