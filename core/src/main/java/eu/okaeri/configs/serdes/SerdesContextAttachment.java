package eu.okaeri.configs.serdes;

/**
 * Marker interface for annotation-resolved data attached to {@link SerdesContext}.
 * <p>
 * Implementations hold data extracted from field annotations by {@link SerdesAnnotationResolver}.
 * Access via {@link SerdesContext#getAttachment(Class)} during serialization/deserialization.
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * @Data
 * public class DurationSpecData implements SerdesContextAttachment {
 *     private final ChronoUnit fallbackUnit;
 *     private final DurationFormat format;
 * }
 * }</pre>
 *
 * @see SerdesAnnotationResolver
 * @see SerdesContext#getAttachment(Class)
 */
public interface SerdesContextAttachment {
}
