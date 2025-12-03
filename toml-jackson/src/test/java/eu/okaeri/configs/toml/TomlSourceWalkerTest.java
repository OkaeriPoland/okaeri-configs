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

        // Simulate what load() does - set rawContent on context
        config.getContext().setRawContent(toml);

        // Verify raw content is accessible via context
        assertThat(config.getContext().getRawContent()).isEqualTo(toml);

        // Verify walker can be created and finds the path
        SourceWalker walker = configurer.createSourceWalker(toml);
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

    // ==================== Edge Cases ====================

    @Test
    void testEmptyFile() {
        String toml = "";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    @Test
    void testPathNotFound() {
        String toml = "name = 'value'";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        assertThat(walker.findPath(ConfigPath.parse("nonexistent"))).isNull();
        // name.nested falls back to parent "name" location
        assertThat(walker.findPath(ConfigPath.parse("name.nested"))).isNotNull();
    }

    @Test
    void testPathNotFound_CompletelyMissing() {
        String toml = """
            [section]
            key = 'value'
            """;
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        // Completely missing path with no parent
        assertThat(walker.findPath(ConfigPath.parse("other"))).isNull();
        assertThat(walker.findPath(ConfigPath.parse("other.nested"))).isNull();
    }

    @Test
    void testComments_Skipped() {
        String toml = """
            # This is a comment
            name = 'John'
            # Another comment
            age = 30
            """;
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        SourceLocation nameLoc = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLoc).isNotNull();
        assertThat(nameLoc.getLineNumber()).isEqualTo(2);
        assertThat(nameLoc.getValue()).isEqualTo("'John'");

        SourceLocation ageLoc = walker.findPath(ConfigPath.parse("age"));
        assertThat(ageLoc).isNotNull();
        assertThat(ageLoc.getLineNumber()).isEqualTo(4);
    }

    @Test
    void testQuotedKey() {
        // Quoted keys with dots are split by appendDottedPath after quote stripping
        // So "key.with.dot" becomes path key → with → dot
        String toml = "\"key.with.dot\" = 'value'";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        // Path is 3 levels deep after dot splitting
        ConfigPath path = ConfigPath.parse("key.with.dot");
        SourceLocation loc = walker.findPath(path);
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isEqualTo("'value'");
    }

    @Test
    void testSingleQuotedKey() {
        // Single quoted key without dots
        String toml = "'quoted-key' = 'value'";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path = ConfigPath.root().property("quoted-key");
        SourceLocation loc = walker.findPath(path);
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isEqualTo("'value'");
    }

    @Test
    void testDoubleQuotedKeyNoDots() {
        // Double quoted key without dots
        String toml = "\"special-key\" = 'value'";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path = ConfigPath.root().property("special-key");
        SourceLocation loc = walker.findPath(path);
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isEqualTo("'value'");
    }

    @Test
    void testEmptyValue() {
        // Empty value after = sign
        String toml = "key =";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path = ConfigPath.root().property("key");
        SourceLocation loc = walker.findPath(path);
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isNull();
        assertThat(loc.getValueColumn()).isEqualTo(-1);
    }

    @Test
    void testNestedSections() {
        String toml = """
            [database.connection]
            host = 'localhost'
            port = 5432

            [database.settings]
            timeout = 30
            """;
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        SourceLocation hostLoc = walker.findPath(ConfigPath.parse("database.connection.host"));
        assertThat(hostLoc).isNotNull();
        assertThat(hostLoc.getValue()).isEqualTo("'localhost'");

        SourceLocation timeoutLoc = walker.findPath(ConfigPath.parse("database.settings.timeout"));
        assertThat(timeoutLoc).isNotNull();
        assertThat(timeoutLoc.getValue()).isEqualTo("30");
    }

    @Test
    void testFindPath_IndexOutOfRange() {
        String toml = "items = [1, 2, 3]";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        // Index beyond array length
        ConfigPath path = ConfigPath.root().property("items").index(10);
        SourceLocation loc = walker.findPath(path);
        // Returns parent location when index out of range
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isEqualTo("[1, 2, 3]");
        assertThat(loc.getKey()).isEqualTo("items");
    }

    @Test
    void testFindPath_PropertyNodeFallback() {
        // Branch: lastNode is PropertyNode (not IndexNode) → return parent
        String toml = "parent.child = 'value'";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        // Query for nested property that doesn't exist
        ConfigPath path = ConfigPath.parse("parent.child.nested");
        SourceLocation loc = walker.findPath(path);
        assertThat(loc).isNotNull();
        // Returns parent location - key is the raw key from the source file
        assertThat(loc.getValue()).isEqualTo("'value'");
        assertThat(loc.getKey()).isEqualTo("parent.child");
    }

    @Test
    void testFindPath_NullValueFallback() {
        // When parent has null value, return the location anyway
        String toml = """
            [section]
            key =
            """;
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path = ConfigPath.parse("section.key.nested");
        SourceLocation loc = walker.findPath(path);
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isNull();
        assertThat(loc.getKey()).isEqualTo("key");
    }

    @Test
    void testArrayWithNestedArrays() {
        String toml = "matrix = [[1, 2], [3, 4], [5, 6]]";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path0 = ConfigPath.root().property("matrix").index(0);
        SourceLocation loc0 = walker.findPath(path0);
        assertThat(loc0).isNotNull();
        assertThat(loc0.getValue()).isEqualTo("[1, 2]");

        ConfigPath path2 = ConfigPath.root().property("matrix").index(2);
        SourceLocation loc2 = walker.findPath(path2);
        assertThat(loc2).isNotNull();
        assertThat(loc2.getValue()).isEqualTo("[5, 6]");
    }

    @Test
    void testArrayWithInlineObjects() {
        // Inline objects without strings (strings inside objects cause early return in findArrayElement)
        String toml = "objects = [{a = 1}, {b = 2}]";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        ConfigPath path0 = ConfigPath.root().property("objects").index(0);
        SourceLocation loc0 = walker.findPath(path0);
        assertThat(loc0).isNotNull();
        assertThat(loc0.getValue()).isEqualTo("{a = 1}");

        ConfigPath path1 = ConfigPath.root().property("objects").index(1);
        SourceLocation loc1 = walker.findPath(path1);
        assertThat(loc1).isNotNull();
        assertThat(loc1.getValue()).isEqualTo("{b = 2}");
    }

    @Test
    void testWhitespaceOnlyFile() {
        String toml = "   \n   \n   ";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    @Test
    void testOnlyComments() {
        String toml = """
            # Comment 1
            # Comment 2
            # Comment 3
            """;
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    @Test
    void testArrayOfTablesWithNestedSections() {
        String toml = """
            [[products]]
            name = 'Widget'

            [[products]]
            name = 'Gadget'
            [products.details]
            sku = 'G001'
            """;
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        SourceLocation widget = walker.findPath(ConfigPath.parse("products[0].name"));
        assertThat(widget).isNotNull();
        assertThat(widget.getValue()).isEqualTo("'Widget'");

        SourceLocation gadget = walker.findPath(ConfigPath.parse("products[1].name"));
        assertThat(gadget).isNotNull();
        assertThat(gadget.getValue()).isEqualTo("'Gadget'");
    }

    @Test
    void testNotArray_ReturnNull() {
        // findArrayElement returns null for non-array values
        String toml = "value = 'not an array'";
        TomlSourceWalker walker = TomlSourceWalker.of(toml);

        // Try to index a non-array value
        ConfigPath path = ConfigPath.root().property("value").index(0);
        SourceLocation loc = walker.findPath(path);
        // Returns parent location (fallback)
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isEqualTo("'not an array'");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class IntConfig extends OkaeriConfig {
        private int value;
    }
}
