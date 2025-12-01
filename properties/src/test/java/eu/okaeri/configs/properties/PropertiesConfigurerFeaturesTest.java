package eu.okaeri.configs.properties;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests PropertiesConfigurer-specific features.
 */
class PropertiesConfigurerFeaturesTest {

    @Test
    void testSimpleValues(@TempDir Path tempDir) throws Exception {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new PropertiesConfigurer());
        config.setName("MyApp");
        config.setValue(42);
        config.setEnabled(true);

        // When: Save to file
        Path file = tempDir.resolve("test.properties");
        config.save(file);
        String content = Files.readString(file);

        // Then
        assertThat(content).contains("name=MyApp");
        assertThat(content).contains("value=42");
        assertThat(content).contains("enabled=true");
    }

    @Test
    void testRoundTrip(@TempDir Path tempDir) throws Exception {
        // Given
        TestConfig original = ConfigManager.create(TestConfig.class);
        original.setConfigurer(new PropertiesConfigurer());
        original.setName("TestApp");
        original.setValue(123);
        original.setEnabled(false);

        // When: Save and load
        Path file = tempDir.resolve("roundtrip.properties");
        original.save(file);

        TestConfig loaded = ConfigManager.create(TestConfig.class);
        loaded.setConfigurer(new PropertiesConfigurer());
        loaded.load(file);

        // Then
        assertThat(loaded.getName()).isEqualTo("TestApp");
        assertThat(loaded.getValue()).isEqualTo(123);
        assertThat(loaded.isEnabled()).isFalse();
    }

    @Test
    void testNestedConfig(@TempDir Path tempDir) throws Exception {
        // Given
        NestedConfig config = ConfigManager.create(NestedConfig.class);
        config.setConfigurer(new PropertiesConfigurer());
        config.getDatabase().setHost("localhost");
        config.getDatabase().setPort(5432);

        // When: Save to file
        Path file = tempDir.resolve("nested.properties");
        config.save(file);
        String content = Files.readString(file);

        // Then: Dot notation used
        assertThat(content).contains("database.host=localhost");
        assertThat(content).contains("database.port=5432");
    }

    @Test
    void testNestedConfigRoundTrip(@TempDir Path tempDir) throws Exception {
        // Given
        NestedConfig original = ConfigManager.create(NestedConfig.class);
        original.setConfigurer(new PropertiesConfigurer());
        original.getDatabase().setHost("db.example.com");
        original.getDatabase().setPort(3306);

        // When: Save and load
        Path file = tempDir.resolve("nested-roundtrip.properties");
        original.save(file);

        NestedConfig loaded = ConfigManager.create(NestedConfig.class);
        loaded.setConfigurer(new PropertiesConfigurer());
        loaded.load(file);

        // Then
        assertThat(loaded.getDatabase().getHost()).isEqualTo("db.example.com");
        assertThat(loaded.getDatabase().getPort()).isEqualTo(3306);
    }

    @Test
    void testListValues(@TempDir Path tempDir) throws Exception {
        // Given
        ListConfig config = ConfigManager.create(ListConfig.class);
        config.setConfigurer(new PropertiesConfigurer());
        config.setItems(List.of("alpha", "beta", "gamma"));

        // When: Save to file
        Path file = tempDir.resolve("list.properties");
        config.save(file);
        String content = Files.readString(file);

        // Then: Index notation used
        assertThat(content).contains("items.0=alpha");
        assertThat(content).contains("items.1=beta");
        assertThat(content).contains("items.2=gamma");
    }

    @Test
    void testListRoundTrip(@TempDir Path tempDir) throws Exception {
        // Given
        ListConfig original = ConfigManager.create(ListConfig.class);
        original.setConfigurer(new PropertiesConfigurer());
        original.setItems(List.of("one", "two", "three"));

        // When: Save and load
        Path file = tempDir.resolve("list-roundtrip.properties");
        original.save(file);

        ListConfig loaded = ConfigManager.create(ListConfig.class);
        loaded.setConfigurer(new PropertiesConfigurer());
        loaded.load(file);

        // Then
        assertThat(loaded.getItems()).containsExactly("one", "two", "three");
    }

    @Test
    void testMapValues(@TempDir Path tempDir) throws Exception {
        // Given
        MapConfig config = ConfigManager.create(MapConfig.class);
        config.setConfigurer(new PropertiesConfigurer());
        config.setSettings(Map.of("timeout", "30", "retries", "3"));

        // When: Save to file
        Path file = tempDir.resolve("map.properties");
        config.save(file);
        String content = Files.readString(file);

        // Then: Dot notation for map keys
        assertThat(content).contains("settings.timeout=30");
        assertThat(content).contains("settings.retries=3");
    }

    @Test
    void testNullHandling(@TempDir Path tempDir) throws Exception {
        // Given: Config with null value
        NullConfig config = ConfigManager.create(NullConfig.class);
        config.setConfigurer(new PropertiesConfigurer());
        config.setNullable(null);

        // When: Save to file
        Path file = tempDir.resolve("null.properties");
        config.save(file);
        String content = Files.readString(file);

        // Then: Null marker used
        assertThat(content).contains("nullable=__null__");
    }

    @Test
    void testNullRoundTrip(@TempDir Path tempDir) throws Exception {
        // Given: Config with null value
        NullConfig original = ConfigManager.create(NullConfig.class);
        original.setConfigurer(new PropertiesConfigurer());
        original.setNullable(null);

        // When: Save and load
        Path file = tempDir.resolve("null-roundtrip.properties");
        original.save(file);

        NullConfig loaded = ConfigManager.create(NullConfig.class);
        loaded.setConfigurer(new PropertiesConfigurer());
        loaded.load(file);

        // Then: Null preserved
        assertThat(loaded.getNullable()).isNull();
    }

    @Test
    void testComments(@TempDir Path tempDir) throws Exception {
        // Given
        CommentedConfig config = ConfigManager.create(CommentedConfig.class);
        config.setConfigurer(new PropertiesConfigurer());
        config.setDatabaseHost("localhost");
        config.setDatabasePort(5432);

        // When: Save to file
        Path file = tempDir.resolve("commented.properties");
        config.save(file);
        String content = Files.readString(file);

        // Then: Comments present
        assertThat(content).contains("# Configuration File");
        assertThat(content).contains("# Database Settings");
        assertThat(content).contains("# The database host");
        assertThat(content).contains("# Port number");
    }

    @Test
    void testSpecialCharacters(@TempDir Path tempDir) throws Exception {
        // Given
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new PropertiesConfigurer());
        config.setName("Line1\nLine2\tTabbed");

        // When: Save and load
        Path file = tempDir.resolve("special.properties");
        config.save(file);

        TestConfig loaded = ConfigManager.create(TestConfig.class);
        loaded.setConfigurer(new PropertiesConfigurer());
        loaded.load(file);

        // Then: Special characters preserved
        assertThat(loaded.getName()).isEqualTo("Line1\nLine2\tTabbed");
    }

    @Test
    void testLoadFromString() throws Exception {
        // Given
        String properties = """
            name=FromString
            value=999
            enabled=true
            """;

        // When
        TestConfig config = ConfigManager.create(TestConfig.class);
        config.setConfigurer(new PropertiesConfigurer());
        config.load(properties);

        // Then
        assertThat(config.getName()).isEqualTo("FromString");
        assertThat(config.getValue()).isEqualTo(999);
        assertThat(config.isEnabled()).isTrue();
    }

    // Test config classes

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TestConfig extends OkaeriConfig {
        private String name = "default";
        private int value = 0;
        private boolean enabled = false;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NestedConfig extends OkaeriConfig {
        private DatabaseConfig database = new DatabaseConfig();

        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class DatabaseConfig extends OkaeriConfig {
            private String host = "localhost";
            private int port = 3306;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class ListConfig extends OkaeriConfig {
        private List<String> items = List.of();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MapConfig extends OkaeriConfig {
        private Map<String, String> settings = Map.of();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NullConfig extends OkaeriConfig {
        private String nullable = "default";
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Header("Configuration File")
    @Header("Database Settings")
    public static class CommentedConfig extends OkaeriConfig {
        @Comment("The database host")
        private String databaseHost = "localhost";

        @Comment("Port number")
        private int databasePort = 3306;
    }
}
