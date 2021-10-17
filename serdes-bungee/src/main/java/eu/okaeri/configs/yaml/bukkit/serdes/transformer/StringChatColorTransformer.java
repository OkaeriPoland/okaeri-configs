package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;

public class StringChatColorTransformer extends BidirectionalTransformer<String, ChatColor> {

    @Override
    public GenericsPair<String, ChatColor> getPair() {
        return this.genericsPair(String.class, ChatColor.class);
    }

    @Override
    public ChatColor leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return ChatColor.of(data);
    }

    @Override
    public String rightToLeft(@NonNull ChatColor data, @NonNull SerdesContext serdesContext) {
        return data.getName();
    }
}
