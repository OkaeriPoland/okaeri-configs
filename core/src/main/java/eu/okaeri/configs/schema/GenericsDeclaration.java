package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class GenericsDeclaration {

    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();
    private static final Set<Class<?>> PRIMITIVE_WRAPPERS = new HashSet<>();

    static {
        PRIMITIVES.put(boolean.class.getName(), boolean.class);
        PRIMITIVES.put(byte.class.getName(), byte.class);
        PRIMITIVES.put(char.class.getName(), char.class);
        PRIMITIVES.put(double.class.getName(), double.class);
        PRIMITIVES.put(float.class.getName(), float.class);
        PRIMITIVES.put(int.class.getName(), int.class);
        PRIMITIVES.put(long.class.getName(), long.class);
        PRIMITIVES.put(short.class.getName(), short.class);
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_WRAPPERS.add(Boolean.class);
        PRIMITIVE_WRAPPERS.add(Byte.class);
        PRIMITIVE_WRAPPERS.add(Character.class);
        PRIMITIVE_WRAPPERS.add(Double.class);
        PRIMITIVE_WRAPPERS.add(Float.class);
        PRIMITIVE_WRAPPERS.add(Integer.class);
        PRIMITIVE_WRAPPERS.add(Long.class);
        PRIMITIVE_WRAPPERS.add(Short.class);
    }

    private Class<?> type;
    private List<GenericsDeclaration> subtype = new ArrayList<>();
    private boolean isEnum;

    private GenericsDeclaration(Class<?> type) {
        this.type = type;
        this.isEnum = type.isEnum();
    }

    public static boolean isUnboxedCompatibleWithBoxed(@NonNull Class<?> unboxedClazz, @NonNull Class<?> boxedClazz) {
        Class<?> primitiveWrapper = PRIMITIVE_TO_WRAPPER.get(unboxedClazz);
        return primitiveWrapper == boxedClazz;
    }

    public static boolean doBoxTypesMatch(@NonNull Class<?> clazz1, @NonNull Class<?> clazz2) {
        return isUnboxedCompatibleWithBoxed(clazz1, clazz2) || isUnboxedCompatibleWithBoxed(clazz2, clazz1);
    }

    public static GenericsDeclaration of(@NonNull Object type, @NonNull List<Object> subtypes) {
        Class<?> finalType = (type instanceof Class<?>) ? (Class<?>) type : type.getClass();
        GenericsDeclaration declaration = new GenericsDeclaration(finalType);
        declaration.setSubtype(subtypes.stream().map(GenericsDeclaration::of).collect(Collectors.toList()));
        return declaration;
    }

    public static GenericsDeclaration of(Object object) {

        if (object == null) {
            return null;
        }

        if (object instanceof GenericsDeclaration) {
            return (GenericsDeclaration) object;
        }

        if (object instanceof Class) {
            return new GenericsDeclaration((Class<?>) object);
        }

        if (object instanceof Type) {
            return from((Type) object);
        }

        return new GenericsDeclaration(object.getClass());
    }

    private static GenericsDeclaration from(Type type) {

        if (type instanceof ParameterizedType) {

            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (rawType instanceof Class<?>) {
                GenericsDeclaration declaration = new GenericsDeclaration((Class<?>) rawType);
                declaration.setSubtype(Arrays.stream(actualTypeArguments)
                    .map(GenericsDeclaration::of)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
                return declaration;
            }
        }

        throw new IllegalArgumentException("cannot process type: " + type + " [" + type.getClass() + "]");
    }

    public GenericsDeclaration getSubtypeAtOrNull(int index) {
        return (this.subtype == null) ? null : ((index >= this.subtype.size()) ? null : this.subtype.get(index));
    }

    public GenericsDeclaration getSubtypeAtOrThrow(int index) {
        GenericsDeclaration subtype = this.getSubtypeAtOrNull(index);
        if (subtype == null) {
            throw new IllegalArgumentException("Cannot resolve subtype with index " + index + " for " + this);
        }
        return subtype;
    }

    public Class<?> wrap() {
        return PRIMITIVE_TO_WRAPPER.get(this.type);
    }

    @SuppressWarnings("UnnecessaryUnboxing")
    public Object unwrapValue(Object object) {
        if (object instanceof Boolean) return ((Boolean) object).booleanValue();
        if (object instanceof Byte) return ((Byte) object).byteValue();
        if (object instanceof Character) return ((Character) object).charValue();
        if (object instanceof Double) return ((Double) object).doubleValue();
        if (object instanceof Float) return ((Float) object).floatValue();
        if (object instanceof Integer) return ((Integer) object).intValue();
        if (object instanceof Long) return ((Long) object).longValue();
        if (object instanceof Short) return ((Short) object).shortValue();
        return object;
    }

    public boolean isPrimitive() {
        return this.type.isPrimitive();
    }

    public boolean isPrimitiveWrapper() {
        return PRIMITIVE_WRAPPERS.contains(this.type);
    }

    public boolean isConfig() {
        return OkaeriConfig.class.isAssignableFrom(this.type);
    }

    public boolean hasSubtypes() {
        return (this.subtype != null) && !this.subtype.isEmpty();
    }
}
