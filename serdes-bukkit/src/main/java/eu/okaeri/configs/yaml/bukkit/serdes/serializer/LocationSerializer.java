package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationSerializer implements ObjectSerializer<Location> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return Location.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull Location location, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.set("world", location.getWorld(), World.class);
        data.set("x", location.getX());
        data.set("y", location.getY());
        data.set("z", location.getZ());
        data.set("yaw", location.getYaw());
        data.set("pitch", location.getPitch());
    }

    @Override
    public Location deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        World world = data.get("world", World.class);
        double x = data.get("x", Double.class);
        double y = data.get("y", Double.class);
        double z = data.get("z", Double.class);
        float yaw = data.get("yaw", Float.class);
        float pitch = data.get("pitch", Float.class);

        return new Location(world, x, y, z, yaw, pitch);
    }
}
