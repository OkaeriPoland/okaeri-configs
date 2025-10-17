package eu.okaeri.configs.types;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.test.TestUtils;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for basic non-primitive types: String, BigInteger, BigDecimal, Object.
 * <p>
 * Scenarios tested:
 * - String: empty, null, unicode, special characters
 * - BigInteger: very large numbers
 * - BigDecimal: precise decimals
 * - Object type: dynamic typing
 */
class BasicTypesTest {

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class BasicTypesConfig extends OkaeriConfig {
        private String normalString = "default";
        private String emptyString = "";
        private String unicodeString = "Hello 世界 🌍";
        private BigInteger bigInt = new BigInteger("123456789012345678901234567890");
        private BigDecimal bigDec = new BigDecimal("123.456789012345678901234567890");
        private Object dynamicObject = "initial value";
    }

    @Test
    void testString_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("string-test.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setNormalString("Test String Value");

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getNormalString()).isEqualTo("Test String Value");
    }

    @Test
    void testString_EmptyString_PreservedCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("empty-string.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setNormalString("");
        config.setEmptyString("");

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getNormalString()).isEmpty();
        assertThat(loaded.getEmptyString()).isEmpty();
    }

    @Test
    void testString_NullValue_PreservedCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("null-string.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setNormalString(null);

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getNormalString()).isNull();
    }

    @Test
    void testString_UnicodeCharacters_PreservedCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("unicode.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        // Various unicode strings
        config.setNormalString("こんにちは世界 🌍");
        config.setEmptyString("Привет мир!");
        config.setUnicodeString("Część świecie! Łódź, Gdańsk, źdźbło");

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getNormalString()).isEqualTo("こんにちは世界 🌍");
        assertThat(loaded.getEmptyString()).isEqualTo("Привет мир!");
        assertThat(loaded.getUnicodeString()).isEqualTo("Część świecie! Łódź, Gdańsk, źdźbło");
    }

    @Test
    void testString_SpecialCharacters_PreservedCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("special-chars.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setNormalString("!@#$%^&*()_+-=[]{}|;':\"<>?,./");
        config.setEmptyString("Line1\nLine2\tTabbed");
        config.setUnicodeString("Path: C:\\Users\\Test\\file.txt");

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getNormalString()).isEqualTo("!@#$%^&*()_+-=[]{}|;':\"<>?,./");
        assertThat(loaded.getEmptyString()).isEqualTo("Line1\nLine2\tTabbed");
        assertThat(loaded.getUnicodeString()).isEqualTo("Path: C:\\Users\\Test\\file.txt");
    }

    @Test
    void testBigInteger_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("bigint.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setBigInt(new BigInteger("999999999999999999999999999999999999999999"));

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getBigInt()).isEqualTo(new BigInteger("999999999999999999999999999999999999999999"));
    }

    @Test
    void testBigInteger_VeryLargeNumber_HandledCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("bigint-large.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        // Number larger than Long.MAX_VALUE
        BigInteger veryLarge = new BigInteger("123456789012345678901234567890123456789012345678901234567890");
        config.setBigInt(veryLarge);

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getBigInt()).isEqualTo(veryLarge);
    }

    @Test
    void testBigDecimal_SaveAndLoad_MaintainsValue() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("bigdec.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setBigDec(new BigDecimal("123.456789012345678901234567890"));

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getBigDec()).isEqualByComparingTo(new BigDecimal("123.456789012345678901234567890"));
    }

    @Test
    void testBigDecimal_PreciseDecimal_NoRoundingErrors() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("bigdec-precise.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);

        // Value that would lose precision with double
        BigDecimal precise = new BigDecimal("0.1234567890123456789012345678901234567890");
        config.setBigDec(precise);

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getBigDec()).isEqualByComparingTo(precise);
    }

    @Test
    void testObject_String_DynamicTyping() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("object-string.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setDynamicObject("String value");

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getDynamicObject()).isInstanceOf(String.class);
        assertThat(loaded.getDynamicObject()).isEqualTo("String value");
    }

    @Test
    void testObject_Integer_DynamicTyping() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("object-int.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setDynamicObject(12345);

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getDynamicObject()).isInstanceOf(Integer.class);
        assertThat(loaded.getDynamicObject()).isEqualTo(12345);
    }

    @Test
    void testObject_Boolean_DynamicTyping() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("object-bool.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setDynamicObject(true);

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getDynamicObject()).isInstanceOf(Boolean.class);
        assertThat(loaded.getDynamicObject()).isEqualTo(true);
    }

    @Test
    void testObject_Null_HandledCorrectly() throws Exception {
        // Arrange
        Path tempDir = TestUtils.createTempTestDir();
        Path tempFile = tempDir.resolve("object-null.yml");

        BasicTypesConfig config = ConfigManager.create(BasicTypesConfig.class);
        config.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        config.setDynamicObject(null);

        // Act
        config.save();
        BasicTypesConfig loaded = ConfigManager.create(BasicTypesConfig.class);
        loaded.withConfigurer(new YamlSnakeYamlConfigurer()).withBindFile(tempFile);
        loaded.load();

        // Assert
        assertThat(loaded.getDynamicObject()).isNull();
    }
}
