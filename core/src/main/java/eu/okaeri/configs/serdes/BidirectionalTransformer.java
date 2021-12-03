package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import lombok.NonNull;

public abstract class BidirectionalTransformer<L, R> {

    public abstract GenericsPair<L, R> getPair();

    public abstract R leftToRight(@NonNull L data, @NonNull SerdesContext serdesContext);

    public abstract L rightToLeft(@NonNull R data, @NonNull SerdesContext serdesContext);

    protected GenericsPair<L, R> generics(@NonNull GenericsDeclaration from, @NonNull GenericsDeclaration to) {
        return new GenericsPair<>(from, to);
    }

    protected GenericsPair<L, R> genericsPair(@NonNull Class<L> from, @NonNull Class<R> to) {
        return new GenericsPair<>(GenericsDeclaration.of(from), GenericsDeclaration.of(to));
    }
}
