package eu.okaeri.configs.schema;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenericsPair {

    private GenericsDeclaration from;
    private GenericsDeclaration to;

    public GenericsPair reverse() {
        return new GenericsPair(this.to, this.from);
    }
}
