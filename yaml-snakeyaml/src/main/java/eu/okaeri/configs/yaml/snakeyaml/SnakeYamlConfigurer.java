package eu.okaeri.configs.yaml.snakeyaml;

import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.FieldDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class SnakeYamlConfigurer extends Configurer {

    private final Yaml yaml;
    private Map<String, Object> data;

    public SnakeYamlConfigurer() {
        this.yaml = new Yaml();
        this.data = new HashMap<>();
    }

    public SnakeYamlConfigurer(Yaml yaml) {
        this.yaml = yaml;
        this.data = new HashMap<>();
    }

    @Override
    public void setValue(String key, Object value, GenericsDeclaration genericType, FieldDeclaration field) {
        Object simplified = this.simplify(value, genericType);
        data.put(key, simplified);
    }

    @Override
    public Object getValue(String key) {
        return data.get(key);
    }

    @Override
    public void writeToFile(File file, ConfigDeclaration declaration) throws Exception {
        yaml.dump(this.data, Files.newBufferedWriter(file.toPath()));
    }

    @Override
    public void loadFromFile(File file, ConfigDeclaration declaration) throws Exception {
        this.data = yaml.load(Files.newBufferedReader(file.toPath()));
    }
}
