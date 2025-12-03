package eu.okaeri.configs.serdes;

import eu.okaeri.configs.schema.GenericsDeclaration;
import lombok.NonNull;

/**
 * Serializer for complex objects that need flexible type matching or multi-key output.
 * <p>
 * Unlike {@link ObjectTransformer}, serializers:
 * <ul>
 *   <li>Use {@link #supports(Class)} for flexible type matching (inheritance, interfaces)</li>
 *   <li>Can output maps with multiple keys via {@link SerializationData}</li>
 *   <li>Can output single values using the {@link #VALUE} magic key</li>
 * </ul>
 * <p>
 * <b>Example (multi-key output):</b>
 * <pre>{@code
 * public class LocationSerializer implements ObjectSerializer<Location> {
 *     @Override
 *     public boolean supports(Class<?> type) {
 *         return Location.class.isAssignableFrom(type);
 *     }
 *
 *     @Override
 *     public void serialize(Location loc, SerializationData data, GenericsDeclaration generics) {
 *         data.set("world", loc.getWorld().getName());
 *         data.set("x", loc.getX());
 *         data.set("y", loc.getY());
 *         data.set("z", loc.getZ());
 *     }
 *
 *     @Override
 *     public Location deserialize(DeserializationData data, GenericsDeclaration generics) {
 *         return new Location(
 *             Bukkit.getWorld(data.get("world", String.class)),
 *             data.get("x", Double.class),
 *             data.get("y", Double.class),
 *             data.get("z", Double.class)
 *         );
 *     }
 * }
 * }</pre>
 * <p>
 * <b>Example (single value output):</b>
 * <pre>{@code
 * @Override
 * public void serialize(Duration duration, SerializationData data, GenericsDeclaration generics) {
 *     data.setValue(duration.toString());  // outputs: "PT1H30M"
 * }
 * }</pre>
 *
 * @param <T> the type this serializer handles
 * @see ObjectTransformer for simple value-to-value conversions
 * @see SerializationData
 * @see DeserializationData
 */
public interface ObjectSerializer<T> extends OkaeriSerdes {

    @Override
    default void register(@NonNull SerdesRegistry registry) {
        registry.register(this);
    }

    /**
     * Magic value representing the name of the key which allows
     * to replace the serialization result with the custom object.
     *
     * <strong>Warning: DO NOT USE 'THE STRING' DIRECTLY</strong>
     *
     * Example serialization:
     * - serializationData.setCollection(VALUE, Arrays.asList("my", "list"), String.class);
     *
     * Example deserialization:
     * - deserializationData.getAsList(VALUE, String.class)
     */
    String VALUE = "$$__value__$$";

    /**
     * @param type the type checked for compatibility
     * @return {@code true} if serializer is able to process the {@code type}
     */
    boolean supports(@NonNull Class<?> type);

    /**
     * @param object   the object to be serialized
     * @param data     the serialization data
     * @param generics the generic information about the {@code object}
     */
    void serialize(@NonNull T object, @NonNull SerializationData data, @NonNull GenericsDeclaration generics);

    /**
     * @param data     the source deserialization data
     * @param generics the target generic type for the {@code data}
     * @return the deserialized object
     */
    T deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics);
}
