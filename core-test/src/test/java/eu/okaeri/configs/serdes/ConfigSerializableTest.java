package eu.okaeri.configs.serdes;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.serializable.ConfigSerializable;
import eu.okaeri.configs.serdes.serializable.ConfigSerializableSerializer;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ConfigSerializable - class-local serdes feature.
 * Tests the built-in serialization mechanism where classes implement their own serialize/deserialize.
 */
class ConfigSerializableTest {

    // === BASIC CONFIGSERIALIZABLE CLASSES ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleSerializable implements ConfigSerializable {
        private String name;
        private int value;

        @Override
        public void serialize(SerializationData data, GenericsDeclaration generics) {
            data.set("name", this.name);
            data.set("value", this.value);
        }

        public static SimpleSerializable deserialize(DeserializationData data, GenericsDeclaration generics) {
            String name = data.get("name", String.class);
            int value = data.get("value", Integer.class);
            return new SimpleSerializable(name, value);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NestedSerializable implements ConfigSerializable {
        private String id;
        private SimpleSerializable inner;

        @Override
        public void serialize(SerializationData data, GenericsDeclaration generics) {
            data.set("id", this.id);
            data.set("inner", this.inner, SimpleSerializable.class);
        }

        public static NestedSerializable deserialize(DeserializationData data, GenericsDeclaration generics) {
            String id = data.get("id", String.class);
            SimpleSerializable inner = data.get("inner", SimpleSerializable.class);
            return new NestedSerializable(id, inner);
        }
    }

    public static class MissingDeserializeMethod implements ConfigSerializable {
        @Override
        public void serialize(SerializationData data, GenericsDeclaration generics) {
            data.set("value", "test");
        }
        // Missing: public static MissingDeserializeMethod deserialize(...)
    }

    // === TEST CONFIGS ===

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SerializableConfig extends OkaeriConfig {
        private SimpleSerializable single = new SimpleSerializable("default", 42);
        private List<SimpleSerializable> list = Arrays.asList(
            new SimpleSerializable("first", 1),
            new SimpleSerializable("second", 2)
        );
        private Map<String, SimpleSerializable> map = new LinkedHashMap<String, SimpleSerializable>() {{
            this.put("key1", new SimpleSerializable("value1", 10));
            this.put("key2", new SimpleSerializable("value2", 20));
        }};
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedSerializableConfig extends OkaeriConfig {
        private NestedSerializable nested = new NestedSerializable("outer", new SimpleSerializable("inner", 99));
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MissingDeserializeConfig extends OkaeriConfig {
        private MissingDeserializeMethod broken = new MissingDeserializeMethod();
    }

    // === CONFIGSERIALIZABLESERIALIZER INTEGRATION TESTS ===

    @Test
    void testConfigSerializableSerializer_AutomaticallyRegistered() {
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();

        ObjectSerializer<?> serializer = configurer.getRegistry().getSerializer(SimpleSerializable.class);

        assertThat(serializer).isInstanceOf(ConfigSerializableSerializer.class);
    }

    @Test
    void testConfigSerializableSerializer_SupportsConfigSerializable() {
        ConfigSerializableSerializer serializer = new ConfigSerializableSerializer();

        assertThat(serializer.supports(SimpleSerializable.class)).isTrue();
        assertThat(serializer.supports(NestedSerializable.class)).isTrue();
        assertThat(serializer.supports(String.class)).isFalse();
    }

    @Test
    void testConfigSerializableSerializer_SerializeViaRegistry() throws Exception {
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        SerdesContext context = SerdesContext.of(configurer, null, null);

        SimpleSerializable obj = new SimpleSerializable("test", 123);
        Object simplifiedObj = configurer.simplify(obj, GenericsDeclaration.of(SimpleSerializable.class), context, true);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) simplifiedObj;

        assertThat(map).containsEntry("name", "test");
        assertThat(map).containsEntry("value", 123);
    }

    @Test
    void testConfigSerializableSerializer_DeserializeViaRegistry() throws Exception {
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        SerdesContext context = SerdesContext.of(configurer, null, null);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "deserialized");
        map.put("value", 456);

        SimpleSerializable obj = configurer.resolveType(
            map,
            GenericsDeclaration.of(map),
            SimpleSerializable.class,
            GenericsDeclaration.of(SimpleSerializable.class),
            context
        );

        assertThat(obj.getName()).isEqualTo("deserialized");
        assertThat(obj.getValue()).isEqualTo(456);
    }

    @Test
    void testConfigSerializableSerializer_NestedSerialization() throws Exception {
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        SerdesContext context = SerdesContext.of(configurer, null, null);

        NestedSerializable obj = new NestedSerializable("outer", new SimpleSerializable("inner", 999));
        Object simplifiedObj = configurer.simplify(obj, GenericsDeclaration.of(NestedSerializable.class), context, true);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) simplifiedObj;

        assertThat(map).containsKey("id");
        assertThat(map).containsKey("inner");

        @SuppressWarnings("unchecked")
        Map<String, Object> innerMap = (Map<String, Object>) map.get("inner");
        assertThat(innerMap).containsEntry("name", "inner");
        assertThat(innerMap).containsEntry("value", 999);
    }

    // === CONFIG INTEGRATION TESTS ===

    @Test
    void testConfigSerializable_InConfig_SingleField_WorksCorrectly() throws Exception {
        SerializableConfig config = new SerializableConfig();
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        config.setSingle(new SimpleSerializable("custom", 777));

        String yaml = config.saveToString();
        assertThat(yaml).contains("name: custom");
        assertThat(yaml).contains("value: 777");

        SerializableConfig loaded = new SerializableConfig();
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.load(yaml);

        assertThat(loaded.getSingle()).isEqualTo(new SimpleSerializable("custom", 777));
    }

