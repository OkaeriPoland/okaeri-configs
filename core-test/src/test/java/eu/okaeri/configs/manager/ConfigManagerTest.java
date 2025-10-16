package eu.okaeri.configs.manager;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.test.configs.PrimitivesTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ConfigManager utility methods.
 * NOTE: Basic create() methods are already tested in ConfigCreationTest.
 * This class focuses on advanced methods: createUnsafe, transformCopy, deepCopy, initialize.
 */
class ConfigManagerTest {

    @Test
    void testCreateUnsafe_CreatesInstanceWithoutConstructor() {
        // When: Creating config using unsafe allocation
        PrimitivesTestConfig config = ConfigManager.createUnsafe(PrimitivesTestConfig.class);

        // Then: Config should be created (constructor may not be called)
        assertThat(config).isNotNull();
        assertThat(config.getDeclaration()).isNotNull();
    }

    @Test
    void testCreateUnsafe_WithNullClass_ThrowsException() {
        // When/Then: Creating unsafe config with null class should throw
        assertThatThrownBy(() -> ConfigManager.createUnsafe(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testTransformCopy_CopiesConfigToSameType() {
        // Given: Source config with values
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());
        source.setIntValue(999);
        source.setLongValue(12345L);
        // Force POJO data into configurer
        source.saveToString();

        // When: Transforming copy to same type
        PrimitivesTestConfig copy = ConfigManager.transformCopy(source, PrimitivesTestConfig.class);

        // Then: Copy should have same values and configurer
        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(source);
        assertThat(copy.getIntValue()).isEqualTo(999);
        assertThat(copy.getLongValue()).isEqualTo(12345L);
        assertThat(copy.getConfigurer()).isSameAs(source.getConfigurer());
    }

    @Test
    void testTransformCopy_CopiesConfigToDifferentType() {
        // Given: Source config with overlapping fields
        SourceConfig source = ConfigManager.create(SourceConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());
        source.setCommonField("shared value");
        source.setSourceOnlyField("source only");
        // Force POJO data into configurer
        source.saveToString();

        // When: Transforming copy to different type with overlapping fields
        TargetConfig copy = ConfigManager.transformCopy(source, TargetConfig.class);

        // Then: Common fields should be copied
        assertThat(copy).isNotNull();
        assertThat(copy.getCommonField()).isEqualTo("shared value");
        assertThat(copy.getTargetOnlyField()).isEqualTo("target default"); // Default value
    }

    @Test
    void testTransformCopy_DocumentWrapperPattern() {
        // Given: Generic Document wrapper loaded with data (like in okaeri-persistence)
        GenericDocument document = ConfigManager.create(GenericDocument.class);
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        document.withConfigurer(configurer);
        // Simulate loading data that matches UserDocument structure
        configurer.setValueUnsafe("userId", 12345);
        configurer.setValueUnsafe("username", "testuser");
        configurer.setValueUnsafe("email", "test@example.com");

        // When: Transforming Document wrapper into specific UserDocument type
        UserDocument userDoc = ConfigManager.transformCopy(document, UserDocument.class);

        // Then: Fields should be properly mapped to specific type
        assertThat(userDoc).isNotNull();
        assertThat(userDoc.getUserId()).isEqualTo(12345);
        assertThat(userDoc.getUsername()).isEqualTo("testuser");
        assertThat(userDoc.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testTransformCopy_PreservesBindFile() {
        // Given: Source config with bind file
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());
        source.withBindFile("test.yml");

        // When: Transforming copy
        PrimitivesTestConfig copy = ConfigManager.transformCopy(source, PrimitivesTestConfig.class);

        // Then: Bind file should be preserved
        assertThat(copy.getBindFile()).isEqualTo(source.getBindFile());
    }

    @Test
    void testTransformCopy_WithTypeConversion() {
        // Given: Source config with String value in configurer
        SourceConfig source = ConfigManager.create(SourceConfig.class);
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        source.withConfigurer(configurer);
        // Use setValueUnsafe for dynamic key that doesn't exist in source declaration
        configurer.setValueUnsafe("numberAsString", "456");

        // When: Transforming to config expecting Integer
        ConfigWithNumber copy = ConfigManager.transformCopy(source, ConfigWithNumber.class);

        // Then: String should be converted to Integer
        assertThat(copy.getNumberAsString()).isEqualTo(456);
    }

    @Test
    void testTransformCopy_WithNullSource_ThrowsException() {
        // When/Then: Transforming null source should throw
        assertThatThrownBy(() -> ConfigManager.transformCopy(null, PrimitivesTestConfig.class))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testTransformCopy_WithNullTargetClass_ThrowsException() {
        // Given: Source config
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());

        // When/Then: Transforming to null class should throw
        assertThatThrownBy(() -> ConfigManager.transformCopy(source, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testDeepCopy_CopiesWithNewConfigurer() {
        // Given: Source config with data
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        YamlSnakeYamlConfigurer originalConfigurer = new YamlSnakeYamlConfigurer();
        source.withConfigurer(originalConfigurer);
        source.setIntValue(777);
        source.setDoubleValue(3.14159);

        // When: Deep copying with new configurer
        YamlSnakeYamlConfigurer newConfigurer = new YamlSnakeYamlConfigurer();
        PrimitivesTestConfig copy = ConfigManager.deepCopy(source, newConfigurer, PrimitivesTestConfig.class);

        // Then: Copy should have same values but different configurer
        assertThat(copy).isNotNull();
        assertThat(copy).isNotSameAs(source);
        assertThat(copy.getIntValue()).isEqualTo(777);
        assertThat(copy.getDoubleValue()).isEqualTo(3.14159);
        assertThat(copy.getConfigurer()).isNotSameAs(originalConfigurer);
        assertThat(copy.getConfigurer()).isSameAs(newConfigurer);
    }

    @Test
    void testDeepCopy_PreservesBindFile() {
        // Given: Source config with bind file
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());
        source.withBindFile("original.yml");

        // When: Deep copying
        PrimitivesTestConfig copy = ConfigManager.deepCopy(source, new YamlSnakeYamlConfigurer(), PrimitivesTestConfig.class);

        // Then: Bind file should be preserved
        assertThat(copy.getBindFile()).isEqualTo(source.getBindFile());
    }

    @Test
    void testDeepCopy_CopiesSerdesRegistry() {
        // Given: Source config with configurer and custom serializer
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        YamlSnakeYamlConfigurer originalConfigurer = new YamlSnakeYamlConfigurer();
        source.withConfigurer(originalConfigurer);

        // Register a dummy serializer
        DummySerializer dummySerializer = new DummySerializer();
        originalConfigurer.getRegistry().register(dummySerializer);

        // When: Deep copying with new configurer
        YamlSnakeYamlConfigurer newConfigurer = new YamlSnakeYamlConfigurer();
        ConfigManager.deepCopy(source, newConfigurer, PrimitivesTestConfig.class);

        // Then: Custom serializer should be present in new configurer (getSerializer returns ObjectSerializer directly, not Optional)
        eu.okaeri.configs.serdes.ObjectSerializer<?> copiedSerializer = newConfigurer.getRegistry().getSerializer(DummyClass.class);
        assertThat(copiedSerializer).isNotNull();
        assertThat(copiedSerializer).isSameAs(dummySerializer);
    }

    @Test
    void testDeepCopy_WithNullSource_ThrowsException() {
        // When/Then: Deep copying null source should throw
        assertThatThrownBy(() -> ConfigManager.deepCopy(null, new YamlSnakeYamlConfigurer(), PrimitivesTestConfig.class))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testDeepCopy_WithNullConfigurer_ThrowsException() {
        // Given: Source config
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());

        // When/Then: Deep copying with null configurer should throw
        assertThatThrownBy(() -> ConfigManager.deepCopy(source, null, PrimitivesTestConfig.class))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testDeepCopy_WithNullTargetClass_ThrowsException() {
        // Given: Source config
        PrimitivesTestConfig source = ConfigManager.create(PrimitivesTestConfig.class);
        source.withConfigurer(new YamlSnakeYamlConfigurer());

        // When/Then: Deep copying to null class should throw
        assertThatThrownBy(() -> ConfigManager.deepCopy(source, new YamlSnakeYamlConfigurer(), null))
            .isInstanceOf(NullPointerException.class);
    }

    // === Test Config Classes ===

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SourceConfig extends OkaeriConfig {
        private String commonField = "default common";
        private String sourceOnlyField = "source default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TargetConfig extends OkaeriConfig {
        private String commonField = "default common";
        private String targetOnlyField = "target default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ConfigWithNumber extends OkaeriConfig {
        private Integer numberAsString = 0;
    }

    /**
     * Generic document wrapper (similar to okaeri-persistence Document base class).
     * Used to load data before knowing the specific type.
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class GenericDocument extends OkaeriConfig {
        // Empty - acts as generic wrapper, fields loaded dynamically
    }

    /**
     * Specific document type extending the base (similar to okaeri-persistence usage).
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UserDocument extends OkaeriConfig {
        private Integer userId = 0;
        private String username = "";
        private String email = "";
    }

    /**
     * Dummy class for serializer testing.
     */
    public static class DummyClass {
        private String value;
    }

    /**
     * Dummy serializer for testing serdes registry copying.
     */
    public static class DummySerializer implements eu.okaeri.configs.serdes.ObjectSerializer<DummyClass> {
        @Override
        public boolean supports(Class<?> type) {
            return DummyClass.class.equals(type);
        }

        @Override
        public void serialize(DummyClass object, eu.okaeri.configs.serdes.SerializationData data, eu.okaeri.configs.schema.GenericsDeclaration generics) {
            data.setValue(object.value);
        }

        @Override
        public DummyClass deserialize(eu.okaeri.configs.serdes.DeserializationData data, eu.okaeri.configs.schema.GenericsDeclaration generics) {
            DummyClass dummy = new DummyClass();
            dummy.value = data.getValue(String.class);
            return dummy;
        }
    }
}
