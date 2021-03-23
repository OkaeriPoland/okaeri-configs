package eu.okaeri.configs.serdes;

public interface SimpleObjectTransformerExecutor<S, D> {
    D transform(S data);
}
