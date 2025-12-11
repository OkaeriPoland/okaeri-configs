package eu.okaeri.configs.integration;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.test.configs.PrimitivesTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cross-format compatibility.
 * Tests data integrity when converting between formats or using transformCopy/deepCopy.
 */
class CrossFormatTest {

    @TempDir
    Path tempDir;

    // Test config classes
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String stringVal = "test";
        private int intVal = 42;
        private double doubleVal = 3.14;
        private Map<String, String> mapVal = new LinkedHashMap<>(Map.of("k1", "v1", "k2", "v2"));
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private String field1 = "value1";
        private int field2 = 123;
        private Map<String, Integer> field3 = new LinkedHashMap<>(Map.of("a", 1, "b", 2));
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Inner extends OkaeriConfig {
        private String innerField = "inner";
        private int innerNum = 99;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Outer extends OkaeriConfig {
        private String outerField = "outer";
        private Inner nested = new Inner();
        private Map<String, Inner> nestedMap = new LinkedHashMap<>(Map.of("first", new Inner()));
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Config1 extends OkaeriConfig {
        private int intField = 42;
        private String stringField = "123";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyConfig extends OkaeriConfig {
        private String nullString = null;
        private String emptyString = "";
        private Map<String, String> emptyMap = new LinkedHashMap<>();
    }

    /**
     * transformCopy between different configurers (YAML to InMemory)
     */
    @Test
    void testCrossFormat_TransformCopyBetweenConfigurers_PreservesData() throws Exception {
        File yamlFile = this.tempDir.resolve("source.yml").toFile();

        // Create and save YAML config
        PrimitivesTestConfig yamlConfig = ConfigManager.create(PrimitivesTestConfig.class);
        yamlConfig.withConfigurer(new YamlSnakeYamlConfigurer());
        yamlConfig.withBindFile(yamlFile);
        yamlConfig.setIntValue(999);
        yamlConfig.setIntWrapper(888);
        yamlConfig.save();

        // Transform copy to new config (no configurer - in-memory only)
        PrimitivesTestConfig memoryConfig = ConfigManager.transformCopy(yamlConfig, PrimitivesTestConfig.class);

        // Verify data preserved
        assertThat(memoryConfig.getIntValue()).isEqualTo(999);
        assertThat(memoryConfig.getIntWrapper()).isEqualTo(888);
        assertThat(memoryConfig.isBoolValue()).isEqualTo(yamlConfig.isBoolValue());
        assertThat(memoryConfig.getDoubleValue()).isEqualTo(yamlConfig.getDoubleValue());
    }

    /**
     * deepCopy with format change (YAML to new YAML instance)
     */
    @Test
    void testCrossFormat_DeepCopyWithNewConfigurer_CreatesIndependentCopy() throws Exception {
        File sourceFile = this.tempDir.resolve("source.yml").toFile();
        File targetFile = this.tempDir.resolve("target.yml").toFile();

        // Create source config
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());
        source.withBindFile(sourceFile);
        source.setIntValue(111);
        source.save();

        // Deep copy with new configurer
        YamlSnakeYamlConfigurer newConfigurer = new YamlSnakeYamlConfigurer();
        PrimitivesTestConfig copy = ConfigManager.deepCopy(source, newConfigurer, PrimitivesTestConfig.class);
        copy.withBindFile(targetFile);

        // Modify copy
        copy.setIntValue(222);
        copy.save();

        // Verify source unchanged
        assertThat(source.getIntValue()).isEqualTo(111);

        // Verify files are different
        source.load(); // Reload from file
        assertThat(source.getIntValue()).isEqualTo(111);

        copy.load();
        assertThat(copy.getIntValue()).isEqualTo(222);
    }

    /**
     * Data integrity across format operations
     */
    @Test
    void testCrossFormat_DataIntegrityAcrossOperations_Maintained() throws Exception {
        File file1 = this.tempDir.resolve("file1.yml").toFile();

        // Save to YAML
        TestConfig config1 = ConfigManager.create(TestConfig.class);
        config1.withConfigurer(new YamlSnakeYamlConfigurer());
        config1.withBindFile(file1);
        config1.save();

        // Load from YAML
        TestConfig config2 = ConfigManager.create(TestConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer());
        config2.withBindFile(file1);
        config2.load();

        // Transform copy
        TestConfig config3 = ConfigManager.transformCopy(config2, TestConfig.class);

        // Deep copy
        TestConfig config4 = ConfigManager.deepCopy(config3, new YamlSnakeYamlConfigurer(), TestConfig.class);

        // Verify all configs have same data
        assertThat(config4.getStringVal()).isEqualTo("test");
        assertThat(config4.getIntVal()).isEqualTo(42);
        assertThat(config4.getDoubleVal()).isEqualTo(3.14);
        assertThat(config4.getMapVal()).containsEntry("k1", "v1").containsEntry("k2", "v2");
    }

    /**
     * Via map conversion (save → asMap → load from map)
     */
    @Test
    void testCrossFormat_ViaMapConversion_PreservesData() throws Exception {
        File sourceFile = this.tempDir.resolve("source.yml").toFile();

        SimpleConfig config1 = ConfigManager.create(SimpleConfig.class);
        config1.withConfigurer(new YamlSnakeYamlConfigurer());
        config1.withBindFile(sourceFile);
        config1.save();

        // Convert to map
        Map<String, Object> map = config1.asMap();

        // Create new config and load from map
        SimpleConfig config2 = ConfigManager.create(SimpleConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer());
        config2.load(map);

        // Verify data preserved
        assertThat(config2.getField1()).isEqualTo("value1");
        assertThat(config2.getField2()).isEqualTo(123);
        assertThat(config2.getField3()).containsEntry("a", 1).containsEntry("b", 2);
    }

    /**
     * Complex nested structures across operations
     */
    @Test
    void testCrossFormat_ComplexNestedStructures_PreservedInOperations() throws Exception {
        // Original config
        Outer original = ConfigManager.create(Outer.class);
        original.withConfigurer(new YamlSnakeYamlConfigurer());
        original.setOuterField("modified");
        original.getNested().setInnerField("modified nested");

        // Transform copy
        Outer copy1 = ConfigManager.transformCopy(original, Outer.class);
        assertThat(copy1.getOuterField()).isEqualTo("modified");
        assertThat(copy1.getNested().getInnerField()).isEqualTo("modified nested");
        assertThat(copy1.getNestedMap()).hasSize(1);
        assertThat(copy1.getNestedMap().get("first").getInnerNum()).isEqualTo(99);

        // Deep copy with configurer
        Outer copy2 = ConfigManager.deepCopy(original, new YamlSnakeYamlConfigurer(), Outer.class);
        assertThat(copy2.getOuterField()).isEqualTo("modified");
        assertThat(copy2.getNested().getInnerField()).isEqualTo("modified nested");
        assertThat(copy2.getNestedMap()).hasSize(1);
    }

    /**
     * Type conversions during cross-format operations
     */
    @Test
    void testCrossFormat_TypeConversionsDuringOperations_WorkCorrectly() throws Exception {
        Config1 source = ConfigManager.create(Config1.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());

        // Transform copy - types should be preserved
        Config1 copy = ConfigManager.transformCopy(source, Config1.class);
        assertThat(copy.getIntField()).isEqualTo(42);
        assertThat(copy.getStringField()).isEqualTo("123");
    }

    /**
     * Empty and null values across operations
     */
    @Test
    void testCrossFormat_EmptyAndNullValues_PreservedCorrectly() throws Exception {
        EmptyConfig source = ConfigManager.create(EmptyConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());

        // Transform copy
        EmptyConfig copy1 = ConfigManager.transformCopy(source, EmptyConfig.class);
        assertThat(copy1.getNullString()).isNull();
        assertThat(copy1.getEmptyString()).isEmpty();
        assertThat(copy1.getEmptyMap()).isEmpty();

        // Deep copy
        EmptyConfig copy2 = ConfigManager.deepCopy(source, new YamlSnakeYamlConfigurer(), EmptyConfig.class);
        assertThat(copy2.getNullString()).isNull();
        assertThat(copy2.getEmptyString()).isEmpty();
        assertThat(copy2.getEmptyMap()).isEmpty();
    }

    // Test config for load-from-config test
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig2 extends OkaeriConfig {
        private String field1 = "value1";
        private int field2 = 100;
    }

    /**
     * Load from another OkaeriConfig (direct config-to-config)
     */
    @Test
    void testCrossFormat_LoadFromAnotherConfig_CopiesData() throws Exception {
        // Create source config
        TestConfig2 source = ConfigManager.create(TestConfig2.class);
        source.setField1("modified");
        source.setField2(999);

        // Create target and load from source
        TestConfig2 target = ConfigManager.create(TestConfig2.class);
        target.withConfigurer(new YamlSnakeYamlConfigurer());
        target.load(source);

        // Verify data copied
        assertThat(target.getField1()).isEqualTo("modified");
        assertThat(target.getField2()).isEqualTo(999);
    }
}
