package eu.okaeri.configs.types;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.test.configs.EnumsTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for enum types.
 * <p>
 * Scenarios tested:
 * - Simple enum serialization/deserialization
 * - Enum.valueOf() exact match
 * - Case-insensitive fallback
 * - List<Enum>, Set<Enum>
 * - Map with enum keys/values
 */
class EnumTypesTest {

    @Test
    void testSingleEnum_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("single-enum.yml");

        EnumsTestConfig config = ConfigManager.create(EnumsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setSingleEnum(EnumsTestConfig.TestEnum.THIRD);

        // Act
        config.save();
        EnumsTestConfig loaded = ConfigManager.create(EnumsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getSingleEnum()).isEqualTo(EnumsTestConfig.TestEnum.THIRD);
    }

    @Test
    void testEnumList_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("enum-list.yml");

        EnumsTestConfig config = ConfigManager.create(EnumsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setEnumList(Arrays.asList(
            EnumsTestConfig.TestEnum.FIRST,
            EnumsTestConfig.TestEnum.THIRD,
            EnumsTestConfig.TestEnum.SECOND
        ));

        // Act
        config.save();
        EnumsTestConfig loaded = ConfigManager.create(EnumsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getEnumList()).containsExactly(
            EnumsTestConfig.TestEnum.FIRST,
            EnumsTestConfig.TestEnum.THIRD,
            EnumsTestConfig.TestEnum.SECOND
        );
    }

    @Test
    void testEnumSet_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("enum-set.yml");

        EnumsTestConfig config = ConfigManager.create(EnumsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        Set<EnumsTestConfig.TestEnum> enumSet = new LinkedHashSet<>();
        enumSet.add(EnumsTestConfig.TestEnum.FIRST);
        enumSet.add(EnumsTestConfig.TestEnum.SECOND);
        config.setEnumSet(enumSet);

        // Act
        config.save();
        EnumsTestConfig loaded = ConfigManager.create(EnumsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getEnumSet()).containsExactly(
            EnumsTestConfig.TestEnum.FIRST,
            EnumsTestConfig.TestEnum.SECOND
        );
    }

    @Test
    void testEnumKeyMap_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("enum-key-map.yml");

        EnumsTestConfig config = ConfigManager.create(EnumsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        Map<EnumsTestConfig.TestEnum, String> map = new LinkedHashMap<>();
        map.put(EnumsTestConfig.TestEnum.FIRST, "first value");
        map.put(EnumsTestConfig.TestEnum.SECOND, "second value");
        config.setEnumKeyMap(map);

        // Act
        config.save();
        EnumsTestConfig loaded = ConfigManager.create(EnumsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getEnumKeyMap()).containsExactly(
            entry(EnumsTestConfig.TestEnum.FIRST, "first value"),
            entry(EnumsTestConfig.TestEnum.SECOND, "second value")
        );
    }

    @Test
    void testEnumValueMap_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("enum-value-map.yml");

        EnumsTestConfig config = ConfigManager.create(EnumsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        Map<String, EnumsTestConfig.TestEnum> map = new LinkedHashMap<>();
        map.put("a", EnumsTestConfig.TestEnum.FIRST);
        map.put("b", EnumsTestConfig.TestEnum.THIRD);
        config.setEnumValueMap(map);

        // Act
        config.save();
        EnumsTestConfig loaded = ConfigManager.create(EnumsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getEnumValueMap()).containsExactly(
            entry("a", EnumsTestConfig.TestEnum.FIRST),
            entry("b", EnumsTestConfig.TestEnum.THIRD)
        );
    }

    @Test
    void testEnum_SerializedAsString_LoadsCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("enum-string.yml");

        // Write YAML manually with enum as string
        String yaml = """
            singleEnum: FIRST
            enumList:
              - SECOND
              - THIRD
            enumSet:
              - FIRST
              - THIRD
            enumKeyMap:
              FIRST: value1
              SECOND: value2
            enumValueMap:
              x: THIRD
              y: FIRST
            """;
        Files.writeString(tempFile, yaml);

        // Act
        EnumsTestConfig loaded = ConfigManager.create(EnumsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getSingleEnum()).isEqualTo(EnumsTestConfig.TestEnum.FIRST);
        assertThat(loaded.getEnumList()).containsExactly(
            EnumsTestConfig.TestEnum.SECOND,
            EnumsTestConfig.TestEnum.THIRD
        );
        assertThat(loaded.getEnumSet()).containsExactlyInAnyOrder(
            EnumsTestConfig.TestEnum.FIRST,
            EnumsTestConfig.TestEnum.THIRD
        );
    }

    @Test
    void testEnum_CaseInsensitive_LoadsCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("enum-case.yml");

        // Write YAML with different cases
        String yaml = """
            singleEnum: first
            enumList:
              - SECOND
              - third
            enumSet:
              - FiRsT
            enumKeyMap:
              first: value1
            enumValueMap:
              x: THIRD
            """;
        Files.writeString(tempFile, yaml);

        // Act
        EnumsTestConfig loaded = ConfigManager.create(EnumsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert - case-insensitive enum parsing should work
        assertThat(loaded.getSingleEnum()).isEqualTo(EnumsTestConfig.TestEnum.FIRST);
        assertThat(loaded.getEnumList()).containsExactly(
            EnumsTestConfig.TestEnum.SECOND,
            EnumsTestConfig.TestEnum.THIRD
        );
        assertThat(loaded.getEnumSet()).contains(EnumsTestConfig.TestEnum.FIRST);
    }

    @Test
    void testAllEnums_SaveAndLoad_Together() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("all-enums.yml");

        EnumsTestConfig config = ConfigManager.create(EnumsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        // Set all enum fields
        config.setSingleEnum(EnumsTestConfig.TestEnum.SECOND);
        config.setEnumList(Arrays.asList(
            EnumsTestConfig.TestEnum.FIRST,
            EnumsTestConfig.TestEnum.THIRD
        ));

        Set<EnumsTestConfig.TestEnum> enumSet = new LinkedHashSet<>();
        enumSet.add(EnumsTestConfig.TestEnum.FIRST);
        enumSet.add(EnumsTestConfig.TestEnum.SECOND);
        config.setEnumSet(enumSet);

        Map<EnumsTestConfig.TestEnum, String> keyMap = new LinkedHashMap<>();
        keyMap.put(EnumsTestConfig.TestEnum.FIRST, "first");
        keyMap.put(EnumsTestConfig.TestEnum.SECOND, "second");
        config.setEnumKeyMap(keyMap);

        Map<String, EnumsTestConfig.TestEnum> valueMap = new LinkedHashMap<>();
        valueMap.put("a", EnumsTestConfig.TestEnum.FIRST);
        valueMap.put("b", EnumsTestConfig.TestEnum.THIRD);
        config.setEnumValueMap(valueMap);

        // Act
        config.save();
        EnumsTestConfig loaded = ConfigManager.create(EnumsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert all
        assertThat(loaded.getSingleEnum()).isEqualTo(EnumsTestConfig.TestEnum.SECOND);
        assertThat(loaded.getEnumList()).hasSize(2);
        assertThat(loaded.getEnumSet()).hasSize(2);
        assertThat(loaded.getEnumKeyMap()).hasSize(2);
        assertThat(loaded.getEnumValueMap()).hasSize(2);
    }
}
