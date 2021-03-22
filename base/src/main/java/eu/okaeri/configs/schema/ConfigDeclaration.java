package eu.okaeri.configs.schema;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Headers;
import eu.okaeri.configs.annotation.Names;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class ConfigDeclaration {

    public static ConfigDeclaration from(OkaeriConfig config) {

        ConfigDeclaration declaration = new ConfigDeclaration();
        Class<? extends OkaeriConfig> clazz = config.getClass();

        declaration.setNameStrategy(clazz.getAnnotation(Names.class));
        declaration.setHeader(readHeader(clazz));
        declaration.setFields(Arrays.stream(clazz.getDeclaredFields())
                .map(field -> FieldDeclaration.from(declaration, field, config))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        return declaration;
    }

    private static String[] readHeader(Class<?> clazz) {

        Headers headers = clazz.getAnnotation(Headers.class);
        if (headers != null) {
            List<String> headerList = new ArrayList<>();
            for (Header header : headers.value()) {
                headerList.addAll(Arrays.asList(header.value()));
            }
            return headerList.toArray(new String[0]);
        }

        Header header = clazz.getAnnotation(Header.class);
        if (header != null) {
            return header.value();
        }

        return null;
    }

    public Optional<FieldDeclaration> getField(String key) {
        return this.fields.stream()
                .filter(field -> field.getName().equals(key))
                .findAny();
    }

    public GenericsDeclaration getFieldDeclarationOrNull(String key) {

        Optional<FieldDeclaration> genericField = this.getField(key);
        GenericsDeclaration genericType = null;

        if (genericField.isPresent()) {
            FieldDeclaration field = genericField.get();
            genericType = field.getType();
        }

        return genericType;
    }

    private Names nameStrategy;
    private String[] header;
    private List<FieldDeclaration> fields;
}
