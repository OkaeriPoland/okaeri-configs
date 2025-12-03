package eu.okaeri.configs.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests TomlJacksonConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests in core-test.
 */
class TomlJacksonConfigurerFeaturesTest {

    @Test
    void testMaxSectionDepth_Default() throws Exception {
        // Given: Default configurer (maxSectionDepth = 2)
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();

        ThreeLevelConfig config = ConfigManager.create(ThreeLevelConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Only 2 levels deep get sections, 3rd level uses dotted keys
        // Note: [level1.level2] is valid TOML without separate [level1]
        assertThat(toml).contains("[level1.level2]");
        // level3 should be dotted key under level2, not a separate section
        assertThat(toml).doesNotContain("[level1.level2.level3]");
        assertThat(toml).contains("level3.value = 'deep'");
    }

    @Test
    void testMaxSectionDepth_Increased() throws Exception {
        // Given: Configurer with increased maxSectionDepth
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();
        configurer.setMaxSectionDepth(3);

        ThreeLevelConfig config = ConfigManager.create(ThreeLevelConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: All 3 levels get sections
        assertThat(toml).contains("[level1.level2.level3]");
        assertThat(toml).contains("value = 'deep'");
    }

    @Test
    void testMaxSectionDepth_Reduced() throws Exception {
        // Given: Configurer with reduced maxSectionDepth
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();
        configurer.setMaxSectionDepth(1);

        TwoLevelConfig config = ConfigManager.create(TwoLevelConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Only 1 level gets section, 2nd level uses dotted keys within section
        assertThat(toml).contains("[level1]");
        assertThat(toml).doesNotContain("[level1.level2]");
        // level2 fields appear as dotted keys within level1 section
        assertThat(toml).contains("level2.value = 'nested'");
    }

    @Test
    void testCustomMapper() throws Exception {
        // Given: Custom TomlMapper
        TomlMapper customMapper = TomlMapper.builder().build();
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();
        configurer.setMapper(customMapper);

        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.setConfigurer(configurer);

        // When: Write and read back
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        SimpleConfig loaded = ConfigManager.create(SimpleConfig.class);
        loaded.setConfigurer(configurer);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));

        // Then: Custom mapper works correctly
        assertThat(toml).contains("name = 'test'");
        assertThat(loaded.getName()).isEqualTo("test");
    }

    @Test
    void testMapperConstructor() throws Exception {
        // Given: Configurer created with mapper in constructor
        TomlMapper mapper = TomlMapper.builder().build();
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer(mapper);

        SimpleConfig config = ConfigManager.create(SimpleConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Works correctly
        assertThat(toml).contains("name = 'test'");
    }


    @Test
    void testHeaderComment() throws Exception {
        // Given: Config with header
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();

        HeaderConfig config = ConfigManager.create(HeaderConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Header is at the top as comments
        assertThat(toml).startsWith("# ===================\n# My Config Header\n# ===================\n");
    }

    @Test
    void testFieldComment() throws Exception {
        // Given: Config with field comments
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();

        CommentedFieldConfig config = ConfigManager.create(CommentedFieldConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Field comments appear before the field
        assertThat(toml).contains("# The application name");
        assertThat(toml).contains("name = 'MyApp'");
    }

    @Test
    void testSectionComment() throws Exception {
        // Given: Config with commented subconfig
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();

        CommentedSubConfigConfig config = ConfigManager.create(CommentedSubConfigConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Comment appears before the section
        assertThat(toml).contains("# Database settings");
        assertThat(toml).contains("[database]");
    }

    @Test
    void testMapUsesDotsNotSection() throws Exception {
        // Given: Config with Map field (not OkaeriConfig)
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();

        MapConfig config = ConfigManager.create(MapConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String toml = output.toString();

        // Then: Map uses dotted keys, not section
        assertThat(toml).contains("settings.debug = true");
        assertThat(toml).contains("settings.timeout = 30");
        assertThat(toml).doesNotContain("[settings]");
    }

    @Test
    void testExtensions() {
        // Given: TomlJacksonConfigurer
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();

        // When: Get extensions
        List<String> extensions = configurer.getExtensions();

        // Then: Returns toml
        assertThat(extensions).containsExactly("toml");
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SimpleConfig extends OkaeriConfig {
        private String name = "test";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TwoLevelConfig extends OkaeriConfig {
        private Level1Config level1 = new Level1Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1Config extends OkaeriConfig {
        private Level2Config level2 = new Level2Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2Config extends OkaeriConfig {
        private String value = "nested";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ThreeLevelConfig extends OkaeriConfig {
        private Level1WithLevel2Config level1 = new Level1WithLevel2Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level1WithLevel2Config extends OkaeriConfig {
        private Level2WithLevel3Config level2 = new Level2WithLevel3Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level2WithLevel3Config extends OkaeriConfig {
        private Level3Config level3 = new Level3Config();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class Level3Config extends OkaeriConfig {
        private String value = "deep";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("===================")
    @Header("My Config Header")
    @Header("===================")
    public static class HeaderConfig extends OkaeriConfig {
        private String field = "value";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedFieldConfig extends OkaeriConfig {
        @Comment("The application name")
        private String name = "MyApp";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedSubConfigConfig extends OkaeriConfig {
        @Comment("Database settings")
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
    public static class MapConfig extends OkaeriConfig {
        private Map<String, Object> settings = new LinkedHashMap<>() {{
            this.put("debug", true);
            this.put("timeout", 30);
        }};
    }
}
