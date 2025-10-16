package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SerializationData - building serialization maps for ObjectSerializer implementations.
 */
class SerializationDataTest {

    private Configurer configurer;
    private SerdesContext context;
    private SerializationData data;

    @BeforeEach
    void setUp() {
        configurer = new YamlSnakeYamlConfigurer();
        context = SerdesContext.of(configurer);
        data = new SerializationData(configurer, context);
    }

    // === BASIC OPERATIONS TESTS ===

    @Test
    void testClear_RemovesAllData() {
        data.addRaw("key1", "value1");
        data.addRaw("key2", "value2");

        data.clear();

        assertThat(data.asMap()).isEmpty();
    }

    @Test
    void testAsMap_ReturnsUnmodifiableMap() {
        data.addRaw("key", "value");

        Map<String, Object> map = data.asMap();

        assertThat(map).containsEntry("key", "value");
        assertThatThrownBy(() -> map.put("newKey", "newValue"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // === SET VALUE TESTS ===

    @Test
    void testSetValueRaw_ReplacesExistingData() {
        data.addRaw("oldKey", "oldValue");

        data.setValueRaw("newValue");

        Map<String, Object> map = data.asMap();
        assertThat(map).hasSize(1);
        assertThat(map).containsEntry(ObjectSerializer.VALUE, "newValue");
        assertThat(map).doesNotContainKey("oldKey");
    }

    @Test
    void testSetValue_SimplifiesAndReplacesData() {
        data.addRaw("oldKey", "oldValue");

        data.setValue(123);

        Map<String, Object> map = data.asMap();
        assertThat(map).hasSize(1);
        assertThat(map).containsEntry(ObjectSerializer.VALUE, 123);
    }

    @Test
    void testSetValue_WithClass_SimplifiesUsingType() {
        data.setValue("test string", String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry(ObjectSerializer.VALUE, "test string");
    }

    @Test
    void testSetValue_WithGenericsDeclaration_SimplifiesUsingType() {
        GenericsDeclaration stringType = GenericsDeclaration.of(String.class);
        data.setValue("test string", stringType);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry(ObjectSerializer.VALUE, "test string");
    }

    @Test
    void testSetValueCollection_WithClass_SimplifiesCollection() {
        List<String> collection = Arrays.asList("a", "b", "c");

        data.setValueCollection(collection, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey(ObjectSerializer.VALUE);
        assertThat(map.get(ObjectSerializer.VALUE)).isInstanceOf(List.class);
    }

    @Test
    void testSetValueCollection_WithGenericsDeclaration_SimplifiesCollection() {
        List<Integer> collection = Arrays.asList(1, 2, 3);
        GenericsDeclaration listType = GenericsDeclaration.of(List.class, Collections.singletonList(Integer.class));

        data.setValueCollection(collection, listType);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey(ObjectSerializer.VALUE);
        assertThat(map.get(ObjectSerializer.VALUE)).isInstanceOf(List.class);
    }

    @Test
    void testSetValueArray_SimplifiesArray() {
        String[] array = {"x", "y", "z"};

        data.setValueArray(array, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey(ObjectSerializer.VALUE);
        assertThat(map.get(ObjectSerializer.VALUE)).isInstanceOf(List.class);
    }

    // === ADD TESTS ===

    @Test
    void testAddRaw_AddsWithoutSimplification() {
        Object rawObject = new Object();

        data.addRaw("key", rawObject);

        assertThat(data.asMap()).containsEntry("key", rawObject);
    }

    @Test
    void testAdd_SimplifiesValue() {
        data.add("key", 42);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("key", 42);
    }

    @Test
    void testAdd_WithClass_SimplifiesUsingType() {
        data.add("key", "value", String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("key", "value");
    }

    @Test
    void testAdd_WithGenericsDeclaration_SimplifiesUsingType() {
        GenericsDeclaration intType = GenericsDeclaration.of(Integer.class);
        data.add("key", 99, intType);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("key", 99);
    }

    @Test
    void testAdd_MultipleKeys_AllPresent() {
        data.add("key1", "value1");
        data.add("key2", "value2");
        data.add("key3", "value3");

        Map<String, Object> map = data.asMap();
        assertThat(map).hasSize(3);
        assertThat(map).containsEntry("key1", "value1");
        assertThat(map).containsEntry("key2", "value2");
        assertThat(map).containsEntry("key3", "value3");
    }

    // === ADD COLLECTION TESTS ===

    @Test
    void testAddCollection_WithClass_SimplifiesCollection() {
        List<String> collection = Arrays.asList("alpha", "beta", "gamma");

        data.addCollection("items", collection, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey("items");
        assertThat(map.get("items")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<String> items = (List<String>) map.get("items");
        assertThat(items).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void testAddCollection_WithGenericsDeclaration_SimplifiesCollection() {
        Set<Integer> collection = new LinkedHashSet<>(Arrays.asList(10, 20, 30));
        GenericsDeclaration setType = GenericsDeclaration.of(Set.class, Collections.singletonList(Integer.class));

        data.addCollection("numbers", collection, setType);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey("numbers");
        assertThat(map.get("numbers")).isInstanceOf(Collection.class);
    }

    @Test
    void testAddCollection_EmptyCollection_AddsEmpty() {
        List<String> emptyList = new ArrayList<>();

        data.addCollection("empty", emptyList, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey("empty");
        assertThat(map.get("empty")).isInstanceOf(List.class);
        assertThat((List<?>) map.get("empty")).isEmpty();
    }

    // === ADD ARRAY TESTS ===

    @Test
    void testAddArray_SimplifiesArrayAsCollection() {
        Integer[] array = {1, 2, 3, 4, 5};

        data.addArray("numbers", array, Integer.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey("numbers");
        assertThat(map.get("numbers")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Integer> numbers = (List<Integer>) map.get("numbers");
        assertThat(numbers).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void testAddArray_NullArray_AddsNull() {
        data.addArray("nullArray", null, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullArray", null);
    }

    // === NULL HANDLING TESTS ===

    @Test
    void testAddCollection_NullCollection_AddsNull() {
        data.addCollection("nullCollection", null, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullCollection", null);
    }

    @Test
    void testAddAsMap_NullMap_AddsNull() {
        data.addAsMap("nullMap", null, String.class, Integer.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullMap", null);
    }

    @Test
    void testSetValueCollection_NullCollection_SetsNull() {
        data.setValueCollection(null, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry(ObjectSerializer.VALUE, null);
    }

    @Test
    void testSetValueArray_NullArray_SetsNull() {
        data.setValueArray(null, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry(ObjectSerializer.VALUE, null);
    }

    @Test
    void testAddRaw_NullValue_AddsNull() {
        data.addRaw("nullValue", null);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullValue", null);
    }

    @Test
    void testSetValueRaw_NullValue_SetsNull() {
        data.setValueRaw(null);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry(ObjectSerializer.VALUE, null);
    }

    @Test
    void testSetValue_NullValue_SetsNull() {
        data.setValue(null);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry(ObjectSerializer.VALUE, null);
    }

    @Test
    void testSetValue_WithClass_NullValue_SetsNull() {
        data.setValue(null, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry(ObjectSerializer.VALUE, null);
    }

    @Test
    void testAdd_NullValue_AddsNull() {
        data.add("nullKey", null);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullKey", null);
    }

    @Test
    void testAdd_WithClass_NullValue_AddsNull() {
        data.add("nullKey", null, String.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullKey", null);
    }

    @Test
    void testSetValueCollection_WithGenericsDeclaration_NullCollection_SetsNull() {
        GenericsDeclaration listType = GenericsDeclaration.of(List.class, Collections.singletonList(String.class));
        data.setValueCollection(null, listType);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry(ObjectSerializer.VALUE, null);
    }

    @Test
    void testAddCollection_WithGenericsDeclaration_NullCollection_AddsNull() {
        GenericsDeclaration listType = GenericsDeclaration.of(List.class, Collections.singletonList(String.class));
        data.addCollection("nullList", null, listType);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullList", null);
    }

    @Test
    void testAddAsMap_WithGenericsDeclaration_NullMap_AddsNull() {
        GenericsDeclaration mapType = GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Integer.class));
        data.addAsMap("nullMap", null, mapType);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullMap", null);
    }

    // === ADD MAP TESTS ===

    @Test
    void testAddAsMap_WithClasses_SimplifiesMap() {
        Map<String, Integer> sourceMap = new LinkedHashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);

        data.addAsMap("map", sourceMap, String.class, Integer.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey("map");
        assertThat(map.get("map")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Integer> resultMap = (Map<String, Integer>) map.get("map");
        assertThat(resultMap).containsEntry("a", 1);
        assertThat(resultMap).containsEntry("b", 2);
    }

    @Test
    void testAddAsMap_WithGenericsDeclaration_SimplifiesMap() {
        Map<Integer, String> sourceMap = new LinkedHashMap<>();
        sourceMap.put(1, "one");
        sourceMap.put(2, "two");

        GenericsDeclaration mapType = GenericsDeclaration.of(Map.class, Arrays.asList(Integer.class, String.class));

        data.addAsMap("map", sourceMap, mapType);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsKey("map");
        assertThat(map.get("map")).isInstanceOf(Map.class);
    }

    // === ADD FORMATTED TESTS ===

    @Test
    void testAddFormatted_FormatsNumber() {
        data.addFormatted("percentage", "%.2f%%", 12.3456);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("percentage", "12.35%");
    }

    @Test
    void testAddFormatted_NullValue_AddsNull() {
        data.addFormatted("nullFormatted", "%.2f", null);

        Map<String, Object> map = data.asMap();
        assertThat(map).containsEntry("nullFormatted", null);
    }

    // === COMPLEX SCENARIOS TESTS ===

    @Test
    void testMixedOperations_BuildsCorrectMap() {
        data.add("name", "TestConfig");
        data.add("version", 1);
        data.addCollection("items", Arrays.asList("a", "b", "c"), String.class);
        
        Map<String, Integer> numbersMap = new LinkedHashMap<>();
        numbersMap.put("x", 10);
        numbersMap.put("y", 20);
        data.addAsMap("numbers", numbersMap, String.class, Integer.class);

        Map<String, Object> map = data.asMap();
        assertThat(map).hasSize(4);
        assertThat(map).containsEntry("name", "TestConfig");
        assertThat(map).containsEntry("version", 1);
        assertThat(map).containsKey("items");
        assertThat(map).containsKey("numbers");
        
        // Verify the numbers map was added correctly
        @SuppressWarnings("unchecked")
        Map<String, Integer> addedNumbers = (Map<String, Integer>) map.get("numbers");
        assertThat(addedNumbers).containsEntry("x", 10);
        assertThat(addedNumbers).containsEntry("y", 20);
    }

    @Test
    void testGetConfigurer_ReturnsConfigurer() {
        assertThat(data.getConfigurer()).isSameAs(configurer);
    }

    @Test
    void testGetContext_ReturnsContext() {
        assertThat(data.getContext()).isSameAs(context);
    }
}
