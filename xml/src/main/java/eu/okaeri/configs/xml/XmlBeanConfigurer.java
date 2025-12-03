package eu.okaeri.configs.xml;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import lombok.NonNull;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * XML configurer using Java's built-in XMLEncoder/XMLDecoder.
 * <p>
 * This produces verbose XML that represents objects as method calls.
 * </p>
 * <p>
 * Example output:
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <java version="1.8.0" class="java.beans.XMLDecoder">
 *  <object class="java.util.LinkedHashMap">
 *   <void method="put">
 *    <string>name</string>
 *    <string>Example</string>
 *   </void>
 *   <void method="put">
 *    <string>count</string>
 *    <int>42</int>
 *   </void>
 *  </object>
 * </java>
 * }</pre>
 */
public class XmlBeanConfigurer extends Configurer {

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("xml");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        try (XMLDecoder decoder = new XMLDecoder(inputStream)) {
            Object decoded = decoder.readObject();
            if (decoded instanceof Map) {
                return (Map<String, Object>) decoded;
            } else {
                throw new IllegalStateException("XML root element must be a map structure, got: " +
                    (decoded == null ? "null" : decoded.getClass().getName()));
            }
        }
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        try (XMLEncoder encoder = new XMLEncoder(outputStream)) {
            encoder.writeObject(data);
        }
    }
}
