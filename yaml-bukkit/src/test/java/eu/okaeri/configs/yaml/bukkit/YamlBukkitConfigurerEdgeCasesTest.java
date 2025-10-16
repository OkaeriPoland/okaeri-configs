package eu.okaeri.configs.yaml.bukkit;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests YAML-specific edge cases and boundary conditions for Bukkit configurer.
 */
class YamlBukkitConfigurerEdgeCasesTest {

    @Test
    void testLoad_EmptyInputStream() throws Exception {
        // Given: Empty YAML content
        String yaml = "";
        YamlBukkitConfigurer configurer = new YamlBukkitConfigurer();
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        
        // When: Load from empty InputStream
        configurer.load(new ByteArrayInputStream(yaml.getBytes()), config.getDeclaration());
        
        // Then: Internal map is initialized as empty
        assertThat(configurer.getAllKeys()).isEmpty();
    }

    @Test
    void testLoad_NullYamlDocument() throws Exception {
        // Given: YAML with null document (just whitespace/comments)
        String yaml = """
            # Just comments
            # No actual content
            """;
        YamlBukkitConfigurer configurer = new YamlBukkitConfigurer();
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        
        // When: Load from InputStream with null document
        configurer.load(new ByteArrayInputStream(yaml.getBytes()), config.getDeclaration());
        
        // Then: Internal map is reset to empty
        assertThat(configurer.getAllKeys()).isEmpty();
    }

    @Test
    void testLoad_MalformedYaml_ThrowsException() {
        // Given: Malformed YAML (invalid syntax)
        String malformedYaml = """
            name: Test
            value: [unclosed bracket
            enabled: true
            """;
        YamlBukkitConfigurer configurer = new YamlBukkitConfigurer();
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        
        // When/Then: Loading malformed YAML throws exception
        assertThatThrownBy(() -> 
            configurer.load(new ByteArrayInputStream(malformedYaml.getBytes()), config.getDeclaration())
        ).isInstanceOf(Exception.class);
    }

