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
    @SuppressWarnings("unchecked")
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        String data = ConfigPostprocessor.of(inputStream).getContext();
        return (Map<String, Object>) new JSONParser().parse(data, CONTAINER_FACTORY);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        Map<Object, Object> cleanedMap = this.removeNullsRecursively(data);
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
            if (item instanceof Map) {
                result.add(this.removeNullsRecursively((Map<?, ?>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }
}
