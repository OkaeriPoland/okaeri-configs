package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class GenericsDeclaration {

    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();
    private static final Set<Class<?>> PRIMITIVE_WRAPPERS = new HashSet<>();

    public static boolean isUnboxedCompatibleWithBoxed(Class<?> unboxedClazz, Class<?> boxedClazz) {
        Class<?> primitiveWrapper = PRIMITIVE_TO_WRAPPER.get(unboxedClazz);
        return primitiveWrapper == boxedClazz;
    }

    public static boolean doBoxTypesMatch(Class<?> clazz1, Class<?> clazz2) {
        return isUnboxedCompatibleWithBoxed(clazz1, clazz2) || isUnboxedCompatibleWithBoxed(clazz2, clazz1);
    }

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

    public static GenericsDeclaration of(Object type, List<Object> subtypes) {
        GenericsDeclaration declaration = of(type);
        declaration.setSubtype(subtypes.stream().map(GenericsDeclaration::of).collect(Collectors.toList()));
        return declaration;
    }

    public static GenericsDeclaration of(Object object) {

        if (object == null) {
            return null;
        }

        if (object instanceof Class) {
            return new GenericsDeclaration((Class<?>) object);
        }

        if (object instanceof Type) {
            return from(((Type) object).getTypeName());
        }

        return new GenericsDeclaration(object.getClass());
    }

    public static GenericsDeclaration from(String typeName) {

        GenericsDeclaration declaration = new GenericsDeclaration();
        StringBuilder buf = new StringBuilder();
        char[] charArray = typeName.toCharArray();

        for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {
            char c = charArray[i];
            if (c == '<') {
                declaration.setType(resolvePrimitiveOrClass(buf.toString()));
                String genericType = typeName.substring(i + 1, typeName.length() - 1);
                List<GenericsDeclaration> separateTypes = separateTypes(genericType).stream()
                        .map(GenericsDeclaration::from)
                        .collect(Collectors.toList());
                declaration.setSubtype(separateTypes);
                return declaration;
            }
            buf.append(c);
        }

        declaration.setType(resolvePrimitiveOrClass(buf.toString()));
        return declaration;
    }

    private static Class<?> resolvePrimitiveOrClass(String type) {

        Class<?> primitiveClass = PRIMITIVES.get(type);
        if (primitiveClass != null) {
            return primitiveClass;
        }

        try {
            return Class.forName(type);
        }
        // generics, eg.:
        // - ? super T
        // - T
        catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private static List<String> separateTypes(String types) {

        StringBuilder buf = new StringBuilder();
        char[] charArray = types.toCharArray();
        boolean skip = false;
        List<String> out = new ArrayList<>();

        for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {

            char c = charArray[i];

            if (c == '<') {
                skip = true;
            }

            if (c == '>') {
                skip = false;
            }

            if (skip) {
                buf.append(c);
                continue;
            }

            if (c == ',') {
                out.add(buf.toString());
                buf.setLength(0);
                i++;
                continue;
            }

            buf.append(c);
        }

        out.add(buf.toString());
        return out;
    }

    public GenericsDeclaration() {
    }

    public GenericsDeclaration(Class<?> type) {
        this.type = type;
    }

    private Class<?> type;
    private List<GenericsDeclaration> subtype;

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
}
