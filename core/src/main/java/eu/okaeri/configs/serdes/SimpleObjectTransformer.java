package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsPair;
import lombok.NonNull;

public abstract class SimpleObjectTransformer {

    private SimpleObjectTransformer() {
    }

    public static <S, D> ObjectTransformer<S, D> of(@NonNull Class<S> from, @NonNull Class<D> to, @NonNull SimpleObjectTransformerExecutor<S, D> transformer) {
        return new ObjectTransformer<S, D>() {
            @Override
            public GenericsPair getPair() {
                return this.genericsPair(from, to);
            }

            @Override
            public D transform(S data) {
                return transformer.transform(data);
            }
        };
    }
}
