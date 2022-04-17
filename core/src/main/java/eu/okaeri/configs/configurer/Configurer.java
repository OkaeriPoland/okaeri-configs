package eu.okaeri.configs.configurer;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.TargetType;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.*;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import lombok.Getter;
import lombok.NonNull;
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

    @Setter
    @Getter
    @NonNull
    private SerdesRegistry registry = new SerdesRegistry();
    {
        this.registry.register(new StandardSerdes());
    }

    public void register(@NonNull OkaeriSerdesPack pack) {
        this.registry.register(pack);
    }

    public List<String> getExtensions() {
        return Collections.emptyList();
    }

    public abstract void setValue(@NonNull String key, Object value, GenericsDeclaration genericType, FieldDeclaration field);

    public abstract Object getValue(@NonNull String key);

    public abstract Object remove(@NonNull String key);

    public boolean isToStringObject(@NonNull Object object, GenericsDeclaration genericType) {
        if (object instanceof Class) {
            Class<?> clazzObject = (Class<?>) object;
            return clazzObject.isEnum() || ((genericType != null) && this.registry.canTransform(genericType, GenericsDeclaration.of(String.class)));
        }
        return object.getClass().isEnum() || this.isToStringObject(object.getClass(), genericType);
    }

    @SuppressWarnings("unchecked")
    public Object simplifyCollection(@NonNull Collection<?> value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        List collection = new ArrayList();
        GenericsDeclaration collectionSubtype = (genericType == null) ? null : genericType.getSubtypeAtOrNull(0);

        for (Object collectionElement : value) {
            collection.add(this.simplify(collectionElement, collectionSubtype, serdesContext, conservative));
        }

        return collection;
    }

    @SuppressWarnings("unchecked")
    public Object simplifyMap(@NonNull Map<Object, Object> value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        Map<Object, Object> map = new LinkedHashMap<>();
        GenericsDeclaration keyDeclaration = (genericType == null) ? null : genericType.getSubtypeAtOrNull(0);
        GenericsDeclaration valueDeclaration = (genericType == null) ? null : genericType.getSubtypeAtOrNull(1);

        for (Map.Entry<Object, Object> entry : value.entrySet()) {
            Object key = this.simplify(entry.getKey(), keyDeclaration, serdesContext, conservative);
            Object kValue = this.simplify(entry.getValue(), valueDeclaration, serdesContext, conservative);
            map.put(key, kValue);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    public Object simplify(Object value, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, boolean conservative) throws OkaeriException {

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
                return this.simplify(wrappedPrimitive.cast(value), GenericsDeclaration.of(wrappedPrimitive), serdesContext, conservative);
            }

            if (genericType == null) {
                GenericsDeclaration valueDeclaration = GenericsDeclaration.of(value);
                if (this.isToStringObject(serializerType, valueDeclaration)) {
                    return this.resolveType(value, genericType, String.class, null, serdesContext);
                }
            }

            if (this.isToStringObject(serializerType, genericType)) {
                return this.resolveType(value, genericType, String.class, null, serdesContext);
            }

            if (value instanceof Collection) {
                return this.simplifyCollection((Collection<?>) value, genericType, serdesContext, conservative);
            }

            if (value instanceof Map) {
                return this.simplifyMap((Map<Object, Object>) value, genericType, serdesContext, conservative);
            }

            throw new OkaeriException("cannot simplify type " + serializerType + " (" + genericType + "): '" + value + "' [" + value.getClass() + "]");
        }

        SerializationData serializationData = new SerializationData(this, serdesContext);
        serializer.serialize(value, serializationData, (genericType == null) ? GenericsDeclaration.of(value) : genericType);
        Map<Object, Object> serializationMap = new LinkedHashMap<>(serializationData.asMap());

        return this.simplifyMap(serializationMap, GenericsDeclaration.of(Map.class, Collections.singletonList(String.class)), serdesContext, conservative);
    }

    public <T> T getValue(@NonNull String key, @NonNull Class<T> clazz, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext) {
        Object value = this.getValue(key);
        if (value == null) return null;
        return this.resolveType(value, GenericsDeclaration.of(value), clazz, genericType, serdesContext);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, @NonNull Class<T> targetClazz, GenericsDeclaration genericTarget, @NonNull SerdesContext serdesContext) throws OkaeriException {

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
            if ((object instanceof String) && target.isEnum()) {
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
            if (source.isEnum() && (targetClazz == String.class)) {
                Method enumMethod = objectClazz.getMethod("name");
                return targetClazz.cast(enumMethod.invoke(object));
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new OkaeriException("failed to resolve enum " + object.getClass() + " <> " + targetClazz, exception);
        }

        // subconfig
        if (OkaeriConfig.class.isAssignableFrom(targetClazz)) {
            OkaeriConfig config = ConfigManager.createUnsafe((Class<? extends OkaeriConfig>) targetClazz);
            Map configMap = this.resolveType(object, source, Map.class, GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Object.class)), serdesContext);
            config.setConfigurer(new InMemoryWrappedConfigurer(this, configMap));
            return (T) config.update();
        }

        // deserialization
        ObjectSerializer objectSerializer = this.registry.getSerializer(targetClazz);
        if ((object instanceof Map) && (objectSerializer != null)) {
            DeserializationData deserializationData = new DeserializationData((Map<String, Object>) object, this, serdesContext);
            Object deserialized = objectSerializer.deserialize(deserializationData, target);
            return targetClazz.cast(deserialized);
        }

        // generics
        if (genericTarget != null) {

            // custom target type
            FieldDeclaration serdesContextField = serdesContext.getField();
            Class<T> localTargetClazz = (serdesContextField == null) ? targetClazz : serdesContextField.getAnnotation(TargetType.class)
                    .map(TargetType::value)
                    .map(type -> (Class<T>) type)
                    .orElse(targetClazz);

            // collections
            if ((object instanceof Collection) && Collection.class.isAssignableFrom(localTargetClazz)) {

                Collection<?> sourceList = (Collection<?>) object;
                Collection<Object> targetList = (Collection<Object>) this.createInstance(localTargetClazz);
                GenericsDeclaration listDeclaration = genericTarget.getSubtypeAtOrNull(0);

                for (Object item : sourceList) {
                    Object converted = this.resolveType(item, GenericsDeclaration.of(item), listDeclaration.getType(), listDeclaration, serdesContext);
                    targetList.add(converted);
                }

                return localTargetClazz.cast(targetList);
            }

            // maps
            if ((object instanceof Map) && Map.class.isAssignableFrom(localTargetClazz)) {

                Map<Object, Object> values = ((Map<Object, Object>) object);
                GenericsDeclaration keyDeclaration = genericTarget.getSubtypeAtOrNull(0);
                GenericsDeclaration valueDeclaration = genericTarget.getSubtypeAtOrNull(1);
                Map<Object, Object> map = (Map<Object, Object>) this.createInstance(localTargetClazz);

                for (Map.Entry<Object, Object> entry : values.entrySet()) {
                    Object key = this.resolveType(entry.getKey(), GenericsDeclaration.of(entry.getKey()), keyDeclaration.getType(), keyDeclaration, serdesContext);
                    Object value = this.resolveType(entry.getValue(), GenericsDeclaration.of(entry.getValue()), valueDeclaration.getType(), valueDeclaration, serdesContext);
                    map.put(key, value);
                }

                return localTargetClazz.cast(map);
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
                Object simplified = this.simplify(object, GenericsDeclaration.of(objectClazz), serdesContext, false);
                return this.resolveType(simplified, GenericsDeclaration.of(simplified), targetClazz, GenericsDeclaration.of(targetClazz), serdesContext);
            }

            // in conservative mode some values may change their type unexpected, e.g. '10' becomes 10
            // this safeguard searches possible two step conversions and performs them if available
            List<ObjectTransformer> transformersFrom = this.getRegistry().getTransformersFrom(source);
            for (ObjectTransformer stepOneTransformer : transformersFrom) {

                // get step one target and search for starting from this type
                GenericsDeclaration stepOneTarget = stepOneTransformer.getPair().getTo();
                ObjectTransformer stepTwoTransformer = this.getRegistry().getTransformer(stepOneTarget, target);

                // found it! convert
                if (stepTwoTransformer != null) {
                    Object transformed = stepOneTransformer.transform(object, serdesContext);
                    Object doubleTransformed = stepTwoTransformer.transform(transformed, serdesContext);
                    return targetClazz.cast(doubleTransformed);
                }
            }

            // no more known options, try casting
            try {
                return targetClazz.cast(object);
            }
            // failed casting, explicit error
            catch (ClassCastException exception) {
                throw new OkaeriException("cannot resolve " + object.getClass() + " to " + targetClazz + " (" + source + " => " + target + "): " + object, exception);
            }
        }

        // primitives transformer
        if (targetClazz.isPrimitive()) {
            Object transformed = transformer.transform(object, serdesContext);
            return (T) GenericsDeclaration.of(targetClazz).unwrapValue(transformed);
        }

        return targetClazz.cast(transformer.transform(object, serdesContext));
    }

    public Object createInstance(@NonNull Class<?> clazz) throws OkaeriException {
        try {
            if (Collection.class.isAssignableFrom(clazz)) {
                if (clazz == Set.class) {
                    return new HashSet<>();
                }
                if (clazz == List.class) {
                    return new ArrayList<>();
                }
                try {
                    return clazz.newInstance();
                } catch (InstantiationException exception) {
                    throw new OkaeriException("cannot create instance of " + clazz + " (tip: provide implementation " +
                            "(e.g. ArrayList) for types with no default constructor using @TargetType annotation)", exception);
                }
            }
            if (Map.class.isAssignableFrom(clazz)) {
                if (clazz == Map.class) {
                    return new LinkedHashMap<>();
                }
                try {
                    return clazz.newInstance();
                } catch (InstantiationException exception) {
                    throw new OkaeriException("cannot create instance of " + clazz + " (tip: provide implementation " +
                            "(e.g. LinkedHashMap) for types with no default constructor using @TargetType annotation)", exception);
                }
            }
            throw new OkaeriException("cannot create instance of " + clazz);
        } catch (Exception exception) {
            throw new OkaeriException("failed to create instance of " + clazz, exception);
        }
    }

    public boolean keyExists(@NonNull String key) {
        return this.getValue(key) != null;
    }

    public boolean isValid(@NonNull FieldDeclaration declaration, Object value) {
        return true;
    }

    public List<String> getAllKeys() {
        return this.getParent().getDeclaration().getFields().stream()
                .map(FieldDeclaration::getName)
                .collect(Collectors.toList());
    }

    public abstract void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception;

    public abstract void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception;
}