    @Test
    void testConfigSerializable_InConfig_ListField_WorksCorrectly() throws Exception {
        SerializableConfig config = new SerializableConfig();
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        String yaml = config.saveToString();
        assertThat(yaml).contains("first");
        assertThat(yaml).contains("second");

        SerializableConfig loaded = new SerializableConfig();
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.load(yaml);

        assertThat(loaded.getList()).hasSize(2);
        assertThat(loaded.getList().get(0)).isEqualTo(new SimpleSerializable("first", 1));
        assertThat(loaded.getList().get(1)).isEqualTo(new SimpleSerializable("second", 2));
    }

    @Test
    void testConfigSerializable_InConfig_MapField_WorksCorrectly() throws Exception {
        SerializableConfig config = new SerializableConfig();
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        String yaml = config.saveToString();
        assertThat(yaml).contains("key1");
        assertThat(yaml).contains("value1");
        assertThat(yaml).contains("key2");
        assertThat(yaml).contains("value2");

        SerializableConfig loaded = new SerializableConfig();
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.load(yaml);

        assertThat(loaded.getMap()).hasSize(2);
        assertThat(loaded.getMap().get("key1")).isEqualTo(new SimpleSerializable("value1", 10));
        assertThat(loaded.getMap().get("key2")).isEqualTo(new SimpleSerializable("value2", 20));
    }

    @Test
    void testConfigSerializable_NestedInConfig_WorksCorrectly() throws Exception {
        NestedSerializableConfig config = new NestedSerializableConfig();
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        String yaml = config.saveToString();
        assertThat(yaml).contains("outer");
        assertThat(yaml).contains("inner");

        NestedSerializableConfig loaded = new NestedSerializableConfig();
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());
        loaded.load(yaml);

        assertThat(loaded.getNested()).isEqualTo(new NestedSerializable("outer", new SimpleSerializable("inner", 99)));
    }

    // === ERROR HANDLING TESTS ===

    @Test
    void testConfigSerializable_MissingDeserializeMethod_ThrowsException() {
        MissingDeserializeConfig config = new MissingDeserializeConfig();
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Saving should work (serialize method exists)
        String yaml = config.saveToString();
        assertThat(yaml).contains("value: test");

        // Loading should fail (no deserialize method)
        MissingDeserializeConfig loaded = new MissingDeserializeConfig();
        loaded.withConfigurer(new YamlSnakeYamlConfigurer());

        assertThatThrownBy(() -> loaded.load(yaml))
            .hasRootCauseInstanceOf(NoSuchMethodException.class)
            .hasStackTraceContaining("deserialize");
    }

    // === SERIALIZER PRECEDENCE TESTS ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverridableSerializable implements ConfigSerializable {
        private String value;

        @Override
        public void serialize(SerializationData data, GenericsDeclaration generics) {
            data.set("value", "from-configserializable");
        }

        public static OverridableSerializable deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new OverridableSerializable("from-configserializable");
        }
    }

    public static class CustomOverrideSerializer implements ObjectSerializer<OverridableSerializable> {
        @Override
        public boolean supports(Class<?> type) {
            return OverridableSerializable.class.isAssignableFrom(type);
        }

        @Override
        public void serialize(OverridableSerializable object, SerializationData data, GenericsDeclaration generics) {
            data.set("value", "from-custom-serializer");
        }

        @Override
        public OverridableSerializable deserialize(DeserializationData data, GenericsDeclaration generics) {
            return new OverridableSerializable("from-custom-serializer");
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class OverrideTestConfig extends OkaeriConfig {
        private OverridableSerializable obj = new OverridableSerializable("original");
    }

    @Test
    void testConfigSerializable_ExplicitSerializerRegistered_OverridesConfigSerializable() throws Exception {
        // Test with ConfigSerializable only (default behavior)
        OverrideTestConfig config1 = new OverrideTestConfig();
        config1.withConfigurer(new YamlSnakeYamlConfigurer());
        String yaml1 = config1.saveToString();
        assertThat(yaml1).contains("from-configserializable");

        // Test with explicit serializer registered (should override)
        OverrideTestConfig config2 = new OverrideTestConfig();
        YamlSnakeYamlConfigurer configurer2 = new YamlSnakeYamlConfigurer();
        configurer2.getRegistry().register(new CustomOverrideSerializer());
        config2.withConfigurer(configurer2);

        String yaml2 = config2.saveToString();
        assertThat(yaml2).contains("from-custom-serializer");
        assertThat(yaml2).doesNotContain("from-configserializable");
    }

    @Test
    void testConfigSerializable_ExplicitSerializerRegistered_OverridesDeserialization() throws Exception {
        // Create YAML with ConfigSerializable
        OverrideTestConfig config1 = new OverrideTestConfig();
        config1.withConfigurer(new YamlSnakeYamlConfigurer());
        String yaml = config1.saveToString();

        // Load with explicit serializer (should use custom deserializer)
        OverrideTestConfig config2 = new OverrideTestConfig();
        YamlSnakeYamlConfigurer configurer2 = new YamlSnakeYamlConfigurer();
        configurer2.getRegistry().register(new CustomOverrideSerializer());
        config2.withConfigurer(configurer2);
        config2.load(yaml);

        assertThat(config2.getObj().getValue()).isEqualTo("from-custom-serializer");
    }
}
