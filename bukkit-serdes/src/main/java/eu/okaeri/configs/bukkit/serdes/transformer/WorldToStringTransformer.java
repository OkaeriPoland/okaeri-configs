package eu.okaeri.configs.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.transformer.ObjectTransformer;
import org.bukkit.World;

public class WorldToStringTransformer extends ObjectTransformer<World, String> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(World.class, String.class);
    }

    @Override
    public String transform(World world) {
        return world.getName();
    }
}
