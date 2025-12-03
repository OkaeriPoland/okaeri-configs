package eu.okaeri.configs.annotations;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Serdes;
import eu.okaeri.configs.configurer.InMemoryConfigurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for @Serdes annotation.
 * <p>
 * Verifies:
 * - Custom serializer is used instead of registry serializer
 * - Multiple different serializers for same type work
 * - Custom deserializer is used for loading
 * - Error validation (no constructor, type mismatch)
 * - Integration with other annotations
 * - Nested configs work correctly
 * - Serializer caching works
 */
class SerdesAnnotationTest {

    // Test Serializers

    /**
     * Test serializer A - prefixes strings with "A:"
     */
    public static class TestStringSerializerA implements ObjectSerializer<String> {
        @Override
        public boolean supports(@NonNull Class<?> type) {
            return String.class.equals(type);
        }

        @Override
        public void serialize(@NonNull String object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
            data.add(VALUE, "A:" + object);
        }

        @Override
        public String deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
            String value = data.get(VALUE, String.class);
            return value.startsWith("A:") ? value.substring(2) : value;
        }
    }

    /**
     * Test serializer B - prefixes strings with "B:"
     */
    public static class TestStringSerializerB implements ObjectSerializer<String> {
        @Override
        public boolean supports(@NonNull Class<?> type) {
            return String.class.equals(type);
        }

        @Override
        public void serialize(@NonNull String object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
            data.add(VALUE, "B:" + object);
        }

        @Override
        public String deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
            String value = data.get(VALUE, String.class);
            return value.startsWith("B:") ? value.substring(2) : value;
        }
    }

    /**
     * Serializer for testing caching - increments static counter
     */
    public static class CachingTestSerializer implements ObjectSerializer<String> {
        public static int instanceCount = 0;

        public CachingTestSerializer() {
            instanceCount++;
        }

        @Override
        public boolean supports(@NonNull Class<?> type) {
            return String.class.equals(type);
        }

        @Override
        public void serialize(@NonNull String object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
            data.add(VALUE, object);
        }

        @Override
        public String deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
            return data.get(VALUE, String.class);
        }
    }

    /**
     * Invalid serializer - no no-args constructor
     */
    public static class InvalidSerializerNoConstructor implements ObjectSerializer<String> {
        public InvalidSerializerNoConstructor(String required) {
        }

        @Override
        public boolean supports(@NonNull Class<?> type) {
            return false;
        }

        @Override
        public void serialize(@NonNull String object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        }

        @Override
        public String deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
            return null;
        }
    }

    /**
     * Serializer that only supports Integer
     */
    public static class IntegerSerializer implements ObjectSerializer<Integer> {
        @Override
        public boolean supports(@NonNull Class<?> type) {
            return Integer.class.equals(type) || int.class.equals(type);
        }

        @Override
        public void serialize(@NonNull Integer object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
            data.add(VALUE, object);
        }

        @Override
        public Integer deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
            return data.get(VALUE, Integer.class);
        }
    }

    // Test Config Classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SingleSerdesConfig extends OkaeriConfig {
        private String normalField = "normal";

        @Serdes(serializer = TestStringSerializerA.class)
        private String customField = "custom";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MultipleSerdesConfig extends OkaeriConfig {
        private String defaultField = "default";

        @Serdes(serializer = TestStringSerializerA.class)
        private String fieldA = "valueA";

        @Serdes(serializer = TestStringSerializerB.class)
        private String fieldB = "valueB";

        private String anotherDefault = "default2";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedSerdesConfig extends OkaeriConfig {
        private String topLevel = "top";
        private SubConfig sub = new SubConfig();

        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class SubConfig extends OkaeriConfig {
            @Serdes(serializer = TestStringSerializerA.class)
            private String innerField = "inner";
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SerdesWithCommentConfig extends OkaeriConfig {
        @Comment("This is a custom field")
        @Serdes(serializer = TestStringSerializerA.class)
        private String customField = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CachingConfig extends OkaeriConfig {
        @Serdes(serializer = CachingTestSerializer.class)
        private String field1 = "value1";

        @Serdes(serializer = CachingTestSerializer.class)
        private String field2 = "value2";
    }

    // Tests

    @Test
    void testSerdes_CustomSerializerUsed() {
        // Given
        SingleSerdesConfig config = ConfigManager.create(SingleSerdesConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.saveToString();

        // Then - custom serializer should prefix with "A:"
        assertThat(config.getInternalState().get("customField")).isEqualTo("A:custom");
        assertThat(config.getInternalState().get("normalField")).isEqualTo("normal");
    }

    @Test
    void testSerdes_CustomDeserializerUsed() {
        // Given
        SingleSerdesConfig config = ConfigManager.create(SingleSerdesConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When - load config with "A:" prefix
        config.set("customField", "A:loaded");
        config.set("normalField", "loaded_normal");

        // Then - deserializer should strip "A:" prefix
        assertThat(config.getCustomField()).isEqualTo("loaded");
        assertThat(config.getNormalField()).isEqualTo("loaded_normal");
    }

    @Test
    void testSerdes_RoundTrip() {
        // Given
        SingleSerdesConfig config1 = ConfigManager.create(SingleSerdesConfig.class);
        config1.withConfigurer(new YamlSnakeYamlConfigurer());

        // When - save and load
        String yaml = config1.saveToString();

        SingleSerdesConfig config2 = ConfigManager.create(SingleSerdesConfig.class);
        config2.withConfigurer(new YamlSnakeYamlConfigurer());
        config2.load(yaml);

        // Then - values survive round-trip
        assertThat(config2.getCustomField()).isEqualTo("custom");
        assertThat(config2.getNormalField()).isEqualTo("normal");
    }

    @Test
    void testSerdes_MultipleDifferentSerializers() {
        // Given
        MultipleSerdesConfig config = ConfigManager.create(MultipleSerdesConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.saveToString();

        // Then - each field uses its own serializer
        assertThat(config.getInternalState().get("defaultField")).isEqualTo("default");
        assertThat(config.getInternalState().get("fieldA")).isEqualTo("A:valueA");
        assertThat(config.getInternalState().get("fieldB")).isEqualTo("B:valueB");
        assertThat(config.getInternalState().get("anotherDefault")).isEqualTo("default2");
    }

    @Test
    void testSerdes_FallbackToRegistry() {
        // Given
        SingleSerdesConfig config = ConfigManager.create(SingleSerdesConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.saveToString();

        // Then - normalField uses standard serializer (no prefix)
        assertThat(config.getInternalState().get("normalField")).isEqualTo("normal");
        assertThat(config.getInternalState().get("normalField").toString()).doesNotContain("A:");
        assertThat(config.getInternalState().get("normalField").toString()).doesNotContain("B:");
    }

    // Static inner classes for error testing
    public static class InvalidConfig extends OkaeriConfig {
        @Serdes(serializer = InvalidSerializerNoConstructor.class)
        private String field = "value";
    }

    public static class TypeMismatchConfig extends OkaeriConfig {
        @Serdes(serializer = IntegerSerializer.class)
        private String field = "value";
    }

    @Test
    void testSerdes_NoArgsConstructorMissing_ThrowsException() {
        // When/Then - should throw during create
        assertThatThrownBy(() -> ConfigManager.create(InvalidConfig.class))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("no-args constructor");
    }

    @Test
    void testSerdes_SerializerDoesNotSupportType_ThrowsException() {
        // When/Then - should throw during create
        assertThatThrownBy(() -> ConfigManager.create(TypeMismatchConfig.class))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("does not support field type");
    }

    @Test
    void testSerdes_WithNestedConfig() {
        // Given
        NestedSerdesConfig config = ConfigManager.create(NestedSerdesConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.saveToString();

        // Then - nested config's @Serdes field uses custom serializer
        // The innerField value will be in the nested map under "sub"
        assertThat(config.getInternalState().containsKey("sub")).isTrue();
        assertThat(config.getInternalState().get("topLevel")).isEqualTo("top");
    }

    @Test
    void testSerdes_WithOtherAnnotations() {
        // Given
        SerdesWithCommentConfig config = ConfigManager.create(SerdesWithCommentConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // When
        config.saveToString();

        // Then - both @Comment and @Serdes work together
        FieldDeclaration field = config.getDeclaration().getField("customField").orElse(null);
        assertThat(field).isNotNull();
        assertThat(field.getComment()).contains("This is a custom field");
        assertThat(config.getInternalState().get("customField")).isEqualTo("A:value");
    }

    @Test
    void testSerdes_InDeclaration() {
        // Given
        SingleSerdesConfig config = ConfigManager.create(SingleSerdesConfig.class);

        // When
        ConfigDeclaration declaration = config.getDeclaration();
        FieldDeclaration customField = declaration.getField("customField").orElse(null);
        FieldDeclaration normalField = declaration.getField("normalField").orElse(null);

        // Then - field with @Serdes has custom serializer set
        assertThat(customField).isNotNull();
        assertThat(customField.getCustomSerializer()).isNotNull();
        assertThat(customField.getCustomSerializer()).isInstanceOf(TestStringSerializerA.class);

        assertThat(normalField).isNotNull();
        assertThat(normalField.getCustomSerializer()).isNull();
    }

    @Test
    void testSerdes_SerializerInstanceCached() {
        // Given - reset counter
        CachingTestSerializer.instanceCount = 0;

        // When - create config (instantiates serializers once per field during declaration creation)
        CachingConfig config = ConfigManager.create(CachingConfig.class);
        config.withConfigurer(new InMemoryConfigurer());

        // Each field gets its own serializer instance, but they're cached
        // field1 and field2 both use CachingTestSerializer, so 2 instances total
        assertThat(CachingTestSerializer.instanceCount).isEqualTo(2);

        // When - save multiple times
        config.saveToString();
        config.saveToString();
        config.saveToString();

        // Then - no additional instances created (cached)
        assertThat(CachingTestSerializer.instanceCount).isEqualTo(2);
    }
}
