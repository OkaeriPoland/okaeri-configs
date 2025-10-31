package eu.okaeri.configs.serdes.okaeri.range;

import eu.okaeri.configs.serdes.SerdesContextAttachment;
import lombok.Value;

/**
 * Context attachment data for Range serialization.
 * Contains the format preference from the {@link RangeSpec} annotation.
 */
@Value(staticConstructor = "of")
public class RangeSpecData implements SerdesContextAttachment {
    RangeFormat format;
}
