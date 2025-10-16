package eu.okaeri.configs.migration;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ConfigMigrationDsl - fluent DSL for creating common migrations.
 */
class ConfigMigrationDslTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String oldKey = "old value";
        private String newKey = "default";
        private String tempKey = "temp";
        private int counter = 0;
        private String status = "inactive";
    }

    // === ACTION MIGRATIONS ===

    @Test
    void testCopy_ExistingKey_CopiesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = copy("oldKey", "newKey").migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("oldKey")).isEqualTo("old value");
        assertThat(view.get("newKey")).isEqualTo("old value");
    }

    @Test
    void testCopy_NonExistentKey_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = copy("nonexistent", "newKey").migrate(config, view);

        // Then
        assertThat(result).isFalse();
        assertThat(view.get("newKey")).isEqualTo("default"); // Unchanged
    }

    @Test
    void testCopy_ToExistingKey_OverwritesAndReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("newKey", "existing");

        // When
        boolean result = copy("oldKey", "newKey").migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("newKey")).isEqualTo("old value");
    }

    @Test
    void testDelete_ExistingKey_RemovesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        
        // Add dynamic key to test deletion
        view.set("dynamicKey", "value");

        // When
        boolean result = delete("dynamicKey").migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.exists("dynamicKey")).isFalse();
    }

    @Test
    void testDelete_NonExistentKey_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = delete("nonexistent").migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testMove_ExistingKey_MovesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        
        // Add dynamic key to test move
        view.set("dynamicFrom", "old value");

        // When
        boolean result = move("dynamicFrom", "dynamicTo").migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.exists("dynamicFrom")).isFalse();
        assertThat(view.get("dynamicTo")).isEqualTo("old value");
    }

    @Test
    void testMove_NonExistentKey_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = move("nonexistent", "newKey").migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testMove_WithUpdateFunction_TransformsValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("dynamicFrom", "lowercase");

        // When
        boolean result = move("dynamicFrom", "dynamicTo", value -> ((String) value).toUpperCase())
            .migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.exists("dynamicFrom")).isFalse();
        assertThat(view.get("dynamicTo")).isEqualTo("LOWERCASE");
    }

    @Test
    void testMove_WithUpdateFunction_NonExistent_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = move("nonexistent", "newKey", value -> value)
            .migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testSupply_CreatesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = supply("nonExistentKey", () -> "supplied value").migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("nonExistentKey")).isEqualTo("supplied value");
    }

    @Test
    void testSupply_ToExistingKey_SkipsAndReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("existingKey", "old");

        // When - supply() returns false if key already exists
        boolean result = supply("existingKey", () -> "new").migrate(config, view);

        // Then
        assertThat(result).isFalse();
        assertThat(view.get("existingKey")).isEqualTo("old"); // Unchanged
    }

    @Test
    void testUpdate_ExistingKey_UpdatesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("counter", 5);

        // When
        boolean result = update("counter", value -> ((Integer) value) + 10).migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("counter")).isEqualTo(15);
    }

    @Test
    void testUpdate_NonExistentKey_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = update("nonexistent", value -> value).migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    // === SPECIAL MIGRATIONS ===

    @Test
    void testWhen_ConditionTrue_ExecutesTrueBranch() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        
        // Add dynamic key for testing move
        view.set("dynamicFrom", "old value");

        // When
        boolean result = when(
            exists("dynamicFrom"),
            move("dynamicFrom", "dynamicTo"),
            noop(false)
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.exists("dynamicFrom")).isFalse();
        assertThat(view.get("dynamicTo")).isEqualTo("old value");
    }

    @Test
    void testWhen_ConditionFalse_ExecutesFalseBranch() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = when(
            exists("nonexistent"),
            noop(false),
            supply("newStatus", () -> "executed false branch")
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("newStatus")).isEqualTo("executed false branch");
    }

    @Test
    void testWhen_WithoutElse_ConditionTrue_ExecutesTrueBranch() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = when(
            exists("oldKey"),
            supply("newStatus", () -> "condition met")
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("newStatus")).isEqualTo("condition met");
    }

    @Test
    void testWhen_WithoutElse_ConditionFalse_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = when(
            exists("nonexistent"),
            supply("status", () -> "should not execute")
        ).migrate(config, view);

        // Then
        assertThat(result).isFalse();
        assertThat(view.get("status")).isEqualTo("inactive"); // Unchanged
    }

    @Test
    void testExists_ExistingKey_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = exists("oldKey").migrate(config, view);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testExists_NonExistentKey_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = exists("nonexistent").migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testMulti_AllSucceed_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = multi(
            copy("oldKey", "copy1"),
            copy("oldKey", "copy2"),
            supply("newField", () -> "value")
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("copy1")).isEqualTo("old value");
        assertThat(view.get("copy2")).isEqualTo("old value");
        assertThat(view.get("newField")).isEqualTo("value");
    }

    @Test
    void testMulti_OneFails_ContinuesExecution() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = multi(
            copy("oldKey", "copy1"),
            copy("nonexistent", "shouldFail"), // This will fail
            supply("newField", () -> "value")
        ).migrate(config, view);

        // Then - multi continues even if one fails
        assertThat(view.get("copy1")).isEqualTo("old value");
        assertThat(view.get("newField")).isEqualTo("value");
    }

    @Test
    void testAny_OneSucceeds_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = any(
            exists("nonexistent1"),
            exists("oldKey"), // This succeeds
            exists("nonexistent2")
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testAny_AllFail_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = any(
            exists("nonexistent1"),
            exists("nonexistent2"),
            exists("nonexistent3")
        ).migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testAll_AllSucceed_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = all(
            exists("oldKey"),
            exists("newKey"),
            exists("counter")
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testAll_OneFails_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = all(
            exists("oldKey"),
            exists("nonexistent"), // This fails
            exists("newKey")
        ).migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testNoop_WithTrue_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = noop(true).migrate(config, view);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testNoop_WithFalse_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = noop(false).migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testNot_TrueMigration_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = not(exists("oldKey")).migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testNot_FalseMigration_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = not(exists("nonexistent")).migrate(config, view);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testMatch_PredicateTrue_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("counter", 10);

        // When
        boolean result = match("counter", (Integer value) -> value > 5).migrate(config, view);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testMatch_PredicateFalse_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("counter", 3);

        // When
        boolean result = match("counter", (Integer value) -> value > 5).migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testMatch_NonExistentKey_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean result = match("nonexistent", (String value) -> true).migrate(config, view);

        // Then
        assertThat(result).isFalse();
    }

    // === COMPLEX SCENARIOS ===

    @Test
    void testComplexMigration_NestedConditionals() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        
        // Add dynamic key
        view.set("dynamicSource", "old value");

        // When - migrate dynamicSource to migratedKey only if dynamicSource exists and migratedKey doesn't
        boolean result = when(
            all(exists("dynamicSource"), not(exists("migratedKey"))),
            move("dynamicSource", "migratedKey")
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.exists("dynamicSource")).isFalse();
        assertThat(view.get("migratedKey")).isEqualTo("old value");
    }

    @Test
    void testComplexMigration_MultiStepWithConditions() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("migrationVersion", 1);
        view.set("dynamicData", "old value");

        // When - complex multi-step migration
        boolean result = multi(
            when(match("migrationVersion", (Integer v) -> v == 1),
                multi(
                    copy("dynamicData", "backup"),
                    move("dynamicData", "newData"),
                    update("migrationVersion", v -> 2)
                )
            ),
            supply("migrated", () -> true)
        ).migrate(config, view);

        // Then
        assertThat(view.get("backup")).isEqualTo("old value");
        assertThat(view.get("newData")).isEqualTo("old value");
        assertThat(view.get("migrationVersion")).isEqualTo(2);
        assertThat(view.get("migrated")).isEqualTo(true);
        assertThat(view.exists("dynamicData")).isFalse();
    }

    @Test
    void testComplexMigration_ConditionalWithTransformation() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("dynamicStatus", "inactive");

        // When
        boolean result = when(
            match("dynamicStatus", (String s) -> "inactive".equals(s)),
            move("dynamicStatus", "state", value -> ((String) value).toUpperCase())
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.exists("dynamicStatus")).isFalse();
        assertThat(view.get("state")).isEqualTo("INACTIVE");
    }

    @Test
    void testComplexMigration_AllOrNothingPattern() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.withConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When - only migrate if all required keys exist
        boolean result = when(
            all(exists("oldKey"), exists("newKey"), exists("counter")),
            multi(
                supply("allKeysPresent", () -> true),
                supply("migrationComplete", () -> "yes")
            )
        ).migrate(config, view);

        // Then
        assertThat(result).isTrue();
        assertThat(view.get("allKeysPresent")).isEqualTo(true);
        assertThat(view.get("migrationComplete")).isEqualTo("yes");
    }
}
