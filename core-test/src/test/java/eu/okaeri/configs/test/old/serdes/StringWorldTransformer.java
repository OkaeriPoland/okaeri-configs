package eu.okaeri.configs.test.old.serdes;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.test.old.obj.CraftWorld;
import eu.okaeri.configs.test.old.obj.World;
import lombok.NonNull;

public class StringWorldTransformer extends BidirectionalTransformer<String, World> {

    @Override
    public GenericsPair<String, World> getPair() {
        return this.genericsPair(String.class, World.class);
    }

    @Override
    public World leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return new CraftWorld(data);
    }

    @Override
    public String rightToLeft(@NonNull World data, @NonNull SerdesContext serdesContext) {
        return data.getName();
    }
}
