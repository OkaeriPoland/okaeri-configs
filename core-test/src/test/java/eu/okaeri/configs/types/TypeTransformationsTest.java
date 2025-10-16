package eu.okaeri.configs.types;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for type transformations and conversions.
 * <p>
 * Validates:
 * - String → Integer conversion
 * - String → Boolean conversion
 * - Integer → String conversion
 * - Integer → Long conversion (primitive cross-conversion)
 * - String → Enum conversion
 * - Enum → String conversion
 * - Two-step transformations (A → B → C)
 * - Custom transformers registered in registry
 * - Primitive unboxing/boxing
 * - Incompatible type conversion (should throw)
 */
class TypeTransformationsTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private int intValue = 0;
        private String stringValue = "";
        private boolean boolValue = false;
    }

    @Test
    void testStringToIntegerConversion_ViaSet_ConvertsCorrectly() throws Exception {
        // Create config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Set string value to int field (should transform)
        config.set("intValue", "42");

        // Verify conversion
        assertThat(config.getIntValue()).isEqualTo(42);
    }

    @Test
    void testStringToBooleanConversion_ViaLoad_ConvertsCorrectly() throws Exception {
        // Create map with string boolean values
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intValue", 0);
        map.put("stringValue", "");
        map.put("boolValue", "true");  // String instead of boolean

        // Load config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(map);

        // Verify conversion
        assertThat(config.isBoolValue()).isTrue();
    }

    @Test
    void testIntegerToStringConversion_ViaLoad_ConvertsCorrectly() throws Exception {
        // Create map with integer value for string field
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intValue", 0);
        map.put("stringValue", 12345);  // Integer instead of String
        map.put("boolValue", false);

        // Load config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(map);

        // Verify conversion
        assertThat(config.getStringValue()).isEqualTo("12345");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NumericConversionConfig extends OkaeriConfig {
        private long longValue = 0L;
        private int intValue = 0;
        private double doubleValue = 0.0;
    }

    @Test
    void testIntegerToLongConversion_ViaSet_ConvertsCorrectly() throws Exception {
        // Create config
        NumericConversionConfig config = ConfigManager.create(NumericConversionConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Set integer value to long field
        config.set("longValue", 999);

        // Verify conversion
        assertThat(config.getLongValue()).isEqualTo(999L);
    }

    @Test
    void testLongToIntegerConversion_ViaSet_ConvertsCorrectly() throws Exception {
        // Create config
        NumericConversionConfig config = ConfigManager.create(NumericConversionConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Set long value to int field
        config.set("intValue", 123L);

        // Verify conversion
        assertThat(config.getIntValue()).isEqualTo(123);
    }

    @Test
    void testIntegerToDoubleConversion_ViaSet_ConvertsCorrectly() throws Exception {
        // Create config
        NumericConversionConfig config = ConfigManager.create(NumericConversionConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Set integer value to double field
        config.set("doubleValue", 42);

        // Verify conversion
        assertThat(config.getDoubleValue()).isEqualTo(42.0);
    }

    public enum TestEnum {
        FIRST, SECOND, THIRD
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EnumConfig extends OkaeriConfig {
        private TestEnum enumValue = TestEnum.FIRST;
        private String stringValue = "";
    }

    @Test
    void testStringToEnumConversion_ExactMatch_ConvertsCorrectly() throws Exception {
        // Create map with string enum value
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enumValue", "SECOND");
        map.put("stringValue", "");

        // Load config
        EnumConfig config = ConfigManager.create(EnumConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(map);

        // Verify conversion
        assertThat(config.getEnumValue()).isEqualTo(TestEnum.SECOND);
    }

    @Test
    void testStringToEnumConversion_CaseInsensitive_ConvertsCorrectly() throws Exception {
        // Create map with lowercase enum value
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enumValue", "third");  // lowercase
        map.put("stringValue", "");

        // Load config
        EnumConfig config = ConfigManager.create(EnumConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(map);

        // Verify conversion (should work with case-insensitive fallback)
        assertThat(config.getEnumValue()).isEqualTo(TestEnum.THIRD);
    }

    @Test
    void testEnumToStringConversion_ViaSave_ConvertsCorrectly(@TempDir Path tempDir) throws Exception {
        // Create config with enum value
        EnumConfig config = ConfigManager.create(EnumConfig.class);
        config.setEnumValue(TestEnum.SECOND);
        config.setStringValue("test");

        // Save to file
        Path configFile = tempDir.resolve("enum-config.yml");
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.withBindFile(configFile);
        config.save();

        // Convert to map
        Map<String, Object> map = config.asMap(new YamlSnakeYamlConfigurer(), false);

        // Verify enum is saved as string
        assertThat(map.get("enumValue")).isInstanceOf(String.class);
        assertThat(map.get("enumValue")).isEqualTo("SECOND");
    }

    @Test
    void testPrimitiveBoxing_IntToInteger_WorksCorrectly() throws Exception {
        // Create config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Set primitive int (should auto-box to Integer internally)
        config.setIntValue(42);

        // Get as object (should return Integer)
        Object value = config.get("intValue");
        assertThat(value).isInstanceOf(Integer.class);
        assertThat(value).isEqualTo(42);
    }

    @Test
    void testPrimitiveUnboxing_IntegerToInt_WorksCorrectly() throws Exception {
        // Create config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Set Integer object (should auto-unbox to primitive int)
        config.set("intValue", Integer.valueOf(99));

        // Verify primitive field is set
        assertThat(config.getIntValue()).isEqualTo(99);
    }

    /**
     * Custom transformer for testing: String <-> CustomObject
     */
    @Data
    @AllArgsConstructor
    public static class CustomObject {
        private final String value;
    }

    public static class CustomObjectTransformer extends BidirectionalTransformer<String, CustomObject> {
        @Override
        public GenericsPair<String, CustomObject> getPair() {
            return this.genericsPair(String.class, CustomObject.class);
        }

        @Override
        public CustomObject leftToRight(String data, SerdesContext serdesContext) {
            return new CustomObject(data);
        }

        @Override
        public String rightToLeft(CustomObject data, SerdesContext serdesContext) {
            return data.getValue();
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CustomObjectConfig extends OkaeriConfig {
        private CustomObject customObj;
    }

    @Test
    void testCustomTransformer_RegisterAndUse_ConvertsCorrectly() throws Exception {
        // Create configurer with custom transformer
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        configurer.getRegistry().register(new CustomObjectTransformer());

        // Create map with string value
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("customObj", "test-value");

        // Load config
        CustomObjectConfig config = ConfigManager.create(CustomObjectConfig.class);
        config.withConfigurer(configurer);
        config.load(map);

        // Verify conversion using custom transformer
        assertThat(config.getCustomObj()).isNotNull();
        assertThat(config.getCustomObj().getValue()).isEqualTo("test-value");
    }

    @Test
    void testCustomTransformer_SaveAndLoad_RoundTripsCorrectly(@TempDir Path tempDir) throws Exception {
        // Create configurer with custom transformer
        YamlSnakeYamlConfigurer configurer = new YamlSnakeYamlConfigurer();
        configurer.getRegistry().register(new CustomObjectTransformer());

        // Create config with custom object
        CustomObjectConfig config = ConfigManager.create(CustomObjectConfig.class);
        config.setCustomObj(new CustomObject("original-value"));
        config.withConfigurer(configurer);

        // Save to file
        Path configFile = tempDir.resolve("custom-object.yml");
        config.withBindFile(configFile);
        config.save();

        // Load in new config
        CustomObjectConfig loaded = ConfigManager.create(CustomObjectConfig.class);
        YamlSnakeYamlConfigurer loadConfigurer = new YamlSnakeYamlConfigurer();
        loadConfigurer.getRegistry().register(new CustomObjectTransformer());
        loaded.withConfigurer(loadConfigurer);
        loaded.withBindFile(configFile);
        loaded.load();

        // Verify round-trip
        assertThat(loaded.getCustomObj()).isNotNull();
        assertThat(loaded.getCustomObj().getValue()).isEqualTo("original-value");
    }

    @Test
    void testInvalidStringToIntConversion_ThrowsException() throws Exception {
        // Create config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Attempt to set invalid string to int field
        assertThatThrownBy(() -> config.set("intValue", "not-a-number"))
            .isInstanceOf(Exception.class);  // NumberFormatException wrapped in transformation exception
    }

    @Test
    void testInvalidEnumValue_ThrowsException() throws Exception {
        // Create map with invalid enum value
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enumValue", "INVALID_VALUE");
        map.put("stringValue", "");

        // Attempt to load config
        EnumConfig config = ConfigManager.create(EnumConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        assertThatThrownBy(() -> config.load(map))
            .isInstanceOf(Exception.class);  // Enum conversion should fail
    }

    @Test
    void testMultipleStringTransformations_AllWork() throws Exception {
        // Create map with various string conversions
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intValue", "123");       // String → int
        map.put("stringValue", "test");   // String → String (no conversion)
        map.put("boolValue", "false");    // String → boolean

        // Load config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(map);

        // Verify all conversions
        assertThat(config.getIntValue()).isEqualTo(123);
        assertThat(config.getStringValue()).isEqualTo("test");
        assertThat(config.isBoolValue()).isFalse();
    }

    @Test
    void testZeroAndNullConversions_HandleCorrectly() throws Exception {
        // Create map with edge cases
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intValue", "0");         // String "0" → int 0
        map.put("stringValue", null);     // null value
        map.put("boolValue", "false");    // String "false" → boolean false

        // Load config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.load(map);

        // Verify conversions
        assertThat(config.getIntValue()).isEqualTo(0);
        assertThat(config.getStringValue()).isNull();
        assertThat(config.isBoolValue()).isFalse();
    }

    @Test
    void testNegativeNumberConversion_WorksCorrectly() throws Exception {
        // Create config
        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Set negative number as string
        config.set("intValue", "-999");

        // Verify conversion
        assertThat(config.getIntValue()).isEqualTo(-999);
    }
}
