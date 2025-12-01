package eu.okaeri.configs.xml;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
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

    private Map<String, Object> map = new LinkedHashMap<>();

    public XmlBeanConfigurer() {
    }

    public XmlBeanConfigurer(@NonNull Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("xml");
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
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
    @SuppressWarnings("unchecked")
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        try (XMLDecoder decoder = new XMLDecoder(inputStream)) {
            Object decoded = decoder.readObject();
            if (decoded instanceof Map) {
                this.map = (Map<String, Object>) decoded;
            } else {
                throw new IllegalStateException("XML root element must be a map structure, got: " +
                    (decoded == null ? "null" : decoded.getClass().getName()));
            }
        }
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        try (XMLEncoder encoder = new XMLEncoder(outputStream)) {
            encoder.writeObject(this.map);
        }
    }
}
