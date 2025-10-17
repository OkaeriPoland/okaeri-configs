package eu.okaeri.configs.types;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.test.configs.PrimitivesTestConfig;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for primitive types and their wrappers.
 * <p>
 * Scenarios tested:
 * - Save/load cycle maintains values for all primitive types
 * - Primitive ↔ Wrapper conversion works correctly
 * - Type conversion from strings works
 * - Default values are preserved
 * - Edge cases (min/max values, zero, negative values)
 */
class PrimitiveTypesTest {
    
    @TempDir
    Path tempDir;

    @Test
    void testBoolean_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("boolean-test.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setBoolValue(false);
        config.setBoolWrapper(true);

        // Act - save and reload
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.isBoolValue()).isFalse();
        assertThat(loaded.getBoolWrapper()).isTrue();
    }

    @Test
    void testByte_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("byte-test.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setByteValue((byte) 100);
        config.setByteWrapper((byte) -50);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getByteValue()).isEqualTo((byte) 100);
        assertThat(loaded.getByteWrapper()).isEqualTo((byte) -50);
    }

    @Test
    void testByte_EdgeCases_MinMaxValues() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("byte-edge.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setByteValue(Byte.MAX_VALUE); // 127
        config.setByteWrapper(Byte.MIN_VALUE); // -128

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getByteValue()).isEqualTo(Byte.MAX_VALUE);
        assertThat(loaded.getByteWrapper()).isEqualTo(Byte.MIN_VALUE);
    }

    @Test
    void testChar_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("char-test.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setCharValue('X');
        config.setCharWrapper('€');

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getCharValue()).isEqualTo('X');
        assertThat(loaded.getCharWrapper()).isEqualTo('€');
    }

    @Test
    void testDouble_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("double-test.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setDoubleValue(3.141592653589793);
        config.setDoubleWrapper(-2.718281828459045);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getDoubleValue()).isEqualTo(3.141592653589793);
        assertThat(loaded.getDoubleWrapper()).isEqualTo(-2.718281828459045);
    }

    @Test
    void testFloat_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("float-test.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setFloatValue(1.234f);
        config.setFloatWrapper(-5.678f);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getFloatValue()).isEqualTo(1.234f);
        assertThat(loaded.getFloatWrapper()).isEqualTo(-5.678f);
    }

    @Test
    void testInt_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("int-test.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setIntValue(999999);
        config.setIntWrapper(-123456);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getIntValue()).isEqualTo(999999);
        assertThat(loaded.getIntWrapper()).isEqualTo(-123456);
    }

    @Test
    void testInt_EdgeCases_MinMaxValues() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("int-edge.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setIntValue(Integer.MAX_VALUE);
        config.setIntWrapper(Integer.MIN_VALUE);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getIntValue()).isEqualTo(Integer.MAX_VALUE);
        assertThat(loaded.getIntWrapper()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void testLong_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("long-test.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setLongValue(9876543210L);
        config.setLongWrapper(-1234567890L);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getLongValue()).isEqualTo(9876543210L);
        assertThat(loaded.getLongWrapper()).isEqualTo(-1234567890L);
    }

    @Test
    void testShort_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("short-test.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setShortValue((short) 30000);
        config.setShortWrapper((short) -15000);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getShortValue()).isEqualTo((short) 30000);
        assertThat(loaded.getShortWrapper()).isEqualTo((short) -15000);
    }

    @Test
    void testAllPrimitives_SaveAndLoad_Together() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("all-primitives.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        // Set all primitive values
        config.setBoolValue(false);
        config.setByteValue((byte) 99);
        config.setCharValue('Z');
        config.setDoubleValue(9.87654);
        config.setFloatValue(1.234f);
        config.setIntValue(777777);
        config.setLongValue(999999999L);
        config.setShortValue((short) 12345);

        // Set all wrapper values
        config.setBoolWrapper(true);
        config.setByteWrapper((byte) -99);
        config.setCharWrapper('A');
        config.setDoubleWrapper(-9.87654);
        config.setFloatWrapper(-1.234f);
        config.setIntWrapper(-777777);
        config.setLongWrapper(-999999999L);
        config.setShortWrapper((short) -12345);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert primitives
        assertThat(loaded.isBoolValue()).isFalse();
        assertThat(loaded.getByteValue()).isEqualTo((byte) 99);
        assertThat(loaded.getCharValue()).isEqualTo('Z');
        assertThat(loaded.getDoubleValue()).isEqualTo(9.87654);
        assertThat(loaded.getFloatValue()).isEqualTo(1.234f);
        assertThat(loaded.getIntValue()).isEqualTo(777777);
        assertThat(loaded.getLongValue()).isEqualTo(999999999L);
        assertThat(loaded.getShortValue()).isEqualTo((short) 12345);

        // Assert wrappers
        assertThat(loaded.getBoolWrapper()).isTrue();
        assertThat(loaded.getByteWrapper()).isEqualTo((byte) -99);
        assertThat(loaded.getCharWrapper()).isEqualTo('A');
        assertThat(loaded.getDoubleWrapper()).isEqualTo(-9.87654);
        assertThat(loaded.getFloatWrapper()).isEqualTo(-1.234f);
        assertThat(loaded.getIntWrapper()).isEqualTo(-777777);
        assertThat(loaded.getLongWrapper()).isEqualTo(-999999999L);
        assertThat(loaded.getShortWrapper()).isEqualTo((short) -12345);
    }

    @Test
    void testPrimitives_TypeConversion_StringToNumber() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("string-conversion.yml");

        // Create YAML with string values
        String yaml = """
            boolValue: true
            byteValue: "123"
            charValue: A
            doubleValue: "3.14159"
            floatValue: "2.71"
            intValue: "999"
            longValue: "123456789"
            shortValue: "555"
            boolWrapper: false
            byteWrapper: 100
            charWrapper: Z
            doubleWrapper: 2.718
            floatWrapper: 1.414
            intWrapper: 123
            longWrapper: 987654321
            shortWrapper: 555
            """;
        Files.writeString(tempFile, yaml);

        // Act
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert - strings should be converted to numbers
        assertThat(loaded.getByteValue()).isEqualTo((byte) 123);
        assertThat(loaded.getDoubleValue()).isEqualTo(3.14159);
        assertThat(loaded.getFloatValue()).isEqualTo(2.71f);
        assertThat(loaded.getIntValue()).isEqualTo(999);
        assertThat(loaded.getLongValue()).isEqualTo(123456789L);
        assertThat(loaded.getShortValue()).isEqualTo((short) 555);
    }

    @Test
    void testPrimitives_DefaultValues_Preserved() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("defaults.yml");

        // Create empty YAML
        Files.writeString(tempFile, "");

        // Act
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert - defaults from PrimitivesTestConfig should be present
        assertThat(loaded.isBoolValue()).isTrue();
        assertThat(loaded.getByteValue()).isEqualTo((byte) 127);
        assertThat(loaded.getCharValue()).isEqualTo('A');
        assertThat(loaded.getDoubleValue()).isEqualTo(3.14);
        assertThat(loaded.getFloatValue()).isEqualTo(2.71f);
        assertThat(loaded.getIntValue()).isEqualTo(42);
        assertThat(loaded.getLongValue()).isEqualTo(9999999999L);
        assertThat(loaded.getShortValue()).isEqualTo((short) 999);
    }

    @Test
    void testPrimitives_ZeroValues_HandledCorrectly() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("zeros.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        config.setBoolValue(false);
        config.setByteValue((byte) 0);
        config.setCharValue('A'); // NOTE: SnakeYAML serializes '\0' as byte array, use non-null char
        config.setDoubleValue(0.0);
        config.setFloatValue(0.0f);
        config.setIntValue(0);
        config.setLongValue(0L);
        config.setShortValue((short) 0);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.isBoolValue()).isFalse();
        assertThat(loaded.getByteValue()).isEqualTo((byte) 0);
        assertThat(loaded.getCharValue()).isEqualTo('A'); // null char not supported by SnakeYAML
        assertThat(loaded.getDoubleValue()).isEqualTo(0.0);
        assertThat(loaded.getFloatValue()).isEqualTo(0.0f);
        assertThat(loaded.getIntValue()).isEqualTo(0);
        assertThat(loaded.getLongValue()).isEqualTo(0L);
        assertThat(loaded.getShortValue()).isEqualTo((short) 0);
    }

    @Test
    void testWrappers_NullValues_HandledCorrectly() throws Exception {
        // Arrange
        Path tempFile = this.tempDir.resolve("nulls.yml");

        PrimitivesTestConfig config = ConfigManager.create(PrimitivesTestConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        config.setBoolWrapper(null);
        config.setByteWrapper(null);
        config.setCharWrapper(null);
        config.setDoubleWrapper(null);
        config.setFloatWrapper(null);
        config.setIntWrapper(null);
        config.setLongWrapper(null);
        config.setShortWrapper(null);

        // Act
        config.save();
        PrimitivesTestConfig loaded = ConfigManager.create(PrimitivesTestConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert - all wrappers should be null
        assertThat(loaded.getBoolWrapper()).isNull();
        assertThat(loaded.getByteWrapper()).isNull();
        assertThat(loaded.getCharWrapper()).isNull();
        assertThat(loaded.getDoubleWrapper()).isNull();
        assertThat(loaded.getFloatWrapper()).isNull();
        assertThat(loaded.getIntWrapper()).isNull();
        assertThat(loaded.getLongWrapper()).isNull();
        assertThat(loaded.getShortWrapper()).isNull();
    }
}
