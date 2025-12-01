package eu.okaeri.configs.xml;

import eu.okaeri.configs.configurer.Configurer;
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
 * Lists use repeated item elements.
 * </p>
 * <p>
 * Example output:
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
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
    private static final String ROOT_ELEMENT = "config";
    private static final String ENTRY_ELEMENT = "entry";
    private static final String ITEM_ELEMENT = "item";
    private static final String KEY_ATTRIBUTE = "key";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String NULL_VALUE = "__null__";

    private Map<String, Object> map = new LinkedHashMap<>();

    private @Setter int indent = 2;

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

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);

        Element root = document.getDocumentElement();
        if (root == null) {
            throw new IllegalStateException("XML document has no root element");
        }

        Object parsed = this.parseElement(root);
        if (parsed instanceof Map) {
            //noinspection unchecked
            this.map = (Map<String, Object>) parsed;
        } else {
            throw new IllegalStateException("XML root element must be a map structure, got: " +
                (parsed == null ? "null" : parsed.getClass().getName()));
        }
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        // Add header comments before root element
        String[] header = declaration.getHeader();
        if (header != null) {
            for (String line : header) {
                Comment comment = document.createComment(" " + line + " ");
                document.appendChild(comment);
            }
        }

        Element root = document.createElement(ROOT_ELEMENT);
        document.appendChild(root);

        this.writeMap(document, root, this.map, declaration);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(this.indent));

        // Transform to byte array first for post-processing
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(buffer);
        transformer.transform(source, result);

        // Post-process: add newlines after header comments (only before <config> to avoid modifying values)
        String xml = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        int configStart = xml.indexOf("<" + ROOT_ELEMENT);
        if (configStart > 0) {
            String headerPart = xml.substring(0, configStart);
            String bodyPart = xml.substring(configStart);
            headerPart = headerPart.replace("--><!--", "-->\n<!--");
            // Add newline before <config> if header ends with comment
            if (headerPart.endsWith("-->")) {
                headerPart = headerPart + "\n";
            }
            xml = headerPart + bodyPart;
        }

        outputStream.write(xml.getBytes(StandardCharsets.UTF_8));
    }

    private void writeMap(Document document, Element parent, Map<?, ?> map, ConfigDeclaration declaration) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            // Add field comment if available
            if (declaration != null) {
                Optional<FieldDeclaration> field = declaration.getField(key);
                if (field.isPresent()) {
                    String[] comment = field.get().getComment();
                    if (comment != null) {
                        for (String line : comment) {
                            Comment xmlComment = document.createComment(" " + line + " ");
                            parent.appendChild(xmlComment);
                        }
                    }
                }
            }

            Element element;
            if (this.isValidXmlName(key)) {
                element = document.createElement(key);
            } else {
                element = document.createElement(ENTRY_ELEMENT);
                element.setAttribute(KEY_ATTRIBUTE, key);
            }

            // Get nested declaration for subconfigs or list element type
            ConfigDeclaration nestedDeclaration = null;
            if (declaration != null) {
                Optional<FieldDeclaration> field = declaration.getField(key);
                if (field.isPresent()) {
                    GenericsDeclaration fieldType = field.get().getType();
                    if (fieldType.isConfig()) {
                        // Direct subconfig
                        nestedDeclaration = ConfigDeclaration.of(fieldType.getType());
                    } else if (value instanceof List) {
                        // List of configs - pass element type declaration
                        GenericsDeclaration elementType = fieldType.getSubtypeAtOrNull(0);
                        if ((elementType != null) && elementType.isConfig()) {
                            nestedDeclaration = ConfigDeclaration.of(elementType.getType());
                        }
                    }
                }
            }

            this.writeValue(document, element, value, nestedDeclaration);
            parent.appendChild(element);
        }
    }

    private void writeValue(Document document, Element element, Object value, ConfigDeclaration declaration) {
        if (value == null) {
            element.setAttribute(TYPE_ATTRIBUTE, "null");
            return;
        }

        if (value instanceof Map) {
            element.setAttribute(TYPE_ATTRIBUTE, "map");
            this.writeMap(document, element, (Map<?, ?>) value, declaration);
            return;
        }

        if (value instanceof List) {
            element.setAttribute(TYPE_ATTRIBUTE, "list");
            boolean first = true;
            for (Object item : (List<?>) value) {
                Element itemElement = document.createElement(ITEM_ELEMENT);
                // Use declaration only for first item (to show field comments as template)
                this.writeValue(document, itemElement, item, first ? declaration : null);
                element.appendChild(itemElement);
                first = false;
            }
            return;
        }

        // Everything else is dumped as string - okaeri-configs handles type resolution
        element.setTextContent(String.valueOf(value));
    }

    private Object parseElement(Element element) {
        // Check for null type
        String type = element.getAttribute(TYPE_ATTRIBUTE);
        if ("null".equals(type)) {
            return null;
        }

        // Check for explicit list type (handles empty lists)
        if ("list".equals(type)) {
            NodeList children = element.getChildNodes();
            List<Element> childElements = this.getChildElements(children);
            List<Object> list = new ArrayList<>();
            for (Element child : childElements) {
                list.add(this.parseElement(child));
            }
            return list;
        }

        // Check for explicit map type (handles empty maps)
        if ("map".equals(type)) {
            NodeList children = element.getChildNodes();
            List<Element> childElements = this.getChildElements(children);
            Map<String, Object> result = new LinkedHashMap<>();
            for (Element child : childElements) {
                String key = child.hasAttribute(KEY_ATTRIBUTE)
                    ? child.getAttribute(KEY_ATTRIBUTE)
                    : child.getTagName();
                result.put(key, this.parseElement(child));
            }
            return result;
        }

        NodeList children = element.getChildNodes();
        List<Element> childElements = this.getChildElements(children);

        // If no child elements, it's a primitive value (returned as string)
        if (childElements.isEmpty()) {
            return element.getTextContent();
        }

        // Check if it's a list (all children are "item" elements)
        boolean isList = childElements.stream().allMatch(e -> ITEM_ELEMENT.equals(e.getTagName()));

        if (isList) {
            List<Object> list = new ArrayList<>();
            for (Element child : childElements) {
                list.add(this.parseElement(child));
            }
            return list;
        }

        // Otherwise it's a map
        Map<String, Object> result = new LinkedHashMap<>();
        for (Element child : childElements) {
            String key = child.hasAttribute(KEY_ATTRIBUTE)
                ? child.getAttribute(KEY_ATTRIBUTE)
                : child.getTagName();
            result.put(key, this.parseElement(child));
        }
        return result;
    }

    private List<Element> getChildElements(NodeList nodes) {
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    private boolean isValidXmlName(String name) {
        return (name != null) && VALID_XML_NAME.matcher(name).matches();
    }
}
