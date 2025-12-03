package eu.okaeri.configs.serdes.adventure.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.*;
import lombok.NonNull;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

public class TextColorSerializer implements ObjectSerializer<TextColor> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return TextColor.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull TextColor object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {

        if (object instanceof NamedTextColor) {
            data.setValue(((NamedTextColor) object).toString());
            return;
        }

        NamedTextColor named = NamedTextColor.namedColor(object.value());
        data.setValue((named == null) ? object.asHexString() : named.toString());
    }

    @Override
    public TextColor deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        String text = data.getValue(String.class);
        if (text == null) {
            throw new IllegalArgumentException("Cannot serialize TextColor from null");
        }

        text = text.toLowerCase(Locale.ROOT);
        TextColor color = NamedTextColor.NAMES.value(text);

        if (color == null) {
            color = TextColor.fromCSSHexString(text);
        }

        if (color != null) {
            return color;
        }

        throw new IllegalArgumentException("Expected color name or hex (e.g. red, #FF0000)");
    }
}
