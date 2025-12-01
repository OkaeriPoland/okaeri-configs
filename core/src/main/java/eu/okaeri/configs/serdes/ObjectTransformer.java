package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import lombok.NonNull;

public abstract class ObjectTransformer<S, D> {

    /**
     * Returns the original class for error reporting.
     * For wrapped BidirectionalTransformers, returns the original transformer class.
     */
    public Class<?> getOriginalClass() {
        return this.getClass();
    }

    public abstract GenericsPair<S, D> getPair();

    public abstract D transform(S data, SerdesContext context);

    protected GenericsPair<S, D> genericsPair(@NonNull Class<S> from, @NonNull Class<D> to) {
        return new GenericsPair<>(GenericsDeclaration.of(from), GenericsDeclaration.of(to));
    }
}
