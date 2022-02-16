package eu.okaeri.configs.xml.jaxb;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.*;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class XmlJaxbConfigurer extends Configurer {

    private final Map<String, Object> map = new LinkedHashMap<>();
    private final Marshaller marshaller;

    @SneakyThrows
    public XmlJaxbConfigurer() {
        JAXBContext context = JAXBContext.newInstance(XmlConfig.class, LinkedHashMap.class);
        this.marshaller = context.createMarshaller();
        this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        this.marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    }

    @Override
    public Object simplifyMap(@NonNull Map<Object, Object> value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        XmlSection entries = new XmlSection();
        GenericsDeclaration keyDeclaration = (genericType == null) ? null : genericType.getSubtypeAtOrNull(0);
        GenericsDeclaration valueDeclaration = (genericType == null) ? null : genericType.getSubtypeAtOrNull(1);

        boolean section = false;
        FieldDeclaration field = serdesContext.getField();

        if ((field != null) && (this.getRegistry().getSerializer(field.getType().getType()) != null)) {
            section = true;
        }

        for (Map.Entry<Object, Object> entry : value.entrySet()) {

            XmlSection xEntry = new XmlSection();
            Object key = this.simplify(entry.getKey(), keyDeclaration, serdesContext, false);
            Object kValue = this.simplify(entry.getValue(), valueDeclaration, serdesContext, conservative);

            if (section) {
                entries.put((String) key, kValue);
                continue;
            }

            xEntry.put("key", key);
            xEntry.put("value", kValue);
            entries.put("entry", xEntry);
        }

        XmlSection root = this.newRoot(serdesContext);
        root.put(section ? "data" : "entries", entries);

        return root;
    }

    @Override
    public Object simplifyCollection(@NonNull Collection<?> value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        XmlSection values = new XmlSection();
        GenericsDeclaration collectionSubtype = (genericType == null) ? null : genericType.getSubtypeAtOrNull(0);

        for (Object collectionElement : value) {
            Object simplified = this.simplify(collectionElement, collectionSubtype, serdesContext, conservative);
            values.put("value", simplified);
        }

        XmlSection root = this.newRoot(serdesContext);
        root.put("values", values);

        return root;
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.map.put(key, simplified);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
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
    public void write(OutputStream outputStream, ConfigDeclaration declaration) throws Exception {
        XmlConfig data = new XmlConfig();
        this.map.forEach(data::put);
        this.marshaller.marshal(data, outputStream);
    }

    @Override
    public void load(InputStream inputStream, ConfigDeclaration declaration) throws Exception {
        // TODO
    }

    private XmlSection newRoot(SerdesContext serdesContext) {

        if (serdesContext.getField() == null) {
            return new XmlSection();
        }

        XmlSection commentRoot = new XmlSection();
        for (String line : serdesContext.getField().getComment()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            commentRoot.put("line", trimmed);
        }

        XmlSection root = new XmlSection();
        root.put("comment", commentRoot);
        return root;
    }

    @XmlRootElement(name = "data")
    static class XmlConfig extends XmlSection {
    }

    @XmlType
    @NoArgsConstructor
    static class XmlSection {

        @Setter
        @Getter(onMethod = @__({@XmlAnyElement}))
        private List<JAXBElement> properties = new ArrayList<>();

        @SuppressWarnings("unchecked")
        public void put(String key, Object value) {
            this.properties.add(new JAXBElement(new QName(key), (value == null) ? String.class : value.getClass(), value));
        }
    }
}
