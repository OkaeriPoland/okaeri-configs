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

    private Map<String, Object> map = new LinkedHashMap<>();
    private @Setter ObjectMapper mapper;
    private @Setter String commentPrefix = "# ";

    public YamlJacksonConfigurer() {
        this.mapper = createDefaultMapper();
    }

    public YamlJacksonConfigurer(@NonNull ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public YamlJacksonConfigurer(@NonNull Map<String, Object> map) {
        this();
        this.map = map;
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
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.map.put(key, simplified);
    }

    @Override
    public void setValueUnsafe(@NonNull String key, Object value) {
        this.map.put(key, value);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
    }

    @Override
    public Object remove(@NonNull String key) {
        return this.map.remove(key);
    }

    @Override
    public boolean keyExists(@NonNull String key) {
        return this.map.containsKey(key);
    }

    @Override
    public List<String> getAllKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.map.keySet()));
    }

    @Override
    public void load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        this.map = this.mapper.readValue(inputStream, MAP_TYPE);
        if (this.map == null) {
            this.map = new LinkedHashMap<>();
        }
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        // Render to string first for post-processing
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.mapper.writeValue(baos, this.map);
        String contents = baos.toString(StandardCharsets.UTF_8.name());

        // Remove existing comments
        contents = ConfigPostprocessor.of(contents)
            .removeLines((line) -> line.startsWith(this.commentPrefix.trim()))
            .getContext();

        // Insert comments using the source walker
        YamlSourceWalker walker = YamlSourceWalker.of(contents);
        contents = walker.insertComments(declaration, this.commentPrefix);

        // Add header and write
        ConfigPostprocessor.of(contents)
            .prependContextComment(this.commentPrefix, declaration.getHeader())
            .write(outputStream);
    }
}
