package eu.okaeri.configs.postprocessor.format;

import eu.okaeri.configs.postprocessor.ConfigSectionWalker;

public abstract class YamlSectionWalker implements ConfigSectionWalker {

    @Override
    public boolean isKeyMultilineStart(String line) {
        String trimmed = line.trim();
        return !line.isEmpty() && (trimmed.endsWith(">")  || trimmed.endsWith(">-") || trimmed.endsWith("|") || trimmed.endsWith("|-"));
    }

    @Override
    public boolean isKey(String line) {
        String name = this.readName(line);
        return !name.isEmpty() && (name.charAt(0) != '-') && (name.charAt(0) != '#');
    }

    @Override
    public String readName(String line) {
        return line.split(":", 2)[0].trim();
    }
}
