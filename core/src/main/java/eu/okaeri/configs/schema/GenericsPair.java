package eu.okaeri.configs.schema;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenericsPair<L, R> {

    private GenericsDeclaration from;
    private GenericsDeclaration to;

    public GenericsPair<R, L> reverse() {
        return new GenericsPair<R, L>(this.to, this.from);
    }
}
