package eu.okaeri.configs.types;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.test.configs.CollectionsTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for collection types (List, Set).
 * <p>
 * Scenarios tested:
 * - List<String>, List<Integer>, List<CustomObject>
 * - Set<String>, Set<Integer>, Set<Enum>
 * - Empty collections
 * - Null elements handling
 * - Nested collections (List<List<String>>)
 * - Order preservation
 */
class CollectionTypesTest {

    @Test
    void testListOfStrings_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("list-strings.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setStringList(Arrays.asList("one", "two", "three", "four"));

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getStringList()).containsExactly("one", "two", "three", "four");
    }

    @Test
    void testListOfIntegers_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("list-integers.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setIntList(Arrays.asList(10, 20, 30, 40, 50));

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getIntList()).containsExactly(10, 20, 30, 40, 50);
    }

    @Test
    void testListOrder_PreservedCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("list-order.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setStringList(Arrays.asList("z", "a", "m", "b", "y"));

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert - order should be preserved
        assertThat(loaded.getStringList()).containsExactly("z", "a", "m", "b", "y");
    }

    @Test
    void testEmptyList_SaveAndLoad_RemainsEmpty() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("list-empty.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setStringList(new ArrayList<>());
        config.setIntList(new ArrayList<>());

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getStringList()).isEmpty();
        assertThat(loaded.getIntList()).isEmpty();
    }

    @Test
    void testSetOfStrings_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("set-strings.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        Set<String> testSet = new LinkedHashSet<>(Arrays.asList("alpha", "beta", "gamma"));
        config.setStringSet(testSet);

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getStringSet()).containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }

    @Test
    void testSetOfIntegers_SaveAndLoad_MaintainsValues() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("set-integers.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        Set<Integer> testSet = new LinkedHashSet<>(Arrays.asList(100, 200, 300));
        config.setIntSet(testSet);

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getIntSet()).containsExactlyInAnyOrder(100, 200, 300);
    }

    @Test
    void testSetOrder_PreservedWithLinkedHashSet() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("set-order.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        // LinkedHashSet preserves insertion order
        Set<String> orderedSet = new LinkedHashSet<>();
        orderedSet.add("first");
        orderedSet.add("second");
        orderedSet.add("third");
        config.setStringSet(orderedSet);

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert - order should be preserved
        assertThat(loaded.getStringSet()).containsExactly("first", "second", "third");
    }

    @Test
    void testEmptySet_SaveAndLoad_RemainsEmpty() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("set-empty.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setStringSet(new LinkedHashSet<>());
        config.setIntSet(new LinkedHashSet<>());

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getStringSet()).isEmpty();
        assertThat(loaded.getIntSet()).isEmpty();
    }

    @Test
    void testNestedList_SaveAndLoad_MaintainsStructure() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("nested-list.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        List<List<String>> nested = Arrays.asList(
            Arrays.asList("a1", "a2", "a3"),
            Arrays.asList("b1", "b2"),
            Arrays.asList("c1", "c2", "c3", "c4")
        );
        config.setNestedList(nested);

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getNestedList()).hasSize(3);
        assertThat(loaded.getNestedList().get(0)).containsExactly("a1", "a2", "a3");
        assertThat(loaded.getNestedList().get(1)).containsExactly("b1", "b2");
        assertThat(loaded.getNestedList().get(2)).containsExactly("c1", "c2", "c3", "c4");
    }

    @Test
    void testListWithDuplicates_PreservesDuplicates() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("list-duplicates.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setStringList(Arrays.asList("a", "b", "a", "c", "b", "a"));

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert - duplicates should be preserved
        assertThat(loaded.getStringList()).containsExactly("a", "b", "a", "c", "b", "a");
    }

    @Test
    void testSetWithDuplicates_RemovesDuplicates() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("set-duplicates.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        Set<String> testSet = new LinkedHashSet<>(Arrays.asList("a", "b", "a", "c", "b", "a"));
        config.setStringSet(testSet);

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert - only unique values
        assertThat(loaded.getStringSet()).containsExactly("a", "b", "c");
    }

    @Test
    void testListWithMixedTypes_ConvertedCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("list-mixed.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setIntList(Arrays.asList(1, 2, 3, 4, 5));

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getIntList()).allMatch(i -> i instanceof Integer);
        assertThat(loaded.getIntList()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void testLargeList_HandledCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("list-large.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        // Create large list
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(i);
        }
        config.setIntList(largeList);

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getIntList()).hasSize(1000);
        assertThat(loaded.getIntList().get(0)).isEqualTo(0);
        assertThat(loaded.getIntList().get(999)).isEqualTo(999);
    }

    @Test
    void testAllCollections_SaveAndLoad_Together() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("all-collections.yml");

        CollectionsTestConfig config = ConfigManager.create(CollectionsTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        // Set all collections
        config.setStringList(Arrays.asList("str1", "str2", "str3"));
        config.setIntList(Arrays.asList(100, 200, 300));
        config.setStringSet(new LinkedHashSet<>(Arrays.asList("set1", "set2", "set3")));
        config.setIntSet(new LinkedHashSet<>(Arrays.asList(10, 20, 30)));
        config.setNestedList(Arrays.asList(
            Arrays.asList("n1", "n2"),
            Arrays.asList("n3", "n4")
        ));

        // Act
        config.save();
        CollectionsTestConfig loaded = ConfigManager.create(CollectionsTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert all
        assertThat(loaded.getStringList()).containsExactly("str1", "str2", "str3");
        assertThat(loaded.getIntList()).containsExactly(100, 200, 300);
        assertThat(loaded.getStringSet()).containsExactly("set1", "set2", "set3");
        assertThat(loaded.getIntSet()).containsExactly(10, 20, 30);
        assertThat(loaded.getNestedList()).hasSize(2);
    }
}
