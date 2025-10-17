package eu.okaeri.configs.lifecycle;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.test.configs.PrimitivesTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OkaeriConfig map conversion operations.
 * <p>
 * Scenarios tested:
 * - asMap(configurer, conservative=false) - non-conservative simplification
 * - asMap(configurer, conservative=true) - preserves primitives
 * - Map contains all declared fields
 * - Map contains orphaned fields (if present)
 * - Map values are properly simplified
 * - Nested configs are converted to maps
 */
class ConfigMapConversionTest {

    @Test
    void testAsMap_NonConservative_ContainsDeclaredFields() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setBoolValue(false);
        config.setIntValue(999);
        config.setDoubleValue(3.14);

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), false);

        // Assert
        assertThat(map).containsKeys("boolValue", "intValue", "doubleValue");
        assertThat(map.get("boolValue")).isEqualTo("false");
        assertThat(map.get("intValue")).isEqualTo("999");
        assertThat(map.get("doubleValue")).isEqualTo("3.14");
    }

    @Test
    void testAsMap_Conservative_PreservesPrimitives() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setBoolValue(false);
        config.setIntValue(777);
        config.setDoubleValue(2.71);

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), true);

        // Assert - conservative mode preserves types
        assertThat(map.get("boolValue")).isInstanceOf(Boolean.class).isEqualTo(false);
        assertThat(map.get("intValue")).isInstanceOf(Integer.class).isEqualTo(777);
        assertThat(map.get("doubleValue")).isInstanceOf(Double.class).isEqualTo(2.71);
    }

    @Test
    void testAsMap_ContainsAllFields() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), true);

        // Assert - all primitive fields should be present
        assertThat(map).containsKeys(
            "boolValue", "byteValue", "charValue", "doubleValue",
            "floatValue", "intValue", "longValue", "shortValue",
            "boolWrapper", "byteWrapper", "charWrapper", "doubleWrapper",
            "floatWrapper", "intWrapper", "longWrapper", "shortWrapper"
        );
    }

    @Test
    void testAsMap_WithOrphans_IncludesOrphans() throws Exception {
        // Arrange
        String yamlContent = """
            boolValue: false
            intValue: 999
            orphanKey: orphan value
            anotherOrphan: 123
            """;

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yamlContent);

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), true);

        // Assert - orphans should be included
        assertThat(map).containsKeys("orphanKey", "anotherOrphan");
        assertThat(map.get("orphanKey")).isEqualTo("orphan value");
        assertThat(map.get("anotherOrphan")).isEqualTo(123);
    }

    @Test
    void testAsMap_WithoutConfigurer_OnlyDeclaredFields() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();

        config.setBoolValue(false);
        config.setIntValue(555);

        // Act - use external configurer, config has no configurer set
        Map<String, Object> map = config.asMap(configurer, true);

        // Assert - only declared fields, no orphans possible
        assertThat(map).containsKeys("boolValue", "intValue");
        assertThat(map.get("boolValue")).isEqualTo(false);
        assertThat(map.get("intValue")).isEqualTo(555);
    }

    /**
     * Test config with nested structure
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class NestedMapTestConfig extends OkaeriConfig {
        private String name = "parent";
        private int value = 42;
        private SubConfig nested = new SubConfig();

        @Data
        @NoArgsConstructor
        @EqualsAndHashCode(callSuper = false)
        public static class SubConfig extends OkaeriConfig {
            private String subName = "child";
            private int subValue = 99;
        }
    }

    @Test
    void testAsMap_NestedConfig_ConvertedToMap() throws Exception {
        // Arrange
        NestedMapTestConfig config = ConfigManager.create(NestedMapTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setName("test");
        config.setValue(123);
        config.getNested().setSubName("nested-test");
        config.getNested().setSubValue(456);

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), true);

        // Assert
        assertThat(map).containsKeys("name", "value", "nested");
        assertThat(map.get("name")).isEqualTo("test");
        assertThat(map.get("value")).isEqualTo(123);

        // Nested config should be a map
        assertThat(map.get("nested")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) map.get("nested");
        assertThat(nestedMap.get("subName")).isEqualTo("nested-test");
        assertThat(nestedMap.get("subValue")).isEqualTo(456);
    }

    @Test
    void testAsMap_AllPrimitiveTypes_Conservative() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        config.setBoolValue(false);
        config.setByteValue((byte) 99);
        config.setCharValue('X');
        config.setDoubleValue(9.87);
        config.setFloatValue(1.23f);
        config.setIntValue(777);
        config.setLongValue(999999L);
        config.setShortValue((short) 555);

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), true);

        // Assert - types preserved in conservative mode
        assertThat(map.get("boolValue")).isInstanceOf(Boolean.class);
        assertThat(map.get("byteValue")).isInstanceOf(Byte.class);
        assertThat(map.get("charValue")).isInstanceOf(Character.class);
        assertThat(map.get("doubleValue")).isInstanceOf(Double.class);
        assertThat(map.get("floatValue")).isInstanceOf(Float.class);
        assertThat(map.get("intValue")).isInstanceOf(Integer.class);
        assertThat(map.get("longValue")).isInstanceOf(Long.class);
        assertThat(map.get("shortValue")).isInstanceOf(Short.class);
    }

    @Test
    void testAsMap_AllPrimitiveTypes_NonConservative() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        config.setBoolValue(false);
        config.setByteValue((byte) 99);
        config.setCharValue('X');
        config.setDoubleValue(9.87);
        config.setFloatValue(1.23f);
        config.setIntValue(777);
        config.setLongValue(999999L);
        config.setShortValue((short) 555);

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), false);

        // Assert - most should be strings in non-conservative mode
        assertThat(map.get("boolValue")).isInstanceOf(String.class);
        assertThat(map.get("byteValue")).isInstanceOf(String.class);
        assertThat(map.get("charValue")).isInstanceOf(String.class);
        assertThat(map.get("doubleValue")).isInstanceOf(String.class);
        assertThat(map.get("floatValue")).isInstanceOf(String.class);
        assertThat(map.get("intValue")).isInstanceOf(String.class);
        assertThat(map.get("longValue")).isInstanceOf(String.class);
        assertThat(map.get("shortValue")).isInstanceOf(String.class);
    }

    @Test
    void testAsMap_RoundTrip_SaveAsMapLoadFromMap() throws Exception {
        // Arrange
        PrimitivesTestConfig config1 = ConfigManager.create(PrimitivesTestConfig.class);
        config1.withConfigurer(new YamlSnakeYamlConfigurer());
        config1.setBoolValue(false);
        config1.setIntValue(999);
        config1.setDoubleValue(7.77);

        // Act - convert to map
        Map<String, Object> map = config1.asMap(config1.getConfigurer(), true);

        // Create new config and load from map
        PrimitivesTestConfig config2 = ConfigManager.create(PrimitivesTestConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer());
        config2.load(map);

        // Assert - values should match
        assertThat(config2.isBoolValue()).isEqualTo(config1.isBoolValue());
        assertThat(config2.getIntValue()).isEqualTo(config1.getIntValue());
        assertThat(config2.getDoubleValue()).isEqualTo(config1.getDoubleValue());
    }

    @Test
    void testAsMap_EmptyConfig_ReturnsDefaultValues() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act - don't modify any values, use defaults
        Map<String, Object> map = config.asMap(config.getConfigurer(), true);

        // Assert - should contain default values from class
        assertThat(map).isNotEmpty();
        assertThat(map.get("boolValue")).isEqualTo(true); // default
        assertThat(map.get("intValue")).isEqualTo(42); // default
        assertThat(map.get("doubleValue")).isEqualTo(3.14); // default
    }

    @Test
    void testAsMap_NullWrapper_IncludedInMap() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setIntWrapper(null);

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), true);

        // Assert - null should be included
        assertThat(map).containsKey("intWrapper");
        assertThat(map.get("intWrapper")).isNull();
    }

    @Test
    void testAsMap_ModifyMap_DoesNotAffectConfig() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setIntValue(999);

        // Act
        Map<String, Object> map = config.asMap(config.getConfigurer(), true);
        map.put("intValue", 111); // Modify the map

        // Assert - config should not be affected
        assertThat(config.getIntValue()).isEqualTo(999);
    }

    @Test
    void testAsMap_MultipleConversions_Consistent() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setBoolValue(false);
        config.setIntValue(777);

        // Act
        Map<String, Object> map1 = config.asMap(config.getConfigurer(), true);
        Map<String, Object> map2 = config.asMap(config.getConfigurer(), true);

        // Assert - multiple conversions should produce same result
        assertThat(map1).isEqualTo(map2);
    }
}
