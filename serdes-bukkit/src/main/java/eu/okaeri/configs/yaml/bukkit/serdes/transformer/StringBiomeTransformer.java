package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.util.EnumMatcher;
import lombok.NonNull;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;

import java.util.Arrays;
import java.util.Locale;

public class StringBiomeTransformer extends BidirectionalTransformer<String, Biome> {

    @Override
    public GenericsPair<String, Biome> getPair() {
        return this.genericsPair(String.class, Biome.class);
    }

    @Override
    public Biome leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {

        NamespacedKey key = data.contains(":")
            ? NamespacedKey.fromString(data)
            : NamespacedKey.minecraft(data.toLowerCase(Locale.ROOT));

        if (key == null) {
            String[] names = Arrays.stream(Biome.values()).map(Biome::name).toArray(String[]::new);
            throw new IllegalArgumentException(EnumMatcher.suggest(data, names, 5));
        }

        try {
            return Registry.BIOME.getOrThrow(key);
        }
        catch (NoSuchMethodError error) {
            try {
                return Biome.valueOf(data.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                String[] names = Arrays.stream(Biome.values()).map(Biome::name).toArray(String[]::new);
                throw new IllegalArgumentException(EnumMatcher.suggest(data, names, 5));
            }
        }
    }

    @Override
    public String rightToLeft(@NonNull Biome data, @NonNull SerdesContext serdesContext) {

        NamespacedKey key;
        try {
            key = data.getKeyOrThrow();
        } catch (NoSuchMethodError error) {
            return data.name();
        }

        if (key.getNamespace().equals(NamespacedKey.MINECRAFT)) {
            return key.getKey().toUpperCase(Locale.ROOT);
        }

        return String.valueOf(key);
    }
}
