package eu.okaeri.configs.integration;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for orphan field handling.
 * Tests how configs handle extra fields in files that don't match declared fields.
 */
class OrphanHandlingTest {

    @TempDir
    Path tempDir;

    // Test config classes
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private String declaredField = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
        private String nestedField = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ParentConfig extends OkaeriConfig {
        private NestedConfig declaredNested = new NestedConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigV1 extends OkaeriConfig {
        private String field1 = "value1";
        private String field2 = "value2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigV2 extends OkaeriConfig {
        private String field1 = "default1";
    }

    /**
     * Load config with extra fields (orphans)
     */
    @Test
    void testOrphans_LoadWithExtraFields_KeepsOrphansInConfigurer() throws Exception {
        File configFile = this.tempDir.resolve("orphans1.yml").toFile();

        // Create file with extra fields
        String yaml = """
            declaredField: "value1"
            orphanField1: "orphan value 1"
            orphanField2: 42
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.load();

        // Declared field should be loaded
        assertThat(config.getDeclaredField()).isEqualTo("value1");

        // Orphans should be in configurer
        assertThat(config.get("orphanField1")).isEqualTo("orphan value 1");
        assertThat(config.get("orphanField2")).isEqualTo(42);
    }

    /**
     * Save with removeOrphans=false (keeps orphans)
     */
    @Test
    void testOrphans_SaveWithRemoveOrphansFalse_KeepsOrphans() throws Exception {
        File configFile = this.tempDir.resolve("orphans2.yml").toFile();

        // Create file with orphans
        String yaml = """
            declaredField: "value1"
            orphanField: "orphan value"
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.load();

        // Modify declared field
        config.setDeclaredField("modified");

        // Save with orphan removal disabled
        config.withRemoveOrphans(false);
        config.save();

        // Read file and verify orphan is still there
        String content = TestUtils.readFile(configFile);
        assertThat(content).contains("orphanField");
        assertThat(content).contains("orphan value");
    }

    /**
     * Save with removeOrphans=true (removes orphans)
     */
    @Test
    void testOrphans_SaveWithRemoveOrphansTrue_RemovesOrphans() throws Exception {
        File configFile = this.tempDir.resolve("orphans3.yml").toFile();

        // Create file with orphans
        String yaml = """
            declaredField: "value1"
            orphanField: "orphan value"
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.load();

        // Save with orphan removal enabled (default)
        config.withRemoveOrphans(true);
        config.save();

        // Read file and verify orphan is gone
        String content = TestUtils.readFile(configFile);
        assertThat(content).doesNotContain("orphanField");
        assertThat(content).doesNotContain("orphan value");
        assertThat(content).contains("declaredField");
    }

    /**
     * Orphan detection and reporting via asMap
     */
    @Test
    void testOrphans_AsMapIncludesOrphans_WhenPresent() throws Exception {
        File configFile = this.tempDir.resolve("orphans4.yml").toFile();

        // Create file with orphans
        String yaml = """
            declaredField: "value1"
            orphan1: "orphan value 1"
            orphan2: 42
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.load();

        // Get as map - should include orphans
        var map = config.asMap(config.getConfigurer(), true);

        assertThat(map).containsKey("declaredField");
        assertThat(map).containsKey("orphan1");
        assertThat(map).containsKey("orphan2");
        assertThat(map.get("orphan1")).isEqualTo("orphan value 1");
        assertThat(map.get("orphan2")).isEqualTo(42);
    }

    /**
     * Orphan preservation across multiple saves (when removeOrphans=false)
     */
    @Test
    void testOrphans_PreservationAcrossMultipleSaves_WorksCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("orphans5.yml").toFile();

        // Create file with orphans
        String yaml = """
            declaredField: "value1"
            orphan1: "orphan value 1"
            orphan2: 42
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.withRemoveOrphans(false);
        config.load();

        // First save
        config.setDeclaredField("modified1");
        config.save();

        // Second save
        config.setDeclaredField("modified2");
        config.save();

        // Third save
        config.setDeclaredField("modified3");
        config.save();

        // Load fresh copy and verify orphans still exist
        SimpleConfig loaded = ConfigManager.create(SimpleConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        assertThat(loaded.getDeclaredField()).isEqualTo("modified3");
        assertThat(loaded.get("orphan1")).isEqualTo("orphan value 1");
        assertThat(loaded.get("orphan2")).isEqualTo(42);
    }

    /**
     * Complex orphan scenario: nested orphans
     */
    @Test
    void testOrphans_NestedOrphans_HandledCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("orphans6.yml").toFile();

        // Create file with nested orphans
        String yaml = """
            declaredNested:
              nestedField: "nested value"
              orphanInNested: "orphan in nested"
            orphanAtRoot: "orphan at root"
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        ParentConfig config = ConfigManager.create(ParentConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.withRemoveOrphans(false);
        config.load();

        // Verify declared nested field loaded
        assertThat(config.getDeclaredNested().getNestedField()).isEqualTo("nested value");

        // Verify root-level orphan
        assertThat(config.get("orphanAtRoot")).isEqualTo("orphan at root");

        // Save and verify orphans preserved
        config.save();

        String content = TestUtils.readFile(configFile);
        assertThat(content).contains("orphanAtRoot");
        assertThat(content).contains("orphanInNested");
    }

    /**
     * Orphan removal with nested configs
     */
    @Test
    void testOrphans_RemovalWithNestedConfigs_RemovesAllOrphans() throws Exception {
        File configFile = this.tempDir.resolve("orphans7.yml").toFile();

        // Create file with nested orphans
        String yaml = """
            declaredNested:
              nestedField: "nested value"
              orphanInNested: "should be removed"
            orphanAtRoot: "should be removed"
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        ParentConfig config = ConfigManager.create(ParentConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.withRemoveOrphans(true);
        config.load();
        config.save();

        // Verify orphans removed
        String content = TestUtils.readFile(configFile);
        assertThat(content).doesNotContain("orphanAtRoot");
        assertThat(content).doesNotContain("orphanInNested");
        assertThat(content).contains("nestedField");
    }

    /**
     * Orphan behavior after field removal (declared field becomes orphan)
     */
    @Test
    void testOrphans_DeclaredFieldBecomesOrphan_HandleCorrectly() throws Exception {
        File configFile = this.tempDir.resolve("orphans8.yml").toFile();

        // Create config with two fields and save
        ConfigV1 v1 = ConfigManager.create(ConfigV1.class);
        v1.withConfigurer(new YamlSnakeYamlConfigurer());
        v1.withBindFile(configFile);
        v1.save();

        // Now use config with only one field (field2 becomes orphan)
        ConfigV2 v2 = ConfigManager.create(ConfigV2.class);
        v2.withConfigurer(new YamlSnakeYamlConfigurer());
        v2.withBindFile(configFile);
        v2.withRemoveOrphans(false);
        v2.load();

        // field1 should be loaded, field2 should be orphan
        assertThat(v2.getField1()).isEqualTo("value1");
        assertThat(v2.get("field2")).isEqualTo("value2");

        // Save and verify field2 still exists
        v2.save();

        String content = TestUtils.readFile(configFile);
        assertThat(content).contains("field1");
        assertThat(content).contains("field2");
    }

    /**
     * Nested orphans without root-level orphans
     * Regression test: ensures nested orphans are removed even when root has no orphans
     */
    @Test
    void testOrphans_NestedOrphansWithoutRootOrphans_RemovesNestedOrphans() throws Exception {
        File configFile = this.tempDir.resolve("orphans9.yml").toFile();

        // Create file with ONLY nested orphans (no root-level orphans)
        String yaml = """
            declaredNested:
              nestedField: "nested value"
              orphanInNested: "should be removed"
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        ParentConfig config = ConfigManager.create(ParentConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.withRemoveOrphans(true);
        config.load();
        config.save();

        // Verify nested orphan removed (even though there were no root-level orphans)
        String content = TestUtils.readFile(configFile);
        assertThat(content).doesNotContain("orphanInNested");
        assertThat(content).contains("nestedField");
    }

    // Create self-referencing config
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SelfRefConfig extends OkaeriConfig {
        private String name = "default";
        private SelfRefConfig child = null;
    }

    /**
     * Self-referencing config with orphan removal
     * Regression test: ensures removeOrphansRecursively doesn't infinite loop on circular references
     */
    @Test
    void testOrphans_SelfReferencingConfig_DoesNotInfiniteLoop() throws Exception {
        File configFile = this.tempDir.resolve("orphans10.yml").toFile();

        // Create file with orphans in self-referencing structure
        String yaml = """
            name: "parent"
            orphanField: "should be removed"
            """;
        TestUtils.writeFile(configFile, yaml);

        // Load
        SelfRefConfig config = ConfigManager.create(SelfRefConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.withRemoveOrphans(true);
        config.load();

        // This should not infinite loop
        config.save();

        // Verify orphan removed
        String content = TestUtils.readFile(configFile);
        assertThat(content).doesNotContain("orphanField");
        assertThat(content).contains("name: parent");
        assertThat(content).contains("child: null");
    }

    // Custom object for serializer test
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CustomObject extends OkaeriConfig {
        private String value = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithCustomSerialized extends OkaeriConfig {
        private CustomObject custom = new CustomObject();
    }

    /**
     * Custom serializer fields with nested config
     * Regression test: ensures removeOrphansRecursively doesn't remove fields added by custom serializers
     */
    @Test
    void testOrphans_CustomSerializerFields_PreservedWithRemoveOrphans() throws Exception {
        File configFile = this.tempDir.resolve("orphans11.yml").toFile();

        // Create custom serializer that adds metadata fields
        var customSerializer = new ObjectSerializer<CustomObject>() {
            @Override
            public boolean supports(@NonNull Class<?> type) {
                return CustomObject.class.isAssignableFrom(type);
            }

            @Override
            public void serialize(@NonNull CustomObject object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
                // Add metadata fields that serializer injects
                data.add("__type", "CustomObject");
                data.add("__version", 1);
                data.add("value", object.getValue());
            }

            @Override
            public CustomObject deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
                CustomObject obj = new CustomObject();
                obj.setValue(data.get("value", String.class));
                return obj;
            }
        };

        // Create config with custom serializer
        ConfigWithCustomSerialized config = ConfigManager.create(ConfigWithCustomSerialized.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.getConfigurer().getRegistry().register(customSerializer);
        config.withBindFile(configFile);
        config.withRemoveOrphans(true);

        // Save (serializer adds __type and __version)
        config.save();

        // Load and save again with removeOrphans=true
        ConfigWithCustomSerialized loaded = ConfigManager.create(ConfigWithCustomSerialized.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.getConfigurer().getRegistry().register(customSerializer);
        loaded.withBindFile(configFile);
        loaded.withRemoveOrphans(true);
        loaded.load();
        loaded.save();

        // Verify serializer-added fields are preserved
        String content = TestUtils.readFile(configFile);
        assertThat(content).contains("__type");
        assertThat(content).contains("__version");
        assertThat(content).contains("value");
    }
}
