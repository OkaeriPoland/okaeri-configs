package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import org.bukkit.util.Vector;

public class VectorSerializer implements ObjectSerializer<Vector> {

    @Override
    public boolean supports(@NonNull Class<? super Vector> type) {
        return Vector.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull Vector object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("x", object.getX());
        data.add("y", object.getY());
        data.add("z", object.getZ());
    }

    @Override
    public Vector deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        double x = data.get("x", double.class);
        double y = data.get("y", double.class);
        double z = data.get("z", double.class);

        return new Vector(x, y, z);
    }
}
