package eu.okaeri.configs.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.TwoSideObjectTransformer;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class StringWorldTransformer extends TwoSideObjectTransformer<String, World> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, World.class);
    }

    @Override
    public World leftToRight(String data) {
        return Bukkit.getWorld(data);
    }

    @Override
    public String rightToLeft(World data) {
        return data.getName();
    }
}
