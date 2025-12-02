package eu.okaeri.configs.postprocessor;

import lombok.Cleanup;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
public class ConfigPostprocessor {

    private String context;

    public static ConfigPostprocessor of(@NonNull InputStream inputStream) {
        ConfigPostprocessor postprocessor = new ConfigPostprocessor();
        postprocessor.setContext(readInput(inputStream));
        return postprocessor;
    }

    public static ConfigPostprocessor of(@NonNull String context) {
        ConfigPostprocessor postprocessor = new ConfigPostprocessor();
        postprocessor.setContext(context);
        return postprocessor;
    }

    private static String readInput(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
            .collect(Collectors.joining("\n"));
    }

    @SneakyThrows
    private static void writeOutput(OutputStream outputStream, String text) {
        @Cleanup PrintStream out = new PrintStream(outputStream, true, StandardCharsets.UTF_8.name());
        out.print(text);
    }

    public ConfigPostprocessor write(@NonNull OutputStream outputStream) {
        writeOutput(outputStream, this.context);
        return this;
    }

    public ConfigPostprocessor removeLines(@NonNull Predicate<String> shouldRemove) {

        String[] lines = this.context.split("\n");
        StringBuilder buf = new StringBuilder();

        for (String line : lines) {
            if (shouldRemove.test(line)) {
                continue;
            }
            buf.append(line).append("\n");
        }

        this.context = buf.toString();
        return this;
    }

    public ConfigPostprocessor removeLinesUntil(@NonNull Predicate<String> shouldStop) {

        String[] lines = this.context.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!shouldStop.test(line)) {
                continue;
            }
            String[] remaining = Arrays.copyOfRange(lines, i, lines.length);
            this.context = String.join("\n", remaining);
            break;
        }

        return this;
    }

    public ConfigPostprocessor updateLines(@NonNull Function<String, String> transform) {

        String[] lines = this.context.split("\n");
        StringBuilder buf = new StringBuilder();

        for (String line : lines) {
            buf.append(transform.apply(line)).append("\n");
        }

        this.context = buf.toString();
        return this;
    }

    public ConfigPostprocessor updateContext(@NonNull Function<String, String> transform) {
        this.context = transform.apply(this.context);
        return this;
    }
}
