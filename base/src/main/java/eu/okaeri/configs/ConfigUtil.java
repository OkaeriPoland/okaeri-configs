package eu.okaeri.configs;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;

import java.util.ArrayList;
import java.util.List;

public final class ConfigUtil {

    public static String convertToComment(String prefix, String[] strings, boolean spacing) {

        if (strings == null) {
            return null;
        }

        List<String> lines = new ArrayList<>();
        for (String line : strings) {
            String[] parts = line.split("\n");
            for (String part : parts) {
                if (part.startsWith(prefix)) {
                    lines.add(part);
                    continue;
                }
                lines.add(prefix + (spacing ? " " : "") + part);
            }
        }

        return String.join("\n", lines) + "\n";
    }

    public static String removeStartingWith(String prefix, String text) {

        String[] lines = text.split("\n");
        StringBuilder buf = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith(prefix)) {
                continue;
            }
            buf.append(line).append("\n");
        }

        return buf.toString();
    }

    public static String addCommentsToFields(String prefix, String separator, String data, ConfigDeclaration declaration) {

        StringBuilder buf = new StringBuilder();
        String[] lines = data.split("\n");

        for (String line : lines) {

            FieldDeclaration field = fieldDeclarationOrNull(declaration, line);
            if (field == null) {
                buf.append(line).append("\n");
                continue;
            }

            String comment = ConfigUtil.convertToComment(prefix, field.getComment(), true);
            if (comment == null) {
                buf.append(line).append("\n");
                continue;
            }

            buf.append(separator).append(comment).append(line).append("\n");
        }

        return buf.toString();
    }

    private static FieldDeclaration fieldDeclarationOrNull(ConfigDeclaration declaration, String line) {
        for (FieldDeclaration field : declaration.getFields()) {
            if (line.startsWith(field.getName() + ":")) {
                return field;
            }
        }
        return null;
    }
}
