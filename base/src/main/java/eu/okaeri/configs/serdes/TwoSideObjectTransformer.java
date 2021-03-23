package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;

public abstract class TwoSideObjectTransformer<L, R> {

    public abstract GenericsPair getPair();

    public abstract R leftToRight(L data);

    public abstract L rightToLeft(R data);

    protected GenericsPair generics(GenericsDeclaration from, GenericsDeclaration to) {
        return new GenericsPair(from, to);
    }

    protected GenericsPair genericsPair(Class<?> from, Class<?> to) {
        return new GenericsPair(GenericsDeclaration.of(from), GenericsDeclaration.of(to));
    }
}
