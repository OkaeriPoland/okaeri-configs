package eu.okaeri.configs.xml;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.format.xml.XmlSourceWalker;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * XML configurer using a simple, human-readable format.
 * <p>
 * Designed for simplified data (Map/List of primitive types/wrappers).
 * Uses element names for map keys, with fallback to entry[@key] for invalid XML names.
 * Lists use repeated item elements. No XML declaration is generated.
 * </p>
 * <p>
 * Example output:
 * <pre>{@code
 * <config>
 *   <name>Example</name>
 *   <count>42</count>
 *   <items>
 *     <item>first</item>
 *     <item>second</item>
 *   </items>
 *   <nested>
 *     <key>value</key>
 *   </nested>
 * </config>
 * }</pre>
 */
@Accessors(chain = true)
public class XmlSimpleConfigurer extends Configurer {

    private static final Pattern VALID_XML_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_\\-]*$");
    private static final String DEFAULT_ROOT_ELEMENT = "config";
    private static final String ENTRY_ELEMENT = "entry";
    private static final String ITEM_ELEMENT = "item";
    private static final String KEY_ATTRIBUTE = "key";
    private static final String NULL_ELEMENT = "null";

    private Map<String, Object> map = new LinkedHashMap<>();

    private @Setter int indent = 2;
    private @Setter String rootElement = DEFAULT_ROOT_ELEMENT;

    public XmlSimpleConfigurer() {
    }

    public XmlSimpleConfigurer(@NonNull Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("xml");
    }

    @Override
    public SourceWalker createSourceWalker() {
        String raw = this.getRawContent();
        return (raw == null) ? null : XmlSourceWalker.of(raw, this.rootElement);
    }

    @Override
    public boolean isCommentLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("<!--");
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), false);
        this.map.put(key, simplified);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.map.put(key, value);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {
        return this.map.remove(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.map.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    // ==================== Loading ====================

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        Document document = this.parseDocument(inputStream);
        Element root = document.getDocumentElement();
        if (root == null) {
            throw new IllegalStateException("XML document has no root element");
        }
        this.map = this.parseMap(root, declaration);
    }

    private Document parseDocument(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(inputStream);
    }

    private Map<String, Object> parseMap(Element parent, ConfigDeclaration declaration) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Element child : getChildElements(parent)) {
            String key = getElementKey(child);
            GenericsDeclaration fieldType = getFieldType(declaration, key);
            result.put(key, this.parseValue(child, fieldType));
        }

        return result;
    }

    private Object parseValue(Element element, GenericsDeclaration expectedType) {
        List<Element> children = getChildElements(element);

        // <null/> element means null value
        if (isNullElement(children)) {
            return null;
        }

        // No child elements: primitive value or empty collection
        if (children.isEmpty()) {
            return this.parseEmptyOrPrimitive(element, expectedType);
        }

        // Has children: list or map
        if (this.isListStructure(children, expectedType)) {
            return this.parseList(children, expectedType);
        }
        return this.parseNestedMap(children, expectedType);
    }

    private Object parseEmptyOrPrimitive(Element element, GenericsDeclaration expectedType) {
        if (isCollectionType(expectedType)) {
            return new ArrayList<>();
        }
        if (isMapOrConfigType(expectedType)) {
            return new LinkedHashMap<>();
        }
        return element.getTextContent();
    }

    private boolean isListStructure(List<Element> children, GenericsDeclaration expectedType) {
        // Trust declaration if available
        if (expectedType != null) {
            return Collection.class.isAssignableFrom(expectedType.getType());
        }
        // Fallback: all children are <item> elements
        return children.stream().allMatch(e -> ITEM_ELEMENT.equals(e.getTagName()));
    }

    private List<Object> parseList(List<Element> children, GenericsDeclaration listType) {
        GenericsDeclaration itemType = (listType != null) ? listType.getSubtypeAtOrNull(0) : null;

        List<Object> list = new ArrayList<>();
        for (Element child : children) {
            list.add(this.parseValue(child, itemType));
        }
        return list;
    }

    private Map<String, Object> parseNestedMap(List<Element> children, GenericsDeclaration expectedType) {
        ConfigDeclaration nestedDecl = getConfigDeclaration(expectedType);
        GenericsDeclaration mapValueType = getMapValueType(expectedType);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Element child : children) {
            String key = getElementKey(child);
            GenericsDeclaration childType = this.resolveChildType(key, nestedDecl, mapValueType);
            result.put(key, this.parseValue(child, childType));
        }
        return result;
    }

    private GenericsDeclaration resolveChildType(String key, ConfigDeclaration nestedDecl, GenericsDeclaration mapValueType) {
        if (nestedDecl != null) {
            Optional<FieldDeclaration> field = nestedDecl.getField(key);
            if (field.isPresent()) {
                return field.get().getType();
            }
        }
        return mapValueType;
    }

    // ==================== Writing ====================

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        Document document = this.createDocument();

        this.writeHeaderComments(document, declaration);

        Element root = document.createElement(this.rootElement);
        document.appendChild(root);
        this.writeMap(document, root, this.map, declaration);

        String xml = this.transformToString(document);
        xml = this.postProcessHeaderComments(xml);

        outputStream.write(xml.getBytes(StandardCharsets.UTF_8));
    }

    private Document createDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    private void writeHeaderComments(Document document, ConfigDeclaration declaration) {
        String[] header = declaration.getHeader();
        if (header != null) {
            for (String line : header) {
                document.appendChild(document.createComment(" " + line + " "));
            }
        }
    }

    private String transformToString(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(this.indent));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(buffer));
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private String postProcessHeaderComments(String xml) {
        int configStart = xml.indexOf("<" + this.rootElement);
        if (configStart <= 0) {
            return xml;
        }

        String header = xml.substring(0, configStart);
        String body = xml.substring(configStart);

        // Add newlines between consecutive comments
        header = header.replace("--><!--", "-->\n<!--");

        // Add newline before root element if header ends with comment
        if (header.endsWith("-->")) {
            header = header + "\n";
        }

        return header + body;
    }

    private void writeMap(Document document, Element parent, Map<?, ?> map, ConfigDeclaration declaration) {
        this.writeMap(document, parent, map, declaration, false);
    }

    private void writeMap(Document document, Element parent, Map<?, ?> map, ConfigDeclaration declaration, boolean isMapValue) {
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            this.writeFieldComment(document, parent, declaration, key);

            Element element = this.createElement(document, key);
            // For Map<K, Config> values, declaration is already the value type - use it directly
            // Only pass for first entry to deduplicate comments
            ConfigDeclaration nestedDecl = isMapValue
                ? (first ? declaration : null)
                : (declaration != null ? declaration.resolveNestedDeclaration(key, value) : null);
            this.writeValue(document, element, value, nestedDecl);
            parent.appendChild(element);
            first = false;
        }
    }

    private void writeFieldComment(Document document, Element parent, ConfigDeclaration declaration, String key) {
        if (declaration == null) {
            return;
        }
        Optional<FieldDeclaration> field = declaration.getField(key);
        if (field.isPresent()) {
            String[] comment = field.get().getComment();
            if (comment != null) {
                for (String line : comment) {
                    // Skip empty/whitespace-only comments - XML has no concept of blank lines
                    // @Comment("") and @Comment(" ") are ignored for XML format
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    parent.appendChild(document.createComment(" " + line + " "));
                }
            }
        }
    }

    private Element createElement(Document document, String key) {
        if (isValidXmlName(key)) {
            return document.createElement(key);
        }
        Element element = document.createElement(ENTRY_ELEMENT);
        element.setAttribute(KEY_ATTRIBUTE, key);
        return element;
    }

    private void writeValue(Document document, Element element, Object value, ConfigDeclaration declaration) {
        if (value == null) {
            element.appendChild(document.createElement(NULL_ELEMENT));
        } else if (value instanceof Map) {
            // If declaration exists, we're writing Map<K, Config> entries where values are configs
            this.writeMap(document, element, (Map<?, ?>) value, declaration, declaration != null);
        } else if (value instanceof List) {
            this.writeList(document, element, (List<?>) value, declaration);
        } else {
            element.setTextContent(String.valueOf(value));
        }
    }

    private void writeList(Document document, Element parent, List<?> list, ConfigDeclaration declaration) {
        boolean first = true;
        for (Object item : list) {
            Element itemElement = document.createElement(ITEM_ELEMENT);
            // Show field comments only for first item (as template)
            this.writeValue(document, itemElement, item, first ? declaration : null);
            parent.appendChild(itemElement);
            first = false;
        }
    }

    // ==================== Utilities ====================

    private static List<Element> getChildElements(Element parent) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    private static String getElementKey(Element element) {
        return element.hasAttribute(KEY_ATTRIBUTE)
            ? element.getAttribute(KEY_ATTRIBUTE)
            : element.getTagName();
    }

    private static boolean isNullElement(List<Element> children) {
        return (children.size() == 1) && NULL_ELEMENT.equals(children.get(0).getTagName());
    }

    private static boolean isValidXmlName(String name) {
        return (name != null) && VALID_XML_NAME.matcher(name).matches();
    }

    private static boolean isCollectionType(GenericsDeclaration type) {
        return (type != null) && Collection.class.isAssignableFrom(type.getType());
    }

    private static boolean isMapOrConfigType(GenericsDeclaration type) {
        return (type != null) && (Map.class.isAssignableFrom(type.getType()) || type.isConfig());
    }

    private static GenericsDeclaration getFieldType(ConfigDeclaration declaration, String key) {
        if (declaration == null) {
            return null;
        }
        return declaration.getField(key)
            .map(FieldDeclaration::getType)
            .orElse(null);
    }

    private static ConfigDeclaration getConfigDeclaration(GenericsDeclaration type) {
        if ((type != null) && type.isConfig()) {
            return ConfigDeclaration.of(type.getType());
        }
        return null;
    }

    private static GenericsDeclaration getMapValueType(GenericsDeclaration type) {
        if ((type != null) && Map.class.isAssignableFrom(type.getType())) {
            return type.getSubtypeAtOrNull(1);
        }
        return null;
    }
}
