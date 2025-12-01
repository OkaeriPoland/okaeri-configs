package eu.okaeri.configs.hjson;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.hjson.*;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Accessors(chain = true)
public class HjsonConfigurer extends Configurer {

    private JsonObject json = new JsonObject();
    @Setter private String commentPrefix = "# ";

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("hjson");
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

        // long/Long values outside double precision range must be converted to String
        // (HJSON library stores all numbers as double, which can only represent integers up to 2^53 precisely)
        if ((genericsDeclaration.getType() == long.class) || (genericsDeclaration.getType() == Long.class)) {
            long longValue = (Long) value;
            // Double can precisely represent integers in range -(2^53) to 2^53
            long maxSafeInteger = 1L << 53; // 9007199254740992
            if ((longValue > maxSafeInteger) || (longValue < -maxSafeInteger)) {
                return super.simplify(value, genericType, serdesContext, false);
            }
        }

        return super.simplify(value, genericType, serdesContext, conservative);
    }

    @Override
    public Object simplifyMap(@NonNull Map<Object, Object> value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        Map<Object, Object> map = new LinkedHashMap<>();
        GenericsDeclaration keyDeclaration = (genericType == null) ? null : genericType.getSubtypeAtOrNull(0);
        GenericsDeclaration valueDeclaration = (genericType == null) ? null : genericType.getSubtypeAtOrNull(1);

        for (Map.Entry<Object, Object> entry : value.entrySet()) {
            Object key = this.simplify(entry.getKey(), keyDeclaration, serdesContext, false);
            Object kValue = this.simplify(entry.getValue(), valueDeclaration, serdesContext, conservative);
            map.put(key, kValue);
        }

        return map;
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        JsonValue jsonValue = this.toJsonValue(simplified);
        this.json.set(key, jsonValue);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.json.set(key, (JsonValue) value);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.fromJsonValue(this.json.get(key));
    }

    @Override
    public Object getValueUnsafe(@NonNull String key) {
        return this.json.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {
        return this.json.remove(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.json.has(key);
    }

    @Override
    public List<String> getAllKeys() {
        List<String> keys = new ArrayList<>();
        for (JsonObject.Member member : this.json) {
            keys.add(member.getName());
        }
        return Collections.unmodifiableList(keys);
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        String data = ConfigPostprocessor.of(inputStream).getContext();
        this.json = JsonValue.readHjson(data).asObject();
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        // add comments to nodes
        this.addComments(this.json, declaration, null);

        // header
        String header = ConfigPostprocessor.createCommentOrEmpty(this.commentPrefix, declaration.getHeader());
        this.json.setFullComment(CommentType.BOL, header);

        // save
        ConfigPostprocessor.of(this.json.toString(Stringify.HJSON_COMMENTS)).write(outputStream);
    }

    private void addComments(Object object, ConfigDeclaration declaration, String key) {

        FieldDeclaration field = (key != null)
            ? declaration.getField(key).orElse(null)
            : null;

        if (object instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) object;
            // root
            if (field == null) {
                jsonObject.names().forEach(name -> {
                    JsonValue value = jsonObject.get(name);
                    this.addComments(value, declaration, name);
                });
            }
            // sub
            else if (field.getType().isConfig()) {
                jsonObject.names().forEach(name -> {
                    ConfigDeclaration configDeclaration = ConfigDeclaration.of(field.getType().getType());
                    this.addComments(jsonObject.get(name), configDeclaration, name);
                });
            }
        }

        if ((object instanceof JsonArray) && (field != null)) {
            GenericsDeclaration arrayType = field.getType().getSubtypeAtOrNull(0);
            if (arrayType.isConfig()) {
                ConfigDeclaration configDeclaration = ConfigDeclaration.of(arrayType.getType());
                JsonArray array = (JsonArray) object;
                if (!array.isEmpty()) {
                    this.addComments(array.get(0), configDeclaration, null);
                }
            }
        }

        JsonValue value = (JsonValue) object;
        if (field == null) {
            return;
        }

        String[] comment = field.getComment();
        if (comment == null) {
            return;
        }

        String commentOrEmpty = ConfigPostprocessor.createCommentOrEmpty(this.commentPrefix, comment);
        value.setFullComment(CommentType.BOL, commentOrEmpty);
    }

    @SuppressWarnings("unchecked")
    private JsonValue toJsonValue(Object object) {

        if (object == null) {
            return JsonValue.valueOf(null);
        }

        if (object instanceof String) {
            return JsonValue.valueOf((String) object);
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

        if ((object instanceof Number) || (object instanceof Boolean)) {
            return JsonValue.valueOf(object);
        }

        throw new IllegalArgumentException("cannot transform element: " + object + " [" + object.getClass() + "]");
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

        return value.asRaw();
    }
}
