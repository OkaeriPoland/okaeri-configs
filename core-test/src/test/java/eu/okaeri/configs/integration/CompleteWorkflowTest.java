package eu.okaeri.configs.integration;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.migrate.ConfigMigration;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.test.configs.*;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for complete config workflows.
 * Tests full end-to-end scenarios combining multiple features.
 */
class CompleteWorkflowTest {

    @TempDir
    Path tempDir;

    // Test config classes
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MigratableConfig extends OkaeriConfig {
        private String newName = "default";
        private int newNumber = 0;
        
        // Migration to rename fields
        public ConfigMigration migration() {
            return (config, view) -> {
                boolean changed = false;
                
                if (view.exists("oldName")) {
                    view.set("newName", view.get("oldName"));
                    view.remove("oldName");
                    changed = true;
                }
                
                if (view.exists("oldNumber")) {
                    view.set("newNumber", view.get("oldNumber"));
                    view.remove("oldNumber");
                    changed = true;
                }
                
                return changed;
            };
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class AllTypesConfig extends OkaeriConfig {
        // Primitives
        private int intVal = 42;
        private String stringVal = "test";
        
        // Collections
        private List<String> stringList = Arrays.asList("a", "b", "c");
        private Set<Integer> intSet = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        
        // Maps
        private Map<String, String> stringMap = new LinkedHashMap<>(Map.of("k1", "v1"));
        private Map<Integer, List<String>> complexMap = new LinkedHashMap<>(Map.of(1, Arrays.asList("a", "b")));
        
        // Nested
        private Nested nested = new Nested();
        
        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class Nested extends OkaeriConfig {
            private String nestedField = "nested";
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level3 extends OkaeriConfig {
        private String level3Field = "L3";
    }
    
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2 extends OkaeriConfig {
        private String level2Field = "L2";
        private Level3 level3 = new Level3();
    }
    
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1 extends OkaeriConfig {
        private String level1Field = "L1";
        private Level2 level2 = new Level2();
    }

    /**
     * Basic workflow: Create → Save → Load → Verify
     */
    @Test
    void testWorkflow_CreateSaveLoadVerify_WorksCorrectly() throws Exception {
        // Create config with values
        File configFile = tempDir.resolve("workflow1.yml").toFile();
        
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        
        // Modify some values
        config.setIntValue(999);
        config.setIntWrapper(888);
        
        // Save
        config.save();
        
        // Load into new instance
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();
        
        // Verify
        assertThat(loaded.getIntValue()).isEqualTo(999);
        assertThat(loaded.getIntWrapper()).isEqualTo(888);
        assertThat(loaded.isBoolValue()).isEqualTo(config.isBoolValue());
        assertThat(loaded.getDoubleValue()).isEqualTo(config.getDoubleValue());
    }

    /**
     * Advanced workflow: Create → Load → Modify → Save → Load → Verify
     */
    @Test
    void testWorkflow_CreateLoadModifySaveLoadVerify_WorksCorrectly() throws Exception {
        // Create and save initial config
        File configFile = tempDir.resolve("workflow2.yml").toFile();
        
        CollectionsTestConfig config1 = ConfigManager.create(CollectionsTestConfig.class);
        config1.withConfigurer(new YamlSnakeYamlConfigurer());
        config1.withBindFile(configFile);
        config1.save();
        
        // Load and modify
        CollectionsTestConfig config2 = ConfigManager.create(CollectionsTestConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer());
        config2.withBindFile(configFile);
        config2.load();
        
        config2.getStringList().add("delta");
        config2.getIntSet().add(40);
        config2.save();
        
        // Load again and verify
        CollectionsTestConfig config3 = ConfigManager.create(CollectionsTestConfig.class);
        config3.withConfigurer(new YamlSnakeYamlConfigurer());
        config3.withBindFile(configFile);
        config3.load();
        
        assertThat(config3.getStringList()).contains("alpha", "beta", "gamma", "delta");
        assertThat(config3.getIntSet()).contains(10, 20, 30, 40);
    }

    /**
     * SaveDefaults workflow: Create with defaults → SaveDefaults → Load
     */
    @Test
    void testWorkflow_SaveDefaults_CreatesFileWithDefaults() throws Exception {
        File configFile = tempDir.resolve("workflow3.yml").toFile();
        
        // First saveDefaults - should create file
        EnumsTestConfig config1 = ConfigManager.create(EnumsTestConfig.class);
        config1.withConfigurer(new YamlSnakeYamlConfigurer());
        config1.withBindFile(configFile);
        config1.saveDefaults();
        
        assertThat(configFile).exists();
        
        // Second saveDefaults - should skip
        config1.setSingleEnum(EnumsTestConfig.TestEnum.FIRST);
        config1.saveDefaults(); // Should not overwrite
        
        // Load and verify - should have original default values
        EnumsTestConfig config2 = ConfigManager.create(EnumsTestConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer());
        config2.withBindFile(configFile);
        config2.load();
        
        assertThat(config2.getSingleEnum()).isEqualTo(EnumsTestConfig.TestEnum.SECOND); // Original default
    }

    /**
     * Migration workflow: Load → Migrate → Save
     */
    @Test
    void testWorkflow_LoadMigrateSave_UpdatesConfig() throws Exception {
        File configFile = tempDir.resolve("workflow4.yml").toFile();
        
        // Create initial config with old structure
        String oldYaml = """
            oldName: "Old Value"
            oldNumber: 100
            """;
        TestUtils.writeFile(configFile, oldYaml);
        
        // Load with migration
        MigratableConfig config = ConfigManager.create(MigratableConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.load();
        config.migrate(config.migration()); // Perform migration after load
        
        // Verify migration worked
        assertThat(config.getNewName()).isEqualTo("Old Value");
        assertThat(config.getNewNumber()).isEqualTo(100);
        
        // Save migrated config
        config.save();
        
        // Load again and verify
        MigratableConfig config2 = ConfigManager.create(MigratableConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer());
        config2.withBindFile(configFile);
        config2.load();
        
        assertThat(config2.getNewName()).isEqualTo("Old Value");
        assertThat(config2.getNewNumber()).isEqualTo(100);
    }

    /**
     * Multiple configs with same backing file - should be safe
     */
    @Test
    void testWorkflow_MultipleConfigsSameFile_LastWriteWins() throws Exception {
        File configFile = tempDir.resolve("workflow5.yml").toFile();
        
        // Create first config and save
        PrimitivesTestConfig config1 = ConfigManager.create(PrimitivesTestConfig.class);
        config1.withConfigurer(new YamlSnakeYamlConfigurer());
        config1.withBindFile(configFile);
        config1.setIntValue(111);
        config1.save();
        
        // Create second config with same file and save
        PrimitivesTestConfig config2 = ConfigManager.create(PrimitivesTestConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer());
        config2.withBindFile(configFile);
        config2.setIntValue(222);
        config2.save();
        
        // Load fresh copy - should have last written value
        PrimitivesTestConfig config3 = ConfigManager.create(PrimitivesTestConfig.class);
        config3.withConfigurer(new YamlSnakeYamlConfigurer());
        config3.withBindFile(configFile);
        config3.load();
        
        assertThat(config3.getIntValue()).isEqualTo(222);
    }

    /**
     * Config with all type combinations
     */
    @Test
    void testWorkflow_AllTypeCombinations_SaveLoadCorrectly() throws Exception {
        File configFile = tempDir.resolve("workflow6.yml").toFile();
        
        AllTypesConfig config = ConfigManager.create(AllTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();
        
        // Load and verify
        AllTypesConfig loaded = ConfigManager.create(AllTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();
        
        assertThat(loaded.getIntVal()).isEqualTo(42);
        assertThat(loaded.getStringVal()).isEqualTo("test");
        assertThat(loaded.getStringList()).containsExactly("a", "b", "c");
        assertThat(loaded.getIntSet()).containsExactly(1, 2, 3);
        assertThat(loaded.getStringMap()).containsEntry("k1", "v1");
        assertThat(loaded.getComplexMap().get(1)).containsExactly("a", "b");
        assertThat(loaded.getNested().getNestedField()).isEqualTo("nested");
    }

    /**
     * Config with all annotations
     */
    @Test
    void testWorkflow_AllAnnotations_PreservedInSaveLoad() throws Exception {
        File configFile = tempDir.resolve("workflow7.yml").toFile();
        
        AnnotationsTestConfig config = ConfigManager.create(AnnotationsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();
        
        // Verify file was created
        assertThat(configFile).exists();
        
        // Load and verify values preserved
        AnnotationsTestConfig loaded = ConfigManager.create(AnnotationsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();
        
        assertThat(loaded.getCommentedField()).isEqualTo("value");
        assertThat(loaded.getMultiCommentField()).isEqualTo("value2");
        assertThat(loaded.getCustomKeyField()).isEqualTo("custom value");
        assertThat(loaded.getVariableField()).isEqualTo("default"); // No env var set
        assertThat(loaded.getNormalField()).isEqualTo("normal");
        
        // Excluded field should not be in file
        String content = TestUtils.readFile(configFile);
        assertThat(content).doesNotContain("excludedField");
    }

    /**
     * Nested config hierarchy - multi-level nesting
     */
    @Test
    void testWorkflow_NestedConfigHierarchy_SaveLoadCorrectly() throws Exception {
        File configFile = tempDir.resolve("workflow8.yml").toFile();
        
        Level1 config = ConfigManager.create(Level1.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();
        
        // Load and verify all levels
        Level1 loaded = ConfigManager.create(Level1.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();
        
        assertThat(loaded.getLevel1Field()).isEqualTo("L1");
        assertThat(loaded.getLevel2()).isNotNull();
        assertThat(loaded.getLevel2().getLevel2Field()).isEqualTo("L2");
        assertThat(loaded.getLevel2().getLevel3()).isNotNull();
        assertThat(loaded.getLevel2().getLevel3().getLevel3Field()).isEqualTo("L3");
    }

    /**
     * Complex workflow: Full lifecycle with modifications
     */
    @Test
    void testWorkflow_ComplexLifecycle_HandlesAllOperations() throws Exception {
        File configFile = tempDir.resolve("workflow9.yml").toFile();
        
        // 1. Create with defaults
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.saveDefaults();
        
        // 2. Load
        config.load();
        
        // 3. Modify
        config.getSimpleMap().put("key3", "value3");
        config.getIntKeyMap().put(3, "three");
        
        // 4. Save
        config.save();
        
        // 5. Load into new instance
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();
        
        // 6. Verify modifications
        assertThat(loaded.getSimpleMap()).containsKeys("key1", "key2", "key3");
        assertThat(loaded.getSimpleMap().get("key3")).isEqualTo("value3");
        assertThat(loaded.getIntKeyMap()).containsKeys(1, 2, 3);
        assertThat(loaded.getIntKeyMap().get(3)).isEqualTo("three");
        
        // 7. Further modifications
        loaded.getSimpleMap().clear();
        loaded.save();
        
        // 8. Final verification
        MapsTestConfig finalConfig = ConfigManager.create(MapsTestConfig.class);
        finalConfig.withConfigurer(new YamlSnakeYamlConfigurer());
        finalConfig.withBindFile(configFile);
        finalConfig.load();
        
        assertThat(finalConfig.getSimpleMap()).isEmpty();
    }
}
