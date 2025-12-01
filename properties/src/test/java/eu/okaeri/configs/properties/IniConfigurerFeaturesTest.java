package eu.okaeri.configs.properties;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests IniConfigurer-specific features.
 * Only tests for backend-specific functionality not covered by parameterized tests in core-test.
 */
class IniConfigurerFeaturesTest {

    @Test
    void testSemicolonCommentPrefix() throws Exception {
        // Given: Default IniConfigurer uses ; for comments
        IniConfigurer configurer = new IniConfigurer();

        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String ini = output.toString();

        // Then: Semicolon comment prefix is used
        assertThat(ini).contains("; This is a field comment");
        assertThat(ini).doesNotContain("# This is a field comment");
    }

    @Test
    void testCustomCommentPrefix() throws Exception {
        // Given: Configurer with custom comment prefix
        IniConfigurer configurer = new IniConfigurer();
        configurer.setCommentPrefix("# ");

        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String ini = output.toString();

        // Then: Custom comment prefix is used
        assertThat(ini).contains("# This is a field comment");
        assertThat(ini).doesNotContain("; This is a field comment");
    }

    @Test
    void testSectionCreation() throws Exception {
        // Given: Config with nested OkaeriConfig
        IniConfigurer configurer = new IniConfigurer();

        SectionedConfig config = ConfigManager.create(SectionedConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String ini = output.toString();

        // Then: Section is created for nested config
        assertThat(ini).contains("[database]");
        assertThat(ini).contains("host=localhost");
        assertThat(ini).contains("port=5432");
    }

    @Test
    void testMaxSectionDepth() throws Exception {
        // Given: Configurer with maxSectionDepth=1
        IniConfigurer configurer = new IniConfigurer();
        configurer.setMaxSectionDepth(1);

        DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String ini = output.toString();

        // Then: Only first level becomes section
        assertThat(ini).contains("[level1]");
        assertThat(ini).contains("level2.value=deep");
        assertThat(ini).doesNotContain("[level1.level2]");
    }

    @Test
    void testMaxSectionDepthTwo() throws Exception {
        // Given: Configurer with maxSectionDepth=2 (default)
        IniConfigurer configurer = new IniConfigurer();
        configurer.setMaxSectionDepth(2);

        DeepNestedConfig config = ConfigManager.create(DeepNestedConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String ini = output.toString();

        // Then: Two levels become sections
        assertThat(ini).contains("[level1.level2]");
        assertThat(ini).contains("value=deep");
    }

    @Test
    void testFileExtensions() {
        // Given: IniConfigurer
        IniConfigurer configurer = new IniConfigurer();

        // Then: Supports .ini and .cfg extensions
        assertThat(configurer.getExtensions()).containsExactly("ini", "cfg");
    }

    @Test
    void testCommentsWithinSections() throws Exception {
        // Given: Config with commented fields in nested config
        IniConfigurer configurer = new IniConfigurer();

        CommentedSectionConfig config = ConfigManager.create(CommentedSectionConfig.class);
        config.setConfigurer(configurer);

        // When: Write to OutputStream
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        config.save(output);
        String ini = output.toString();

        // Then: Comments appear within sections
        assertThat(ini).contains("[database]");
        assertThat(ini).contains("; Database hostname");
        assertThat(ini).contains("; Database port number");
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedConfig extends OkaeriConfig {
        @Comment("This is a field comment")
        private String field = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionedConfig extends OkaeriConfig {
        private String appName = "MyApp";
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
    public static class CommentedSectionConfig extends OkaeriConfig {
        private String appName = "MyApp";
        private CommentedDbConfig database = new CommentedDbConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedDbConfig extends OkaeriConfig {
        @Comment("Database hostname")
        private String host = "localhost";
        @Comment("Database port number")
        private int port = 5432;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeepNestedConfig extends OkaeriConfig {
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
        private String value = "deep";
    }
}
