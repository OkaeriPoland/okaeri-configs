package eu.okaeri.configs.serdes.adventure.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

@RequiredArgsConstructor
public class ComponentSerializer implements ObjectSerializer<Component> {

    private final net.kyori.adventure.text.serializer.ComponentSerializer<Component, ? extends Component, String> serializer;

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return Component.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull Component object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        String value = this.serializer.serialize(object);
        data.setValue(value);
    }

    @Override
    public Component deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
        String value = data.getValue(String.class);
        return this.serializer.deserialize(value);
    }
}
