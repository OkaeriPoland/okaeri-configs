package eu.okaeri.configs.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility for matching and suggesting enum values.
 */
public final class EnumMatcher {

    private static final int DEFAULT_MAX_SUGGESTIONS = 5;

    private EnumMatcher() {
    }

    /**
     * Generates a suggestion hint for an invalid enum value.
     *
     * @param input     the invalid input value
     * @param enumClass the target enum class
     * @return formatted hint like "Expected LOW, MEDIUM or HIGH"
     */
    public static String suggest(String input, Class<? extends Enum<?>> enumClass) {
        String[] names = Arrays.stream(enumClass.getEnumConstants())
            .map(Enum::name)
            .toArray(String[]::new);
        return suggest(input, names, DEFAULT_MAX_SUGGESTIONS);
    }

    /**
     * Generates a suggestion hint for an invalid enum value.
     *
     * @param input          the invalid input value
     * @param enumNames      all valid enum names
     * @param maxSuggestions maximum number of suggestions to show
     * @return formatted hint like "Expected LOW, MEDIUM or HIGH" or "Expected MEDIUM, HIGH, LOW (+7 more)"
     */
    public static String suggest(String input, String[] enumNames, int maxSuggestions) {
        if ((enumNames == null) || (enumNames.length == 0)) {
            return "???";
        }

        // Sort by similarity (lower distance = more similar)
        String inputUpper = input.toUpperCase();
        List<String> sorted = Arrays.stream(enumNames)
            .sorted(Comparator.comparingInt(name -> levenshtein(inputUpper, name.toUpperCase())))
            .limit(maxSuggestions)
            .collect(Collectors.toList());

        boolean truncated = enumNames.length > maxSuggestions;
        int remaining = enumNames.length - sorted.size();

        if (sorted.size() == 1) {
            return truncated
                ? ("Expected " + sorted.get(0) + " (+" + remaining + " more)")
                : ("Expected " + sorted.get(0));
        }

        StringBuilder sb = new StringBuilder("Expected ");
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                // Use "or" only when showing complete list
                sb.append((!truncated && (i == (sorted.size() - 1))) ? " or " : ", ");
            }
            sb.append(sorted.get(i));
        }

        if (truncated) {
            sb.append(" (+").append(remaining).append(" more)");
        }

        return sb.toString();
    }

    /**
     * Calculates Levenshtein (edit) distance between two strings.
     */
    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }
}
