package eu.okaeri.configs.configurer;

import eu.okaeri.configs.annotation.Serdes;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.SerdesRegistry;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * In-memory configurer that doesn't persist to any storage.
 * Useful for testing or temporary configurations.
 */
@NoArgsConstructor
public class InMemoryConfigurer extends Configurer {

    @Override
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        return new LinkedHashMap<>();
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        // No-op for in-memory configurer
    }
}
