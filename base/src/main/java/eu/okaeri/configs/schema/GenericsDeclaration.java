package eu.okaeri.configs.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class GenericsDeclaration {

    public static GenericsDeclaration single(Object object) {
        if (object == null) {
            return null;
        }
        return new GenericsDeclaration(object.getClass());
    }

    public static GenericsDeclaration from(Type type) {
        return from(type.getTypeName());
    }

    @SneakyThrows
    public static GenericsDeclaration from(String typeName) {

        GenericsDeclaration declaration = new GenericsDeclaration();
        StringBuilder buf = new StringBuilder();
        char[] charArray = typeName.toCharArray();

        for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {
            char c = charArray[i];
            if (c == '<') {
                declaration.setType(Class.forName(buf.toString()));
                String genericType = typeName.substring(i + 1, typeName.length() - 1);
                List<GenericsDeclaration> separateTypes = separateTypes(genericType).stream()
                        .map(GenericsDeclaration::from)
                        .collect(Collectors.toList());
                declaration.setSubtype(separateTypes);
                return declaration;
            }
            buf.append(c);
        }

        declaration.setType(Class.forName(buf.toString()));
        return declaration;
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
}
