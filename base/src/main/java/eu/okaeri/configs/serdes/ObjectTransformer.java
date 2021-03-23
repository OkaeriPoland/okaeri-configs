package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;

public abstract class ObjectTransformer<S, D> {

    public abstract GenericsPair getPair();

    public abstract D transform(S data);

    protected GenericsPair genericsPair(Class<?> from, Class<?> to) {
        return new GenericsPair(GenericsDeclaration.of(from), GenericsDeclaration.of(to));
    }
}
