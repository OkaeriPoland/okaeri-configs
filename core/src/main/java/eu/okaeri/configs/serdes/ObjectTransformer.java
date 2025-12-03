package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;
import lombok.NonNull;

/**
 * Unidirectional type transformer for simple value-to-value conversions.
 * <p>
 * Transforms data from source type {@code S} to destination type {@code D}.
 * Registered in {@link SerdesRegistry} using exact type pair as key for O(1) lookup.
 * <p>
 * <b>Limitations:</b>
 * <ul>
 *   <li>Single value output only - cannot produce maps or complex structures</li>
 *   <li>Exact type matching - no inheritance or interface support</li>
 * </ul>
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * public class StringToUUIDTransformer extends ObjectTransformer<String, UUID> {
 *     @Override
 *     public GenericsPair<String, UUID> getPair() {
 *         return genericsPair(String.class, UUID.class);
 *     }
 *
 *     @Override
 *     public UUID transform(String data, SerdesContext context) {
 *         return UUID.fromString(data);
 *     }
 * }
 * }</pre>
 *
 * @param <S> source type
 * @param <D> destination type
 * @see BidirectionalTransformer for two-way conversions
 * @see ObjectSerializer for complex objects or flexible type matching
 */
public abstract class ObjectTransformer<S, D> implements OkaeriSerdes {

    @Override
    public void register(@NonNull SerdesRegistry registry) {
        registry.register(this);
    }

    /**
     * Returns the original class for error reporting.
     * For wrapped {@link BidirectionalTransformer}s, returns the original transformer class.
     *
     * @return the class to use in error messages
     */
    public Class<?> getOriginalClass() {
        return this.getClass();
    }

    /**
     * Returns the type pair for this transformer.
     * Used as the registry key for O(1) lookup.
     *
     * @return the type pair (source type, destination type)
     */
    public abstract GenericsPair<S, D> getPair();

    /**
     * Transforms data from source type to destination type.
     *
     * @param data the source data to transform
     * @param context the serialization context
     * @return the transformed value
     */
    public abstract D transform(S data, SerdesContext context);

    /**
     * Helper to create a type pair from classes.
     *
     * @param from the source class
     * @param to the destination class
     * @return the type pair
     */
    protected GenericsPair<S, D> genericsPair(@NonNull Class<S> from, @NonNull Class<D> to) {
        return new GenericsPair<>(GenericsDeclaration.of(from), GenericsDeclaration.of(to));
    }
}
