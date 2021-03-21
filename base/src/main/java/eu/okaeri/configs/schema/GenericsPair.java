package eu.okaeri.configs.schema;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenericsPair {
    private GenericsDeclaration from;
    private GenericsDeclaration to;
}
