package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.format.SourceLocation;
import eu.okaeri.configs.format.ini.IniSourceWalker;
import eu.okaeri.configs.serdes.ConfigPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IniSourceWalkerTest {

    // ==================== Basic Key-Value Tests ====================

    @Test
    void testSimpleKeyValue() {
        String ini = """
            name=John
            age=30""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(1);
        assertThat(nameLine.getKey()).isEqualTo("name");
        assertThat(nameLine.getValue()).isEqualTo("John");

        SourceLocation ageLine = walker.findPath(ConfigPath.parse("age"));
        assertThat(ageLine).isNotNull();
        assertThat(ageLine.getLineNumber()).isEqualTo(2);
        assertThat(ageLine.getKey()).isEqualTo("age");
        assertThat(ageLine.getValue()).isEqualTo("30");
    }

    @Test
    void testKeyValueWithSpaces() {
        String ini = "  key  =  value with spaces  ";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("key"));
        assertThat(loc).isNotNull();
        assertThat(loc.getKey()).isEqualTo("key");
        assertThat(loc.getValue()).isEqualTo("value with spaces  ");
    }

    @Test
    void testValueColumn() {
        String ini = """
            short=x
            longer_key=value""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation shortLine = walker.findPath(ConfigPath.parse("short"));
        assertThat(shortLine.getValueColumn()).isEqualTo(6); // "short=x" -> x at index 6

        SourceLocation longerLine = walker.findPath(ConfigPath.parse("longer_key"));
        assertThat(longerLine.getValueColumn()).isEqualTo(11); // "longer_key=value" -> value at index 11
    }

    // ==================== Properties Format (Dotted Keys) ====================

    @Test
    void testDottedKeyProperties() {
        String ini = """
            database.host=localhost
            database.port=5432""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation hostLine = walker.findPath(ConfigPath.parse("database.host"));
        assertThat(hostLine).isNotNull();
        assertThat(hostLine.getLineNumber()).isEqualTo(1);
        assertThat(hostLine.getValue()).isEqualTo("localhost");

        SourceLocation portLine = walker.findPath(ConfigPath.parse("database.port"));
        assertThat(portLine).isNotNull();
        assertThat(portLine.getLineNumber()).isEqualTo(2);
        assertThat(portLine.getValue()).isEqualTo("5432");
    }

    @Test
    void testDeeplyNestedDottedKey() {
        String ini = "level1.level2.level3.value=deep";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation valueLine = walker.findPath(ConfigPath.parse("level1.level2.level3.value"));
        assertThat(valueLine).isNotNull();
        assertThat(valueLine.getLineNumber()).isEqualTo(1);
        assertThat(valueLine.getValue()).isEqualTo("deep");
    }

    // ==================== INI Format (Sections) ====================

    @Test
    void testSectionHeader() {
        String ini = """
            [database]
            host=localhost
            port=5432""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation hostLine = walker.findPath(ConfigPath.parse("database.host"));
        assertThat(hostLine).isNotNull();
        assertThat(hostLine.getLineNumber()).isEqualTo(2);
        assertThat(hostLine.getValue()).isEqualTo("localhost");

        SourceLocation portLine = walker.findPath(ConfigPath.parse("database.port"));
        assertThat(portLine).isNotNull();
        assertThat(portLine.getLineNumber()).isEqualTo(3);
        assertThat(portLine.getValue()).isEqualTo("5432");
    }

    @Test
    void testNestedSectionHeader() {
        String ini = """
            [database.primary]
            host=primary.example.com
            port=5432""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation hostLine = walker.findPath(ConfigPath.parse("database.primary.host"));
        assertThat(hostLine).isNotNull();
        assertThat(hostLine.getLineNumber()).isEqualTo(2);
        assertThat(hostLine.getValue()).isEqualTo("primary.example.com");
    }

    @Test
    void testMultipleSections() {
        String ini = """
            [database]
            host=db.example.com

            [server]
            host=server.example.com""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation dbHost = walker.findPath(ConfigPath.parse("database.host"));
        assertThat(dbHost).isNotNull();
        assertThat(dbHost.getValue()).isEqualTo("db.example.com");

        SourceLocation serverHost = walker.findPath(ConfigPath.parse("server.host"));
        assertThat(serverHost).isNotNull();
        assertThat(serverHost.getValue()).isEqualTo("server.example.com");
    }

    @Test
    void testSectionWithDottedKey() {
        // Section + dotted key within section
        String ini = """
            [database]
            connection.timeout=30
            connection.retries=3""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation timeout = walker.findPath(ConfigPath.parse("database.connection.timeout"));
        assertThat(timeout).isNotNull();
        assertThat(timeout.getValue()).isEqualTo("30");
    }

    // ==================== Comments ====================

    @Test
    void testSemicolonComments() {
        String ini = """
            ; This is a comment
            name=value
            ; Another comment""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("name"));
        assertThat(loc).isNotNull();
        assertThat(loc.getLineNumber()).isEqualTo(2);
        assertThat(loc.getValue()).isEqualTo("value");
    }

    @Test
    void testHashComments() {
        String ini = """
            # This is a comment
            name=value
            # Another comment""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("name"));
        assertThat(loc).isNotNull();
        assertThat(loc.getLineNumber()).isEqualTo(2);
        assertThat(loc.getValue()).isEqualTo("value");
    }

    @Test
    void testEmptyLines() {
        String ini = """

            name=value

            age=30
            """;
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation nameLoc = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLoc).isNotNull();
        assertThat(nameLoc.getLineNumber()).isEqualTo(2);

        SourceLocation ageLoc = walker.findPath(ConfigPath.parse("age"));
        assertThat(ageLoc).isNotNull();
        assertThat(ageLoc.getLineNumber()).isEqualTo(4);
    }

    // ==================== Empty Values ====================

    @Test
    void testEmptyValue() {
        String ini = "emptyKey=";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("emptyKey"));
        assertThat(loc).isNotNull();
        assertThat(loc.getKey()).isEqualTo("emptyKey");
        assertThat(loc.getValue()).isNull();
        assertThat(loc.getValueColumn()).isEqualTo(-1);
    }

    @Test
    void testWhitespaceOnlyValue() {
        String ini = "whitespaceKey=   ";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("whitespaceKey"));
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isNull();
        assertThat(loc.getValueColumn()).isEqualTo(-1);
    }

    // ==================== Comma-Separated List Access ====================

    @Test
    void testCommaSeparatedListAccess() {
        String ini = "items=alpha,beta,gamma";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        // Direct access to the key
        SourceLocation itemsLoc = walker.findPath(ConfigPath.parse("items"));
        assertThat(itemsLoc).isNotNull();
        assertThat(itemsLoc.getValue()).isEqualTo("alpha,beta,gamma");

        // Indexed access
        SourceLocation item0 = walker.findPath(ConfigPath.parse("items[0]"));
        assertThat(item0).isNotNull();
        assertThat(item0.getValue()).isEqualTo("alpha");
        assertThat(item0.getValueColumn()).isEqualTo(6); // "items=alpha,beta,gamma" -> alpha at 6

        SourceLocation item1 = walker.findPath(ConfigPath.parse("items[1]"));
        assertThat(item1).isNotNull();
        assertThat(item1.getValue()).isEqualTo("beta");
        assertThat(item1.getValueColumn()).isEqualTo(12); // beta at 12

        SourceLocation item2 = walker.findPath(ConfigPath.parse("items[2]"));
        assertThat(item2).isNotNull();
        assertThat(item2.getValue()).isEqualTo("gamma");
        assertThat(item2.getValueColumn()).isEqualTo(17); // gamma at 17
    }

    @Test
    void testCommaSeparatedWithSpaces() {
        // Note: spaces are preserved in comma-separated values
        String ini = "items=a, b, c";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation item0 = walker.findPath(ConfigPath.parse("items[0]"));
        assertThat(item0.getValue()).isEqualTo("a");

        SourceLocation item1 = walker.findPath(ConfigPath.parse("items[1]"));
        assertThat(item1.getValue()).isEqualTo(" b");

        SourceLocation item2 = walker.findPath(ConfigPath.parse("items[2]"));
        assertThat(item2.getValue()).isEqualTo(" c");
    }

    @Test
    void testCommaSeparatedInSection() {
        String ini = """
            [config]
            numbers=1,2,invalid,4""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation item2 = walker.findPath(ConfigPath.parse("config.numbers[2]"));
        assertThat(item2).isNotNull();
        assertThat(item2.getValue()).isEqualTo("invalid");
        assertThat(item2.getLineNumber()).isEqualTo(2);
    }

    // ==================== findPath Branch Coverage ====================

    @Test
    void testFindPath_DirectMatch() {
        // Branch: Direct match found in pathToLocation
        String ini = "key=value";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("key"));
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isEqualTo("value");
    }

    @Test
    void testFindPath_NotFound_ReturnsNull() {
        // Branch: No match found at all → return null
        String ini = "existing=value";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("nonexistent"));
        assertThat(loc).isNull();
    }

    @Test
    void testFindPath_NestedNotFound_ReturnsNull() {
        // Branch: Path has multiple levels, none exist
        String ini = "other=value";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("nonexistent.nested.path"));
        assertThat(loc).isNull();
    }

    @Test
    void testFindPath_ParentFoundButValueNull() {
        // Branch: Found parent location but its value is null
        String ini = "parent=";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        // Query for nested path under empty parent
        SourceLocation loc = walker.findPath(ConfigPath.parse("parent[0]"));
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isNull();
    }

    @Test
    void testFindPath_IndexOutOfRange_ReturnsFallback() {
        // Branch: IndexNode but element not found → return parent location
        String ini = "items=a,b,c";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("items[10]"));
        assertThat(loc).isNotNull();
        // Returns parent location when index out of range
        assertThat(loc.getValue()).isEqualTo("a,b,c");
        assertThat(loc.getKey()).isEqualTo("items");
    }

    @Test
    void testFindPath_PropertyNodeFallback() {
        // Branch: lastNode is PropertyNode (not IndexNode) → return parent
        String ini = "parent.child=value";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        // Query for nested property that doesn't exist
        SourceLocation loc = walker.findPath(ConfigPath.parse("parent.child.nested"));
        assertThat(loc).isNotNull();
        // Returns parent location - key is the raw key from the source file
        assertThat(loc.getValue()).isEqualTo("value");
        assertThat(loc.getKey()).isEqualTo("parent.child");
    }

    // ==================== Edge Cases ====================

    @Test
    void testEmptyFile() {
        String ini = "";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    @Test
    void testOnlyComments() {
        String ini = """
            ; comment 1
            # comment 2
            ; comment 3""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    @Test
    void testOnlyEmptyLines() {
        String ini = "\n\n\n";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    @Test
    void testSectionWithNoKeys() {
        String ini = """
            [empty_section]

            [next_section]
            key=value""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        assertThat(walker.findPath(ConfigPath.parse("empty_section.anything"))).isNull();
        assertThat(walker.findPath(ConfigPath.parse("next_section.key"))).isNotNull();
    }

    @Test
    void testKeyWithEqualsInValue() {
        String ini = "equation=a=b+c";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("equation"));
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isEqualTo("a=b+c");
    }

    @Test
    void testSectionHeaderWithSpaces() {
        String ini = """
            [  section.name  ]
            key=value""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation loc = walker.findPath(ConfigPath.parse("section.name.key"));
        assertThat(loc).isNotNull();
        assertThat(loc.getValue()).isEqualTo("value");
    }

    @Test
    void testSingleElementList() {
        String ini = "items=single";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation item0 = walker.findPath(ConfigPath.parse("items[0]"));
        assertThat(item0).isNotNull();
        assertThat(item0.getValue()).isEqualTo("single");
    }

    @Test
    void testEmptyElementsInCommaSeparated() {
        String ini = "items=a,,c";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation item0 = walker.findPath(ConfigPath.parse("items[0]"));
        assertThat(item0.getValue()).isEqualTo("a");

        SourceLocation item1 = walker.findPath(ConfigPath.parse("items[1]"));
        assertThat(item1.getValue()).isEqualTo("");

        SourceLocation item2 = walker.findPath(ConfigPath.parse("items[2]"));
        assertThat(item2.getValue()).isEqualTo("c");
    }

    @Test
    void testTrailingComma() {
        String ini = "items=a,b,";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        SourceLocation item2 = walker.findPath(ConfigPath.parse("items[2]"));
        assertThat(item2).isNotNull();
        assertThat(item2.getValue()).isEqualTo("");
    }

    // ==================== Mixed Format ====================

    @Test
    void testMixedPropertiesAndSections() {
        String ini = """
            global.setting=true

            [database]
            host=localhost

            [server]
            port=8080""";
        IniSourceWalker walker = IniSourceWalker.of(ini);

        assertThat(walker.findPath(ConfigPath.parse("global.setting")).getValue()).isEqualTo("true");
        assertThat(walker.findPath(ConfigPath.parse("database.host")).getValue()).isEqualTo("localhost");
        assertThat(walker.findPath(ConfigPath.parse("server.port")).getValue()).isEqualTo("8080");
    }
}
