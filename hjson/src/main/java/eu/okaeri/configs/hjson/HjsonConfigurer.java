package eu.okaeri.configs.hjson;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import org.hjson.*;

import java.io.File;
import java.util.*;

public class HjsonConfigurer extends Configurer {

    private JsonObject json = new JsonObject();

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type, FieldDeclaration field) {

        Object simplified = this.simplify(value, type);
        JsonValue jsonValue = this.toJsonValue(simplified);

//        if ((field != null) && (field.getComment() != null)) {
//            jsonValue.setComment(String.join("\n", field.getComment()));
//        }

        this.json.set(key, jsonValue);
    }

    @Override
    public Object getValue(String key) {
        return this.fromJsonValue(this.json.get(key));
    }

    @Override
    public boolean keyExists(String key) {
        return this.json.has(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadFromFile(File file, ConfigDeclaration declaration) throws Exception {

        if (!file.exists()) {
            return;
        }

        String data = ConfigPostprocessor.of(file).read().getContext();
        this.json = JsonValue.readHjson(data).asObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeToFile(File file, ConfigDeclaration declaration) throws Exception {
        this.addComments(this.json, declaration, null);
        ConfigPostprocessor.of(file, this.json.toString(Stringify.HJSON_COMMENTS)).write();
    }

    private void addComments(Object object, ConfigDeclaration declaration, String key) {

        FieldDeclaration field = declaration.getField(key).orElse(null);

        if (object instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) object;
            jsonObject.names().forEach(name -> this.addComments(jsonObject.get(name), declaration, name));
        }

        if ((object instanceof JsonArray) && (field != null)) {
            GenericsDeclaration arrayType = field.getType().getSubtype().get(0);
            ConfigDeclaration configDeclaration = ConfigDeclaration.of(arrayType.getType());
            ((JsonArray) object).forEach(item -> this.addComments(item, configDeclaration, null));
        }

        if (field == null) {
            return;
        }

        String[] comment = field.getComment();
        JsonValue value = (JsonValue) object;

        if (comment == null) {
            return;
        }

        value.setComment(String.join("\n", comment));
    }

    @SuppressWarnings("unchecked")
    private JsonValue toJsonValue(Object object) {

        if (object == null) {
            return JsonValue.valueOf(null);
        }

        if (object instanceof Collection) {
            JsonArray array = new JsonArray();
            ((Collection<?>) object).forEach(item -> array.add(this.toJsonValue(item)));
            return array;
        }

        if (object instanceof Map) {
            JsonObject map = new JsonObject();
            ((Map<String, ?>) object).forEach((key, value) -> map.add(key, this.toJsonValue(value)));
            return map;
        }

        if (!(object instanceof String)) {
            throw new IllegalArgumentException("cannot transform non-string element: " + object + " [" + object.getClass() + "]");
        }

        return JsonValue.valueOf((String) object);
    }

    private Object fromJsonValue(JsonValue value) {

        if (value.isNull()) {
            return null;
        }

        if (value instanceof JsonArray) {
            List<Object> values = new ArrayList<>();
            JsonArray array = (JsonArray) value;
            array.forEach(item -> values.add(this.fromJsonValue(item)));
            return values;
        }

        if (value instanceof JsonObject) {
            Map<String, Object> map = new LinkedHashMap<>();
            JsonObject object = (JsonObject) value;
            object.names().forEach(name -> map.put(name, this.fromJsonValue(object.get(name))));
            return map;
        }

        return value.asString();
    }

    private void jsonValueWithComment(String key, JsonValue value, ConfigDeclaration declaration) {
        declaration.getField(key).ifPresent(field -> {
            String[] comment = field.getComment();
            if (comment == null) {
                return;
            }
            value.setComment(String.join("\n", comment));
        });
    }
}
