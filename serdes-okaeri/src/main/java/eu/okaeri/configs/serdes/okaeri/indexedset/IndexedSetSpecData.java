package eu.okaeri.configs.serdes.okaeri.indexedset;

import eu.okaeri.configs.serdes.SerdesContextAttachment;
import lombok.Value;

@Value(staticConstructor = "of")
public class IndexedSetSpecData implements SerdesContextAttachment {
    private final String key;
}
