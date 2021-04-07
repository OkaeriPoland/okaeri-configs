package eu.okaeri.configs.configurer;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.*;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Configurer {

    @Getter
    @Setter
    private OkaeriConfig parent;

    private TransformerRegistry registry = new TransformerRegistry();
    {
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

    public abstract void setValue(String key, Object value, GenericsDeclaration genericType, FieldDeclaration field);

    public abstract Object getValue(String key);

    public boolean isToStringObject(Object object) {
        if (object instanceof Class) {
            Class<?> clazzObject = (Class<?>) object;
            return clazzObject.isEnum() || this.registry.canTransform(clazzObject, String.class);
        }
        return object.getClass().isEnum() || this.isToStringObject(object.getClass());
    }

    @SuppressWarnings("unchecked")
    public Object simplifyCollection(Collection<?> value, GenericsDeclaration genericType, boolean conservative) throws OkaeriException {

        List collection = new ArrayList();
        GenericsDeclaration collectionSubtype = (genericType == null) ? null : genericType.getSubtype().get(0);

        for (Object collectionElement : value) {
            collection.add(this.simplify(collectionElement, collectionSubtype, conservative));
        }

        return collection;
    }

    @SuppressWarnings("unchecked")
    public Object simplifyMap(Map<Object, Object> value, GenericsDeclaration genericType, boolean conservative) throws OkaeriException {

        Map<Object, Object> map = new LinkedHashMap<>();
        GenericsDeclaration keyDeclaration = (genericType == null) ? null : genericType.getSubtype().get(0);
        GenericsDeclaration valueDeclaration = (genericType == null) ? null : genericType.getSubtype().get(1);

        for (Map.Entry<Object, Object> entry : value.entrySet()) {
            Object key = this.simplify(entry.getKey(), keyDeclaration, conservative);
            Object kValue = this.simplify(entry.getValue(), valueDeclaration, conservative);
            map.put(key, kValue);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    public Object simplify(Object value, GenericsDeclaration genericType, boolean conservative) throws OkaeriException {

        if (value == null) {
            return null;
        }

        if (OkaeriConfig.class.isAssignableFrom(value.getClass())) {
            OkaeriConfig config = (OkaeriConfig) value;
            return config.asMap(this, conservative);
        }

        Class<?> serializerType = (genericType != null) ? genericType.getType() : value.getClass();
        ObjectSerializer serializer = this.registry.getSerializer(serializerType);

        if (serializer == null) {

            if (conservative && (serializerType.isPrimitive() || GenericsDeclaration.of(serializerType).isPrimitiveWrapper())) {
                return value;
            }

            if (serializerType.isPrimitive()) {
                Class<?> wrappedPrimitive = GenericsDeclaration.of(serializerType).wrap();
                return this.simplify(wrappedPrimitive.cast(value), GenericsDeclaration.of(wrappedPrimitive), conservative);
            }

            if (this.isToStringObject(serializerType)) {
                return this.resolveType(value, genericType, String.class, null);
            }

            if (value instanceof Collection) {
                return this.simplifyCollection((Collection<?>) value, genericType, conservative);
            }

            if (value instanceof Map) {
                return this.simplifyMap((Map<Object, Object>) value, genericType, conservative);
            }

            throw new OkaeriException("cannot simplify type " + serializerType + " (" + genericType + "): '" + value + "' [" + value.getClass() + "]");
        }

        SerializationData serializationData = new SerializationData(this);
        serializer.serialize(value, serializationData);
        Map<String, Object> serializationMap = serializationData.asMap();

        if (!conservative) {
            Map<String, Object> newSerializationMap = new LinkedHashMap<>();
            serializationMap.forEach((mKey, mValue) -> newSerializationMap.put(mKey, this.simplify(mValue, GenericsDeclaration.of(mValue), false)));
            return newSerializationMap;
        }

        return serializationMap;
    }

    public <T> T getValue(String key, Class<T> clazz, GenericsDeclaration genericType) {
        Object value = this.getValue(key);
        if (value == null) return null;
        return this.resolveType(value, GenericsDeclaration.of(value), clazz, genericType);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, Class<T> targetClazz, GenericsDeclaration genericTarget) throws OkaeriException {

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
        Class<?> objectClazz = object.getClass();
        try {
            if ((object instanceof String) && targetClazz.isEnum()) {
                String strObject = (String) object;
                // 1:1 match ONE=ONE
                try {
                    Method enumMethod = targetClazz.getMethod("valueOf", String.class);
                    Object enumValue = enumMethod.invoke(null, strObject);
                    if (enumValue != null) {
                        return targetClazz.cast(enumValue);
                    }
                }
                // match first case-insensitive
                catch (InvocationTargetException ignored) {
                    Enum[] enumValues = (Enum[]) targetClazz.getEnumConstants();
                    for (Enum value : enumValues) {
                        if (!strObject.equalsIgnoreCase(value.name())) {
                            continue;
                        }
                        return targetClazz.cast(value);
                    }
                }
                // match fail
                String enumValuesStr = Arrays.stream(targetClazz.getEnumConstants()).map(item -> ((Enum) item).name()).collect(Collectors.joining(", "));
                throw new IllegalArgumentException("no enum value for name " + strObject + " (available: " + enumValuesStr + ")");
            }
            if (objectClazz.isEnum() && (targetClazz == String.class)) {
                Method enumMethod = objectClazz.getMethod("name");
                return targetClazz.cast(enumMethod.invoke(object));
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new OkaeriException("failed to resolve enum " + object.getClass() + " <> " + targetClazz, exception);
        }

        // subconfig
        if (OkaeriConfig.class.isAssignableFrom(targetClazz)) {
            OkaeriConfig config = ConfigManager.create((Class<? extends OkaeriConfig>) targetClazz);
            Map configMap = this.resolveType(object, source, Map.class, GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Object.class)));
            config.setConfigurer(new InMemoryWrappedConfigurer(this, configMap));
            return (T) config.update();
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

            // unbox primitive (Integer -> int)
            if (targetClazz.isPrimitive() && GenericsDeclaration.doBoxTypesMatch(targetClazz, objectClazz)) {
                GenericsDeclaration primitiveDeclaration = GenericsDeclaration.of(object);
                return (T) primitiveDeclaration.unwrapValue(object);
            }

            // transform primitives/primitive wrappers through String (int -> long)
            if (targetClazz.isPrimitive() || GenericsDeclaration.of(targetClazz).isPrimitiveWrapper()) {
                Object simplified = this.simplify(object, GenericsDeclaration.of(objectClazz), false);
                return this.resolveType(simplified, GenericsDeclaration.of(simplified), targetClazz, GenericsDeclaration.of(targetClazz));
            }

            return targetClazz.cast(object);
        }

        // primitives transformer
        if (targetClazz.isPrimitive()) {
            Object transformed = transformer.transform(object);
            return (T) GenericsDeclaration.of(targetClazz).unwrapValue(transformed);
        }

        return targetClazz.cast(transformer.transform(object));
    }

    public Object createInstance(Class<?> clazz) throws OkaeriException {
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

            throw new OkaeriException("cannot create instance of " + clazz);
        } catch (Exception exception) {
            throw new OkaeriException("failed to create instance of " + clazz, exception);
        }
    }

    public boolean keyExists(String key) {
        return this.getValue(key) != null;
    }

    public boolean isValid(FieldDeclaration declaration, Object value) {
        return true;
    }

    public abstract void write(OutputStream outputStream, ConfigDeclaration declaration) throws Exception;

    public abstract void load(InputStream inputStream, ConfigDeclaration declaration) throws Exception;
}
