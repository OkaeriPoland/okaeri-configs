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
import eu.okaeri.configs.util.UnsafeUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
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

    public abstract void setValueUnsafe(@NonNull String key, Object value);

    public abstract Object getValue(@NonNull String key);

    public Object getValueUnsafe(@NonNull String key) {
        return this.getValue(key);
    }

    public abstract Object remove(@NonNull String key);

    public boolean isToStringObject(@NonNull Object object, GenericsDeclaration genericType, SerdesContext serdesContext) {

        if (!(object instanceof Class)) {
            return object.getClass().isEnum() || this.isToStringObject(object.getClass(), genericType, serdesContext);
        }

        Class<?> clazzObject = (Class<?>) object;
        if (clazzObject.isEnum()) {
            return true;
        }

        if (genericType == null) {
            return false;
        }

        if (this.registry.canTransform(genericType, GenericsDeclaration.of(String.class))) {
            return true;
        }

        List<ObjectTransformer> transformersFrom = this.getRegistry().getTransformersFrom(genericType);
        for (ObjectTransformer stepOneTransformer : transformersFrom) {

            GenericsDeclaration stepOneTarget = stepOneTransformer.getPair().getTo();
            ObjectTransformer stepTwoTransformer = this.getRegistry().getTransformer(stepOneTarget, GenericsDeclaration.of(String.class));

            if (stepTwoTransformer != null) {
                return true;
            }
        }

        return false;
    }

    @Deprecated
    public boolean isToStringObject(@NonNull Object object, GenericsDeclaration genericType) {
        return this.isToStringObject(object, genericType, SerdesContext.of(this));
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

        if ((genericType != null) && (genericType.getType() == Object.class) && !genericType.hasSubtypes()) {
            genericType = GenericsDeclaration.of(value);
        }

        Class<?> serializerType = (genericType != null) ? genericType.getType() : value.getClass();
        ObjectSerializer serializer = this.registry.getSerializer(serializerType);

        if (serializer == null) {

            if (OkaeriConfig.class.isAssignableFrom(value.getClass())) {
                OkaeriConfig config = (OkaeriConfig) value;
                return config.asMap(this, conservative);
            }

            if (conservative && (serializerType.isPrimitive() || GenericsDeclaration.of(serializerType).isPrimitiveWrapper())) {
                return value;
            }

            if (serializerType.isPrimitive()) {
                Class<?> wrappedPrimitive = GenericsDeclaration.of(serializerType).wrap();
                return this.simplify(wrappedPrimitive.cast(value), GenericsDeclaration.of(wrappedPrimitive), serdesContext, conservative);
            }

            if (genericType == null) {
                GenericsDeclaration valueDeclaration = GenericsDeclaration.of(value);
                if (this.isToStringObject(serializerType, valueDeclaration, serdesContext)) {
                    return this.resolveType(value, genericType, String.class, null, serdesContext);
                }
            }

            if (this.isToStringObject(serializerType, genericType, serdesContext)) {
                return this.resolveType(value, genericType, String.class, null, serdesContext);
            }

            if (value instanceof Collection) {
                return this.simplifyCollection((Collection<?>) value, genericType, serdesContext, conservative);
            }

            if (value instanceof Map) {
                return this.simplifyMap((Map<Object, Object>) value, genericType, serdesContext, conservative);
            }

            if (value instanceof Serializable) {

                ConfigDeclaration declaration = ConfigDeclaration.of(value);
                SerializationData data = new SerializationData(this, serdesContext);

                declaration.getFields().forEach(field -> data.add(field.getName(), field.getValue(), field.getType()));
                LinkedHashMap<Object, Object> serializationMap = new LinkedHashMap<>(data.asMap());

                return this.simplifyMap(serializationMap, GenericsDeclaration.of(Map.class, Collections.singletonList(String.class)), serdesContext, conservative);
            }

            throw new OkaeriException("cannot simplify type " + serializerType + " (" + genericType + "): '" + value + "' [" + value.getClass() + "]");
        }

        Configurer configurer = (this.getParent() == null) ? this : this.getParent().getConfigurer();
        SerializationData serializationData = new SerializationData(configurer, serdesContext);
        serializer.serialize(value, serializationData, (genericType == null) ? GenericsDeclaration.of(value) : genericType);
        Map<Object, Object> serializationMap = new LinkedHashMap<>(serializationData.asMap());

        // replace result object (see ObjectSerializer#VALUE)
        if (serializationMap.containsKey(ObjectSerializer.VALUE)) {
            if (serializationMap.size() == 1) {
                return serializationMap.get(ObjectSerializer.VALUE);
            }
            throw new OkaeriException("magic value key is not allowed with other keys (" + serializationMap.keySet() + ")"
                + " in the SerializationData for " + serializerType + " (" + genericType + "): '" + value + "' [" + value.getClass() + "]");
        }

        // serialize as map
        return this.simplifyMap(serializationMap, GenericsDeclaration.of(Map.class, Collections.singletonList(String.class)), serdesContext, conservative);
    }

    public <T> T getValue(@NonNull String key, @NonNull Class<T> clazz, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext) {
        return this.getValue(key, clazz, genericType, serdesContext, null);
    }

    public <T> T getValue(@NonNull String key, @NonNull Class<T> clazz, GenericsDeclaration genericType, @NonNull SerdesContext serdesContext, Object originalValue) {
        Object value = this.getValue(key);
        if (value == null) return null;
        return this.resolveType(value, GenericsDeclaration.of(value), clazz, genericType, serdesContext, originalValue);
    }

    public <T> T resolveType(Object object, GenericsDeclaration genericSource, @NonNull Class<T> targetClazz, GenericsDeclaration genericTarget, @NonNull SerdesContext serdesContext) throws OkaeriException {
        return this.resolveType(object, genericSource, targetClazz, genericTarget, serdesContext, null);
    }

        @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, @NonNull Class<T> targetClazz, GenericsDeclaration genericTarget, @NonNull SerdesContext serdesContext, Object originalValue) throws OkaeriException {

        if (object == null) {
            return null;
        }

        GenericsDeclaration source = (genericSource == null) ? GenericsDeclaration.of(object) : genericSource;
        GenericsDeclaration target = (genericTarget == null) ? GenericsDeclaration.of(targetClazz) : genericTarget;

        // primitives
        if (target.isPrimitive()) {
            target = GenericsDeclaration.of(target.wrap());
        }

        // deserialization
        ObjectSerializer objectSerializer = this.registry.getSerializer(targetClazz);
        if (objectSerializer != null) {
            Configurer configurer = (this.getParent() == null) ? this : this.getParent().getConfigurer();
            DeserializationData deserializationData = (object instanceof Map)
                ? new DeserializationData((Map<String, Object>) object, configurer, serdesContext)
                : new DeserializationData(Collections.singletonMap(ObjectSerializer.VALUE, object), configurer, serdesContext);
            Object deserialized = objectSerializer.deserialize(deserializationData, target);
            return targetClazz.cast(deserialized);
        }

        // subconfig
        if (OkaeriConfig.class.isAssignableFrom(targetClazz)) {
            OkaeriConfig config = ConfigManager.createUnsafe((Class<? extends OkaeriConfig>) targetClazz);
            Map configMap = this.resolveType(object, source, Map.class, GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Object.class)), serdesContext);
            config.setConfigurer(new InMemoryWrappedConfigurer(this, configMap));
            config.setSpecialFieldsBy(originalValue);
            return (T) config.update();
        }

        // generics
        if (genericTarget != null) {

            // custom target type
            Class<T> localTargetClazz = (Class<T>) this.resolveTargetBaseType(serdesContext, target, source);

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

            // attempt serializable construction
            if ((object instanceof Map) && Serializable.class.isAssignableFrom(targetClazz)) {

                T serializableInstance = UnsafeUtil.allocateInstance(targetClazz);
                ConfigDeclaration declaration = ConfigDeclaration.of(targetClazz, serializableInstance);

                Map serializableMap = this.resolveType(
                    object, source, Map.class,
                    GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Object.class)), serdesContext
                );

                for (FieldDeclaration field : declaration.getFields()) {

                    Object serializedValue = serializableMap.get(field.getName());
                    if (serializedValue == null) {
                        continue;
                    }

                    Object deserializedValue = this.resolveType(
                        serializedValue, GenericsDeclaration.of(serializedValue),
                        field.getType().getType(), field.getType(), SerdesContext.of(this, field)
                    );

                    try {
                        field.getField().set(serializableInstance, deserializedValue);
                    } catch (IllegalAccessException exception) {
                        throw new OkaeriException("cannot set field of serializable " + field, exception);
                    }
                }

                return serializableInstance;
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

    @SuppressWarnings("unchecked")
    public Class<?> resolveTargetBaseType(@NonNull SerdesContext serdesContext, @NonNull GenericsDeclaration target, @NonNull GenericsDeclaration source) {

        FieldDeclaration serdesContextField = serdesContext.getField();
        Class<?> targetType = target.getType();

        // @TargetType shall apply only to the parent field
        if ((serdesContextField != null) && !serdesContextField.getType().equals(target)) {
            return targetType;
        }

        // no field, no annotation, return default
        if (serdesContextField == null) {
            return targetType;
        }

        // resolve from @TargetType
        Optional<TargetType> targetTypeAnnotation = serdesContextField.getAnnotation(TargetType.class);
        if (targetTypeAnnotation.isPresent()) {
            return targetTypeAnnotation.get().value();
        }

        // no annotation, return default
        return targetType;
    }

    public Object createInstance(@NonNull Class<?> clazz) throws OkaeriException {
        try {
            if (Collection.class.isAssignableFrom(clazz)) {
                if (clazz == Set.class) {
                    return new LinkedHashSet<>();
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

    public Set<String> sort(@NonNull ConfigDeclaration declaration) {

        // extract current data in declaration order
        Map<String, Object> reordered = declaration.getFields().stream().collect(
            LinkedHashMap::new,
            (map, field) -> {
                Object oldValue = this.getValueUnsafe(field.getName());
                this.remove(field.getName());
                map.put(field.getName(), oldValue);
            },
            LinkedHashMap::putAll);

        // save the orphans!
        Set<String> orphans = new LinkedHashSet<>(this.getAllKeys());

        // load new order
        reordered.forEach(this::setValueUnsafe);

        // get rid of the problem
        return orphans;
    }

    public abstract void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception;

    public abstract void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception;
}
