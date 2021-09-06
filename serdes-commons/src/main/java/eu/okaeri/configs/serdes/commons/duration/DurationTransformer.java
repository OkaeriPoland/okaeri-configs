package eu.okaeri.configs.serdes.commons.duration;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.TwoSideObjectTransformer;
import lombok.NonNull;

import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spec breaking duration transformer that aims at supporting
 * more noob friendly formats and combinations like:
 * - 7d
 * - 1h
 * - 30m
 * - 5s
 * - 100ms
 * - 233ns
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

    private static final Pattern SIMPLE_ISO_DURATION_PATTERN = Pattern.compile("PT(?<value>-?[0-9]+)(?<unit>H|M|S)");
    private static final Pattern SUBSEC_ISO_DURATION_PATTERN = Pattern.compile("PT(?<value>-?[0-9]\\.[0-9]+)S?");
    private static final Pattern SIMPLE_DURATION_PATTERN = Pattern.compile("(?<value>-?[0-9]+)(?<unit>ms|ns|d|h|m|s)");
    private static final Pattern JBOD_FULL_DURATION_PATTERN = Pattern.compile("((-?[0-9]+)(ms|ns|d|h|m|s))+");

    @Override
    public GenericsPair<String, Duration> getPair() {
        return this.genericsPair(String.class, Duration.class);
    }

    @Override
    public Duration leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {

        // try reading jbod formats
        Optional<Duration> jbodResult = readJbodPattern(data);
        if (jbodResult.isPresent()) {
            return jbodResult.get();
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

        // resolve default unit
        TemporalUnit fallbackUnit = serdesContext.getAttachment(DurationSpecData.class)
                .map(DurationSpecData::getFallbackUnit)
                .orElse(ChronoUnit.SECONDS);

        // ISO format, no need for additional processing
        if (durationFormat == DurationFormat.ISO) {
            return data.toString();
        }

        // preserve zero
        if (data.isZero()) {
            return "0";
        }

        // matcher for simplified format
        String stringDuration = data.toString();
        Matcher matcher = SIMPLE_ISO_DURATION_PATTERN.matcher(stringDuration);

        // check if simplified format is applicable and return if so
        if (matcher.matches()) {
            // get value and unit
            long longValue = Long.parseLong(matcher.group("value"));
            String unit = matcher.group("unit").toLowerCase(Locale.ROOT);
            // value represents multiple of full days in hours and fallback unit is not hours
            if ("h".equals(unit) && ((longValue % 24) == 0) && (fallbackUnit != ChronoUnit.HOURS)) {
                return (longValue < 0 ? "-" : "") + (longValue / 24) + "d";
            }
            // save as provided by Duration
            return (longValue < 0 ? "-" : "") + longValue + unit;
        }

        // matcher for sub-second format
        Matcher subsecMatcher = SUBSEC_ISO_DURATION_PATTERN.matcher(stringDuration);

        // check of sub-second format is applicable and return if so
        if (subsecMatcher.matches()) {
            // get value in seconds
            double doubleValue = Double.parseDouble(subsecMatcher.group("value"));
            // check if negative
            boolean negative = doubleValue < 0;
            // convert to absolute for scale checking
            double absoluteValue = Math.abs(doubleValue);
            // resolve biggest possible unit
            int msValue = (int) Math.round(absoluteValue * 1.0e3d);
            if (msValue > 0) {
                return (negative ? "-" : "") + msValue + "ms";
            }
            int nsValue = (int) Math.ceil(absoluteValue * 1.0e9d);
            return (negative ? "-" : "") + nsValue + "ns";
        }

        // not applicable, return ISO
        return stringDuration;
    }

    /**
     * Converts raw units to {@link Duration}.
     *
     * @param longValue amount of units
     * @param unit      string unit representation
     * @return resolved duration
     */
    private static Duration timeToDuration(long longValue, String unit) {
        switch (unit) {
            case "d":
                return Duration.ofDays(longValue);
            case "h":
                return Duration.ofHours(longValue);
            case "m":
                return Duration.ofMinutes(longValue);
            case "s":
                return Duration.ofSeconds(longValue);
            case "ms":
                return Duration.ofMillis(longValue);
            case "ns":
                return Duration.ofNanos(longValue);
            default:
                throw new IllegalArgumentException("Really, this one should not be possible: " + unit);
        }
    }

    /**
     * Reads "Just a Bunch of Durations" patterns.
     * <p>
     * Example valid formats:
     * - 14d
     * - 14D
     * - 14d12h
     * - 14d 12h
     * - 14d1d
     * - 14d6H30m30s
     * - 30s
     * <p>
     * Example invalid formats:
     * - 14d huh 6h ???
     * - 1y
     *
     * @param text value to be parsed
     * @return parsed {@link Duration} or empty when failed to parse
     */
    private static Optional<Duration> readJbodPattern(String text) {

        // basic preprocessing & cleanup
        text = text.toLowerCase(Locale.ROOT);
        text = text.replace(" ", "");

        // does not match full pattern (may contain bloat)
        Matcher fullMatcher = JBOD_FULL_DURATION_PATTERN.matcher(text);
        if (!fullMatcher.matches()) {
            return Optional.empty();
        }

        // match duration elements one by one
        Matcher matcher = SIMPLE_DURATION_PATTERN.matcher(text);
        boolean matched = false;
        BigInteger currentValue = BigInteger.valueOf(0);

        while (matcher.find()) {
            matched = true;
            long longValue = Long.parseLong(matcher.group("value"));
            String unit = matcher.group("unit");
            currentValue = currentValue.add(BigInteger.valueOf(timeToDuration(longValue, unit).toNanos()));
        }

        // if found and only if found return duration
        return matched ? Optional.of(Duration.ofNanos(currentValue.longValueExact())) : Optional.empty();
    }
}