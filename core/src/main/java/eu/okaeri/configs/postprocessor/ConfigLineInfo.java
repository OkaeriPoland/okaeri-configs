package eu.okaeri.configs.postprocessor;

import lombok.Data;
import lombok.NonNull;

@Data
public class ConfigLineInfo {

    private int indent;
    private int change;
    private String name;

    public static ConfigLineInfo of(int indent, int change, @NonNull String name) {
        ConfigLineInfo info = new ConfigLineInfo();
        info.indent = indent;
        info.change = change;
        info.name = name;
        return info;
    }
}