    @Test
    void testWrite_EmptyConfig() throws Exception {
        // Given: Config with no fields
        EmptyConfig config = ConfigManager.create(EmptyConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        
        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();
        
        // Then: YAML output is minimal (just {} or empty)
        assertThat(yaml.trim()).isIn("{}", "");
    }

    @Test
    void testWrite_VeryLargeValues() throws Exception {
        // Given: Config with very large string value
        LargeValueConfig config = ConfigManager.create(LargeValueConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        config.setLargeValue("x".repeat(10000)); // 10k characters
        
        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();
        
        // Then: Large value is written correctly
        assertThat(yaml).contains("largeValue:");
        assertThat(yaml.length()).isGreaterThan(10000);
    }

    @Test
    void testWrite_SpecialCharactersInStrings() throws Exception {
        // Given: Config with special characters
        SpecialCharsConfig config = ConfigManager.create(SpecialCharsConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        config.setQuotes("She said: \"Hello!\"");
        config.setBackslash("C:\\Users\\test\\path");
        config.setNewlines("Line 1\nLine 2\nLine 3");
        config.setTabs("Column1\tColumn2\tColumn3");
        config.setUnicode("Emoji: 🎉 Japanese: こんにちは Russian: Привет Polish: Łódź");
        
        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();
        
        // Then: Special characters are properly escaped/encoded
        assertThat(yaml).contains("quotes:");
        assertThat(yaml).contains("backslash:");
        assertThat(yaml).contains("newlines:");
        assertThat(yaml).contains("tabs:");
        assertThat(yaml).contains("unicode:");
    }

    @Test
    void testWrite_VeryDeeplyNestedStructure() throws Exception {
        // Given: Config with deep nesting
        String yaml = """
            level1:
              level2:
                level3:
                  level4:
                    level5:
                      value: deep
            """;
        
        DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        config.load(yaml);
        
        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String resultYaml = output.toString();
        
        // Then: Nesting structure is maintained
        assertThat(resultYaml).contains("level1:");
        assertThat(resultYaml).contains("level2:");
        assertThat(resultYaml).contains("level3:");
        assertThat(resultYaml).contains("level4:");
        assertThat(resultYaml).contains("level5:");
    }

    @Test
    void testWrite_VeryLargeCollection() throws Exception {
        // Given: Config with very large list
        LargeCollectionConfig config = ConfigManager.create(LargeCollectionConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add("item-" + i);
        }
        config.setLargeList(largeList);
        
        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();
        
        // Then: All items are written
        assertThat(yaml).contains("largeList:");
        assertThat(yaml).contains("item-0");
        assertThat(yaml).contains("item-999");
    }

    @Test
    @Disabled("Bukkit's YamlConfiguration has no way to differentiate between removing a top-level key and setting it to null")
    void testWrite_NullValues() throws Exception {
        // Given: Config with null values
        NullValueConfig config = ConfigManager.create(NullValueConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        config.setNullString(null);
        config.setNullList(null);
        config.setNullMap(null);
        
        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();
        
        // Then: Null values are represented in YAML (typically as 'null' or omitted)
        // YAML null is typically represented as empty or 'null' keyword
        assertThat(yaml).containsAnyOf("nullString: null", "nullString:");
        assertThat(yaml).containsAnyOf("nullList: null", "nullList:");
        assertThat(yaml).containsAnyOf("nullMap: null", "nullMap:");
    }

    @Test
    void testWrite_NestedNullValues() throws Exception {
        // Given: Config with nested OkaeriConfig containing null values
        NestedNullConfig config = ConfigManager.create(NestedNullConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        config.getNested().setNestedNullString(null);
        config.getNested().setNestedNullList(null);
        
        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();
        
        // Then: Nested null values are represented in YAML
        assertThat(yaml).contains("nested:");
        assertThat(yaml).containsAnyOf("nestedNullString: null", "nestedNullString:");
        assertThat(yaml).containsAnyOf("nestedNullList: null", "nestedNullList:");
    }

    @Test
    void testRoundTrip_EmptyStrings() throws Exception {
        // Given: Config with empty strings
        EmptyStringConfig config = ConfigManager.create(EmptyStringConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        config.setEmptyString("");
        config.setWhitespaceString("   ");
        
        // When: Save and load again
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        
        EmptyStringConfig loaded = ConfigManager.create(EmptyStringConfig.class);
        loaded.withConfigurer(new YamlBukkitConfigurer());
        loaded.load(new ByteArrayInputStream(output.toByteArray()));
        
        // Then: Empty strings are preserved
        assertThat(loaded.getEmptyString()).isEqualTo("");
        assertThat(loaded.getWhitespaceString()).isEqualTo("   ");
    }

    @Test
    void testWrite_YamlReservedWords() throws Exception {
        // Given: Config with YAML reserved words as values
        ReservedWordsConfig config = ConfigManager.create(ReservedWordsConfig.class);
        config.withConfigurer(new YamlBukkitConfigurer());
        config.setTrueString("true");
        config.setFalseString("false");
        config.setNullString("null");
        config.setYesString("yes");
        config.setNoString("no");
        
        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String yaml = output.toString();
        
        // Then: Reserved words are properly quoted or handled
        assertThat(yaml).contains("trueString:");
        assertThat(yaml).contains("falseString:");
        assertThat(yaml).contains("nullString:");
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
        private String unicode;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeepNestedConfig extends OkaeriConfig {
        private java.util.Map<String, Object> level1;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LargeCollectionConfig extends OkaeriConfig {
        private List<String> largeList;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullValueConfig extends OkaeriConfig {
        private String nullString;
        private List<String> nullList;
        private Map<String, String> nullMap;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedNullConfig extends OkaeriConfig {
        private NestedPart nested = new NestedPart();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedPart extends OkaeriConfig {
        private String nestedNullString = "default";
        private List<String> nestedNullList = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class EmptyStringConfig extends OkaeriConfig {
        private String emptyString;
        private String whitespaceString;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ReservedWordsConfig extends OkaeriConfig {
        private String trueString;
        private String falseString;
        private String nullString;
        private String yesString;
        private String noString;
    }
}
