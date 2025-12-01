package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StringToBooleanTransformer extends ObjectTransformer<String, Boolean> {

    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays.asList(
        "true", "yes", "on", "1", "enabled"
    ));
    private static final Set<String> FALSE_VALUES = new HashSet<>(Arrays.asList(
        "false", "no", "off", "0", "disabled"
    ));

    @Override
    public GenericsPair<String, Boolean> getPair() {
        return this.genericsPair(String.class, Boolean.class);
    }

    @Override
    public Boolean transform(@NonNull String data, @NonNull SerdesContext serdesContext) {
        String lower = data.toLowerCase();
        if (TRUE_VALUES.contains(lower)) {
            return true;
        }
        if (FALSE_VALUES.contains(lower)) {
            return false;
        }
        throw new IllegalArgumentException("Expected true or false");
    }
}
