package eu.okaeri.configs.hocon;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.postprocessor.SectionSeparator;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.io.File;
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

    public HoconLightbendConfigurer(String sectionSeparator) {
        this.sectionSeparator = sectionSeparator;
    }

    public HoconLightbendConfigurer(String commentPrefix, String sectionSeparator) {
        this.commentPrefix = commentPrefix;
        this.sectionSeparator = sectionSeparator;
    }

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type, FieldDeclaration field) {
        Object simplified = this.simplify(value, type);
        this.map.put(key, simplified);
    }

    @Override
    public Object getValue(String key) {
        return this.map.get(key);
    }

    @Override
    public boolean keyExists(String key) {
        return this.map.containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadFromFile(File file, ConfigDeclaration declaration) throws Exception {

        if (!file.exists()) {
            return;
        }

        this.config = ConfigFactory.parseFile(file);
        this.map = this.hoconToMap(this.config, declaration);
    }

    @Override
    public void writeToFile(File file, ConfigDeclaration declaration) throws Exception {

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
        ConfigPostprocessor.of(file, buf.toString())
                // remove all current commments
                .removeLines((line) -> line.startsWith(this.commentPrefix))
                // add new comments
                .updateLines((line) -> declaration.getFields().stream()
                        .filter(this.isFieldDeclaredForLine(line))
                        .findAny()
                        .map(FieldDeclaration::getComment)
                        .map(comment -> this.sectionSeparator + this.buildComment(comment) + line)
                        .orElse(line))
                // add header if available
                .updateContext(context -> (declaration.getHeader() != null)
                        ? (this.buildComment(declaration.getHeader()) + this.sectionSeparator + context)
                        : context)
                .write();
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

    private String buildComment(String[] strings) {
        if (strings == null) return null;
        List<String> lines = new ArrayList<>();
        for (String line : strings) {
            String[] parts = line.split("\n");
            String prefix = line.startsWith(this.commentPrefix.trim()) ? "" : this.commentPrefix;
            lines.add((line.isEmpty() ? "" : prefix) + line);
        }
        return String.join("\n", lines) + "\n";
    }
}
