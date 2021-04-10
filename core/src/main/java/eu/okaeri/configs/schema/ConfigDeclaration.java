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

    public static ConfigDeclaration of(Class<?> clazz, OkaeriConfig config) {

        ConfigDeclaration declaration = new ConfigDeclaration();
        declaration.setNameStrategy(clazz.getAnnotation(Names.class));
        declaration.setHeader(readHeader(clazz));
        declaration.setReal(OkaeriConfig.class.isAssignableFrom(clazz));
        declaration.setFields(Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !"this$0".equals(field.getName()))
                .map(field -> FieldDeclaration.of(declaration, field, config))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        return declaration;
    }

    public static ConfigDeclaration of(OkaeriConfig config) {
        return of(config.getClass(), config);
    }

    public static ConfigDeclaration of(Class<?> clazz) {
        return of(clazz, null);
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

    public GenericsDeclaration getGenericsOrNull(String key) {
        return this.getField(key)
                .map(FieldDeclaration::getType)
                .orElse(null);
    }

    private Names nameStrategy;
    private String[] header;
    private List<FieldDeclaration> fields;
    private boolean real;
}
