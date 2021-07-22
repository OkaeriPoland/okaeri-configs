package eu.okaeri.configs.serdes.commons.duration;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;
import lombok.NonNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spec breaking duration transformer that aims at supporting
 * more noob friendly formats like '1h', '30m', '5s', etc.
 * <p>
 * Allows to also specify default duration unit for values
 * that do not have any suffix and are just plain number
 * using {@link DurationSpec} annotation.
 * <p>
 * It is also possible to define with the same annotation
 * the format in which the value would be saved back to
 * the string.
 */
public class DurationTransformer extends TwoSideObjectTransformer<String, Duration> {

    private static final Pattern SIMPLE_ISO_DURATION_PATTERN = Pattern.compile("PT(?<value>[0-9]+)(?<unit>H|M|S)");
    private static final Pattern SIMPLE_DURATION_PATTERN = Pattern.compile("(?<value>[0-9]+)(?<unit>h|m|s)");

    @Override
    public GenericsPair<String, Duration> getPair() {
        return this.genericsPair(String.class, Duration.class);
    }

    @Override
    public Duration leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {

        // parse simplified if applicable
        Matcher simpleDurationMatcher = SIMPLE_DURATION_PATTERN.matcher(data.toLowerCase(Locale.ROOT));
        if (simpleDurationMatcher.matches()) {
            // get value and unit
            long longValue = Long.parseLong(simpleDurationMatcher.group("value"));
            String unit = simpleDurationMatcher.group("unit");
            // resolve unit from shorthand
            switch (unit) {
                case "h":
                    return Duration.ofHours(longValue);
                case "m":
                    return Duration.ofMinutes(longValue);
                case "s":
                    return Duration.ofSeconds(longValue);
                default:
                    throw new IllegalArgumentException("Really, this one should not be possible: " + unit);
            }
        }

        // parse plain number duration if applicable
        if (data.matches("-?\\d+")) {

            // parse text as long
            long longValue = Long.parseLong(data);

            // resolve default unit
            TemporalUnit unit = serdesContext.getAttachment(DurationSpecData.class)
                    .map(DurationSpecData::getFallbackUnit)
                    .orElse(ChronoUnit.SECONDS);

            // create duration
            return Duration.of(longValue, unit);
        }

        // parse iso spec duration
        return Duration.parse(data);
    }

    @Override
    public String rightToLeft(@NonNull Duration data, @NonNull SerdesContext serdesContext) {

        // resolve save format
        DurationFormat durationFormat = serdesContext.getAttachment(DurationSpecData.class)
                .map(DurationSpecData::getFormat)
                .orElse(DurationFormat.SIMPLIFIED);

        // ISO format, no need for additional processing
        if (durationFormat == DurationFormat.ISO) {
            return data.toString();
        }

        // matcher for simplified format
        String stringDuration = data.toString();
        Matcher matcher = SIMPLE_ISO_DURATION_PATTERN.matcher(stringDuration);

        // check if simplified format is applicable and return if so
        if (matcher.matches()) {
            long longValue = Long.parseLong(matcher.group("value"));
            String unit = matcher.group("unit").toLowerCase(Locale.ROOT);
            return longValue + unit;
        }

        // not applicable, return ISO
        return stringDuration;
    }
}