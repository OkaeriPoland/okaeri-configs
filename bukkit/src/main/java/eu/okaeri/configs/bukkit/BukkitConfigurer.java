package eu.okaeri.configs.bukkit;

import eu.okaeri.configs.Configurer;
import eu.okaeri.configs.schema.ConfigDeclaration;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.util.ConfigUtil;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.Map;
import java.util.Scanner;

public class BukkitConfigurer extends Configurer {

    private YamlConfiguration config;

    public BukkitConfigurer() {
        this(new YamlConfiguration());
    }

    public BukkitConfigurer(YamlConfiguration config) {
        this.config = config;
    }

    @Override
    public String getCommentPrefix() {
        return "#";
    }

    @Override
    public String getSectionSeparator() {
        return "\n";
    }

    @Override
    public void setValue(String key, Object value, GenericsDeclaration type) {
        Object simplified = this.simplify(value, type);
        this.config.set(key, simplified);
    }

    @Override
    public Object getValue(String key) {
        return this.config.get(key);
    }

    @Override
    public boolean keyExists(String key) {
        return this.config.getKeys(false).contains(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T resolveType(Object object, GenericsDeclaration genericSource, Class<T> targetClazz, GenericsDeclaration genericTarget) {

        if (object instanceof MemorySection) {
            Map<String, Object> values = ((MemorySection) object).getValues(false);
            return super.resolveType(values, GenericsDeclaration.single(values), targetClazz, genericTarget);
        }

        return super.resolveType(object, GenericsDeclaration.single(object), targetClazz, genericTarget);
    }

    @Override
    public void loadFromFile(File file, ConfigDeclaration declaration) throws IOException {
        try {
            this.config.load(file);
        } catch (InvalidConfigurationException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public void writeToFile(File file, ConfigDeclaration declaration) throws IOException {

        this.config.save(file);

        String data = this.readFile(file);
        data = ConfigUtil.removeStartingWith(this.getCommentPrefix(), data);
        data = ConfigUtil.addCommentsToFields(this.getCommentPrefix(), this.getSectionSeparator(), data, declaration);

        String header = ConfigUtil.convertToComment(this.getCommentPrefix(), declaration.getHeader(), true);
        String output = "";

        if (header != null) {
            output += header;
            output += this.getSectionSeparator();
        }

        output += data;
        this.writeFile(file, output);
    }

    private String readFile(File file) throws IOException {
        StringBuilder fileContents = new StringBuilder((int) file.length());
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine()).append("\n");
            }
            return fileContents.toString();
        }
    }

    private void writeFile(File file, String text) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            out.print(text);
        }
    }
}
