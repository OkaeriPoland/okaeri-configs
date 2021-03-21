package eu.okaeri.configs;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.transformer.ObjectTransformer;
import eu.okaeri.configs.transformer.TransformerRegistry;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public abstract class Configurer {

    public abstract String getCommentPrefix();

    public abstract String getSectionSeparator();

    public abstract void setValue(String key, Object value, GenericsDeclaration genericType);

    public abstract Object getValue(String key);

    public boolean isToStringObject(Object object) {
        return object.getClass().isEnum() || TransformerRegistry.canTransform(object.getClass(), String.class);
    }

    @SuppressWarnings("unchecked")
    public Object simplifyCollection(Collection<?> value, GenericsDeclaration genericType) {

        List collection = new ArrayList();
        GenericsDeclaration collectionSubtype = (genericType == null) ? null : genericType.getSubtype().get(0);

        for (Object collectionElement : value) {

            SerializationData serialized = TransformerRegistry.serializeOrNull(collectionElement);
            if (serialized == null) {
                collection.add(this.simplify(collectionElement, collectionSubtype));
                continue;
            }

            collection.add(serialized.asMap());
        }

        return collection;
    }

    @SuppressWarnings("unchecked")
    public Object simplifyMap(Map<Object, Object> value, GenericsDeclaration genericType) {

        Map<Object, Object> map = new LinkedHashMap<>();
        GenericsDeclaration keyDeclaration = (genericType == null) ? null : genericType.getSubtype().get(0);
        GenericsDeclaration valueDeclaration = (genericType == null) ? null : genericType.getSubtype().get(1);

        for (Map.Entry<Object, Object> entry : value.entrySet()) {
            Object key = this.simplify(entry.getKey(), keyDeclaration);
            Object kValue = this.simplify(entry.getValue(), valueDeclaration);
            map.put(key, kValue);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    public Object simplify(Object value, GenericsDeclaration genericType) {

        ObjectSerializer serializer = TransformerRegistry.getSerializer(value.getClass());
        if (serializer == null) {

            if (this.isToStringObject(value)) {
                return this.resolveType(value, String.class, null);
            }

            if (value instanceof Collection) {
                return this.simplifyCollection((Collection<?>) value, genericType);
            }

            if (value instanceof Map) {
                return this.simplifyMap((Map<Object, Object>) value, genericType);
            }

            return value;
        }

        SerializationData serializationData = new SerializationData();
        serializer.serialize(value, serializationData);

        return serializationData.asMap();
    }

    public <T> T getValue(String key, Class<T> clazz, GenericsDeclaration genericType) {
        Object value = this.getValue(key);
        if (value == null) return null;
        return this.resolveType(value, clazz, genericType);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, Class<T> clazz, GenericsDeclaration genericType) {

        GenericsDeclaration source = new GenericsDeclaration(object.getClass());
        GenericsDeclaration target = (genericType == null) ? new GenericsDeclaration(clazz) : genericType;

        // enums
        if ((object instanceof String) && clazz.isEnum()) {
            String strObject = (String) object;
            Method enumMethod = clazz.getMethod("valueOf", String.class);
            return clazz.cast(enumMethod.invoke(null, strObject));
        }

        if (object.getClass().isEnum() && (clazz == String.class)) {
            Method enumMethod = object.getClass().getMethod("name");
            return clazz.cast(enumMethod.invoke(object));
        }

        // deserialization
        ObjectSerializer objectSerializer = TransformerRegistry.getSerializer(clazz);
        if ((object instanceof Map) && (objectSerializer != null)) {
            DeserializationData deserializationData = new DeserializationData((Map<String, Object>) object);
            Object deserialized = objectSerializer.deserialize(deserializationData, genericType);
            return clazz.cast(deserialized);
        }

        // generics
        if (genericType != null) {

            // collections
            if ((object instanceof Collection) && (clazz == List.class)) {

                Collection<?> sourceList = (Collection<?>) object;
                List<Object> targetList = new ArrayList<>();
                GenericsDeclaration listDeclaration = genericType.getSubtype().get(0);

                for (Object item : sourceList) {
                    Object converted = this.resolveType(item, listDeclaration.getType(), listDeclaration);
                    targetList.add(converted);
                }

                return clazz.cast(targetList);
            }

            if ((object instanceof Collection) && (clazz == Set.class)) {

                Collection<?> sourceList = (Collection<?>) object;
                Set<Object> targetList = new HashSet<>();
                GenericsDeclaration listDeclaration = genericType.getSubtype().get(0);

                for (Object item : sourceList) {
                    Object converted = this.resolveType(item, listDeclaration.getType(), listDeclaration);
                    targetList.add(converted);
                }

                return clazz.cast(targetList);
            }

            // maps
            if ((object instanceof Map) && (clazz == Map.class)) {

                Map<Object, Object> values = ((Map<Object, Object>) object);
                GenericsDeclaration keyDeclaration = genericType.getSubtype().get(0);
                GenericsDeclaration valueDeclaration = genericType.getSubtype().get(1);
                Map<Object, Object> map = new LinkedHashMap<>();

                for (Map.Entry<Object, Object> entry : values.entrySet()) {
                    Object key = this.resolveType(entry.getKey(), keyDeclaration.getType(), keyDeclaration);
                    Object value = this.resolveType(entry.getValue(), valueDeclaration.getType(), valueDeclaration);
                    map.put(key, value);
                }

                return clazz.cast(map);
            }
        }

        // basic transformer
        ObjectTransformer transformer = TransformerRegistry.getTransformer(source, target);
        if (transformer == null) {
            return clazz.cast(object);
        }

        //noinspection unchecked
        return clazz.cast(transformer.transform(object));
    }

    public boolean keyExists(String key) {
        return this.getValue(key) != null;
    }

    public abstract void writeToFile(File file, ConfigDeclaration declaration) throws IOException;

    public abstract void loadFromFile(File file, ConfigDeclaration declaration) throws IOException;
}
