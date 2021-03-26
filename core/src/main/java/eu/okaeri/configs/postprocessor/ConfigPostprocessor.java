package eu.okaeri.configs.postprocessor;

import lombok.Data;
import lombok.SneakyThrows;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Data
public class ConfigPostprocessor {

    private File file;
    private String context;

    public ConfigPostprocessor(File file) {
        this.file = file;
    }

    public static ConfigPostprocessor of(File file) {
        return new ConfigPostprocessor(file);
    }

    public static ConfigPostprocessor of(File file, String context) {
        return new ConfigPostprocessor(file).updateContext(value -> context);
    }

    @SneakyThrows
    public ConfigPostprocessor read() {
        if (!this.file.exists()) return this;
        this.context = this.readFile(this.file);
        return this;
    }

    @SneakyThrows
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public ConfigPostprocessor write() {
        if (!this.file.exists()) this.file.createNewFile();
        this.writeFile(this.file, this.context);
        return this;
    }

    public ConfigPostprocessor removeLines(ConfigLineFilter filter) {

        String[] lines = this.context.split("\n");
        StringBuilder buf = new StringBuilder();

        for (String line : lines) {
            if (filter.remove(line)) {
                continue;
            }
            buf.append(line).append("\n");
        }

        this.context = buf.toString();
        return this;
    }

    public ConfigPostprocessor updateLines(ConfigContextManipulator manipulator) {

        String[] lines = this.context.split("\n");
        StringBuilder buf = new StringBuilder();

        for (String line : lines) {
            buf.append(manipulator.convert(line)).append("\n");
        }

        this.context = buf.toString();
        return this;
    }

    public ConfigPostprocessor updateContext(ConfigContextManipulator manipulator) {
        this.context = manipulator.convert(this.context);
        return this;
    }

    public ConfigPostprocessor prependContextComment(String prefix, String[] strings) {
        return this.prependContextComment(prefix, "", strings);
    }

    public ConfigPostprocessor prependContextComment(String prefix, String separator, String[] strings) {
        if (strings != null) this.context = createComment(prefix, strings) + separator + this.context;
        return this;
    }

    public ConfigPostprocessor appendContextComment(String prefix, String[] strings) {
        return this.appendContextComment(prefix, "", strings);
    }

    public ConfigPostprocessor appendContextComment(String prefix, String separator, String[] strings) {
        if (strings != null) this.context += separator + createComment(prefix, strings);
        return this;
    }

    public static String createComment(String commentPrefix, String[] strings) {
        if (strings == null) return null;
        List<String> lines = new ArrayList<>();
        for (String line : strings) {
            String[] parts = line.split("\n");
            String prefix = line.startsWith(commentPrefix.trim()) ? "" : commentPrefix;
            lines.add((line.isEmpty() ? "" : prefix) + line);
        }
        return String.join("\n", lines) + "\n";
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
