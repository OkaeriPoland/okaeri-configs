package eu.okaeri.configs.lifecycle;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.test.configs.PrimitivesTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OkaeriConfig get/set operations.
 * <p>
 * Scenarios tested:
 * - set(key, value) updates field
 * - set(key, value) updates configurer
 * - set(key, value) with type transformation
 * - get(key) returns field value
 * - get(key) for undeclared key returns configurer value
 * - get(key, Class) with type conversion
 * - get(key, GenericsDeclaration) with generics
 * - Error cases (no configurer)
 */
class ConfigGetSetTest {

    @Test
    void testSet_UpdatesField() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Initial value
        assertThat(config.getIntValue()).isEqualTo(42);

        // Act
        config.set("intValue", 999);

        // Assert
        assertThat(config.getIntValue()).isEqualTo(999);
    }

    @Test
    void testSet_UpdatesConfigurer() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.set("intValue", 777);

        // Assert - configurer should also have the value
        assertThat(config.getConfigurer().getValue("intValue")).isEqualTo(777);
    }

    @Test
    void testSet_WithTypeTransformation_StringToInt() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act - set String value for int field
        config.set("intValue", "555");

        // Assert - should convert String to int
        assertThat(config.getIntValue()).isEqualTo(555);
    }

    @Test
    void testSet_WithTypeTransformation_StringToBoolean() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.set("boolValue", "false");

        // Assert
        assertThat(config.isBoolValue()).isFalse();
    }

    @Test
    void testSet_WithTypeTransformation_StringToDouble() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.set("doubleValue", "9.87654");

        // Assert
        assertThat(config.getDoubleValue()).isEqualTo(9.87654);
    }

    @Test
    void testSet_UndeclaredKey_StoresInConfigurer() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act - set key that's not a field
        config.set("customKey", "custom value");

        // Assert - should be stored in configurer
        assertThat(config.getConfigurer().getValue("customKey")).isEqualTo("custom value");
    }

    @Test
    void testGet_ReturnsFieldValue() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setIntValue(999);

        // Act
        Object value = config.get("intValue");

        // Assert
        assertThat(value).isEqualTo(999);
    }

    @Test
    void testGet_UndeclaredKey_ReturnsConfigurerValue() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.getConfigurer().setValue("orphanKey", "orphan value", null, null);

        // Act
        Object value = config.get("orphanKey");

        // Assert
        assertThat(value).isEqualTo("orphan value");
    }

    @Test
    void testGet_WithClass_TypeConversion() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setIntValue(123);

        // Act - get int value as String
        String value = config.get("intValue", String.class);

        // Assert - should convert int to String
        assertThat(value).isEqualTo("123");
    }

    @Test
    void testGet_WithClass_IntToLong() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setIntValue(999);

        // Act
        Long value = config.get("intValue", Long.class);

        // Assert
        assertThat(value).isEqualTo(999L);
    }

    @Test
    void testGet_WithClass_StringToBoolean() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.getConfigurer().setValue("stringBool", "true", null, null);

        // Act
        Boolean value = config.get("stringBool", Boolean.class);

        // Assert
        assertThat(value).isTrue();
    }

    @Test
    void testGet_WithGenericsDeclaration_SimpleType() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());
        config.setIntValue(777);

        // Act
        Integer value = config.get("intValue", GenericsDeclaration.of(Integer.class));

        // Assert
        assertThat(value).isEqualTo(777);
    }

    @Test
    void testGet_WithGenericsDeclaration_ListType() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        List<String> testList = Arrays.asList("a", "b", "c");
        config.set("testList", testList);

        // Act - get with generic type
        @SuppressWarnings("unchecked")
        List<String> value = config.get("testList", GenericsDeclaration.of(List.class));

        // Assert
        assertThat(value).containsExactly("a", "b", "c");
    }

    @Test
    void testSet_MultipleUpdates_AllApplied() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.set("intValue", 100);
        config.set("boolValue", false);
        config.set("doubleValue", 1.23);

        // Assert
        assertThat(config.getIntValue()).isEqualTo(100);
        assertThat(config.isBoolValue()).isFalse();
        assertThat(config.getDoubleValue()).isEqualTo(1.23);
    }

    @Test
    void testSet_OverwritesPreviousValue() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.set("intValue", 100);
        assertThat(config.getIntValue()).isEqualTo(100);

        config.set("intValue", 200);

        // Assert
        assertThat(config.getIntValue()).isEqualTo(200);
    }

    @Test
    void testSet_WithoutConfigurer_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);

        // Act & Assert
        assertThatThrownBy(() -> config.set("intValue", 999))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("configurer cannot be null");
    }

    @Test
    void testGet_WithoutConfigurer_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);

        // Act & Assert
        assertThatThrownBy(() -> config.get("intValue"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("configurer cannot be null");
    }

    @Test
    void testGet_WithClass_WithoutConfigurer_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);

        // Act & Assert
        assertThatThrownBy(() -> config.get("intValue", Integer.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("configurer cannot be null");
    }

    @Test
    void testGet_WithGenericsDeclaration_WithoutConfigurer_ThrowsException() {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);

        // Act & Assert
        assertThatThrownBy(() -> config.get("intValue", GenericsDeclaration.of(Integer.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("configurer cannot be null");
    }

    @Test
    void testSetThenGet_RoundTrip() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.set("intValue", 555);
        Object retrieved = config.get("intValue");

        // Assert
        assertThat(retrieved).isEqualTo(555);
    }

    @Test
    void testSetThenGetWithClass_RoundTrip() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act
        config.set("doubleValue", 3.14159);
        Double retrieved = config.get("doubleValue", Double.class);

        // Assert
        assertThat(retrieved).isEqualTo(3.14159);
    }

    @Test
    void testSet_NullValue_Handled() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        // Act - set null for wrapper type
        config.set("intWrapper", null);

        // Assert
        assertThat(config.getIntWrapper()).isNull();
    }

    @Test
    void testGet_AllPrimitiveTypes() throws Exception {
        // Arrange
        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer());

        config.setBoolValue(false);
        config.setByteValue((byte) 99);
        config.setCharValue('X');
        config.setDoubleValue(9.87);
        config.setFloatValue(1.23f);
        config.setIntValue(777);
        config.setLongValue(999999L);
        config.setShortValue((short) 555);

        // Act & Assert
        assertThat(config.get("boolValue")).isEqualTo(false);
        assertThat(config.get("byteValue")).isEqualTo((byte) 99);
        assertThat(config.get("charValue")).isEqualTo('X');
        assertThat(config.get("doubleValue")).isEqualTo(9.87);
        assertThat(config.get("floatValue")).isEqualTo(1.23f);
        assertThat(config.get("intValue")).isEqualTo(777);
        assertThat(config.get("longValue")).isEqualTo(999999L);
        assertThat(config.get("shortValue")).isEqualTo((short) 555);
    }
}
