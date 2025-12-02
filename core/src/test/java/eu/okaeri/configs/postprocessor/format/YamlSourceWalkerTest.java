package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.format.SourceLocation;
import eu.okaeri.configs.format.yaml.YamlSourceWalker;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.serdes.ConfigPath;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YamlSourceWalkerTest {

    @Test
    void testSimpleKeyValue() {
        String yaml = """
            name: John
            age: 30""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

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
    void testNestedObjects() {
        String yaml = """
            database:
              host: localhost
              port: 5432""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation dbLine = walker.findPath(ConfigPath.parse("database"));
        assertThat(dbLine).isNotNull();
        assertThat(dbLine.getLineNumber()).isEqualTo(1);
        assertThat(dbLine.getValue()).isNull(); // key-only has no value

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
    void testDeeplyNested() {
        String yaml = """
            level1:
              level2:
                level3:
                  value: deep""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation valueLine = walker.findPath(ConfigPath.parse("level1.level2.level3.value"));
        assertThat(valueLine).isNotNull();
        assertThat(valueLine.getLineNumber()).isEqualTo(4);
        assertThat(valueLine.getValue()).isEqualTo("deep");
    }

    @Test
    void testSimpleList() {
        String yaml = """
            items:
              - first
              - second
              - third""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation item0 = walker.findPath(ConfigPath.parse("items[0]"));
        assertThat(item0).isNotNull();
        assertThat(item0.getLineNumber()).isEqualTo(2);
        assertThat(item0.getValue()).isEqualTo("first");

        SourceLocation item1 = walker.findPath(ConfigPath.parse("items[1]"));
        assertThat(item1).isNotNull();
        assertThat(item1.getLineNumber()).isEqualTo(3);
        assertThat(item1.getValue()).isEqualTo("second");

        SourceLocation item2 = walker.findPath(ConfigPath.parse("items[2]"));
        assertThat(item2).isNotNull();
        assertThat(item2.getLineNumber()).isEqualTo(4);
        assertThat(item2.getValue()).isEqualTo("third");
    }

    @Test
    void testListOfObjects() {
        String yaml = """
            servers:
              - host: server1.com
                port: 8080
              - host: server2.com
                port: 9090""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation server0Host = walker.findPath(ConfigPath.parse("servers[0].host"));
        assertThat(server0Host).isNotNull();
        assertThat(server0Host.getLineNumber()).isEqualTo(2);
        assertThat(server0Host.getValue()).isEqualTo("server1.com");

        SourceLocation server0Port = walker.findPath(ConfigPath.parse("servers[0].port"));
        assertThat(server0Port).isNotNull();
        assertThat(server0Port.getLineNumber()).isEqualTo(3);
        assertThat(server0Port.getValue()).isEqualTo("8080");

        SourceLocation server1Host = walker.findPath(ConfigPath.parse("servers[1].host"));
        assertThat(server1Host).isNotNull();
        assertThat(server1Host.getLineNumber()).isEqualTo(4);
        assertThat(server1Host.getValue()).isEqualTo("server2.com");

        SourceLocation server1Port = walker.findPath(ConfigPath.parse("servers[1].port"));
        assertThat(server1Port).isNotNull();
        assertThat(server1Port.getLineNumber()).isEqualTo(5);
        assertThat(server1Port.getValue()).isEqualTo("9090");
    }

    @Test
    void testCommentsAndBlanks() {
        String yaml = """
            # Header comment

            name: value
            # Inline comment""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        assertThat(walker.getLocations()).hasSize(4);
        // Comment line has no configPath
        assertThat(walker.getLocations().get(0).getConfigPath()).isNull();
        assertThat(walker.getLocations().get(0).getRawLine()).startsWith("#");
        // Blank line
        assertThat(walker.getLocations().get(1).getRawLine()).isEmpty();
        // Key-value line has configPath
        assertThat(walker.getLocations().get(2).getConfigPath()).isNotNull();
        // Comment line
        assertThat(walker.getLocations().get(3).getRawLine()).startsWith("#");
    }

    @Test
    void testMultilineValue() {
        String yaml = """
            description: |
              This is a
              multiline value
            name: test""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation descLine = walker.findPath(ConfigPath.parse("description"));
        assertThat(descLine).isNotNull();
        assertThat(descLine.getLineNumber()).isEqualTo(1);
        assertThat(descLine.getValue()).isEqualTo("|");

        // Multiline content lines have no configPath
        assertThat(walker.getLocations().get(1).getConfigPath()).isNull();
        assertThat(walker.getLocations().get(2).getConfigPath()).isNull();

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(4);
    }

    @Test
    void testValueColumn() {
        String yaml = """
            short: x
            longer_key: value""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation shortLine = walker.findPath(ConfigPath.parse("short"));
        assertThat(shortLine.getValueColumn()).isEqualTo(7); // "short: x" -> x at index 7

        SourceLocation longerLine = walker.findPath(ConfigPath.parse("longer_key"));
        assertThat(longerLine.getValueColumn()).isEqualTo(12); // "longer_key: value" -> value at index 12
    }

    @Test
    void testMixedStructure() {
        String yaml = """
            app:
              name: MyApp
              servers:
                - host: primary.com
                  port: 80
                - host: secondary.com
                  port: 443
              settings:
                timeout: 30""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        assertThat(walker.findPath(ConfigPath.parse("app"))).isNotNull();
        assertThat(walker.findPath(ConfigPath.parse("app.name"))).isNotNull();
        assertThat(walker.findPath(ConfigPath.parse("app.name")).getValue()).isEqualTo("MyApp");

        assertThat(walker.findPath(ConfigPath.parse("app.servers[0].host"))).isNotNull();
        assertThat(walker.findPath(ConfigPath.parse("app.servers[0].host")).getValue()).isEqualTo("primary.com");

        assertThat(walker.findPath(ConfigPath.parse("app.servers[1].port"))).isNotNull();
        assertThat(walker.findPath(ConfigPath.parse("app.servers[1].port")).getValue()).isEqualTo("443");

        assertThat(walker.findPath(ConfigPath.parse("app.settings.timeout"))).isNotNull();
        assertThat(walker.findPath(ConfigPath.parse("app.settings.timeout")).getValue()).isEqualTo("30");
    }

    // ==================== Edge Cases ====================

    @Test
    void testEmptyFile() {
        String yaml = "";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
        assertThat(walker.getLocations()).hasSize(1); // Single empty line
    }

    @Test
    void testPathNotFound() {
        String yaml = "name: value";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        assertThat(walker.findPath(ConfigPath.parse("nonexistent"))).isNull();
        assertThat(walker.findPath(ConfigPath.parse("name.nested"))).isNull();
        assertThat(walker.findPath(ConfigPath.parse("name[0]"))).isNull();
    }

    @Test
    void testTabIndentation() {
        // Tab characters are counted as 2 spaces in indent calculation
        String yaml = "parent:\n\tchild: value";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation childLine = walker.findPath(ConfigPath.parse("parent.child"));
        assertThat(childLine).isNotNull();
        assertThat(childLine.getValue()).isEqualTo("value");
        assertThat(childLine.getIndent()).isEqualTo(2); // Tab = 2 spaces
    }

    @Test
    void testMultilineIndicator_LiteralStrip() {
        // |- indicator (literal strip)
        String yaml = """
            text: |-
              line1
              line2
            next: value""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation textLine = walker.findPath(ConfigPath.parse("text"));
        assertThat(textLine).isNotNull();
        assertThat(textLine.getValue()).isEqualTo("|-");

        // Multiline content has no configPath
        assertThat(walker.getLocations().get(1).getConfigPath()).isNull();
        assertThat(walker.getLocations().get(2).getConfigPath()).isNull();

        SourceLocation nextLine = walker.findPath(ConfigPath.parse("next"));
        assertThat(nextLine).isNotNull();
        assertThat(nextLine.getLineNumber()).isEqualTo(4);
    }

    @Test
    void testMultilineIndicator_Folded() {
        // > indicator (folded)
        String yaml = """
            text: >
              folded
              content
            next: value""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation textLine = walker.findPath(ConfigPath.parse("text"));
        assertThat(textLine).isNotNull();
        assertThat(textLine.getValue()).isEqualTo(">");

        SourceLocation nextLine = walker.findPath(ConfigPath.parse("next"));
        assertThat(nextLine).isNotNull();
        assertThat(nextLine.getLineNumber()).isEqualTo(4);
    }

    @Test
    void testMultilineIndicator_FoldedStrip() {
        // >- indicator (folded strip)
        String yaml = """
            text: >-
              folded strip
            next: value""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation textLine = walker.findPath(ConfigPath.parse("text"));
        assertThat(textLine).isNotNull();
        assertThat(textLine.getValue()).isEqualTo(">-");

        SourceLocation nextLine = walker.findPath(ConfigPath.parse("next"));
        assertThat(nextLine).isNotNull();
        assertThat(nextLine.getLineNumber()).isEqualTo(3);
    }

    @Test
    void testTopLevelList() {
        // List at root level (getListParentPath returns root)
        String yaml = """
            - first
            - second
            - third""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation item0 = walker.findPath(ConfigPath.parse("[0]"));
        assertThat(item0).isNotNull();
        assertThat(item0.getLineNumber()).isEqualTo(1);
        assertThat(item0.getValue()).isEqualTo("first");

        SourceLocation item1 = walker.findPath(ConfigPath.parse("[1]"));
        assertThat(item1).isNotNull();
        assertThat(item1.getLineNumber()).isEqualTo(2);
        assertThat(item1.getValue()).isEqualTo("second");
    }

    @Test
    void testTopLevelListOfObjects() {
        String yaml = """
            - name: first
              value: 1
            - name: second
              value: 2""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation item0Name = walker.findPath(ConfigPath.parse("[0].name"));
        assertThat(item0Name).isNotNull();
        assertThat(item0Name.getValue()).isEqualTo("first");

        SourceLocation item1Value = walker.findPath(ConfigPath.parse("[1].value"));
        assertThat(item1Value).isNotNull();
        assertThat(item1Value.getValue()).isEqualTo("2");
    }

    @Test
    void testEmptyListItem() {
        String yaml = """
            items:
              -
              - value""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation item0 = walker.findPath(ConfigPath.parse("items[0]"));
        assertThat(item0).isNotNull();
        assertThat(item0.getValue()).isNull(); // Empty list item

        SourceLocation item1 = walker.findPath(ConfigPath.parse("items[1]"));
        assertThat(item1).isNotNull();
        assertThat(item1.getValue()).isEqualTo("value");
    }

    @Test
    void testWhitespaceOnlyFile() {
        String yaml = "   \n   \n   ";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
        // All lines are blank
        assertThat(walker.getLocations()).hasSize(3);
    }

    @Test
    void testOnlyComments() {
        String yaml = """
            # Comment 1
            # Comment 2
            # Comment 3""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
        assertThat(walker.getLocations()).hasSize(3);
        // All lines are comments
        walker.getLocations().forEach(loc -> {
            assertThat(loc.getConfigPath()).isNull();
            assertThat(loc.getRawLine()).startsWith("#");
        });
    }

    @Test
    void testNestedMultilineFollowedBySibling() {
        // Multiline in nested object, followed by sibling at same level
        String yaml = """
            config:
              desc: |
                multiline
                content
              name: test""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);

        SourceLocation descLine = walker.findPath(ConfigPath.parse("config.desc"));
        assertThat(descLine).isNotNull();
        assertThat(descLine.getValue()).isEqualTo("|");

        // Locations: [0]=config:, [1]=desc:|, [2]=multiline, [3]=content, [4]=name:test
        // Multiline content lines have no configPath
        assertThat(walker.getLocations().get(2).getConfigPath()).isNull();
        assertThat(walker.getLocations().get(3).getConfigPath()).isNull();

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("config.name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getValue()).isEqualTo("test");
    }

    // ==================== insertComments Tests ====================

    @Test
    void testInsertComments_ListOfConfigs() {
        String yaml = """
            items:
            - name: first
              value: 10
            - name: second
              value: 20""";
        YamlSourceWalker walker = YamlSourceWalker.of(yaml);
        ConfigDeclaration declaration = ConfigDeclaration.of(ListConfigTest.class);

        // Debug: verify the path exists and can be found
        SourceLocation nameLoc = walker.findPath(ConfigPath.parse("items[0].name"));
        assertThat(nameLoc).as("items[0].name should be found").isNotNull();
        assertThat(nameLoc.getKey()).isEqualTo("name");

        // Debug: verify declaration structure
        var itemsField = declaration.getField("items").orElse(null);
        assertThat(itemsField).as("items field should exist").isNotNull();
        var itemsType = itemsField.getType();
        assertThat(itemsType.getType()).isEqualTo(List.class);
        var elementType = itemsType.getSubtypeAtOrNull(0);
        assertThat(elementType).as("List element type should exist").isNotNull();
        assertThat(elementType.isConfig()).as("Element type should be config").isTrue();
        assertThat(elementType.getType()).isEqualTo(ItemConfig.class);

        // Debug: verify subconfig declaration
        ConfigDeclaration itemDecl = ConfigDeclaration.of(ItemConfig.class);
        var nameField = itemDecl.getField("name").orElse(null);
        assertThat(nameField).as("name field in ItemConfig should exist").isNotNull();
        assertThat(nameField.getComment()).as("name field should have comment").isNotNull();

        String result = walker.insertComments(declaration, "# ");

        // Comment should appear before first item's name field (on its own line)
        assertThat(result).contains("# Item name comment");
        assertThat(result).contains("# Item name comment\n- name: first");
        // Second item should NOT have a comment (already commented via pattern)
        long count = result.lines().filter(line -> line.contains("# Item name comment")).count();
        assertThat(count).isEqualTo(1);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    static class ListConfigTest extends OkaeriConfig {
        @Comment("List of items")
        private List<ItemConfig> items = List.of();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    static class ItemConfig extends OkaeriConfig {
        @Comment("Item name comment")
        private String name;
        private int value;
    }
}
