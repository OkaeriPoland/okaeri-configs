package eu.okaeri.configs.types;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.test.configs.MapsTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for map types with various key/value combinations.
 * 
 * Scenarios tested:
 * - Map<String, String> - simple maps
 * - Map<String, Integer> - mixed types
 * - Map<Integer, String> - non-string keys
 * - Map<String, List<String>> - complex values
 * - Map<String, Map<String, Integer>> - nested maps
 * - Empty maps
 * - Key ordering preservation (LinkedHashMap)
 */
class MapTypesTest {

    @Test
    void testSimpleMap_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("simple-map.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<String, String> testMap = new LinkedHashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", "value2");
        testMap.put("key3", "value3");
        config.setSimpleMap(testMap);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getSimpleMap()).containsExactly(
            entry("key1", "value1"),
            entry("key2", "value2"),
            entry("key3", "value3")
        );
    }

    @Test
    void testIntKeyMap_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("int-key-map.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<Integer, String> testMap = new LinkedHashMap<>();
        testMap.put(1, "one");
        testMap.put(2, "two");
        testMap.put(3, "three");
        config.setIntKeyMap(testMap);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getIntKeyMap()).containsExactly(
            entry(1, "one"),
            entry(2, "two"),
            entry(3, "three")
        );
    }

    @Test
    void testIntValueMap_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("int-value-map.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<String, Integer> testMap = new LinkedHashMap<>();
        testMap.put("a", 100);
        testMap.put("b", 200);
        testMap.put("c", 300);
        config.setIntValueMap(testMap);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getIntValueMap()).containsExactly(
            entry("a", 100),
            entry("b", 200),
            entry("c", 300)
        );
    }

    @Test
    void testComplexValueMap_SaveAndLoad_MaintainsStructure() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("complex-value-map.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<String, List<String>> testMap = new LinkedHashMap<>();
        testMap.put("group1", Arrays.asList("item1", "item2", "item3"));
        testMap.put("group2", Arrays.asList("item4", "item5"));
        testMap.put("group3", List.of("item6"));
        config.setComplexValueMap(testMap);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getComplexValueMap()).hasSize(3);
        assertThat(loaded.getComplexValueMap().get("group1")).containsExactly("item1", "item2", "item3");
        assertThat(loaded.getComplexValueMap().get("group2")).containsExactly("item4", "item5");
        assertThat(loaded.getComplexValueMap().get("group3")).containsExactly("item6");
    }

    @Test
    void testNestedMap_SaveAndLoad_MaintainsStructure() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("nested-map.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<String, Map<String, Integer>> testMap = new LinkedHashMap<>();
        
        Map<String, Integer> inner1 = new LinkedHashMap<>();
        inner1.put("a", 1);
        inner1.put("b", 2);
        testMap.put("outer1", inner1);
        
        Map<String, Integer> inner2 = new LinkedHashMap<>();
        inner2.put("c", 3);
        inner2.put("d", 4);
        testMap.put("outer2", inner2);
        
        config.setNestedMap(testMap);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getNestedMap()).hasSize(2);
        assertThat(loaded.getNestedMap().get("outer1")).containsExactly(entry("a", 1), entry("b", 2));
        assertThat(loaded.getNestedMap().get("outer2")).containsExactly(entry("c", 3), entry("d", 4));
    }

    @Test
    void testEmptyMap_SaveAndLoad_RemainsEmpty() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("empty-map.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setSimpleMap(new LinkedHashMap<>());
        config.setEmptyMap(new LinkedHashMap<>());
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getSimpleMap()).isEmpty();
        assertThat(loaded.getEmptyMap()).isEmpty();
    }

    @Test
    void testMapKeyOrder_PreservedWithLinkedHashMap() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("map-order.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<String, String> orderedMap = new LinkedHashMap<>();
        orderedMap.put("z", "last");
        orderedMap.put("a", "first");
        orderedMap.put("m", "middle");
        config.setSimpleMap(orderedMap);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert - insertion order should be preserved
        assertThat(loaded.getSimpleMap()).containsExactly(
            entry("z", "last"),
            entry("a", "first"),
            entry("m", "middle")
        );
    }

    @Test
    void testMapWithNullValues_HandledCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("map-null-values.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<String, String> mapWithNulls = new LinkedHashMap<>();
        mapWithNulls.put("key1", "value1");
        mapWithNulls.put("key2", null);
        mapWithNulls.put("key3", "value3");
        config.setSimpleMap(mapWithNulls);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getSimpleMap()).hasSize(3);
        assertThat(loaded.getSimpleMap().get("key1")).isEqualTo("value1");
        assertThat(loaded.getSimpleMap().get("key2")).isNull();
        assertThat(loaded.getSimpleMap().get("key3")).isEqualTo("value3");
    }

    @Test
    void testLargeMap_HandledCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("large-map.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<String, Integer> largeMap = new LinkedHashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, i);
        }
        config.setIntValueMap(largeMap);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getIntValueMap()).hasSize(1000);
        assertThat(loaded.getIntValueMap().get("key0")).isEqualTo(0);
        assertThat(loaded.getIntValueMap().get("key999")).isEqualTo(999);
    }

    @Test
    void testMapWithSpecialKeyCharacters_HandledCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("special-keys.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        Map<String, String> specialKeysMap = new LinkedHashMap<>();
        specialKeysMap.put("key-with-dashes", "value1");
        specialKeysMap.put("key.with.dots", "value2");
        specialKeysMap.put("key_with_underscores", "value3");
        specialKeysMap.put("key with spaces", "value4");
        config.setSimpleMap(specialKeysMap);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert
        assertThat(loaded.getSimpleMap()).containsExactly(
            entry("key-with-dashes", "value1"),
            entry("key.with.dots", "value2"),
            entry("key_with_underscores", "value3"),
            entry("key with spaces", "value4")
        );
    }

    @Test
    void testAllMaps_SaveAndLoad_Together() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("all-maps.yml");
        
        MapsTestConfig config = ConfigManager.create(MapsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        
        // Set all map types
        Map<String, String> simple = new LinkedHashMap<>();
        simple.put("s1", "v1");
        simple.put("s2", "v2");
        config.setSimpleMap(simple);
        
        Map<Integer, String> intKey = new LinkedHashMap<>();
        intKey.put(10, "ten");
        intKey.put(20, "twenty");
        config.setIntKeyMap(intKey);
        
        Map<String, Integer> intValue = new LinkedHashMap<>();
        intValue.put("x", 100);
        intValue.put("y", 200);
        config.setIntValueMap(intValue);
        
        Map<String, List<String>> complex = new LinkedHashMap<>();
        complex.put("list1", Arrays.asList("a", "b"));
        config.setComplexValueMap(complex);
        
        Map<String, Map<String, Integer>> nested = new LinkedHashMap<>();
        Map<String, Integer> inner = new LinkedHashMap<>();
        inner.put("n1", 1);
        nested.put("outer", inner);
        config.setNestedMap(nested);
        
        // Act
        config.save();
        MapsTestConfig loaded = ConfigManager.create(MapsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();
        
        // Assert all
        assertThat(loaded.getSimpleMap()).hasSize(2);
        assertThat(loaded.getIntKeyMap()).hasSize(2);
        assertThat(loaded.getIntValueMap()).hasSize(2);
        assertThat(loaded.getComplexValueMap()).hasSize(1);
        assertThat(loaded.getNestedMap()).hasSize(1);
    }
}
