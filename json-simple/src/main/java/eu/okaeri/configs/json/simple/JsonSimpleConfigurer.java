package eu.okaeri.configs.json.simple;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class JsonSimpleConfigurer extends Configurer {

    private static final ContainerFactory CONTAINER_FACTORY = new ContainerFactory() {

        @Override
        public Map createObjectContainer() {
            return new LinkedHashMap<>();
        }

        @Override
        public List creatArrayContainer() {
            return new ArrayList<>();
        }
    };

    private Map<String, Object> map;
    private JSONParser parser;

    public JsonSimpleConfigurer() {
        this.parser = new JSONParser();
        this.map = new LinkedHashMap<>();
    }

    public JsonSimpleConfigurer(@NonNull JSONParser parser) {
        this(parser, new LinkedHashMap<>());
    }

    public JsonSimpleConfigurer(@NonNull JSONParser parser, @NonNull Map<String, Object> map) {
        this.parser = parser;
        this.map = map;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("json");
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        if (value == null) {
            return null;
        }

        GenericsDeclaration genericsDeclaration = GenericsDeclaration.of(value);
        if ((genericsDeclaration.getType() == char.class) || (genericsDeclaration.getType() == Character.class)) {
            return super.simplify(value, genericType, serdesContext, false);
        }

        return super.simplify(value, genericType, serdesContext, conservative);
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        if (value == null) {
            this.map.remove(key);
            return;
        }
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

        String data = ConfigPostprocessor.of(inputStream).getContext();
        this.map = (Map<String, Object>) this.parser.parse(data, CONTAINER_FACTORY);

        if (this.map != null) {
            return;
        }

        this.map = new LinkedHashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        Map<Object, Object> cleanedMap = this.removeNullsRecursively(this.map);
        String jsonString = JSONObject.toJSONString(cleanedMap);
        ConfigPostprocessor.of(jsonString).write(outputStream);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> removeNullsRecursively(Map<?, ?> map) {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof Map) {
                result.put(entry.getKey(), this.removeNullsRecursively((Map<?, ?>) value));
            } else if (value instanceof List) {
                result.put(entry.getKey(), this.removeNullsFromList((List<?>) value));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<?> removeNullsFromList(List<?> list) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            if (item instanceof Map) {
                result.add(this.removeNullsRecursively((Map<?, ?>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }
}
