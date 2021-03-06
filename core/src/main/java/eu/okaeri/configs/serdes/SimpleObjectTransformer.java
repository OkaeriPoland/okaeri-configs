package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsPair;
import lombok.NonNull;

public abstract class SimpleObjectTransformer {

    private SimpleObjectTransformer() {
    }

    public static <S, D> ObjectTransformer<S, D> of(@NonNull Class<S> from, @NonNull Class<D> to, @NonNull SimpleObjectTransformerExecutor<S, D> transformer) {
        return new ObjectTransformer<S, D>() {
            @Override
            public GenericsPair<S, D> getPair() {
                return this.genericsPair(from, to);
            }

            @Override
            public D transform(@NonNull S data, @NonNull SerdesContext serdesContext) {
                return transformer.transform(data);
            }
        };
    }
}
