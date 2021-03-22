package eu.okaeri.configs.serdes;

import eu.okaeri.configs.serdes.transformer.TransformerRegistry;

public interface OkaeriSerdesPack {
    void register(TransformerRegistry registry);
}
