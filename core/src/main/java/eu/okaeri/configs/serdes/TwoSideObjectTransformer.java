package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import lombok.NonNull;

public abstract class TwoSideObjectTransformer<L, R> {

    public abstract GenericsPair getPair();

    public abstract R leftToRight(L data);

    public abstract L rightToLeft(R data);

    protected GenericsPair generics(@NonNull GenericsDeclaration from, @NonNull GenericsDeclaration to) {
        return new GenericsPair(from, to);
    }

    protected GenericsPair genericsPair(@NonNull Class<?> from, @NonNull Class<?> to) {
        return new GenericsPair(GenericsDeclaration.of(from), GenericsDeclaration.of(to));
    }
}
