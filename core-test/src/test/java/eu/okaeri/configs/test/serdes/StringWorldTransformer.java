package eu.okaeri.configs.test.serdes;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;
import eu.okaeri.configs.test.obj.CraftWorld;
import eu.okaeri.configs.test.obj.World;

public class StringWorldTransformer extends TwoSideObjectTransformer<String, World> {

    @Override
    public GenericsPair getPair() {
        return this.genericsPair(String.class, World.class);
    }

    @Override
    public World leftToRight(String data) {
        return new CraftWorld(data);
    }

    @Override
    public String rightToLeft(World data) {
        return data.getName();
    }
}