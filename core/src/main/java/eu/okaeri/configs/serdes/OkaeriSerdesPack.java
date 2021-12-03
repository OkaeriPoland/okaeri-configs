package eu.okaeri.configs.serdes;

import lombok.NonNull;

public interface OkaeriSerdesPack {
    void register(@NonNull SerdesRegistry registry);
}
