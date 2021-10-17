package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class StringWorldTransformer extends BidirectionalTransformer<String, World> {

    @Override
    public GenericsPair<String, World> getPair() {
        return this.genericsPair(String.class, World.class);
    }

    @Override
    public World leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return Bukkit.getWorld(data);
    }

    @Override
    public String rightToLeft(@NonNull World data, @NonNull SerdesContext serdesContext) {
        return data.getName();
    }
}
