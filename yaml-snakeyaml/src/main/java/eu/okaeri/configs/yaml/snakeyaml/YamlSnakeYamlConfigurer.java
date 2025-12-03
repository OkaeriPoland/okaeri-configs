package eu.okaeri.configs.yaml.snakeyaml;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.format.SourceWalker;
import eu.okaeri.configs.format.yaml.YamlSourceWalker;
import eu.okaeri.configs.postprocessor.ConfigPostprocessor;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Accessors(chain = true)
public class YamlSnakeYamlConfigurer extends Configurer {

    private Yaml yaml;

    @Setter private String commentPrefix = "# ";

    public YamlSnakeYamlConfigurer(@NonNull Yaml yaml) {
        this.yaml = yaml;
    }

    public YamlSnakeYamlConfigurer() {
        this(createYaml());
    }

    private static Yaml createYaml() {

        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(loaderOptions);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Representer representer = new Representer(dumperOptions);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Resolver resolver = new Resolver();

        return new Yaml(constructor, representer, dumperOptions, loaderOptions, resolver);
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("yml", "yaml");
    }

    @Override
    public boolean isCommentLine(String line) {
        return line.trim().startsWith("#");
    }

    @Override
    public Map<String, Object> load(@NonNull InputStream inputStream, @NonNull ConfigDeclaration declaration) throws Exception {
        return this.yaml.load(inputStream);
    }

    @Override
    public void write(@NonNull OutputStream outputStream, @NonNull Map<String, Object> data, @NonNull ConfigDeclaration declaration) throws Exception {
        ConfigPostprocessor.of(this.yaml.dump(data))
            .removeLines(line -> line.startsWith(this.commentPrefix.trim()))
            .updateContext(ctx -> YamlSourceWalker.of(ctx).insertComments(declaration, this.commentPrefix))
            .write(outputStream);
    }
}
