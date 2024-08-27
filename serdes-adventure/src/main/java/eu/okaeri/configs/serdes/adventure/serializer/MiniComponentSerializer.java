package eu.okaeri.configs.serdes.adventure.serializer;

import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

public class MiniComponentSerializer extends ComponentSerializer {

    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
        .preProcessor(text -> Pattern.compile("§([0-9A-Fa-fK-Ok-oRXrx])").matcher(text).replaceAll("&$1"))
        .postProcessor(component -> component.replaceText(TextReplacementConfig.builder()
            .match(Pattern.compile(".*"))
            .replacement((result, input) -> LegacyComponentSerializer.legacyAmpersand().deserialize(result.group()))
            .build()))
        .build();

    public MiniComponentSerializer(boolean legacy) {
        super(legacy ? MINI_MESSAGE : MiniMessage.miniMessage());
    }

    public MiniComponentSerializer() {
        this(true);
    }
}
