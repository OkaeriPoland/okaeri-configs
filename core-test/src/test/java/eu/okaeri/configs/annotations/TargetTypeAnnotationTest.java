package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.TargetType;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @TargetType annotation.
 * 
 * Verifies:
 * - Collection with non-default implementation (List → LinkedList, Vector)
 * - Map with non-default implementation (Map → TreeMap)
 * - Set with non-default implementation (Set → TreeSet)
 * - TargetType determines actual runtime type after deserialization
 * - Without TargetType, default implementation is used (ArrayList, LinkedHashMap)
 */
class TargetTypeAnnotationTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleTargetTypeConfig extends OkaeriConfig {
        @TargetType(LinkedList.class)
        private List<String> linkedList = new LinkedList<>();
        
        private List<String> defaultList = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MapTargetTypeConfig extends OkaeriConfig {
        @TargetType(TreeMap.class)
        private Map<String, String> treeMap = new TreeMap<>();
        
        private Map<String, Integer> defaultMap = new LinkedHashMap<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SetTargetTypeConfig extends OkaeriConfig {
        @TargetType(TreeSet.class)
        private Set<String> treeSet = new TreeSet<>();
        
        private Set<Integer> defaultSet = new LinkedHashSet<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultipleTargetTypesConfig extends OkaeriConfig {
        @TargetType(LinkedList.class)
        private List<String> linkedList = new LinkedList<>();
        
        @TargetType(TreeSet.class)
        private Set<String> treeSet = new TreeSet<>();
        
        @TargetType(TreeMap.class)
        private Map<String, String> treeMap = new TreeMap<>();
    }

    // Tests

    @Test
    void testTargetType_LinkedList_DeserializesToLinkedList() {
        // Given
        String yaml = """
            linkedList:
              - item1
              - item2
              - item3
            defaultList:
              - item1
              - item2
            """;

        // When
        SimpleTargetTypeConfig config = ConfigManager.create(SimpleTargetTypeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);

        // Then - @TargetType should create LinkedList (not default ArrayList)
        assertThat(config.getLinkedList()).isInstanceOf(LinkedList.class);
        assertThat(config.getLinkedList()).containsExactly("item1", "item2", "item3");
        
        // defaultList should use ArrayList (default for List)
        assertThat(config.getDefaultList()).isInstanceOf(ArrayList.class);
    }

    @Test
    void testTargetType_TreeMap_DeserializesToTreeMap() {
        // Given
        String yaml = """
            treeMap:
              key1: value1
              key2: value2
            defaultMap:
              keyA: 1
              keyB: 2
            """;

        // When
        MapTargetTypeConfig config = ConfigManager.create(MapTargetTypeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);

        // Then - @TargetType should create TreeMap (not default LinkedHashMap)
        assertThat(config.getTreeMap()).isInstanceOf(TreeMap.class);
        assertThat(config.getTreeMap()).containsEntry("key1", "value1");
        assertThat(config.getTreeMap()).containsEntry("key2", "value2");
        
        // defaultMap should use LinkedHashMap (default for Map)
        assertThat(config.getDefaultMap()).isInstanceOf(LinkedHashMap.class);
        assertThat(config.getDefaultMap()).containsEntry("keyA", 1);
        assertThat(config.getDefaultMap()).containsEntry("keyB", 2);
    }

    @Test
    void testTargetType_TreeSet_DeserializesToTreeSet() {
        // Given
        String yaml = """
            treeSet:
              - item1
              - item2
              - item3
            defaultSet:
              - 10
              - 20
              - 30
            """;

        // When
        SetTargetTypeConfig config = ConfigManager.create(SetTargetTypeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);

        // Then - @TargetType should create TreeSet (not default LinkedHashSet)
        assertThat(config.getTreeSet()).isInstanceOf(TreeSet.class);
        assertThat(config.getTreeSet()).containsExactly("item1", "item2", "item3");
        
        // defaultSet should use LinkedHashSet (default for Set)
        assertThat(config.getDefaultSet()).isInstanceOf(LinkedHashSet.class);
        assertThat(config.getDefaultSet()).contains(10, 20, 30);
    }

    @Test
    void testTargetType_MultipleExoticTypes_AllCorrect() {
        // Given
        String yaml = """
            linkedList:
              - a
              - b
            treeSet:
              - x
              - y
            treeMap:
              k1: v1
            """;

        // When
        MultipleTargetTypesConfig config = ConfigManager.create(MultipleTargetTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);

        // Then
        assertThat(config.getLinkedList()).isInstanceOf(LinkedList.class);
        assertThat(config.getTreeSet()).isInstanceOf(TreeSet.class);
        assertThat(config.getTreeMap()).isInstanceOf(TreeMap.class);
    }

    @Test
    void testTargetType_EmptyCollection_StillUsesTargetType() {
        // Given
        String yaml = """
            linkedList: []
            defaultList: []
            """;

        // When
        SimpleTargetTypeConfig config = ConfigManager.create(SimpleTargetTypeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);

        // Then - Even empty collections should use target type
        assertThat(config.getLinkedList()).isInstanceOf(LinkedList.class);
        assertThat(config.getLinkedList()).isEmpty();
        assertThat(config.getDefaultList()).isInstanceOf(ArrayList.class);
        assertThat(config.getDefaultList()).isEmpty();
    }

    @Test
    void testTargetType_EmptyMap_StillUsesTargetType() {
        // Given
        String yaml = """
            treeMap: {}
            defaultMap: {}
            """;

        // When
        MapTargetTypeConfig config = ConfigManager.create(MapTargetTypeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);

        // Then
        assertThat(config.getTreeMap()).isInstanceOf(TreeMap.class);
        assertThat(config.getTreeMap()).isEmpty();
        assertThat(config.getDefaultMap()).isInstanceOf(LinkedHashMap.class);
        assertThat(config.getDefaultMap()).isEmpty();
    }

    @Test
    void testTargetType_SaveAndLoad_PreservesType() {
        // Given
        SimpleTargetTypeConfig config = ConfigManager.create(SimpleTargetTypeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setLinkedList(new LinkedList<>(Arrays.asList("test1", "test2")));
        config.setDefaultList(new ArrayList<>(Arrays.asList("test3", "test4")));

        // When - Save to string and reload
        String yaml = config.saveToString();
        SimpleTargetTypeConfig loaded = ConfigManager.create(SimpleTargetTypeConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.load(yaml);

        // Then - Types should be preserved based on @TargetType
        assertThat(loaded.getLinkedList()).isInstanceOf(LinkedList.class);
        assertThat(loaded.getLinkedList()).containsExactly("test1", "test2");
        assertThat(loaded.getDefaultList()).isInstanceOf(ArrayList.class);
        assertThat(loaded.getDefaultList()).containsExactly("test3", "test4");
    }

    @Test
    void testTargetType_NaturalOrdering_TreeMap() {
        // Given
        String yaml = """
            treeMap:
              zebra: z
              apple: a
              middle: m
            """;

        // When
        MapTargetTypeConfig config = ConfigManager.create(MapTargetTypeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);

        // Then - TreeMap should sort keys naturally
        assertThat(config.getTreeMap()).isInstanceOf(TreeMap.class);
        List<String> keys = new ArrayList<>(config.getTreeMap().keySet());
        assertThat(keys).containsExactly("apple", "middle", "zebra");
    }

    @Test
    void testTargetType_NaturalOrdering_TreeSet() {
        // Given
        String yaml = """
            treeSet:
              - zebra
              - apple
              - middle
            """;

        // When
        SetTargetTypeConfig config = ConfigManager.create(SetTargetTypeConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(yaml);

        // Then - TreeSet should sort elements naturally
        assertThat(config.getTreeSet()).isInstanceOf(TreeSet.class);
        List<String> items = new ArrayList<>(config.getTreeSet());
        assertThat(items).containsExactly("apple", "middle", "zebra");
    }
}
