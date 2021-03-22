package eu.okaeri.configs.annotation;

import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public enum NameStrategy {

    IDENTITY("", ""),
    SNAKE_CASE("$1_$2", "(\\G(?!^)|\\b(?:[A-Z]{2}|[a-zA-Z][a-z]*))(?=[a-zA-Z]{2,}|\\d)([A-Z](?:[A-Z]|[a-z]*)|\\d+)"),
    HYPHEN_CASE("$1-$2", "(\\G(?!^)|\\b(?:[A-Z]{2}|[a-zA-Z][a-z]*))(?=[a-zA-Z]{2,}|\\d)([A-Z](?:[A-Z]|[a-z]*)|\\d+)"),
    ;

    private final String replacement;
    private final Pattern regex;

    NameStrategy(String replacement, String regex) {
        this.replacement = replacement;
        this.regex = Pattern.compile(regex);
    }
}
