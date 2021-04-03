package eu.okaeri.configs.postprocessor;

import lombok.Data;

@Data
public class ConfigLineInfo {

    public static ConfigLineInfo of(int indent, int change, String name) {
        ConfigLineInfo info = new ConfigLineInfo();
        info.indent = indent;
        info.change = change;
        info.name = name;
        return info;
    }

    private int indent;
    private int change;
    private String name;
}
