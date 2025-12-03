package eu.okaeri.configs.serdes;

import lombok.NonNull;

/**
 * Base interface for all serialization/deserialization components.
 * <p>
 * All serdes types implement this interface, enabling unified registration:
 * <ul>
 *   <li>{@link ObjectTransformer} - converts between two types</li>
 *   <li>{@link BidirectionalTransformer} - bidirectional type conversion</li>
 *   <li>{@link ObjectSerializer} - serializes complex objects to/from maps</li>
 *   <li>{@link SerdesAnnotationResolver} - resolves field annotations</li>
 * </ul>
 *
 * <h2>Transformer vs Serializer</h2>
 *
 * <h3>ObjectTransformer</h3>
 * <p>
 * Best for <b>simple value-to-value conversions</b> where types are known at registration time.
 * <p>
 * <b>Pros:</b>
 * <ul>
 *   <li>Fast O(1) lookup via HashMap using exact type pair (from→to) as key</li>
 * </ul>
 * <b>Cons:</b>
 * <ul>
 *   <li><b>Single value output only</b> - cannot produce maps or complex structures</li>
 *   <li>Exact type matching - no inheritance or interface support</li>
 *   <li>Must register each type pair explicitly (String→Integer, String→Long, etc.)</li>
 * </ul>
 * <b>Use when:</b> Converting between primitive wrappers, enums, or simple types
 * (e.g., String↔Integer, String↔UUID, String↔Duration)
 *
 * <h3>ObjectSerializer</h3>
 * <p>
 * More flexible alternative that can handle <b>anything a transformer can, and more</b>.
 * <p>
 * <b>Pros:</b>
 * <ul>
 *   <li>Flexible type matching via {@code supports(Class)} - can handle inheritance hierarchies</li>
 *   <li><b>Can output maps/objects</b> with multiple keys via {@link SerializationData}</li>
 *   <li>Can handle polymorphic types (one serializer for interface + all implementations)</li>
 * </ul>
 * <b>Cons:</b>
 * <ul>
 *   <li>Slower O(n) lookup - iterates through all serializers calling {@code supports()}</li>
 * </ul>
 * <b>Use when:</b> You need flexible type matching, inheritance support, or multi-key output
 *
 * <h3>Quick Reference</h3>
 * <pre>
 * ┌─────────────────┬────────────────────────┬────────────────────────┐
 * │                 │ ObjectTransformer      │ ObjectSerializer       │
 * ├─────────────────┼────────────────────────┼────────────────────────┤
 * │ Output          │ Single value only      │ Map/Object (multi-key) │
 * │ Lookup          │ O(1) HashMap           │ O(n) iteration         │
 * │ Type matching   │ Exact pair             │ supports() method      │
 * │ Inheritance     │ No                     │ Yes                    │
 * │ Example         │ String → Integer       │ Location → {x,y,z}     │
 * └─────────────────┴────────────────────────┴────────────────────────┘
 * </pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Register multiple components at once
 * registry.add(
 *     new MyTransformer(),
 *     new MySerializer(),
 *     new MySerdesPack()
 * );
 *
 * // Or via config options
 * opt.serdes(
 *     new SerdesCommons(),
 *     new MyCustomTransformer()
 * );
 * }</pre>
 *
 * <h2>Creating a Serdes Pack</h2>
 * <pre>{@code
 * public class MySerdes implements OkaeriSerdes {
 *     public void register(SerdesRegistry registry) {
 *         registry.add(
 *             new FooTransformer(),
 *             new BarSerializer()
 *         );
 *     }
 * }
 * }</pre>
 *
 * @see SerdesRegistry
 * @see ObjectTransformer
 * @see ObjectSerializer
 */
public interface OkaeriSerdes {

    /**
     * Registers this component with the given registry.
     * <p>
     * For individual components (transformers, serializers), this method calls
     * the appropriate registration method on the registry. For packs, this method
     * typically registers multiple components.
     *
     * @param registry the registry to register with
     */
    void register(@NonNull SerdesRegistry registry);
}
