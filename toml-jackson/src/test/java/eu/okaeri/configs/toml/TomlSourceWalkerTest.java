package eu.okaeri.configs.toml;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.format.SourceLocation;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.format.toml.TomlSourceWalker;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TomlSourceWalkerTest {

    @Test
    void testSimpleKey() {
        String toml = "value = 'not_a_number'";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path = ConfigPath.root().property("value");
        SourceLocation location = walker.findPath(path);

        assertThat(location).isNotNull();
        assertThat(location.getLineNumber()).isEqualTo(1);
        assertThat(location.getKey()).isEqualTo("value");
        assertThat(location.getValue()).isEqualTo("'not_a_number'");
        assertThat(location.getValueColumn()).isEqualTo(8);
    }

    @Test
    void testSection() {
        String toml = """
            [database]
            host = 'localhost'
            port = 5432
            """;
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath portPath = ConfigPath.root().property("database").property("port");
        SourceLocation location = walker.findPath(portPath);

        assertThat(location).isNotNull();
        assertThat(location.getLineNumber()).isEqualTo(3);
        assertThat(location.getKey()).isEqualTo("port");
        assertThat(location.getValue()).isEqualTo("5432");
    }

    @Test
    void testDottedKey() {
        String toml = "database.port = 5432";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path = ConfigPath.root().property("database").property("port");
        SourceLocation location = walker.findPath(path);

        assertThat(location).isNotNull();
        assertThat(location.getLineNumber()).isEqualTo(1);
        assertThat(location.getKey()).isEqualTo("database.port");
        assertThat(location.getValue()).isEqualTo("5432");
    }

    @Test
    void testArray() {
        String toml = "numbers = [1, 2, 'bad', 4]";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path = ConfigPath.root().property("numbers");
        SourceLocation location = walker.findPath(path);

        assertThat(location).isNotNull();
        assertThat(location.getValue()).isEqualTo("[1, 2, 'bad', 4]");

        // Index access
        ConfigPath indexPath = path.index(2);
        SourceLocation indexLocation = walker.findPath(indexPath);

        assertThat(indexLocation).isNotNull();
        assertThat(indexLocation.getValue()).isEqualTo("'bad'");
    }

    @Test
    void testArrayOfTables() {
        String toml = """
            [[servers]]
            host = 'server1'
            port = 8080

            [[servers]]
            host = 'server2'
            port = 9090
            """;
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        // servers[0].host
        ConfigPath path0Host = ConfigPath.root().property("servers").index(0).property("host");
        SourceLocation loc0Host = walker.findPath(path0Host);
        assertThat(loc0Host).isNotNull();
        assertThat(loc0Host.getLineNumber()).isEqualTo(2);
        assertThat(loc0Host.getValue()).isEqualTo("'server1'");

        // servers[0].port
        ConfigPath path0Port = ConfigPath.root().property("servers").index(0).property("port");
        SourceLocation loc0Port = walker.findPath(path0Port);
        assertThat(loc0Port).isNotNull();
        assertThat(loc0Port.getLineNumber()).isEqualTo(3);
        assertThat(loc0Port.getValue()).isEqualTo("8080");

        // servers[1].host
        ConfigPath path1Host = ConfigPath.root().property("servers").index(1).property("host");
        SourceLocation loc1Host = walker.findPath(path1Host);
        assertThat(loc1Host).isNotNull();
        assertThat(loc1Host.getLineNumber()).isEqualTo(6);
        assertThat(loc1Host.getValue()).isEqualTo("'server2'");

        // servers[1].port
        ConfigPath path1Port = ConfigPath.root().property("servers").index(1).property("port");
        SourceLocation loc1Port = walker.findPath(path1Port);
        assertThat(loc1Port).isNotNull();
        assertThat(loc1Port.getLineNumber()).isEqualTo(7);
        assertThat(loc1Port.getValue()).isEqualTo("9090");
    }

    @Test
    void testE2E_RawContentAndWalker() {
        String toml = "value = 'not_a_number'";
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();

        IntConfig config = ConfigManager.create(IntConfig.class);
        config.setConfigurer(configurer);

        // Simulate what load() does
        configurer.setRawContent(toml);

        // Verify raw content is set
        assertThat(configurer.getRawContent()).isEqualTo(toml);

        // Verify walker can be created and finds the path
        SourceWalker walker = configurer.createSourceWalker();
        assertThat(walker).isNotNull();

        ConfigPath path = ConfigPath.root().property("value");
        SourceLocation location = walker.findPath(path);
        assertThat(location).isNotNull();
        assertThat(location.getValue()).isEqualTo("'not_a_number'");
    }

    @Test
    void testE2E_ErrorWithSourceMarker() {
        String toml = "value = 'not_a_number'";
        TomlJacksonConfigurer configurer = new TomlJacksonConfigurer();

        IntConfig config = ConfigManager.create(IntConfig.class);
        config.setConfigurer(configurer);

        assertThatThrownBy(() -> config.load(toml))
            .isInstanceOf(OkaeriException.class)
            .hasMessageContaining("value")
            .hasMessageContaining("Integer");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class IntConfig extends OkaeriConfig {
        private int value;
    }
}
