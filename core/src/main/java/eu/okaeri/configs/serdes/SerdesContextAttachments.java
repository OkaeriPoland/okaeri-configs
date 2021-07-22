package eu.okaeri.configs.serdes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class SerdesContextAttachments extends LinkedHashMap<Class<? extends SerdesContextAttachment>, SerdesContextAttachment> {

    @Override
    public SerdesContextAttachment put(Class<? extends SerdesContextAttachment> key, SerdesContextAttachment value) {
        if (this.containsKey(key)) {
            throw new IllegalArgumentException("cannot override SerdesContext attachment of type " + key);
        }
        return super.put(key, value);
    }

    @Override
    public SerdesContextAttachment putIfAbsent(Class<? extends SerdesContextAttachment> key, SerdesContextAttachment value) {
        if (this.containsKey(key)) {
            throw new IllegalArgumentException("cannot override SerdesContext attachment of type " + key);
        }
        return super.putIfAbsent(key, value);
    }

    @Override
    public void putAll(Map<? extends Class<? extends SerdesContextAttachment>, ? extends SerdesContextAttachment> map) {
        for (Class<? extends SerdesContextAttachment> key : map.keySet()) {
            if (this.containsKey(key)) {
                throw new IllegalArgumentException("cannot override SerdesContext attachment of type " + key);
            }
        }
        super.putAll(map);
    }

    @Override
    public SerdesContextAttachment computeIfPresent(Class<? extends SerdesContextAttachment> key, BiFunction<? super Class<? extends SerdesContextAttachment>, ? super SerdesContextAttachment, ? extends SerdesContextAttachment> remappingFunction) {
        throw new RuntimeException("???");
    }

    @Override
    public SerdesContextAttachments clone() {
        return (SerdesContextAttachments) super.clone();
    }
}
