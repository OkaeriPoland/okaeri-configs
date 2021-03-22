package eu.okaeri.configs.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class StringToWorldTransformer extends ObjectTransformer<String, World> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, World.class);
    }

    @Override
    public World transform(String data) {
        return Bukkit.getWorld(data);
    }
}
