package eu.okaeri.configs.format.ini;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.properties.IniConfigurer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structure tests for IniConfigurer to verify INI format output.
 */
class IniConfigurerStructureTest {

    @Test
    void testSaveToString_SimpleFlatConfig_NoSections() throws Exception {
        // Given: Simple flat config (no nested OkaeriConfig)
        FlatConfig config = ConfigManager.create(FlatConfig.class);
        config.setConfigurer(new IniConfigurer());

        // When: Save to string
        String ini = config.saveToString();

        // Then: No sections, just key=value pairs
        assertThat(ini).contains("name=TestApp");
        assertThat(ini).contains("version=1.0.0");
        assertThat(ini).doesNotContain("[");
    }

    @Test
    void testSaveToString_NestedConfig_CreatesSections() throws Exception {
        // Given: Config with nested OkaeriConfig
        SectionedConfig config = ConfigManager.create(SectionedConfig.class);
        config.setConfigurer(new IniConfigurer());

        // When: Save to string
        String ini = config.saveToString();

        // Then: Creates section for nested config
        assertThat(ini).contains("[database]");
        assertThat(ini).contains("host=localhost");
        assertThat(ini).contains("port=5432");
        assertThat(ini).contains("[server]");
        assertThat(ini).contains("host=0.0.0.0");
    }

    @Test
    void testSaveToString_WithHeader_HeaderAtTop() throws Exception {
        // Given: Config with header
        HeaderedConfig config = ConfigManager.create(HeaderedConfig.class);
        config.setConfigurer(new IniConfigurer());

        // When: Save to string
        String ini = config.saveToString();

        // Then: Header is at the top (INI uses ; for comments)
        assertThat(ini).startsWith("; Application Config");
        assertThat(ini).contains("; Version 1.0");
    }

    @Test
    void testSaveToString_WithComments_CommentsBeforeSections() throws Exception {
        // Given: Config with commented fields
        CommentedSectionConfig config = ConfigManager.create(CommentedSectionConfig.class);
        config.setConfigurer(new IniConfigurer());

        // When: Save to string
        String ini = config.saveToString();

        // Then: Comments appear before sections (INI uses ; for comments)
        assertThat(ini).contains("; Database configuration");
        assertThat(ini).contains("[database]");
    }

    @Test
    void testRoundTrip_SectionedConfig_PreservesStructure() throws Exception {
        // Given: Config with sections
        SectionedConfig original = ConfigManager.create(SectionedConfig.class);
        original.setConfigurer(new IniConfigurer());
        original.getDatabase().setHost("myhost.com");
        original.getDatabase().setPort(3306);

        // When: Save and reload
        String ini = original.saveToString();
        SectionedConfig loaded = ConfigManager.create(SectionedConfig.class);
        loaded.setConfigurer(new IniConfigurer());
        loaded.load(ini);

        // Then: Values are preserved
        assertThat(loaded.getDatabase().getHost()).isEqualTo("myhost.com");
        assertThat(loaded.getDatabase().getPort()).isEqualTo(3306);
    }

    @Test
    void testLoad_IniWithSections_ParsesCorrectly() throws Exception {
        // Given: INI content with sections
        String ini = """
            appName=MyApp

            [database]
            host=db.example.com
            port=5432

            [server]
            host=0.0.0.0
            port=8080
            """;

        // When: Load into config
        SectionedConfig config = ConfigManager.create(SectionedConfig.class);
        config.setConfigurer(new IniConfigurer());
        config.load(ini);

        // Then: Values are parsed correctly
        assertThat(config.getAppName()).isEqualTo("MyApp");
        assertThat(config.getDatabase().getHost()).isEqualTo("db.example.com");
        assertThat(config.getDatabase().getPort()).isEqualTo(5432);
        assertThat(config.getServer().getHost()).isEqualTo("0.0.0.0");
        assertThat(config.getServer().getPort()).isEqualTo(8080);
    }

    @Test
    void testSaveToString_ListValues_CommaNotation() throws Exception {
        // Given: Config with list
        ListConfig config = ConfigManager.create(ListConfig.class);
        config.setConfigurer(new IniConfigurer());

        // When: Save to string
        String ini = config.saveToString();

        // Then: List uses comma notation
        assertThat(ini).contains("tags=dev,test,prod");
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class FlatConfig extends OkaeriConfig {
        private String name = "TestApp";
        private String version = "1.0.0";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class SectionedConfig extends OkaeriConfig {
        private String appName = "MyApp";
        private DatabaseConfig database = new DatabaseConfig();
        private ServerConfig server = new ServerConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DatabaseConfig extends OkaeriConfig {
        private String host = "localhost";
        private int port = 5432;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ServerConfig extends OkaeriConfig {
        private String host = "0.0.0.0";
        private int port = 8080;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("Application Config")
    @Header("Version 1.0")
    public static class HeaderedConfig extends OkaeriConfig {
        private String name = "App";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CommentedSectionConfig extends OkaeriConfig {
        @Comment("Database configuration")
        private DatabaseConfig database = new DatabaseConfig();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ListConfig extends OkaeriConfig {
        private List<String> tags = Arrays.asList("dev", "test", "prod");
    }
}
