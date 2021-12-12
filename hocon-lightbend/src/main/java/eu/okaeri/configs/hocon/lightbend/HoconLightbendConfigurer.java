package eu.okaeri.configs.hocon.lightbend;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.exception.OkaeriException;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.postprocessor.SectionSeparator;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;

public class HoconLightbendConfigurer extends Configurer {

    private ConfigRenderOptions renderOpts = ConfigRenderOptions.defaults()
        .setFormatted(true)
        .setOriginComments(false)
        .setComments(true)
        .setJson(false);

    private String commentPrefix = "# ";
    private String sectionSeparator = SectionSeparator.NONE;

    private Map<String, Object> map = new LinkedHashMap<>();
    private Config config = ConfigFactory.parseMap(new LinkedHashMap<>());

    public HoconLightbendConfigurer() {
    }

    public HoconLightbendConfigurer(@NonNull String sectionSeparator) {
        this.sectionSeparator = sectionSeparator;
    }

    public HoconLightbendConfigurer(@NonNull String commentPrefix, @NonNull String sectionSeparator) {
        this.commentPrefix = commentPrefix;
        this.sectionSeparator = sectionSeparator;
    }

    @Override
    public Object simplify(Object value, GenericsDeclaration genericType, SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        if (value == null) {
            return null;
        }

        GenericsDeclaration genericsDeclaration = GenericsDeclaration.of(value);
        if ((genericsDeclaration.getType() == char.class) || (genericsDeclaration.getType() == Character.class)) {
            return super.simplify(value, genericType, serdesContext, false);
        }

        return super.simplify(value, genericType, serdesContext, conservative);
    }

    @Override
    public Object simplifyMap(@NonNull Map<Object, Object> value, GenericsDeclaration genericType, SerdesContext serdesContext, boolean conservative) throws OkaeriException {

        Map<Object, Object> map = new LinkedHashMap<>();
        GenericsDeclaration keyDeclaration = (genericType == null) ? null : genericType.getSubtypeAtOrNull(0);
        GenericsDeclaration valueDeclaration = (genericType == null) ? null : genericType.getSubtypeAtOrNull(1);

        for (Map.Entry<Object, Object> entry : value.entrySet()) {
            Object key = this.simplify(entry.getKey(), keyDeclaration, serdesContext, false);
            Object kValue = this.simplify(entry.getValue(), valueDeclaration, serdesContext, conservative);
            map.put(key, kValue);
        }

        return map;
    }

    @Override
    public void setValue(@NonNull String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type, SerdesContext.of(this, field), true);
        this.map.put(key, simplified);
    }

    @Override
    public Object getValue(@NonNull String key) {
        return this.map.get(key);
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
        this.config = ConfigFactory.parseString(ConfigPostprocessor.of(inputStream).getContext());
        this.map = this.hoconToMap(this.config, declaration);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull ConfigDeclaration declaration) throws Exception {

        this.config = ConfigFactory.parseMap(this.map);
        StringBuilder buf = new StringBuilder();

        // hack ordered rendering
        if (!declaration.getFields().isEmpty()) {
            for (FieldDeclaration field : declaration.getFields()) {
                Map<String, Object> entryMap = Collections.singletonMap(field.getName(), this.getValue(field.getName()));
                Config entryConfig = ConfigFactory.parseMap(entryMap);
                buf.append(entryConfig.root().render(this.renderOpts)).append(this.sectionSeparator);
            }
        }
        // unofficial support for "empty configs" (see TestRunner in core-test)
        else {
            buf.append(this.config.root().render(this.renderOpts));
        }

        // postprocess
        ConfigPostprocessor.of(buf.toString())
            // remove all current commments
            .removeLines((line) -> line.startsWith(this.commentPrefix.trim()))
            // add new comments
            .updateLines((line) -> declaration.getFields().stream()
                .filter(this.isFieldDeclaredForLine(line))
                .findAny()
                .map(FieldDeclaration::getComment)
                .map(comment -> this.sectionSeparator + ConfigPostprocessor.createComment(this.commentPrefix, comment) + line)
                .orElse(line))
            // add header if available
            .prependContextComment(this.commentPrefix, declaration.getHeader())
            // save
            .write(outputStream);
    }

    private Predicate<FieldDeclaration> isFieldDeclaredForLine(String line) {
        return field -> line.startsWith(field.getName() + "=") // key=
            || line.startsWith(field.getName() + " =") // key =
            || line.startsWith("\"" + field.getName() + "\"") // "key"
            || line.startsWith(field.getName() + "{") // key{
            || line.startsWith(field.getName() + " {"); // key {
    }

    private Map<String, Object> hoconToMap(Config config, ConfigDeclaration declaration) {

        Map<String, Object> map = new LinkedHashMap<>();

        for (FieldDeclaration field : declaration.getFields()) {
            if (!config.hasPath(field.getName())) {
                continue;
            }
            Object value = config.getValue(field.getName()).unwrapped();
            map.put(field.getName(), value);
        }

        return map;
    }
}
