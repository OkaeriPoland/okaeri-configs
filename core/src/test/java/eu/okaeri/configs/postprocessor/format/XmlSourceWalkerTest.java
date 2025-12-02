package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.format.SourceLocation;
import eu.okaeri.configs.format.xml.XmlSourceWalker;
import eu.okaeri.configs.serdes.ConfigPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XmlSourceWalkerTest {

    private static final String ROOT = "config";

    // ==================== Basic Element Tests ====================

    @Test
    void testSimpleKeyValue() {
        String xml = """
            <config>
              <name>John</name>
              <age>30</age>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(2);
        assertThat(nameLine.getKey()).isEqualTo("name");
        assertThat(nameLine.getValue()).isEqualTo("John");

        SourceLocation ageLine = walker.findPath(ConfigPath.parse("age"));
        assertThat(ageLine).isNotNull();
        assertThat(ageLine.getLineNumber()).isEqualTo(3);
        assertThat(ageLine.getKey()).isEqualTo("age");
        assertThat(ageLine.getValue()).isEqualTo("30");
    }

    @Test
    void testNestedObjects() {
        String xml = """
            <config>
              <database>
                <host>localhost</host>
                <port>5432</port>
              </database>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation hostLine = walker.findPath(ConfigPath.parse("database.host"));
        assertThat(hostLine).isNotNull();
        assertThat(hostLine.getLineNumber()).isEqualTo(3);
        assertThat(hostLine.getValue()).isEqualTo("localhost");

        SourceLocation portLine = walker.findPath(ConfigPath.parse("database.port"));
        assertThat(portLine).isNotNull();
        assertThat(portLine.getLineNumber()).isEqualTo(4);
        assertThat(portLine.getValue()).isEqualTo("5432");
    }

    @Test
    void testDeeplyNested() {
        String xml = """
            <config>
              <level1>
                <level2>
                  <level3>
                    <value>deep</value>
                  </level3>
                </level2>
              </level1>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation valueLine = walker.findPath(ConfigPath.parse("level1.level2.level3.value"));
        assertThat(valueLine).isNotNull();
        assertThat(valueLine.getLineNumber()).isEqualTo(5);
        assertThat(valueLine.getValue()).isEqualTo("deep");
    }

    // ==================== List/Item Tests ====================

    @Test
    void testItemElements() {
        String xml = """
            <config>
              <items>
                <item>first</item>
                <item>second</item>
                <item>third</item>
              </items>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation item0 = walker.findPath(ConfigPath.parse("items[0]"));
        assertThat(item0).isNotNull();
        assertThat(item0.getLineNumber()).isEqualTo(3);
        assertThat(item0.getValue()).isEqualTo("first");

        SourceLocation item1 = walker.findPath(ConfigPath.parse("items[1]"));
        assertThat(item1).isNotNull();
        assertThat(item1.getLineNumber()).isEqualTo(4);
        assertThat(item1.getValue()).isEqualTo("second");

        SourceLocation item2 = walker.findPath(ConfigPath.parse("items[2]"));
        assertThat(item2).isNotNull();
        assertThat(item2.getLineNumber()).isEqualTo(5);
        assertThat(item2.getValue()).isEqualTo("third");
    }

    // ==================== Entry with Key Attribute Tests ====================

    @Test
    void testEntryWithKeyAttribute() {
        // Map keys that are invalid XML names use <entry key="...">value</entry>
        String xml = """
            <config>
              <settings>
                <entry key="123-invalid">value1</entry>
                <entry key="key with spaces">value2</entry>
                <entry key="special!@#">value3</entry>
              </settings>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation entry1 = walker.findPath(ConfigPath.parse("settings.123-invalid"));
        assertThat(entry1).isNotNull();
        assertThat(entry1.getLineNumber()).isEqualTo(3);
        assertThat(entry1.getKey()).isEqualTo("123-invalid");
        assertThat(entry1.getValue()).isEqualTo("value1");

        SourceLocation entry2 = walker.findPath(ConfigPath.parse("settings.key with spaces"));
        assertThat(entry2).isNotNull();
        assertThat(entry2.getLineNumber()).isEqualTo(4);
        assertThat(entry2.getKey()).isEqualTo("key with spaces");
        assertThat(entry2.getValue()).isEqualTo("value2");

        SourceLocation entry3 = walker.findPath(ConfigPath.parse("settings.special!@#"));
        assertThat(entry3).isNotNull();
        assertThat(entry3.getLineNumber()).isEqualTo(5);
        assertThat(entry3.getKey()).isEqualTo("special!@#");
        assertThat(entry3.getValue()).isEqualTo("value3");
    }

    @Test
    void testMixedValidAndInvalidKeys() {
        String xml = """
            <config>
              <map>
                <validKey>normalValue</validKey>
                <entry key="0-starts-with-digit">digitValue</entry>
              </map>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation validKey = walker.findPath(ConfigPath.parse("map.validKey"));
        assertThat(validKey).isNotNull();
        assertThat(validKey.getValue()).isEqualTo("normalValue");

        SourceLocation invalidKey = walker.findPath(ConfigPath.parse("map.0-starts-with-digit"));
        assertThat(invalidKey).isNotNull();
        assertThat(invalidKey.getValue()).isEqualTo("digitValue");
    }

    // ==================== Self-Closing Tag Tests ====================

    @Test
    void testSelfClosingTag() {
        // Self-closing tags like <empty/> represent empty string in config
        // but have no value content in source to point to for error reporting
        String xml = """
            <config>
              <name>John</name>
              <empty/>
              <other>value</other>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation emptyLine = walker.findPath(ConfigPath.parse("empty"));
        assertThat(emptyLine).isNotNull();
        assertThat(emptyLine.getLineNumber()).isEqualTo(3);
        assertThat(emptyLine.getKey()).isEqualTo("empty");
        // No value content between tags to point to
        assertThat(emptyLine.getValue()).isNull();
        assertThat(emptyLine.getValueColumn()).isEqualTo(-1);
    }

    @Test
    void testMultipleSelfClosingTags() {
        String xml = """
            <config>
              <field1/>
              <field2/>
              <field3/>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        assertThat(walker.findPath(ConfigPath.parse("field1"))).isNotNull();
        assertThat(walker.findPath(ConfigPath.parse("field2"))).isNotNull();
        assertThat(walker.findPath(ConfigPath.parse("field3"))).isNotNull();
    }

    @Test
    void testNestedSelfClosingTag() {
        String xml = """
            <config>
              <parent>
                <child/>
              </parent>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation childLine = walker.findPath(ConfigPath.parse("parent.child"));
        assertThat(childLine).isNotNull();
        assertThat(childLine.getLineNumber()).isEqualTo(3);
        assertThat(childLine.getValue()).isNull();
    }

    // ==================== XML Declaration Tests ====================

    @Test
    void testXmlDeclaration_Skipped() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
              <name>John</name>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(3);
        assertThat(nameLine.getValue()).isEqualTo("John");
    }

    @Test
    void testXmlDeclarationInline() {
        String xml = "<?xml version=\"1.0\"?><config><name>John</name></config>";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(1);
        assertThat(nameLine.getValue()).isEqualTo("John");
    }

    // ==================== Comment Tests ====================

    @Test
    void testXmlComment_Skipped() {
        String xml = """
            <config>
              <!-- This is a comment -->
              <name>John</name>
              <!-- Another comment -->
              <age>30</age>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(3);
        assertThat(nameLine.getValue()).isEqualTo("John");

        SourceLocation ageLine = walker.findPath(ConfigPath.parse("age"));
        assertThat(ageLine).isNotNull();
        assertThat(ageLine.getLineNumber()).isEqualTo(5);
        assertThat(ageLine.getValue()).isEqualTo("30");
    }

    @Test
    void testMultilineComment() {
        String xml = """
            <config>
              <!--
                Multi-line
                comment
              -->
              <name>John</name>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getLineNumber()).isEqualTo(6);
        assertThat(nameLine.getValue()).isEqualTo("John");
    }

    @Test
    void testInlineComment() {
        String xml = "<config><!-- comment --><name>John</name></config>";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        SourceLocation nameLine = walker.findPath(ConfigPath.parse("name"));
        assertThat(nameLine).isNotNull();
        assertThat(nameLine.getValue()).isEqualTo("John");
    }

    // ==================== Empty File Tests ====================

    @Test
    void testEmptyFile() {
        String xml = "";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    @Test
    void testEmptyConfig() {
        String xml = "<config></config>";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    @Test
    void testWhitespaceOnly() {
        String xml = "   \n   \n   ";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        assertThat(walker.findPath(ConfigPath.parse("anything"))).isNull();
    }

    // ==================== Edge Cases ====================

    @Test
    void testMinifiedXml() {
        String xml = "<config><name>John</name><age>30</age><nested><value>x</value></nested></config>";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        assertThat(walker.findPath(ConfigPath.parse("name")).getValue()).isEqualTo("John");
        assertThat(walker.findPath(ConfigPath.parse("age")).getValue()).isEqualTo("30");
        assertThat(walker.findPath(ConfigPath.parse("nested.value")).getValue()).isEqualTo("x");
    }

    @Test
    void testNullElement() {
        // <null/> is used for null values and should be skipped
        String xml = """
            <config>
              <field>
                <null/>
              </field>
            </config>""";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        // null element should not create a path entry
        assertThat(walker.findPath(ConfigPath.parse("field.null"))).isNull();
    }

    @Test
    void testPathNotFound() {
        String xml = "<config><name>test</name></config>";
        XmlSourceWalker walker = XmlSourceWalker.of(xml, ROOT);

        assertThat(walker.findPath(ConfigPath.parse("nonexistent"))).isNull();
        assertThat(walker.findPath(ConfigPath.parse("name.nested"))).isNull();
    }
}
