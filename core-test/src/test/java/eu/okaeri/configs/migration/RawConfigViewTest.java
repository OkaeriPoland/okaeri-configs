package eu.okaeri.configs.migration;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.migrate.view.RawConfigView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for RawConfigView - raw map-based config access with nested path support.
 */
class RawConfigViewTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 42;
        private NestedConfig nested = new NestedConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
        private String field = "nested value";
        private int number = 100;
    }

    @Test
    void testExists_TopLevelKey_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean exists = view.exists("name");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExists_NonExistentKey_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean exists = view.exists("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testExists_NestedKey_ReturnsTrue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean exists = view.exists("nested.field");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExists_DeepNestedNonExistent_ReturnsFalse() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        boolean exists = view.exists("nested.nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testGet_TopLevelKey_ReturnsValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        Object value = view.get("name");

        // Then
        assertThat(value).isEqualTo("default");
    }

    @Test
    void testGet_NestedKey_ReturnsValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        Object value = view.get("nested.field");

        // Then
        assertThat(value).isEqualTo("nested value");
    }

    @Test
    void testGet_NonExistentKey_ReturnsNull() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        Object value = view.get("nonexistent");

        // Then
        assertThat(value).isNull();
    }

    @Test
    void testGet_InvalidNestedPath_ReturnsNull() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When - "name" is a String, can't traverse into it
        Object result = view.get("name.invalid");

        // Then - returns null rather than throwing
        assertThat(result).isNull();
    }

    @Test
    void testSet_TopLevelKey_UpdatesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        Object oldValue = view.set("name", "new name");

        // Then
        assertThat(oldValue).isEqualTo("default");
        assertThat(config.getName()).isEqualTo("new name");
    }

    @Test
    void testSet_NestedKey_UpdatesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        Object oldValue = view.set("nested.field", "updated");

        // Then
        assertThat(oldValue).isEqualTo("nested value");
        assertThat(config.getNested().getField()).isEqualTo("updated");
    }

    @Test
    void testSet_NewKey_CreatesKey() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        Object oldValue = view.set("newKey", "new value");

        // Then
        assertThat(oldValue).isNull();
        assertThat(view.get("newKey")).isEqualTo("new value");
    }

    @Test
    void testSet_NewNestedPath_CreatesIntermediateMaps() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        view.set("new.nested.path", "deep value");

        // Then
        assertThat(view.get("new.nested.path")).isEqualTo("deep value");
    }

    @Test
    void testSet_TypeConflict_ThrowsException() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When/then - "name" is a String, can't treat it as a map
        assertThatThrownBy(() -> view.set("name.invalid", "value"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot insert 'name.invalid'")
            .hasMessageContaining("type conflict");
    }

    @Test
    void testRemove_TopLevelKey_RemovesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // Add a dynamic (undeclared) key to test removal
        view.set("dynamicKey", "dynamic value");

        // When
        Object oldValue = view.remove("dynamicKey");

        // Then
        assertThat(oldValue).isEqualTo("dynamic value");
        assertThat(view.exists("dynamicKey")).isFalse();
    }

    @Test
    void testRemove_NestedKey_RemovesValue() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // Add a dynamic nested key to test removal
        view.set("dynamic.nested.key", "nested value");

        // When
        Object oldValue = view.remove("dynamic.nested.key");

        // Then
        assertThat(oldValue).isEqualTo("nested value");
        assertThat(view.exists("dynamic.nested.key")).isFalse();
    }

    @Test
    void testRemove_NonExistentKey_ReturnsNull() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        Object oldValue = view.remove("nonexistent");

        // Then
        assertThat(oldValue).isNull();
    }

    @Test
    void testRemove_InvalidNestedPath_ReturnsNull() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        Object oldValue = view.remove("name.invalid");

        // Then
        assertThat(oldValue).isNull();
    }

    @Test
    void testCustomSeparator_UsesCustomSeparator() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config, "/");

        // When
        Object value = view.get("nested/field");

        // Then
        assertThat(value).isEqualTo("nested value");
    }

    @Test
    void testSequentialOperations_MaintainsConsistency() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When - perform sequence of operations
        view.set("key1", "value1");
        view.set("key2", "value2");
        view.set("nested.key3", "value3");
        view.remove("key1");

        // Then
        assertThat(view.exists("key1")).isFalse();
        assertThat(view.get("key2")).isEqualTo("value2");
        assertThat(view.get("nested.key3")).isEqualTo("value3");
    }

    @Test
    void testMutatesConfig_AfterSet_ConfigUpdated() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);

        // When
        view.set("value", 999);

        // Then - verify config field is updated
        assertThat(config.getValue()).isEqualTo(999);
    }

    @Test
    void testMutatesConfig_AfterRemove_ConfigUpdated() {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new InMemoryConfigurer());
        RawConfigView view = new RawConfigView(config);
        view.set("tempField", "temporary");

        // When
        view.remove("tempField");

        // Then - verify dynamic field is removed
        assertThat(view.exists("tempField")).isFalse();
    }
}
