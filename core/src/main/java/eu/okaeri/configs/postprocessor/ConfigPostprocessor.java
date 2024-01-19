package eu.okaeri.configs.postprocessor;

import lombok.Cleanup;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public static int countIndent(@NonNull String line) {
        int whitespaces = 0;
        for (char c : line.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                return whitespaces;
            }
            whitespaces++;
        }
        return whitespaces;
    }

    public static String addIndent(@NonNull String line, int size) {

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < size; i++) buf.append(" ");
        String indent = buf.toString();

        return Arrays.stream(line.split("\n"))
            .map(part -> indent + part)
            .collect(Collectors.joining("\n"))
            + "\n";
    }

    public static String createCommentOrEmpty(String commentPrefix, String[] strings) {
        return (strings == null) ? "" : createComment(commentPrefix, strings);
    }

    public static String createComment(String commentPrefix, String[] strings) {

        if (strings == null) return null;
        if (commentPrefix == null) commentPrefix = "";
        List<String> lines = new ArrayList<>();

        for (String line : strings) {
            String[] parts = line.split("\n");
            String prefix = line.startsWith(commentPrefix.trim()) ? "" : commentPrefix;
            lines.add((line.isEmpty() ? "" : prefix) + line);
        }

        return String.join("\n", lines) + "\n";
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

    public ConfigPostprocessor removeLines(@NonNull ConfigLineFilter filter) {

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

    public ConfigPostprocessor updateLines(@NonNull ConfigContextManipulator manipulator) {

        String[] lines = this.context.split("\n");
        StringBuilder buf = new StringBuilder();

        for (String line : lines) {
            buf.append(manipulator.convert(line)).append("\n");
        }

        this.context = buf.toString();
        return this;
    }

    public ConfigPostprocessor updateLinesKeys(@NonNull ConfigSectionWalker walker) {

        String[] lines = this.context.split("\n");
        List<ConfigLineInfo> currentPath = new ArrayList<>();
        int lastIndent = 0;
        int level = 0;
        StringBuilder newContext = new StringBuilder();
        boolean multilineSkip = false;

        for (String line : lines) {

            int indent = countIndent(line);

            int collections = 0;

            /*
            Replace start collection chars to whitespaces
            Add 2 whitespaces to indent
            Example: "  - key:" to "    key:"
            */
            while (walker.isKeyCollectionStart(line)) {
                collections++;
                int index = line.indexOf(walker.getCollectionStartChar());
                line = replaceChar(line, index, ' ');
                indent += 2;
                multilineSkip = false;
            }

            int change = indent - lastIndent;
            String key = walker.readName(line);

            // skip non-keys
            if (!walker.isKey(line)) {
                newContext.append(line).append("\n");
                multilineSkip = false;
                continue;
            }

            if (currentPath.isEmpty()) {
                currentPath.add(ConfigLineInfo.of(indent, change, key));
            }

            if (change > 0) {
                if (!multilineSkip) {
                    level++;
                    currentPath.add(ConfigLineInfo.of(indent, change, key));
                }
            } else {
                if (change != 0) {
                    int step = 2;
                    level -= ((change * -1) / step);
                    currentPath = currentPath.subList(0, Math.max(Math.min(level + 1, currentPath.size() - 1), 0));
                    multilineSkip = false;
                }
                if (!multilineSkip) {
                    currentPath.set(currentPath.size() - 1, ConfigLineInfo.of(indent, change, key));
                }
            }

            lastIndent = indent;

            if (multilineSkip) {
                newContext.append(line).append("\n");
                continue;
            } else if (walker.isKeyMultilineStart(line)) {
                multilineSkip = true;
                level++;
            }

            String updatedLine = walker.update(line, currentPath.get(currentPath.size() - 1), currentPath);

            // Replace whitespaces to start collection chars
            while (collections != 0) {
                updatedLine = replaceChar(updatedLine, indent - collections * 2, walker.getCollectionStartChar());
                collections--;
            }

            newContext.append(updatedLine).append("\n");
        }

        this.context = newContext.toString();
        return this;
    }

    private String replaceChar(String line, int index, char c) {
        line = line.substring(0, index) + c + line.substring(index + 1);
        return line;
    }

    public ConfigPostprocessor updateContext(@NonNull ConfigContextManipulator manipulator) {
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
}
