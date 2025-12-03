package eu.okaeri.configs.yaml.bungee.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;

public class StringChatColorTransformer extends BidirectionalTransformer<String, ChatColor> {

    @Override
    public GenericsPair<String, ChatColor> getPair() {
        return this.genericsPair(String.class, ChatColor.class);
    }

    @Override
    public ChatColor leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        try {
            return ChatColor.of(data);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Expected color name or hex (e.g. RED, #FF0000)");
        }
    }

    @Override
    public String rightToLeft(@NonNull ChatColor data, @NonNull SerdesContext serdesContext) {
        return data.getName();
    }
}
