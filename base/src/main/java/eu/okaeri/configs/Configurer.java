package eu.okaeri.configs;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.*;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.TransformerRegistry;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public Object simplify(Object value, GenericsDeclaration genericType) {

        if (value == null) {
            return null;
        }

        if (OkaeriConfig.class.isAssignableFrom(value.getClass())) {
            OkaeriConfig config = (OkaeriConfig) value;
            return config.asMap(this);
        }

        Class<?> serializerType = (genericType != null) ? genericType.getType() : value.getClass();
        ObjectSerializer serializer = this.registry.getSerializer(serializerType);

        if (serializer == null) {

            if (serializerType.isPrimitive()) {
                Class<?> wrappedPrimitive = GenericsDeclaration.of(serializerType).wrap();
                return this.simplify(wrappedPrimitive.cast(value), GenericsDeclaration.of(wrappedPrimitive));
            }

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
        return this.resolveType(value, GenericsDeclaration.of(value), clazz, genericType);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, Class<T> targetClazz, GenericsDeclaration genericTarget) {

        if (object == null) {
            return null;
        }

        GenericsDeclaration source = (genericSource == null) ? GenericsDeclaration.of(object) : genericSource;
        GenericsDeclaration target = (genericTarget == null) ? GenericsDeclaration.of(targetClazz) : genericTarget;

        // primitives
        if (target.isPrimitive()) {
            target = GenericsDeclaration.of(target.wrap());
        }

        // enums
        if ((object instanceof String) && targetClazz.isEnum()) {
            String strObject = (String) object;
            Method enumMethod = targetClazz.getMethod("valueOf", String.class);
            return targetClazz.cast(enumMethod.invoke(null, strObject));
        }

        Class<?> objectClazz = object.getClass();
        if (objectClazz.isEnum() && (targetClazz == String.class)) {
            Method enumMethod = objectClazz.getMethod("name");
            return targetClazz.cast(enumMethod.invoke(object));
        }

        // subconfig
        if (OkaeriConfig.class.isAssignableFrom(targetClazz)) {

            OkaeriConfig config;
            try {
                config = (OkaeriConfig) targetClazz.newInstance();
            } catch (InstantiationException | IllegalAccessException exception) {
                Class<?> unsafeClazz = Class.forName("sun.misc.Unsafe");
                Field theUnsafeField = unsafeClazz.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Object unsafeInstance = theUnsafeField.get(null);
                Method allocateInstance = unsafeClazz.getDeclaredMethod("allocateInstance", Class.class);
                config = ((OkaeriConfig) allocateInstance.invoke(unsafeInstance, targetClazz)).updateDeclaration();
            }

            Map configMap = this.resolveType(object, source, Map.class, GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Object.class)));
            config.setConfigurer(this);
            config.update(configMap);

            return (T) config;
        }

        // deserialization
        ObjectSerializer objectSerializer = this.registry.getSerializer(targetClazz);
        if ((object instanceof Map) && (objectSerializer != null)) {
            DeserializationData deserializationData = new DeserializationData((Map<String, Object>) object, this);
            Object deserialized = objectSerializer.deserialize(deserializationData, genericTarget);
            return targetClazz.cast(deserialized);
        }

        // generics
        if (genericTarget != null) {

            // collections
            if ((object instanceof Collection) && Collection.class.isAssignableFrom(targetClazz)) {

                Collection<?> sourceList = (Collection<?>) object;
                Collection<Object> targetList = (Collection<Object>) this.createInstance(targetClazz);
                GenericsDeclaration listDeclaration = genericTarget.getSubtype().get(0);

                for (Object item : sourceList) {
                    Object converted = this.resolveType(item, GenericsDeclaration.of(item), listDeclaration.getType(), listDeclaration);
                    targetList.add(converted);
                }

                return targetClazz.cast(targetList);
            }

            // maps
            if ((object instanceof Map) && Map.class.isAssignableFrom(targetClazz)) {

                Map<Object, Object> values = ((Map<Object, Object>) object);
                GenericsDeclaration keyDeclaration = genericTarget.getSubtype().get(0);
                GenericsDeclaration valueDeclaration = genericTarget.getSubtype().get(1);
                Map<Object, Object> map = (Map<Object, Object>) this.createInstance(targetClazz);

                for (Map.Entry<Object, Object> entry : values.entrySet()) {
                    Object key = this.resolveType(entry.getKey(), GenericsDeclaration.of(entry.getKey()), keyDeclaration.getType(), keyDeclaration);
                    Object value = this.resolveType(entry.getValue(), GenericsDeclaration.of(entry.getValue()), valueDeclaration.getType(), valueDeclaration);
                    map.put(key, value);
                }

                return targetClazz.cast(map);
            }
        }

        // basic transformer
        ObjectTransformer transformer = this.registry.getTransformer(source, target);
        if (transformer == null) {
            return targetClazz.cast(object);
        }

        // primitives
        if (targetClazz.isPrimitive()) {
            Object transformed = transformer.transform(object);
            return (T) GenericsDeclaration.of(targetClazz).unwrapValue(transformed);
        }

        return targetClazz.cast(transformer.transform(object));
    }

    public Object createInstance(Class<?> clazz) {
        try {
            if (Collection.class.isAssignableFrom(clazz)) {
                if (clazz == Set.class) return new HashSet<>();
                if (clazz == List.class) return new ArrayList<>();
                return clazz.newInstance();
            }

            if (Map.class.isAssignableFrom(clazz)) {
                if (clazz == Map.class) return new LinkedHashMap<>();
                return clazz.newInstance();
            }

            throw new IllegalArgumentException("cannot create instance of " + clazz);
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("failed to create instance of " + clazz, exception);
        }
    }

    public boolean keyExists(String key) {
        return this.getValue(key) != null;
    }

    public abstract void writeToFile(File file, ConfigDeclaration declaration) throws IOException;

    public abstract void loadFromFile(File file, ConfigDeclaration declaration) throws IOException;
}
