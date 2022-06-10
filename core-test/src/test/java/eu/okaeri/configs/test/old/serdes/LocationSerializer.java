package eu.okaeri.configs.test.old.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import eu.okaeri.configs.test.old.obj.Location;
import eu.okaeri.configs.test.old.obj.World;
import lombok.NonNull;

public class LocationSerializer implements ObjectSerializer<Location> {

    @Override
    public boolean supports(@NonNull Class<? super Location> type) {
        return Location.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull Location location, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.add("world", location.getWorld(), World.class);
        data.add("x", location.getX());
        data.add("y", location.getY());
        data.add("z", location.getZ());
        data.add("yaw", location.getYaw());
        data.add("pitch", location.getPitch());
    }

    @Override
    public Location deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        World world = data.get("world", World.class);
        double x = data.get("x", Double.class);
        double y = data.get("y", Double.class);
        double z = data.get("z", Double.class);
        float yaw = data.get("yaw", Float.class);
        float pitch = data.get("pitch", Float.class);

        return new Location(world, x, y, z, pitch, yaw);
    }
}
