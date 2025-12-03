package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import lombok.NonNull;

/**
 * Bidirectional type transformer for two-way value conversions.
 * <p>
 * When registered, creates two {@link ObjectTransformer} instances:
 * one for L→R and one for R→L conversion.
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * public class StringIntTransformer extends BidirectionalTransformer<String, Integer> {
 *     @Override
 *     public GenericsPair<String, Integer> getPair() {
 *         return genericsPair(String.class, Integer.class);
 *     }
 *
 *     @Override
 *     public Integer leftToRight(String data, SerdesContext context) {
 *         return Integer.parseInt(data);
 *     }
 *
 *     @Override
 *     public String rightToLeft(Integer data, SerdesContext context) {
 *         return data.toString();
 *     }
 * }
 * }</pre>
 *
 * @param <L> left type
 * @param <R> right type
 * @see ObjectTransformer for unidirectional conversions
 */
public abstract class BidirectionalTransformer<L, R> implements OkaeriSerdes {

    @Override
    public void register(@NonNull SerdesRegistry registry) {
        registry.register(this);
    }

    /**
     * Returns the type pair for this transformer.
     * Used as the registry key for both directions.
     *
     * @return the type pair (left type, right type)
     */
    public abstract GenericsPair<L, R> getPair();

    /**
     * Transforms data from left type to right type.
     *
     * @param data the source data
     * @param serdesContext the serialization context
     * @return the transformed value
     */
    public abstract R leftToRight(@NonNull L data, @NonNull SerdesContext serdesContext);

    /**
     * Transforms data from right type to left type.
     *
     * @param data the source data
     * @param serdesContext the serialization context
     * @return the transformed value
     */
    public abstract L rightToLeft(@NonNull R data, @NonNull SerdesContext serdesContext);

    /**
     * Helper to create a type pair from {@link GenericsDeclaration} instances.
     *
     * @param from the source type declaration
     * @param to the destination type declaration
     * @return the type pair
     */
    protected GenericsPair<L, R> generics(@NonNull GenericsDeclaration from, @NonNull GenericsDeclaration to) {
        return new GenericsPair<>(from, to);
    }

    /**
     * Helper to create a type pair from classes.
     *
     * @param from the source class
     * @param to the destination class
     * @return the type pair
     */
    protected GenericsPair<L, R> genericsPair(@NonNull Class<L> from, @NonNull Class<R> to) {
        return new GenericsPair<>(GenericsDeclaration.of(from), GenericsDeclaration.of(to));
    }
}
