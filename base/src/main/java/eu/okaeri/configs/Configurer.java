package eu.okaeri.configs;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.*;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.TransformerRegistry;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public abstract class Configurer {

    private TransformerRegistry registry = new TransformerRegistry(); {
        this.registry.register(new DefaultSerdes());
    }

    public void setRegistry(TransformerRegistry registry) {
        this.registry = registry;
    }

    public TransformerRegistry getRegistry() {
        return this.registry;
    }

    public void register(OkaeriSerdesPack pack) {
        this.registry.register(pack);
    }

    public abstract String getCommentPrefix();

    public abstract String getSectionSeparator();

    public abstract void setValue(String key, Object value, GenericsDeclaration genericType);

    public abstract Object getValue(String key);

    public boolean isToStringObject(Object object) {
        if (object instanceof Class) {
            Class<?> clazzObject = (Class<?>) object;
            return clazzObject.isEnum() || this.registry.canTransform(clazzObject, String.class);
        }
        return object.getClass().isEnum() || this.isToStringObject(object.getClass());
    }

    @SuppressWarnings("unchecked")
    public Object simplifyCollection(Collection<?> value, GenericsDeclaration genericType) {

        List collection = new ArrayList();
        GenericsDeclaration collectionSubtype = (genericType == null) ? null : genericType.getSubtype().get(0);

        for (Object collectionElement : value) {
            collection.add(this.simplify(collectionElement, collectionSubtype));
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

        if (value == null) {
            return null;
        }

        Class<?> serializerType = (genericType != null) ? genericType.getType() : value.getClass();
        ObjectSerializer serializer = this.registry.getSerializer(serializerType);

        if (serializer == null) {

            if (this.isToStringObject(serializerType)) {
                return this.resolveType(value, genericType, String.class, null);
            }

            if (value instanceof Collection) {
                return this.simplifyCollection((Collection<?>) value, genericType);
            }

            if (value instanceof Map) {
                return this.simplifyMap((Map<Object, Object>) value, genericType);
            }

            throw new IllegalArgumentException("cannot simplify type " + serializerType + " (" + genericType + "): '" + value + "' [" + value.getClass() + "]");
            // return value; // - disallow unprocessed fallback (strict mode)
        }

        SerializationData serializationData = new SerializationData(this);
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
    public <T> T resolveType(Object object, Class<T> clazz, GenericsDeclaration genericTarget) {

        if (object == null) {
            return null;
        }

        return this.resolveType(object, new GenericsDeclaration(object.getClass()), clazz, genericTarget);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, Class<T> clazz, GenericsDeclaration genericTarget) {

        if (object == null) {
            return null;
        }

        GenericsDeclaration source = (genericSource == null) ? new GenericsDeclaration(object.getClass()) : genericSource;
        GenericsDeclaration target = (genericTarget == null) ? new GenericsDeclaration(clazz) : genericTarget;

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
        ObjectSerializer objectSerializer = this.registry.getSerializer(clazz);
        if ((object instanceof Map) && (objectSerializer != null)) {
            DeserializationData deserializationData = new DeserializationData((Map<String, Object>) object, this);
            Object deserialized = objectSerializer.deserialize(deserializationData, genericTarget);
            return clazz.cast(deserialized);
        }

        // generics
        if (genericTarget != null) {

            // collections
            if ((object instanceof Collection) && Collection.class.isAssignableFrom(clazz)) {

                Collection<?> sourceList = (Collection<?>) object;
                Collection<Object> targetList = (clazz == Set.class) ? new HashSet<>() : new ArrayList<>();
                GenericsDeclaration listDeclaration = genericTarget.getSubtype().get(0);

                for (Object item : sourceList) {
                    Object converted = this.resolveType(item, listDeclaration.getType(), listDeclaration);
                    targetList.add(converted);
                }

                return clazz.cast(targetList);
            }

            // maps
            if ((object instanceof Map) && (clazz == Map.class)) {

                Map<Object, Object> values = ((Map<Object, Object>) object);
                GenericsDeclaration keyDeclaration = genericTarget.getSubtype().get(0);
                GenericsDeclaration valueDeclaration = genericTarget.getSubtype().get(1);
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
        ObjectTransformer transformer = this.registry.getTransformer(source, target);
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
