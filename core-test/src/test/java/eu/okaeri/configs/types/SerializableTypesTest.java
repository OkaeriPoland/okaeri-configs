package eu.okaeri.configs.types;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Serializable custom object types.
 * <p>
 * Validates:
 * - Simple Serializable class
 * - Serializable with various field types
 * - Nested Serializable objects
 * - List<Serializable>
 * - Map<String, Serializable>
 * - Serializable to/from map conversion
 */
class SerializableTypesTest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int id;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithSerializable extends OkaeriConfig {
        private SimpleSerializable object = new SimpleSerializable("default", 0);
    }

    @Test
    void testSimpleSerializable_SaveAndLoad_PreservesData(@TempDir Path tempDir) throws Exception {
        // Create config with serializable object
        ConfigWithSerializable config = ConfigManager.create(ConfigWithSerializable.class);
        config.setObject(new SimpleSerializable("test-object", 42));

        // Save and load
        Path configFile = tempDir.resolve("serializable.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithSerializable loaded = ConfigManager.create(ConfigWithSerializable.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify
        assertThat(loaded.getObject()).isNotNull();
        assertThat(loaded.getObject().getName()).isEqualTo("test-object");
        assertThat(loaded.getObject().getId()).isEqualTo(42);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String stringField;
        private int intField;
        private double doubleField;
        private boolean boolField;
        private List<String> listField;
        private Map<String, Integer> mapField;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithComplexSerializable extends OkaeriConfig {
        private ComplexSerializable complex;

        public ConfigWithComplexSerializable() {
            this.complex = new ComplexSerializable();
            this.complex.setStringField("default");
            this.complex.setIntField(0);
            this.complex.setDoubleField(0.0);
            this.complex.setBoolField(false);
            this.complex.setListField(new ArrayList<>());
            this.complex.setMapField(new LinkedHashMap<>());
        }
    }

    @Test
    void testComplexSerializable_SaveAndLoad_PreservesAllFields(@TempDir Path tempDir) throws Exception {
        // Create config with complex serializable
        ConfigWithComplexSerializable config = ConfigManager.create(ConfigWithComplexSerializable.class);

        ComplexSerializable complex = new ComplexSerializable();
        complex.setStringField("complex-test");
        complex.setIntField(999);
        complex.setDoubleField(3.14159);
        complex.setBoolField(true);
        complex.setListField(Arrays.asList("a", "b", "c"));

        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("key1", 100);
        map.put("key2", 200);
        complex.setMapField(map);

        config.setComplex(complex);

        // Save and load
        Path configFile = tempDir.resolve("complex-serializable.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithComplexSerializable loaded = ConfigManager.create(ConfigWithComplexSerializable.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify all fields
        assertThat(loaded.getComplex()).isNotNull();
        assertThat(loaded.getComplex().getStringField()).isEqualTo("complex-test");
        assertThat(loaded.getComplex().getIntField()).isEqualTo(999);
        assertThat(loaded.getComplex().getDoubleField()).isEqualTo(3.14159);
        assertThat(loaded.getComplex().isBoolField()).isTrue();
        assertThat(loaded.getComplex().getListField()).containsExactly("a", "b", "c");
        assertThat(loaded.getComplex().getMapField()).hasSize(2);
        assertThat(loaded.getComplex().getMapField().get("key1")).isEqualTo(100);
        assertThat(loaded.getComplex().getMapField().get("key2")).isEqualTo(200);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InnerSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String innerValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OuterSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String outerValue;
        private InnerSerializable inner;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithNestedSerializable extends OkaeriConfig {
        private OuterSerializable nested;

        public ConfigWithNestedSerializable() {
            this.nested = new OuterSerializable("outer", new InnerSerializable("inner"));
        }
    }

    @Test
    void testNestedSerializable_SaveAndLoad_PreservesStructure(@TempDir Path tempDir) throws Exception {
        // Create config with nested serializable
        ConfigWithNestedSerializable config = ConfigManager.create(ConfigWithNestedSerializable.class);

        InnerSerializable inner = new InnerSerializable("inner-test");
        OuterSerializable outer = new OuterSerializable("outer-test", inner);
        config.setNested(outer);

        // Save and load
        Path configFile = tempDir.resolve("nested-serializable.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithNestedSerializable loaded = ConfigManager.create(ConfigWithNestedSerializable.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify nested structure
        assertThat(loaded.getNested()).isNotNull();
        assertThat(loaded.getNested().getOuterValue()).isEqualTo("outer-test");
        assertThat(loaded.getNested().getInner()).isNotNull();
        assertThat(loaded.getNested().getInner().getInnerValue()).isEqualTo("inner-test");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithSerializableList extends OkaeriConfig {
        private List<SimpleSerializable> objectList = new ArrayList<>();
    }

    @Test
    void testListOfSerializable_SaveAndLoad_PreservesAll(@TempDir Path tempDir) throws Exception {
        // Create config with list of serializables
        ConfigWithSerializableList config = ConfigManager.create(ConfigWithSerializableList.class);

        config.getObjectList().add(new SimpleSerializable("first", 1));
        config.getObjectList().add(new SimpleSerializable("second", 2));
        config.getObjectList().add(new SimpleSerializable("third", 3));

        // Save and load
        Path configFile = tempDir.resolve("list-serializable.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithSerializableList loaded = ConfigManager.create(ConfigWithSerializableList.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify list contents
        assertThat(loaded.getObjectList()).hasSize(3);
        assertThat(loaded.getObjectList().get(0).getName()).isEqualTo("first");
        assertThat(loaded.getObjectList().get(0).getId()).isEqualTo(1);
        assertThat(loaded.getObjectList().get(1).getName()).isEqualTo("second");
        assertThat(loaded.getObjectList().get(1).getId()).isEqualTo(2);
        assertThat(loaded.getObjectList().get(2).getName()).isEqualTo("third");
        assertThat(loaded.getObjectList().get(2).getId()).isEqualTo(3);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithSerializableMap extends OkaeriConfig {
        private Map<String, SimpleSerializable> objectMap = new LinkedHashMap<>();
    }

    @Test
    void testMapOfSerializable_SaveAndLoad_PreservesAll(@TempDir Path tempDir) throws Exception {
        // Create config with map of serializables
        ConfigWithSerializableMap config = ConfigManager.create(ConfigWithSerializableMap.class);

        config.getObjectMap().put("obj1", new SimpleSerializable("first", 10));
        config.getObjectMap().put("obj2", new SimpleSerializable("second", 20));
        config.getObjectMap().put("obj3", new SimpleSerializable("third", 30));

        // Save and load
        Path configFile = tempDir.resolve("map-serializable.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithSerializableMap loaded = ConfigManager.create(ConfigWithSerializableMap.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify map contents
        assertThat(loaded.getObjectMap()).hasSize(3);
        assertThat(loaded.getObjectMap().get("obj1").getName()).isEqualTo("first");
        assertThat(loaded.getObjectMap().get("obj1").getId()).isEqualTo(10);
        assertThat(loaded.getObjectMap().get("obj2").getName()).isEqualTo("second");
        assertThat(loaded.getObjectMap().get("obj2").getId()).isEqualTo(20);
        assertThat(loaded.getObjectMap().get("obj3").getName()).isEqualTo("third");
        assertThat(loaded.getObjectMap().get("obj3").getId()).isEqualTo(30);
    }

    @Test
    void testSerializableToMap_ReturnsCorrectStructure() throws Exception {
        // Create config with serializable
        ConfigWithSerializable config = ConfigManager.create(ConfigWithSerializable.class);
        config.setObject(new SimpleSerializable("map-test", 123));

        // Convert to map (conservative=true preserves number types)
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        Map<String, Object> map = config.asMap(configurer, true);

        // Verify structure
        assertThat(map).containsKey("object");

        Object objectValue = map.get("object");
        assertThat(objectValue).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> objectMap = (Map<String, Object>) objectValue;
        assertThat(objectMap.get("name")).isEqualTo("map-test");
        assertThat(objectMap.get("id")).isEqualTo(123);
    }

    @Test
    void testSerializableFromMap_LoadsCorrectly() throws Exception {
        // Create map representing config with serializable
        Map<String, Object> map = new LinkedHashMap<>();

        Map<String, Object> objectMap = new LinkedHashMap<>();
        objectMap.put("name", "from-map");
        objectMap.put("id", 456);
        map.put("object", objectMap);

        // Load from map
        ConfigWithSerializable config = ConfigManager.create(ConfigWithSerializable.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(map);

        // Verify loaded data
        assertThat(config.getObject()).isNotNull();
        assertThat(config.getObject().getName()).isEqualTo("from-map");
        assertThat(config.getObject().getId()).isEqualTo(456);
    }

    @Test
    void testEmptySerializableList_SaveAndLoad_HandlesCorrectly(@TempDir Path tempDir) throws Exception {
        // Create config with empty list
        ConfigWithSerializableList config = ConfigManager.create(ConfigWithSerializableList.class);
        config.setObjectList(new ArrayList<>());

        // Save and load
        Path configFile = tempDir.resolve("empty-list.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithSerializableList loaded = ConfigManager.create(ConfigWithSerializableList.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify empty list
        assertThat(loaded.getObjectList()).isEmpty();
    }

    @Test
    void testEmptySerializableMap_SaveAndLoad_HandlesCorrectly(@TempDir Path tempDir) throws Exception {
        // Create config with empty map
        ConfigWithSerializableMap config = ConfigManager.create(ConfigWithSerializableMap.class);
        config.setObjectMap(new LinkedHashMap<>());

        // Save and load
        Path configFile = tempDir.resolve("empty-map.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithSerializableMap loaded = ConfigManager.create(ConfigWithSerializableMap.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify empty map
        assertThat(loaded.getObjectMap()).isEmpty();
    }

    @Test
    void testNullSerializable_SaveAndLoad_HandlesCorrectly(@TempDir Path tempDir) throws Exception {
        // Create config with null object
        ConfigWithSerializable config = ConfigManager.create(ConfigWithSerializable.class);
        config.setObject(null);

        // Save and load
        Path configFile = tempDir.resolve("null-serializable.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithSerializable loaded = ConfigManager.create(ConfigWithSerializable.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify null is preserved
        assertThat(loaded.getObject()).isNull();
    }
}
