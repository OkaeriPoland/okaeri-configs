package eu.okaeri.configs.bukkit.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationSerializer implements ObjectSerializer<Location> {

    @Override
    public Class<? super Location> getType() {
        return Location.class;
    }

    @Override
    public void serialize(Location location, SerializationData data) {
        data.add("world", location.getWorld());
        data.addFormatted("x", "%.2f", location.getX());
        data.addFormatted("y", "%.2f", location.getY());
        data.addFormatted("z", "%.2f", location.getZ());
        data.addFormatted("yaw", "%.2f", location.getYaw());
        data.addFormatted("pitch", "%.2f", location.getPitch());
    }

    @Override
    public Location deserialize(DeserializationData data, GenericsDeclaration generics) {

        World world = data.get("world", World.class);
        double x = data.get("x", Double.class);
        double y = data.get("y", Double.class);
        double z = data.get("z", Double.class);
        float yaw = data.get("yaw", Float.class);
        float pitch = data.get("pitch", Float.class);

        return new Location(world, x, y, z, pitch, yaw);
    }
}
