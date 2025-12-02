package eu.okaeri.configs.format.properties;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.properties.PropertiesConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge case tests for PropertiesConfigurer.
 * Tests boundary conditions and error handling.
 */
class PropertiesConfigurerEdgeCasesTest {

    static Stream<Arguments> propertiesConfigurers() {
        return Stream.of(
            Arguments.of("Properties", new PropertiesConfigurer())
        );
    }

    @ParameterizedTest(name = "{0}: Empty config")
    @MethodSource("propertiesConfigurers")
    void testWrite_EmptyConfig(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Properties output is empty or minimal
        assertThat(properties.trim()).isEmpty();
    }

    @ParameterizedTest(name = "{0}: Very large string values")
    @MethodSource("propertiesConfigurers")
    void testWrite_VeryLargeValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.setConfigurer(configurer);
        config.setLargeValue("x".repeat(10000)); // 10k characters

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Large value is written correctly
        assertThat(properties).contains("largeValue=");
        assertThat(properties.length()).isGreaterThan(10000);
    }

    @ParameterizedTest(name = "{0}: Special characters in strings")
    @MethodSource("propertiesConfigurers")
    void testWrite_SpecialCharactersInStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.setConfigurer(configurer);
        config.setQuotes("She said: \"Hello!\"");
        config.setBackslash("C:\\Users\\test\\path");
        config.setNewlines("Line 1\nLine 2\nLine 3");
        config.setTabs("Column1\tColumn2\tColumn3");

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Special characters are properly escaped
        assertThat(properties).contains("quotes=");
        assertThat(properties).contains("backslash=");
        assertThat(properties).contains("\\n"); // Newlines escaped
        assertThat(properties).contains("\\t"); // Tabs escaped
    }

    @ParameterizedTest(name = "{0}: Special characters round-trip")
    @MethodSource("propertiesConfigurers")
    void testRoundTrip_SpecialCharacters(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.setConfigurer(configurer);
        config.setQuotes("She said: \"Hello!\"");
        config.setBackslash("C:\\Users\\test\\path");
        config.setNewlines("Line 1\nLine 2\nLine 3");
        config.setTabs("Column1\tColumn2\tColumn3");

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        SpecialCharsConfig loaded = ConfigManager.create(SpecialCharsConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Special characters are preserved
        assertThat(loaded.getQuotes()).isEqualTo("She said: \"Hello!\"");
        assertThat(loaded.getBackslash()).isEqualTo("C:\\Users\\test\\path");
        assertThat(loaded.getNewlines()).isEqualTo("Line 1\nLine 2\nLine 3");
        assertThat(loaded.getTabs()).isEqualTo("Column1\tColumn2\tColumn3");
    }

    @ParameterizedTest(name = "{0}: Very deeply nested structure")
    @MethodSource("propertiesConfigurers")
    void testWrite_VeryDeeplyNestedStructure(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with deep nesting via properties
        String properties = """
            level1.level2.level3.level4.level5.value=deep
            """;

        DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
        config.setConfigurer(configurer);
        config.load(properties);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String resultProperties = output.toString();

        // Then: Nesting structure is maintained via dot notation
        assertThat(resultProperties).contains("level1.level2.level3.level4.level5");
    }

    @ParameterizedTest(name = "{0}: Very large collection")
    @MethodSource("propertiesConfigurers")
    void testWrite_VeryLargeCollection(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with very large list
        LargeCollectionConfig config = ConfigManager.create(LargeCollectionConfig.class);
        config.setConfigurer(configurer);
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add("item-" + i);
        }
        config.setLargeList(largeList);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: All items are written with index notation
        assertThat(properties).contains("largeList.0=item-0");
        assertThat(properties).contains("largeList.999=item-999");
    }

    @ParameterizedTest(name = "{0}: Null values")
    @MethodSource("propertiesConfigurers")
    void testWrite_NullValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null values
        NullValueConfig config = ConfigManager.create(NullValueConfig.class);
        config.setConfigurer(configurer);
        config.setNullString(null);
        config.setNullList(null);
        config.setNullMap(null);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Null values use __null__ marker
        assertThat(properties).contains("nullString=__null__");
        assertThat(properties).contains("nullList=__null__");
        assertThat(properties).contains("nullMap=__null__");
    }

    @ParameterizedTest(name = "{0}: Null values round-trip")
    @MethodSource("propertiesConfigurers")
    void testRoundTrip_NullValues(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with null values
        NullValueConfig config = ConfigManager.create(NullValueConfig.class);
        config.setConfigurer(configurer);
        config.setNullString(null);

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        NullValueConfig loaded = ConfigManager.create(NullValueConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Null is preserved
        assertThat(loaded.getNullString()).isNull();
    }

    @ParameterizedTest(name = "{0}: Round-trip empty strings")
    @MethodSource("propertiesConfigurers")
    void testRoundTrip_EmptyStrings(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with empty strings
        EmptyStringConfig config = ConfigManager.create(EmptyStringConfig.class);
        config.setConfigurer(configurer);
        config.setEmptyString("");
        config.setWhitespaceString("   ");

        // When: Save and load again
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        EmptyStringConfig loaded = ConfigManager.create(EmptyStringConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Empty and whitespace strings are preserved (leading spaces escaped as "\ ")
        assertThat(loaded.getEmptyString()).isEqualTo("");
        assertThat(loaded.getWhitespaceString()).isEqualTo("   ");
    }

    @ParameterizedTest(name = "{0}: Keys with special characters")
    @MethodSource("propertiesConfigurers")
    void testWrite_KeysWithSpecialCharacters(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with map containing keys that need escaping
        SpecialKeyConfig config = ConfigManager.create(SpecialKeyConfig.class);
        config.setConfigurer(configurer);
        config.setSettings(Map.of(
            "normal", "value1",
            "with space", "value2",
            "with=equals", "value3"
        ));

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Keys are properly escaped
        assertThat(properties).contains("settings.normal=value1");
        assertThat(properties).contains("settings.with\\ space=value2");
        assertThat(properties).contains("settings.with\\=equals=value3");
    }

    @ParameterizedTest(name = "{0}: Unicode round-trip")
    @MethodSource("propertiesConfigurers")
    void testRoundTrip_Unicode(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with unicode strings
        UnicodeConfig config = ConfigManager.create(UnicodeConfig.class);
        config.setConfigurer(configurer);
        config.setJapanese("こんにちは");
        config.setEmoji("🎉🌍");

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        UnicodeConfig loaded = ConfigManager.create(UnicodeConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Unicode is preserved through escaping
        assertThat(loaded.getJapanese()).isEqualTo("こんにちは");
        assertThat(loaded.getEmoji()).isEqualTo("🎉🌍");
    }

    // === Simple comma-separated list tests ===

    @ParameterizedTest(name = "{0}: Simple list uses comma format")
    @MethodSource("propertiesConfigurers")
    void testWrite_SimpleShortList_UsesCommaFormat(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with short simple list
        SimpleListConfig config = ConfigManager.create(SimpleListConfig.class);
        config.setConfigurer(configurer);
        config.setTags(List.of("java", "config", "yaml"));

        // When: Write to string
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Uses comma-separated format
        assertThat(properties).contains("tags=java,config,yaml");
        assertThat(properties).doesNotContain("tags.0");
    }

    @ParameterizedTest(name = "{0}: Long list uses index format")
    @MethodSource("propertiesConfigurers")
    void testWrite_LongList_UsesIndexFormat(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with list that exceeds 80 char threshold (81 chars total)
        SimpleListConfig config = ConfigManager.create(SimpleListConfig.class);
        config.setConfigurer(configurer);
        config.setTags(List.of("very-long-tag-one", "very-long-tag-two", "very-long-tag-three", "very-long-tag-four-x"));

        // When: Write to string
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Uses index format (exceeds 80 chars)
        assertThat(properties).contains("tags.0=very-long-tag-one");
        assertThat(properties).contains("tags.1=very-long-tag-two");
    }

    @ParameterizedTest(name = "{0}: List with comma in value uses index format")
    @MethodSource("propertiesConfigurers")
    void testWrite_ListWithCommaInValue_UsesIndexFormat(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with list containing comma in value
        SimpleListConfig config = ConfigManager.create(SimpleListConfig.class);
        config.setConfigurer(configurer);
        config.setTags(List.of("Doe, John", "Smith, Jane"));

        // When: Write to string
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Uses index format to preserve commas
        assertThat(properties).contains("tags.0=Doe, John");
        assertThat(properties).contains("tags.1=Smith, Jane");
    }

    @ParameterizedTest(name = "{0}: List with newline in value uses index format")
    @MethodSource("propertiesConfigurers")
    void testWrite_ListWithNewlineInValue_UsesIndexFormat(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with list containing newline
        SimpleListConfig config = ConfigManager.create(SimpleListConfig.class);
        config.setConfigurer(configurer);
        config.setTags(List.of("line1\nline2", "normal"));

        // When: Write to string
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Uses index format
        assertThat(properties).contains("tags.0=");
        assertThat(properties).contains("tags.1=normal");
    }

    @ParameterizedTest(name = "{0}: Simple list round-trip")
    @MethodSource("propertiesConfigurers")
    void testRoundTrip_SimpleList(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with simple list
        SimpleListConfig config = ConfigManager.create(SimpleListConfig.class);
        config.setConfigurer(configurer);
        config.setTags(List.of("alpha", "beta", "gamma"));

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        SimpleListConfig loaded = ConfigManager.create(SimpleListConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: List is preserved
        assertThat(loaded.getTags()).containsExactly("alpha", "beta", "gamma");
    }

    @ParameterizedTest(name = "{0}: Single element list")
    @MethodSource("propertiesConfigurers")
    void testRoundTrip_SingleElementList(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with single element list
        SimpleListConfig config = ConfigManager.create(SimpleListConfig.class);
        config.setConfigurer(configurer);
        config.setTags(List.of("only-one"));

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        SimpleListConfig loaded = ConfigManager.create(SimpleListConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Single element preserved (no comma in output)
        assertThat(properties).contains("tags=only-one");
        assertThat(properties).doesNotContain(",");
        assertThat(loaded.getTags()).containsExactly("only-one");
    }

    @ParameterizedTest(name = "{0}: Number list uses comma format")
    @MethodSource("propertiesConfigurers")
    void testWrite_NumberList_UsesCommaFormat(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with number list
        NumberListConfig config = ConfigManager.create(NumberListConfig.class);
        config.setConfigurer(configurer);
        config.setScores(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        // When: Write to string
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Uses comma format (numbers are short)
        assertThat(properties).contains("scores=1,2,3,4,5,6,7,8,9,10");
    }

    @ParameterizedTest(name = "{0}: Long comma list converts to index on save")
    @MethodSource("propertiesConfigurers")
    void testRoundTrip_LongCommaList_ConvertsToIndex(String configurerName, Configurer configurer) throws Exception {
        // Given: Properties file with long comma-separated list (exceeds 80 chars)
        String input = "tags=very-long-tag-one,very-long-tag-two,very-long-tag-three,very-long-tag-four-x\n";

        SimpleListConfig config = ConfigManager.create(SimpleListConfig.class);
        config.setConfigurer(configurer);
        config.load(new ByteArrayInputStream(input.getBytes()));

        // When: Save
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Uses index format (exceeds 80 chars on save)
        assertThat(properties).contains("tags.0=very-long-tag-one");
        assertThat(properties).contains("tags.1=very-long-tag-two");
        assertThat(config.getTags()).containsExactly("very-long-tag-one", "very-long-tag-two", "very-long-tag-three", "very-long-tag-four-x");
    }

    @ParameterizedTest(name = "{0}: List with nulls uses comma format")
    @MethodSource("propertiesConfigurers")
    void testWrite_ListWithNulls_UsesCommaFormat(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with list containing nulls
        NullListConfig config = ConfigManager.create(NullListConfig.class);
        config.setConfigurer(configurer);
        List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add("first");
        listWithNulls.add(null);
        listWithNulls.add("third");
        config.setItems(listWithNulls);

        // When: Write to string
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String properties = output.toString();

        // Then: Uses comma format with __null__ marker
        assertThat(properties).contains("items=first,__null__,third");
    }

    @ParameterizedTest(name = "{0}: Empty comment creates blank line")
    @MethodSource("propertiesConfigurers")
    void testWrite_EmptyCommentCreatesBlankLine(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with empty comment annotation
        EmptyCommentConfig config = ConfigManager.create(EmptyCommentConfig.class);
        config.setConfigurer(configurer);

        // When: Write to string
        String properties = config.saveToString();

        // Then: @Comment(" ") creates "#", @Comment("") creates empty line (no #)
        assertThat(properties).isEqualTo("""
                #
                # Field after space comment
                afterSpaceComment=value1

                # Field after empty line
                afterEmptyLine=value2
                """);
    }

    @ParameterizedTest(name = "{0}: List with nulls round-trip")
    @MethodSource("propertiesConfigurers")
    void testRoundTrip_ListWithNulls(String configurerName, Configurer configurer) throws Exception {
        // Given: Config with list containing nulls
        NullListConfig config = ConfigManager.create(NullListConfig.class);
        config.setConfigurer(configurer);
        List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add("first");
        listWithNulls.add(null);
        listWithNulls.add("third");
        config.setItems(listWithNulls);

        // When: Save and load
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);

        NullListConfig loaded = ConfigManager.create(NullListConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Nulls are preserved
        assertThat(loaded.getItems()).containsExactly("first", null, "third");
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyConfig extends OkaeriConfig {
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeValueConfig extends OkaeriConfig {
        private String largeValue;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SpecialCharsConfig extends OkaeriConfig {
        private String quotes;
        private String backslash;
        private String newlines;
        private String tabs;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeepNestedConfig extends OkaeriConfig {
        private Map<String, Object> level1;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeCollectionConfig extends OkaeriConfig {
        private List<String> largeList;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullValueConfig extends OkaeriConfig {
        private String nullString = "default";
        private List<String> nullList = new ArrayList<>();
        private Map<String, String> nullMap;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyStringConfig extends OkaeriConfig {
        private String emptyString;
        private String whitespaceString;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SpecialKeyConfig extends OkaeriConfig {
        private Map<String, String> settings;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UnicodeConfig extends OkaeriConfig {
        private String japanese;
        private String emoji;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleListConfig extends OkaeriConfig {
        private List<String> tags;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NumberListConfig extends OkaeriConfig {
        private List<Integer> scores;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullListConfig extends OkaeriConfig {
        private List<String> items;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyCommentConfig extends OkaeriConfig {
        @Comment(" ")
        @Comment("Field after space comment")
        private String afterSpaceComment = "value1";

        @Comment("")
        @Comment("Field after empty line")
        private String afterEmptyLine = "value2";
    }
}
