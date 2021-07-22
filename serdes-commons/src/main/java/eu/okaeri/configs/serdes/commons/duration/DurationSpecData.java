package eu.okaeri.configs.serdes.commons.duration;

import eu.okaeri.configs.serdes.SerdesContextAttachment;
import lombok.Value;

import java.time.temporal.TemporalUnit;

@Value(staticConstructor = "of")
public class DurationSpecData implements SerdesContextAttachment {
    private final TemporalUnit fallbackUnit;
    private final DurationFormat format;
}
