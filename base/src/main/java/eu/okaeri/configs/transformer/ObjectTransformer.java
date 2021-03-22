package eu.okaeri.configs.transformer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;

import java.util.Arrays;
import java.util.List;

public abstract class ObjectTransformer<S, D> {

    public abstract GenericsPair getPair();

    public abstract D transform(S data);

    protected GenericsPair generics(GenericsDeclaration from, GenericsDeclaration to) {
        return new GenericsPair(from, to);
    }

    protected GenericsPair genericsPair(Class<?> from, Class<?> to) {
        return new GenericsPair(this.declaration(from), this.declaration(to));
    }

    protected GenericsDeclaration declaration(Class<?> type) {
        return new GenericsDeclaration(type);
    }

    protected GenericsDeclaration declaration(Class<?> type, List<GenericsDeclaration> subtypes) {
        return new GenericsDeclaration(type, subtypes);
    }

    protected GenericsDeclaration declaration(Class<?> type, GenericsDeclaration... subtypes) {
        return new GenericsDeclaration(type, Arrays.asList(subtypes));
    }
}
