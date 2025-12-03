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

    @Setter private String commentPrefix = "# ";

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("hjson");
    }

    @Override
    public boolean isCommentLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith("/*");
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
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        String data = ConfigPostprocessor.of(inputStream).getContext();
        JsonObject json = JsonValue.readHjson(data).asObject();
        return this.jsonObjectToMap(json);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        JsonObject json = this.mapToJsonObject(data);

        // add comments to nodes
        this.addComments(json, declaration, null);

        // header
        String header = this.formatComment(declaration.getHeader());
        json.setFullComment(CommentType.BOL, header);

        // save
        ConfigPostprocessor.of(json.toString(Stringify.HJSON_COMMENTS)).write(outputStream);
    }

    @SuppressWarnings("unchecked")
    private JsonObject mapToJsonObject(Map<String, Object> map) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            json.add(entry.getKey(), this.toJsonValue(entry.getValue()));
        }
        return json;
    }

    private Map<String, Object> jsonObjectToMap(JsonObject json) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (JsonObject.Member member : json) {
            map.put(member.getName(), this.fromJsonValue(member.getValue()));
        }
        return map;
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

        String commentOrEmpty = this.formatComment(comment);
        value.setFullComment(CommentType.BOL, commentOrEmpty);
    }

    private String formatComment(String[] lines) {
        if ((lines == null) || (lines.length == 0)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            if (line.isEmpty()) {
                // @Comment("") -> empty line (space for HJSON compatibility)
                result.append(" \n");
            } else if (line.trim().isEmpty()) {
                // @Comment(" ") -> just #
                result.append(this.commentPrefix.trim()).append("\n");
            } else if (line.startsWith(this.commentPrefix.trim())) {
                result.append(line).append("\n");
            } else {
                result.append(this.commentPrefix).append(line).append("\n");
            }
        }
        return result.toString();
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
