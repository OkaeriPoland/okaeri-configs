package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.lang.reflect.*;
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

        // Handle Class directly
        if (type instanceof Class<?>) {
            return new GenericsDeclaration((Class<?>) type);
        }

        // Handle ParameterizedType (e.g., List<String>, Map<K, V>)
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

        // Handle WildcardType (e.g., ? extends T, ? super K)
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;

            // Try upper bounds first (? extends T)
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0) {
                // Use first upper bound (typically Object or the constraint)
                return from(upperBounds[0]);
            }

            // Try lower bounds (? super T)
            Type[] lowerBounds = wildcardType.getLowerBounds();
            if (lowerBounds.length > 0) {
                // Use first lower bound
                return from(lowerBounds[0]);
            }

            // Unbounded wildcard (?), use Object
            return new GenericsDeclaration(Object.class);
        }

        // Handle TypeVariable (e.g., T, K, V in generic declarations)
        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            Type[] bounds = typeVariable.getBounds();

            // Use first bound (typically Object or a constraint)
            if (bounds.length > 0) {
                return from(bounds[0]);
            }

            // No bounds, use Object
            return new GenericsDeclaration(Object.class);
        }

        // Handle GenericArrayType (e.g., T[], List<String>[])
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type componentType = genericArrayType.getGenericComponentType();

            // Resolve component type and create array class
            GenericsDeclaration componentDeclaration = from(componentType);

            // Create array class from component type
            Class<?> arrayClass = Array.newInstance(componentDeclaration.getType(), 0).getClass();
            return new GenericsDeclaration(arrayClass);
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
