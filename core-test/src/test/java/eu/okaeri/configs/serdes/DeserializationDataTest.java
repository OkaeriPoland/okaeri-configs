package eu.okaeri.configs.serdes;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DeserializationData - extracting data from maps for ObjectSerializer implementations.
 */
class DeserializationDataTest {

    private Configurer configurer;
    private SerdesContext context;

    @BeforeEach
    void setUp() {
        configurer = new YamlSnakeYamlConfigurer();
        context = SerdesContext.of(configurer);
    }

    // === BASIC OPERATIONS TESTS ===

    @Test
    void testAsMap_ReturnsUnmodifiableMap() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("key", "value");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        Map<String, Object> map = data.asMap();

        assertThat(map).containsEntry("key", "value");
        assertThatThrownBy(() -> map.put("newKey", "newValue"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testContainsKey_ExistingKey_ReturnsTrue() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("existingKey", "value");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        assertThat(data.containsKey("existingKey")).isTrue();
    }

    @Test
    void testContainsKey_NonExistingKey_ReturnsFalse() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        assertThat(data.containsKey("nonExistingKey")).isFalse();
    }

    // === VALUE TESTS ===

    @Test
    void testIsValue_ValueKeyPresent_ReturnsTrue() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put(ObjectSerializer.VALUE, "test");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        assertThat(data.isValue()).isTrue();
    }

    @Test
    void testIsValue_ValueKeyAbsent_ReturnsFalse() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("otherKey", "test");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        assertThat(data.isValue()).isFalse();
    }

    @Test
    void testGetValueRaw_ReturnsRawValue() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put(ObjectSerializer.VALUE, "rawValue");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        Object value = data.getValueRaw();

        assertThat(value).isEqualTo("rawValue");
    }

    @Test
    void testGetValue_WithClass_ReturnsTypedValue() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put(ObjectSerializer.VALUE, "test string");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        String value = data.getValue(String.class);

        assertThat(value).isEqualTo("test string");
    }

    @Test
    void testGetValueDirect_WithGenericsDeclaration_ReturnsTypedValue() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put(ObjectSerializer.VALUE, 42);
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        GenericsDeclaration intType = GenericsDeclaration.of(Integer.class);
        Integer value = data.getValueDirect(intType);

        assertThat(value).isEqualTo(42);
    }

    @Test
    void testGetValueAsList_ReturnsListOfType() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put(ObjectSerializer.VALUE, Arrays.asList("a", "b", "c"));
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        List<String> list = data.getValueAsList(String.class);

        assertThat(list).containsExactly("a", "b", "c");
    }

    @Test
    void testGetValueAsSet_ReturnsSetOfType() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put(ObjectSerializer.VALUE, Arrays.asList("x", "y", "z"));
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        Set<String> set = data.getValueAsSet(String.class);

        assertThat(set).containsExactlyInAnyOrder("x", "y", "z");
    }

    @Test
    void testGetValueAsCollection_WithGenericsDeclaration_ReturnsCollection() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put(ObjectSerializer.VALUE, Arrays.asList(1, 2, 3));
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        GenericsDeclaration listType = GenericsDeclaration.of(List.class, Collections.singletonList(Integer.class));
        Collection<Integer> collection = data.getValueAsCollection(listType);

        assertThat(collection).containsExactly(1, 2, 3);
    }

    // === GET RAW TESTS ===

    @Test
    void testGetRaw_ExistingKey_ReturnsValue() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("key", "value");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        Object value = data.getRaw("key");

        assertThat(value).isEqualTo("value");
    }

    @Test
    void testGetRaw_NonExistingKey_ReturnsNull() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        Object value = data.getRaw("nonExisting");

        assertThat(value).isNull();
    }

    @Test
    void testGetRaw_ValueKeyWhenNotValue_ReturnsWholeMap() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("field1", "value1");
        sourceMap.put("field2", "value2");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        // When VALUE key is not present, getRaw(VALUE) returns the whole map
        Object value = data.getRaw(ObjectSerializer.VALUE);

        assertThat(value).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> mapValue = (Map<String, Object>) value;
        assertThat(mapValue).containsEntry("field1", "value1");
        assertThat(mapValue).containsEntry("field2", "value2");
    }

    // === GET WITH TYPE TESTS ===

    @Test
    void testGet_WithClass_ResolvesType() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("number", "123");  // String that can be converted to Integer
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        Integer value = data.get("number", Integer.class);

        assertThat(value).isEqualTo(123);
    }

    @Test
    void testGetDirect_WithGenericsDeclaration_ResolvesType() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("flag", "true");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        GenericsDeclaration boolType = GenericsDeclaration.of(Boolean.class);
        Boolean value = data.getDirect("flag", boolType);

        assertThat(value).isTrue();
    }

    // === GET COLLECTION TESTS ===

    @Test
    void testGetAsList_ResolvesListOfType() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("items", Arrays.asList("alpha", "beta", "gamma"));
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        List<String> list = data.getAsList("items", String.class);

        assertThat(list).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void testGetAsSet_ResolvesSetOfType() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("items", Arrays.asList(10, 20, 30));
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        Set<Integer> set = data.getAsSet("items", Integer.class);

        assertThat(set).containsExactlyInAnyOrder(10, 20, 30);
    }

    @Test
    void testGetAsCollection_WithGenericsDeclaration_ResolvesCollection() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("numbers", Arrays.asList(1, 2, 3, 4, 5));
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        GenericsDeclaration listType = GenericsDeclaration.of(List.class, Collections.singletonList(Integer.class));
        Collection<Integer> collection = data.getAsCollection("numbers", listType);

        assertThat(collection).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void testGetAsCollection_NonCollectionType_ThrowsException() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("value", "not a collection");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        // Try to get as collection but with String type (not a Collection subclass)
        GenericsDeclaration stringType = GenericsDeclaration.of(String.class);

        assertThatThrownBy(() -> data.getAsCollection("value", stringType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a superclass of Collection");
    }

    // === GET MAP TESTS ===

    @Test
    void testGetAsMap_WithClasses_ResolvesMap() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        Map<String, Integer> innerMap = new LinkedHashMap<>();
        innerMap.put("a", 1);
        innerMap.put("b", 2);
        sourceMap.put("mapping", innerMap);
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        Map<String, Integer> map = data.getAsMap("mapping", String.class, Integer.class);

        assertThat(map).containsEntry("a", 1);
        assertThat(map).containsEntry("b", 2);
    }

    @Test
    void testGetAsMap_WithGenericsDeclaration_ResolvesMap() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        Map<Integer, String> innerMap = new LinkedHashMap<>();
        innerMap.put(1, "one");
        innerMap.put(2, "two");
        sourceMap.put("mapping", innerMap);
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        GenericsDeclaration mapType = GenericsDeclaration.of(Map.class, Arrays.asList(Integer.class, String.class));
        Map<Integer, String> map = data.getAsMap("mapping", mapType);

        assertThat(map).containsEntry(1, "one");
        assertThat(map).containsEntry(2, "two");
    }

    @Test
    void testGetAsMap_NonMapType_ThrowsException() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("value", "not a map");
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        // Try to get as map but with String type (not a Map subclass)
        GenericsDeclaration stringType = GenericsDeclaration.of(String.class);

        assertThatThrownBy(() -> data.getAsMap("value", stringType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a superclass of Map");
    }

    // === COMPLEX SCENARIOS TESTS ===

    @Test
    void testMixedOperations_ExtractsAllTypes() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("name", "TestConfig");
        sourceMap.put("version", 1);
        sourceMap.put("items", Arrays.asList("a", "b", "c"));
        
        Map<String, Integer> numbersMap = new LinkedHashMap<>();
        numbersMap.put("x", 10);
        numbersMap.put("y", 20);
        sourceMap.put("numbers", numbersMap);

        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        assertThat(data.get("name", String.class)).isEqualTo("TestConfig");
        assertThat(data.get("version", Integer.class)).isEqualTo(1);
        assertThat(data.getAsList("items", String.class)).containsExactly("a", "b", "c");
        
        Map<String, Integer> numbers = data.getAsMap("numbers", String.class, Integer.class);
        assertThat(numbers).containsEntry("x", 10);
        assertThat(numbers).containsEntry("y", 20);
    }

    @Test
    void testGetConfigurer_ReturnsConfigurer() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        assertThat(data.getConfigurer()).isSameAs(configurer);
    }

    @Test
    void testGetContext_ReturnsContext() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        assertThat(data.getContext()).isSameAs(context);
    }

    // === NULL HANDLING TESTS ===

    @Test
    void testGet_NullValue_ReturnsNull() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("nullKey", null);
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        String value = data.get("nullKey", String.class);

        assertThat(value).isNull();
    }

    @Test
    void testGetAsList_NullValue_ReturnsNull() {
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("nullList", null);
        DeserializationData data = new DeserializationData(sourceMap, configurer, context);

        List<String> list = data.getAsList("nullList", String.class);

        assertThat(list).isNull();
    }
}
