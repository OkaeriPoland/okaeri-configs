package eu.okaeri.configs.error.xml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriConfigException;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.xml.XmlSimpleConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
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
                     --> 2:10
                      |
                    2 |   <value>not_a_number</value>
                      |          ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Nested Paths ====================

    @ParameterizedTest(name = "{0}: Invalid nested field")
    @MethodSource("xmlConfigurers")
    void testError_NestedField(String name, Configurer configurer) {
        String xml = """
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
                     --> 4:11
                      |
                    4 |     <port>not_a_port</port>
                      |           ^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== List Paths ====================

    @ParameterizedTest(name = "{0}: Invalid list element")
    @MethodSource("xmlConfigurers")
    void testError_ListElement(String name, Configurer configurer) {
        String xml = """
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
                     --> 5:11
                      |
                    5 |     <item>not_a_number</item>
                      |           ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Map Paths ====================

    @ParameterizedTest(name = "{0}: Invalid map value")
    @MethodSource("xmlConfigurers")
    void testError_MapValue(String name, Configurer configurer) {
        String xml = """
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
                     --> 4:13
                      |
                    4 |     <weekly>not_a_number</weekly>
                      |             ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Enum Errors ====================

    @ParameterizedTest(name = "{0}: Invalid enum value")
    @MethodSource("xmlConfigurers")
    void testError_InvalidEnum(String name, Configurer configurer) {
        String xml = """
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
                     --> 2:10
                      |
                    2 |   <level>MDIUM</level>
                      |          ^^^^^ Expected MEDIUM, HIGH or LOW""");
            });
    }

    // ==================== Inline/Minified XML ====================

    @ParameterizedTest(name = "{0}: Inline XML - Invalid Integer")
    @MethodSource("xmlConfigurers")
    void testError_InlineXml_InvalidInteger(String name, Configurer configurer) {
        String xml = "<config><value>not_a_number</value></config>";

        assertThatThrownBy(() -> this.loadConfig(IntegerConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 1:16
                      |
                    1 | <config><value>not_a_number</value></config>
                      |                ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
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

    // ==================== Sibling Subconfig Accumulation Test ====================

    /**
     * Tests that error paths don't accumulate across sibling subconfigs.
     * This was a bug where processing subconfig A would pollute the parent's basePath,
     * causing subconfig B's error to show path "A.B.field" instead of just "B.field".
     */
    @ParameterizedTest(name = "{0}: Sibling subconfigs don't accumulate paths")
    @MethodSource("xmlConfigurers")
    void testError_SiblingSubconfigsDoNotAccumulatePaths(String name, Configurer configurer) {
        // Config with 3 sibling subconfigs, error in the last one
        String xml = """
            <config>
              <sectionA>
                <valueA>100</valueA>
              </sectionA>
              <sectionB>
                <valueB>200</valueB>
              </sectionB>
              <sectionC>
                <valueC>not_a_number</valueC>
              </sectionC>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(SiblingSubconfigsConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                // Path should be just "sectionC.valueC", NOT "sectionA.sectionB.sectionC.valueC"
                assertThat(e.getPath().toString()).isEqualTo("sectionC.valueC");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'sectionC.valueC' to Integer from String
                     --> 9:13
                      |
                    9 |     <valueC>not_a_number</valueC>
                      |             ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== @CustomKey Annotation Tests ====================

    /**
     * Tests that error messages work correctly with @CustomKey annotation.
     * The XML uses the custom key name (e.g., "custom-value"), and the error
     * message should correctly locate and display the source line.
     */
    @ParameterizedTest(name = "{0}: @CustomKey simple field")
    @MethodSource("xmlConfigurers")
    void testError_CustomKey_SimpleField(String name, Configurer configurer) {
        String xml = """
            <config>
              <custom-value>not_a_number</custom-value>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(CustomKeyConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-value' to Integer from String
                     --> 2:17
                      |
                    2 |   <custom-value>not_a_number</custom-value>
                      |                 ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that error messages work correctly with nested @CustomKey annotations.
     * Both the parent field and nested field use custom key names.
     */
    @ParameterizedTest(name = "{0}: @CustomKey nested field")
    @MethodSource("xmlConfigurers")
    void testError_CustomKey_NestedField(String name, Configurer configurer) {
        String xml = """
            <config>
              <some>
                <custom-key>
                  <nested-value>not_a_number</nested-value>
                </custom-key>
              </some>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(NestedCustomKeyConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("some.custom-key.nested-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'some.custom-key.nested-value' to Integer from String
                     --> 4:21
                      |
                    4 |       <nested-value>not_a_number</nested-value>
                      |                     ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that error messages work correctly when only the parent has @CustomKey.
     */
    @ParameterizedTest(name = "{0}: @CustomKey on parent only")
    @MethodSource("xmlConfigurers")
    void testError_CustomKey_ParentOnly(String name, Configurer configurer) {
        String xml = """
            <config>
              <custom-parent>
                <value>not_a_number</value>
              </custom-parent>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(CustomKeyParentConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("custom-parent.value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'custom-parent.value' to Integer from String
                     --> 3:12
                      |
                    3 |     <value>not_a_number</value>
                      |            ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== @Names Annotation Tests (HYPHEN_CASE) ====================

    /**
     * Tests that error messages work correctly with @Names(strategy = HYPHEN_CASE).
     * This tests configs using @Names strategy where field names are transformed
     * to hyphen-case (e.g., updateRate -> update-rate).
     */
    @ParameterizedTest(name = "{0}: @Names HYPHEN_CASE nested field (scoreboard.dummy.update-rate pattern)")
    @MethodSource("xmlConfigurers")
    void testError_Names_HyphenCase_NestedField(String name, Configurer configurer) {
        String xml = """
            <config>
              <scoreboard>
                <dummy>
                  <update-rate>hello</update-rate>
                </dummy>
              </scoreboard>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfigWithCommons(ScoreboardConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("scoreboard.dummy.update-rate");
                assertThat(e.getMessage()).isEqualTo("""
                    error[DurationTransformer]: Cannot transform 'scoreboard.dummy.update-rate' to Duration from String
                     --> 4:20
                      |
                    4 |       <update-rate>hello</update-rate>
                      |                    ^^^^^ Text cannot be parsed to a Duration""");
            });
    }

    /**
     * Tests @Names HYPHEN_CASE with a simple nested field.
     */
    @ParameterizedTest(name = "{0}: @Names HYPHEN_CASE simple nested")
    @MethodSource("xmlConfigurers")
    void testError_Names_HyphenCase_SimpleNested(String name, Configurer configurer) {
        String xml = """
            <config>
              <my-section>
                <my-value>not_a_number</my-value>
              </my-section>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfig(HyphenCaseConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("my-section.my-value");
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'my-section.my-value' to Integer from String
                     --> 3:15
                      |
                    3 |     <my-value>not_a_number</my-value>
                      |               ^^^^^^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== errorComments Option Tests ====================

    /**
     * Tests that errorComments(true) config option automatically includes
     * all consecutive comment lines above the error field.
     */
    @ParameterizedTest(name = "{0}: errorComments includes consecutive comments above field")
    @MethodSource("xmlConfigurers")
    void testError_ErrorCommentsOption_IncludesConsecutiveComments(String name, Configurer configurer) {
        String xml = """
            <config>
              <name>test</name>
              <!-- This is a comment about the value -->
              <!-- Value must be a valid integer -->
              <value>invalid</value>
              <debug>false</debug>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfigWithErrorComments(IntegerConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // errorComments(true) should include both comment lines above the field
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 5:10
                      |
                    3 |   <!-- This is a comment about the value -->
                    4 |   <!-- Value must be a valid integer -->
                    5 |   <value>invalid</value>
                      |          ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    /**
     * Tests that errorComments stops at non-comment lines (doesn't cross blank lines).
     */
    @ParameterizedTest(name = "{0}: errorComments stops at blank lines")
    @MethodSource("xmlConfigurers")
    void testError_ErrorCommentsOption_StopsAtBlankLines(String name, Configurer configurer) {
        String xml = """
            <config>
              <name>test</name>
              <!-- This comment is separated by blank line -->

              <!-- This is the only comment directly above -->
              <value>invalid</value>
            </config>
            """;

        assertThatThrownBy(() -> this.loadConfigWithErrorComments(IntegerConfig.class, configurer, xml))
            .isInstanceOf(OkaeriConfigException.class)
            .satisfies(ex -> {
                OkaeriConfigException e = (OkaeriConfigException) ex;
                assertThat(e.getPath().toString()).isEqualTo("value");
                // Only the comment directly above (no blank line) should be included
                assertThat(e.getMessage()).isEqualTo("""
                    error[StringToIntegerTransformer]: Cannot transform 'value' to Integer from String
                     --> 6:10
                      |
                    5 |   <!-- This is the only comment directly above -->
                    6 |   <value>invalid</value>
                      |          ^^^^^^^ Expected whole number (e.g. 42, -10, 0)""");
            });
    }

    // ==================== Helper Methods ====================

    private <T extends OkaeriConfig> T loadConfig(Class<T> clazz, Configurer configurer, String xml) {
        T config = ConfigManager.create(clazz);
        config.setConfigurer(configurer);
        config.load(xml);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithErrorComments(Class<T> clazz, Configurer configurer, String xml) {
        T config = ConfigManager.create(clazz, it -> {
            it.configure(opt -> {
                opt.configurer(configurer);
                opt.errorComments(true);
            });
        });
        config.load(xml);
        return config;
    }

    private <T extends OkaeriConfig> T loadConfigWithCommons(Class<T> clazz, Configurer configurer, String xml) {
        T config = ConfigManager.create(clazz);
        configurer.register(new SerdesCommons());
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

    // Sibling subconfigs for accumulation test
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SiblingSubconfigsConfig extends OkaeriConfig {
        private SectionA sectionA = new SectionA();
        private SectionB sectionB = new SectionB();
        private SectionC sectionC = new SectionC();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionA extends OkaeriConfig {
        private int valueA = 0;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionB extends OkaeriConfig {
        private int valueB = 0;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionC extends OkaeriConfig {
        private int valueC = 0;
    }

    // ==================== @CustomKey Config Classes ====================

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CustomKeyConfig extends OkaeriConfig {
        @CustomKey("custom-value")
        private int myValue;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedCustomKeyConfig extends OkaeriConfig {
        private SomeConfig some = new SomeConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SomeConfig extends OkaeriConfig {
        @CustomKey("custom-key")
        private InnerCustomKeyConfig customKey = new InnerCustomKeyConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class InnerCustomKeyConfig extends OkaeriConfig {
        @CustomKey("nested-value")
        private int nestedValue;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CustomKeyParentConfig extends OkaeriConfig {
        @CustomKey("custom-parent")
        private ChildConfig customParent = new ChildConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ChildConfig extends OkaeriConfig {
        private int value;
    }

    // ==================== @Names HYPHEN_CASE Config Classes ====================

    /**
     * Tests deeply nested config structure with @Names(strategy = HYPHEN_CASE)
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ScoreboardConfig extends OkaeriConfig {
        private ScoreboardSection scoreboard = new ScoreboardSection();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class ScoreboardSection extends OkaeriConfig {
        private DummySection dummy = new DummySection();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class DummySection extends OkaeriConfig {
        private Duration updateRate = Duration.ofMinutes(1);
    }

    /**
     * Simple config with @Names HYPHEN_CASE for testing field name transformation.
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class HyphenCaseConfig extends OkaeriConfig {
        private HyphenCaseSection mySection = new HyphenCaseSection();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
    public static class HyphenCaseSection extends OkaeriConfig {
        private int myValue;
    }
}
