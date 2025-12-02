package eu.okaeri.configs.error.xml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriConfigException;
import eu.okaeri.configs.xml.XmlSimpleConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests error messages for XML configuration loading failures.
 */
class XmlErrorMessageTest {

    static Stream<Arguments> xmlConfigurers() {
        return Stream.of(
            Arguments.of("XmlSimple", new XmlSimpleConfigurer())
        );
    }

    // ==================== Standard Types ====================

    @ParameterizedTest(name = "{0}: Invalid Integer")
    @MethodSource("xmlConfigurers")
    void testError_InvalidInteger(String name, Configurer configurer) {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
              <value>not_a_number</value>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 3:10
                      |
                    3 |   <value>not_a_number</value>
                      |          ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Nested Paths ====================

    @ParameterizedTest(name = "{0}: Invalid nested field")
    @MethodSource("xmlConfigurers")
    void testError_NestedField(String name, Configurer configurer) {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
              <database>
                <host>localhost</host>
                <port>not_a_port</port>
              </database>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 5:11
                      |
                    5 |     <port>not_a_port</port>
                      |           ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== List Paths ====================

    @ParameterizedTest(name = "{0}: Invalid list element")
    @MethodSource("xmlConfigurers")
    void testError_ListElement(String name, Configurer configurer) {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
              <numbers>
                <item>1</item>
                <item>2</item>
                <item>not_a_number</item>
                <item>4</item>
              </numbers>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(ListConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("numbers[2]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'numbers[2]' to Integer from String
                     --> 6:11
                      |
                    6 |     <item>not_a_number</item>
                      |           ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Map Paths ====================

    @ParameterizedTest(name = "{0}: Invalid map value")
    @MethodSource("xmlConfigurers")
    void testError_MapValue(String name, Configurer configurer) {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
              <limits>
                <daily>100</daily>
                <weekly>not_a_number</weekly>
              </limits>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(MapConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("limits[\"weekly\"]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'limits["weekly"]' to Integer from String
                     --> 5:13
                      |
                    5 |     <weekly>not_a_number</weekly>
                      |             ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Enum Errors ====================

    @ParameterizedTest(name = "{0}: Invalid enum value")
    @MethodSource("xmlConfigurers")
    void testError_InvalidEnum(String name, Configurer configurer) {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
              <level>MDIUM</level>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(EnumConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("level");
                assertThat(e.getMessage()).isEqualTo("""
                    Cannot resolve 'level' to Level from String
                     --> 3:10
                      |
                    3 |   <level>MDIUM</level>
                      |          ^^^^^ Expected MEDIUM, HIGH or LOW""");
            });
    }

    // ==================== Inline/Minified XML ====================

    @ParameterizedTest(name = "{0}: Inline XML - Invalid Integer")
    @MethodSource("xmlConfigurers")
    void testError_InlineXml_InvalidInteger(String name, Configurer configurer) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><config><value>not_a_number</value></config>";

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 1:48
                      |
                    1 | <?xml version="1.0" encoding="UTF-8"?><config><value>not_a_number</value></config>
                      |                                                      ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Inline XML - Nested field")
    @MethodSource("xmlConfigurers")
    void testError_InlineXml_NestedField(String name, Configurer configurer) {
        String xml = "<config><database><host>localhost</host><port>invalid</port></database></config>";

        assertThatThrownBy(() -> this.loadConfig(NestedConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("database.port");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'database.port' to Integer from String
                     --> 1:47
                      |
                    1 | <config><database><host>localhost</host><port>invalid</port></database></config>
                      |                                               ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    @ParameterizedTest(name = "{0}: Inline XML - List element")
    @MethodSource("xmlConfigurers")
    void testError_InlineXml_ListElement(String name, Configurer configurer) {
        String xml = "<config><numbers><item>1</item><item>bad</item><item>3</item></numbers></config>";

        assertThatThrownBy(() -> this.loadConfig(ListConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("numbers[1]");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'numbers[1]' to Integer from String
                     --> 1:38
                      |
                    1 | <config><numbers><item>1</item><item>bad</item><item>3</item></numbers></config>
                      |                                      ^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Helper Methods ====================

    private <T extends OkaeriConfig> T loadConfig(Class<T> clazz, Configurer configurer, String xml) {
        T config = ConfigManager.create(clazz);
        config.setConfigurer(configurer);
        config.load(xml);
        return config;
    }

    // ==================== Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class IntegerConfig extends OkaeriConfig {
        private int value;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
        private DatabaseConfig database = new DatabaseConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DatabaseConfig extends OkaeriConfig {
        private String host = "localhost";
        private int port = 5432;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ListConfig extends OkaeriConfig {
        private List<Integer> numbers;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MapConfig extends OkaeriConfig {
        private Map<String, Integer> limits;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EnumConfig extends OkaeriConfig {
        private Level level = Level.MEDIUM;
    }

    public enum Level {
        LOW, MEDIUM, HIGH
    }
}
