package eu.okaeri.configs.postprocessor;

import lombok.SneakyThrows;

import java.io.*;
import java.util.Scanner;

public class ConfigPostprocessor {

    private File file;
    private String context;

    public ConfigPostprocessor(File file) {
        this.file = file;
    }

    public static ConfigPostprocessor of(File file) {
        return new ConfigPostprocessor(file);
    }

    @SneakyThrows
    public ConfigPostprocessor read() {
        this.context = this.readFile(this.file);
        return this;
    }

    @SneakyThrows
    public ConfigPostprocessor write() {
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
