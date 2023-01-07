package eu.okaeri.configs.yaml.bukkit.serdes.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bukkit.Tag;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class StringTagTransformer extends BidirectionalTransformer<String, Tag> {

    private final Map<String, Tag> tagMap = new HashMap<>();

    @SneakyThrows
    public StringTagTransformer() {
        for (Field field : Tag.class.getFields()) {
            if (!Tag.class.isAssignableFrom(field.getType())) {
                continue;
            }
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Tag tag = (Tag) field.get(null);
            if (tag == null) {
                continue;
            }
            this.tagMap.put(tag.getKey().toString(), tag);
        }
    }

    @Override
    public GenericsPair<String, Tag> getPair() {
        return this.genericsPair(String.class, Tag.class);
    }

    @Override
    public Tag leftToRight(@NonNull String data, @NonNull SerdesContext context) {
        Tag tag = this.tagMap.get(data);
        if (tag == null) {
            throw new IllegalArgumentException("Unknown tag: " + data);
        }
        return tag;
    }

    @Override
    public String rightToLeft(Tag data, @NonNull SerdesContext context) {
        return data.getKey().toString();
    }
}
