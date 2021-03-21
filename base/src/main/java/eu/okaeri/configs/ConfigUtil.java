package eu.okaeri.configs;

import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;

public final class ConfigUtil {

    public static String convertToComment(String prefix, String[] strings, boolean spacing) {

        StringBuilder buf = new StringBuilder();

        for (String line : strings) {
            if (line.contains("\n")) {
                String[] parts = line.split("\n");
                for (String part : parts) {
                    if (!part.startsWith(prefix)) {
                        buf.append(prefix).append(spacing ? " " : "");
                    }
                    buf.append(part).append("\n");
                }
            } else {
                if (!line.startsWith(prefix)) {
                    buf.append(prefix).append(spacing ? " " : "");
                }
                buf.append(line).append("\n");
            }
        }

        return buf.toString();
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
            for (FieldDeclaration field : declaration.getFields()) {
                String name = field.getName();
                if (line.startsWith(name + ":")) {
                    buf.append(separator);
                    buf.append(ConfigUtil.convertToComment(prefix, field.getComment(), true));
                }
            }
            buf.append(line).append("\n");
        }

        return buf.toString();
    }
}
