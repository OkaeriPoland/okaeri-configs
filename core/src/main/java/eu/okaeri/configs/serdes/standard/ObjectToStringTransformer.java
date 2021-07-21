package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.ObjectTransformer;
import lombok.NonNull;

public class ObjectToStringTransformer extends ObjectTransformer<Object, String> {

    @Override
    public GenericsPair<Object, String> getPair() {
        return this.genericsPair(Object.class, String.class);
    }

    @Override
    public String transform(@NonNull Object data) {
        return data.toString();
    }
}
