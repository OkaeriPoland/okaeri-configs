package eu.okaeri.configs.yaml.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.format.yaml.YamlSourceWalker;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * YAML configurer using Jackson's YAML dataformat for parsing and serialization.
 * <p>
 * Features:
 * <ul>
 *   <li>Full YAML 1.1 support via Jackson</li>
 *   <li>Header and field comments support via post-processing</li>
 *   <li>Preserves field declaration order</li>
 *   <li>Configurable YAMLMapper</li>
 * </ul>
 */
@Accessors(chain = true)
public class YamlJacksonConfigurer extends Configurer {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<LinkedHashMap<String, Object>>() {
    };

    private @Setter ObjectMapper mapper;
    private @Setter String commentPrefix = "# ";

    public YamlJacksonConfigurer() {
        this.mapper = createDefaultMapper();
    }

    public YamlJacksonConfigurer(@NonNull ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static ObjectMapper createDefaultMapper() {
        YAMLFactory factory = YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
            .build();
        YAMLMapper mapper = new YAMLMapper(factory);
        mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        return mapper;
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("yml", "yaml");
    }

    @Override
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        return this.mapper.readValue(inputStream, MAP_TYPE);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.mapper.writeValue(baos, data);

        ConfigPostprocessor.of(baos.toString(StandardCharsets.UTF_8.name()))
            .removeLines(line -> line.startsWith(this.commentPrefix.trim()))
            .updateContext(ctx -> YamlSourceWalker.of(ctx).insertComments(declaration, this.commentPrefix))
            .write(outputStream);
    }
}
