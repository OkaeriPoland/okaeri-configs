package eu.okaeri.configs.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class GenericsDeclaration {

    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<>();

    static {
        PRIMITIVES.put(boolean.class.getName(), boolean.class);
        PRIMITIVES.put(byte.class.getName(), byte.class);
        PRIMITIVES.put(char.class.getName(), char.class);
        PRIMITIVES.put(double.class.getName(), double.class);
        PRIMITIVES.put(float.class.getName(), float.class);
        PRIMITIVES.put(int.class.getName(), int.class);
        PRIMITIVES.put(long.class.getName(), long.class);
        PRIMITIVES.put(short.class.getName(), short.class);
        PRIMITIVE_WRAPPERS.put(boolean.class, Boolean.class);
        PRIMITIVE_WRAPPERS.put(byte.class, Byte.class);
        PRIMITIVE_WRAPPERS.put(char.class, Character.class);
        PRIMITIVE_WRAPPERS.put(double.class, Double.class);
        PRIMITIVE_WRAPPERS.put(float.class, Float.class);
        PRIMITIVE_WRAPPERS.put(int.class, Integer.class);
        PRIMITIVE_WRAPPERS.put(long.class, Long.class);
        PRIMITIVE_WRAPPERS.put(short.class, Short.class);
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

    @SneakyThrows
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

    private static Class<?> resolvePrimitiveOrClass(String type) throws ClassNotFoundException {

        Class<?> primitiveClass = PRIMITIVES.get(type);
        if (primitiveClass != null) {
            return primitiveClass;
        }

        return Class.forName(type);
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
        return PRIMITIVE_WRAPPERS.get(this.type);
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
}
