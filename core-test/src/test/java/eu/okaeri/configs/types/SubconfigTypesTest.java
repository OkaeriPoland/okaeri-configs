package eu.okaeri.configs.types;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for nested subconfig types (OkaeriConfig as fields).
 * <p>
 * Validates:
 * - Nested OkaeriConfig as field
 * - Multiple levels of nesting
 * - Subconfig with its own annotations
 * - Subconfig serialization to map
 * - Subconfig deserialization from map
 * - Subconfig with custom types
 * - List<OkaeriConfig>
 * - Map<String, OkaeriConfig>
 */
class SubconfigTypesTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleSubConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 42;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithSubconfig extends OkaeriConfig {
        private String mainField = "main";
        private SimpleSubConfig subConfig = new SimpleSubConfig();
    }

    @Test
    void testNestedSubconfig_SaveAndLoad_PreservesData(@TempDir Path tempDir) throws Exception {
        // Create config with nested subconfig
        ConfigWithSubconfig config = ConfigManager.create(ConfigWithSubconfig.class);
        config.setMainField("test-main");
        config.getSubConfig().setName("nested-name");
        config.getSubConfig().setValue(999);

        // Save and load
        Path configFile = tempDir.resolve("subconfig.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithSubconfig loaded = ConfigManager.create(ConfigWithSubconfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify
        assertThat(loaded.getMainField()).isEqualTo("test-main");
        assertThat(loaded.getSubConfig()).isNotNull();
        assertThat(loaded.getSubConfig().getName()).isEqualTo("nested-name");
        assertThat(loaded.getSubConfig().getValue()).isEqualTo(999);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level3Config extends OkaeriConfig {
        private String level3Field = "L3";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2Config extends OkaeriConfig {
        private String level2Field = "L2";
        private Level3Config level3 = new Level3Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1Config extends OkaeriConfig {
        private String level1Field = "L1";
        private Level2Config level2 = new Level2Config();
    }

    @Test
    void testMultipleLevelsNesting_SaveAndLoad_PreservesStructure(@TempDir Path tempDir) throws Exception {
        // Create deeply nested config
        Level1Config config = ConfigManager.create(Level1Config.class);
        config.setLevel1Field("first");
        config.getLevel2().setLevel2Field("second");
        config.getLevel2().getLevel3().setLevel3Field("third");

        // Save and load
        Path configFile = tempDir.resolve("nested.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        Level1Config loaded = ConfigManager.create(Level1Config.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify all levels
        assertThat(loaded.getLevel1Field()).isEqualTo("first");
        assertThat(loaded.getLevel2()).isNotNull();
        assertThat(loaded.getLevel2().getLevel2Field()).isEqualTo("second");
        assertThat(loaded.getLevel2().getLevel3()).isNotNull();
        assertThat(loaded.getLevel2().getLevel3().getLevel3Field()).isEqualTo("third");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithList extends OkaeriConfig {
        private List<SimpleSubConfig> subConfigList = new ArrayList<>();
    }

    @Test
    void testListOfSubconfigs_SaveAndLoad_PreservesAll(@TempDir Path tempDir) throws Exception {
        // Create config with list of subconfigs
        ConfigWithList config = ConfigManager.create(ConfigWithList.class);

        SimpleSubConfig sub1 = new SimpleSubConfig();
        sub1.setName("first");
        sub1.setValue(10);

        SimpleSubConfig sub2 = new SimpleSubConfig();
        sub2.setName("second");
        sub2.setValue(20);

        config.setSubConfigList(Arrays.asList(sub1, sub2));

        // Save and load
        Path configFile = tempDir.resolve("list-subconfig.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithList loaded = ConfigManager.create(ConfigWithList.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify list contents
        assertThat(loaded.getSubConfigList()).hasSize(2);
        assertThat(loaded.getSubConfigList().get(0).getName()).isEqualTo("first");
        assertThat(loaded.getSubConfigList().get(0).getValue()).isEqualTo(10);
        assertThat(loaded.getSubConfigList().get(1).getName()).isEqualTo("second");
        assertThat(loaded.getSubConfigList().get(1).getValue()).isEqualTo(20);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithMap extends OkaeriConfig {
        private Map<String, SimpleSubConfig> subConfigMap = new LinkedHashMap<>();
    }

    @Test
    void testMapOfSubconfigs_SaveAndLoad_PreservesAll(@TempDir Path tempDir) throws Exception {
        // Create config with map of subconfigs
        ConfigWithMap config = ConfigManager.create(ConfigWithMap.class);

        SimpleSubConfig sub1 = new SimpleSubConfig();
        sub1.setName("config1");
        sub1.setValue(100);

        SimpleSubConfig sub2 = new SimpleSubConfig();
        sub2.setName("config2");
        sub2.setValue(200);

        config.getSubConfigMap().put("key1", sub1);
        config.getSubConfigMap().put("key2", sub2);

        // Save and load
        Path configFile = tempDir.resolve("map-subconfig.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithMap loaded = ConfigManager.create(ConfigWithMap.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify map contents
        assertThat(loaded.getSubConfigMap()).hasSize(2);
        assertThat(loaded.getSubConfigMap().get("key1").getName()).isEqualTo("config1");
        assertThat(loaded.getSubConfigMap().get("key1").getValue()).isEqualTo(100);
        assertThat(loaded.getSubConfigMap().get("key2").getName()).isEqualTo("config2");
        assertThat(loaded.getSubConfigMap().get("key2").getValue()).isEqualTo(200);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ComplexSubConfig extends OkaeriConfig {
        private String name;
        private List<Integer> numbers;
        private Map<String, String> metadata;

        public ComplexSubConfig() {
            this.name = "default";
            this.numbers = new ArrayList<>();
            this.metadata = new LinkedHashMap<>();
        }

        public ComplexSubConfig(String name, List<Integer> numbers, Map<String, String> metadata) {
            this.name = name;
            this.numbers = numbers;
            this.metadata = metadata;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithComplexSubconfig extends OkaeriConfig {
        private ComplexSubConfig complex = new ComplexSubConfig();
    }

    @Test
    void testSubconfigWithCustomTypes_SaveAndLoad_PreservesComplexData(@TempDir Path tempDir) throws Exception {
        // Create config with complex subconfig
        ConfigWithComplexSubconfig config = ConfigManager.create(ConfigWithComplexSubconfig.class);

        ComplexSubConfig complex = new ComplexSubConfig();
        complex.setName("complex-config");
        complex.setNumbers(Arrays.asList(1, 2, 3, 5, 8));
        complex.getMetadata().put("author", "test");
        complex.getMetadata().put("version", "1.0");

        config.setComplex(complex);

        // Save and load
        Path configFile = tempDir.resolve("complex-subconfig.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithComplexSubconfig loaded = ConfigManager.create(ConfigWithComplexSubconfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify complex data
        assertThat(loaded.getComplex()).isNotNull();
        assertThat(loaded.getComplex().getName()).isEqualTo("complex-config");
        assertThat(loaded.getComplex().getNumbers()).containsExactly(1, 2, 3, 5, 8);
        assertThat(loaded.getComplex().getMetadata()).hasSize(2);
        assertThat(loaded.getComplex().getMetadata().get("author")).isEqualTo("test");
        assertThat(loaded.getComplex().getMetadata().get("version")).isEqualTo("1.0");
    }

    @Test
    void testSubconfigSerializationToMap_ReturnsCorrectStructure(@TempDir Path tempDir) throws Exception {
        // Create config with subconfig
        ConfigWithSubconfig config = ConfigManager.create(ConfigWithSubconfig.class);
        config.setMainField("test");
        config.getSubConfig().setName("nested");
        config.getSubConfig().setValue(123);

        // Convert to map (conservative=true preserves number types)
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        Map<String, Object> map = config.asMap(configurer, true);

        // Verify structure
        assertThat(map).containsKey("mainField");
        assertThat(map.get("mainField")).isEqualTo("test");
        assertThat(map).containsKey("subConfig");

        // Subconfig should be represented as a map
        Object subConfigValue = map.get("subConfig");
        assertThat(subConfigValue).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> subConfigMap = (Map<String, Object>) subConfigValue;
        assertThat(subConfigMap.get("name")).isEqualTo("nested");
        assertThat(subConfigMap.get("value")).isEqualTo(123);
    }

    @Test
    void testSubconfigDeserializationFromMap_LoadsCorrectly(@TempDir Path tempDir) throws Exception {
        // Create map representing config with subconfig
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mainField", "from-map");

        Map<String, Object> subConfigMap = new LinkedHashMap<>();
        subConfigMap.put("name", "map-nested");
        subConfigMap.put("value", 456);
        map.put("subConfig", subConfigMap);

        // Load from map
        ConfigWithSubconfig config = ConfigManager.create(ConfigWithSubconfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(map);

        // Verify loaded data
        assertThat(config.getMainField()).isEqualTo("from-map");
        assertThat(config.getSubConfig()).isNotNull();
        assertThat(config.getSubConfig().getName()).isEqualTo("map-nested");
        assertThat(config.getSubConfig().getValue()).isEqualTo(456);
    }

    @Test
    void testEmptySubconfigList_SaveAndLoad_HandlesCorrectly(@TempDir Path tempDir) throws Exception {
        // Create config with empty list
        ConfigWithList config = ConfigManager.create(ConfigWithList.class);
        config.setSubConfigList(new ArrayList<>());

        // Save and load
        Path configFile = tempDir.resolve("empty-list.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithList loaded = ConfigManager.create(ConfigWithList.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify empty list
        assertThat(loaded.getSubConfigList()).isEmpty();
    }

    @Test
    void testEmptySubconfigMap_SaveAndLoad_HandlesCorrectly(@TempDir Path tempDir) throws Exception {
        // Create config with empty map
        ConfigWithMap config = ConfigManager.create(ConfigWithMap.class);
        config.setSubConfigMap(new LinkedHashMap<>());

        // Save and load
        Path configFile = tempDir.resolve("empty-map.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        ConfigWithMap loaded = ConfigManager.create(ConfigWithMap.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify empty map
        assertThat(loaded.getSubConfigMap()).isEmpty();
    }
}
