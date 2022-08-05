package eu.okaeri.configs.postprocessor;

import java.util.List;

public interface ConfigSectionWalker {

    boolean isKey(String line);

    boolean isKeyMultilineStart(String line);

    boolean isKeyCollectionStart(String line);

    char getCollectionStartChar();

    String readName(String line);

    String update(String line, ConfigLineInfo lineInfo, List<ConfigLineInfo> path);
}
