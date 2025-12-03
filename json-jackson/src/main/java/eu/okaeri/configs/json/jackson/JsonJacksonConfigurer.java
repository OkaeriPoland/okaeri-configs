package eu.okaeri.configs.json.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * JSON configurer using Jackson's databind for parsing and serialization.
 * <p>
 * Features:
 * <ul>
 *   <li>Full JSON support via Jackson</li>
 *   <li>Pretty printing by default</li>
 *   <li>Preserves field declaration order</li>
 *   <li>Configurable ObjectMapper</li>
 * </ul>
 */
@Accessors(chain = true)
public class JsonJacksonConfigurer extends Configurer {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<LinkedHashMap<String, Object>>() {
    };

    private @Setter ObjectMapper mapper;

    public JsonJacksonConfigurer() {
        this.mapper = createDefaultMapper();
    }

    public JsonJacksonConfigurer(@NonNull ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Configure pretty printer:
        // - Standard colon separator (": " instead of " : ")
        // - Array elements on separate lines (like GSON)
        // - Compact empty objects/arrays ({} instead of { })
        Separators separators = Separators.createDefaultInstance()
            .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
            .withObjectEmptySeparator("")
            .withArrayEmptySeparator("");
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter()
            .withSeparators(separators)
            .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        mapper.setDefaultPrettyPrinter(prettyPrinter);

        return mapper;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("json");
    }

    @Override
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        return this.mapper.readValue(inputStream, MAP_TYPE);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        this.mapper.writeValue(outputStream, data);
    }
}
