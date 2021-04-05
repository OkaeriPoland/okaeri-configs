package eu.okaeri.configs.serdes;

import eu.okaeri.configs.serdes.impl.*;

public class DefaultSerdes implements OkaeriSerdesPack {

    @Override
    public void register(TransformerRegistry registry) {

        registry.register(new ObjectToStringTransformer());
        registry.register(new StringToStringTransformer());

        registry.registerWithReversedToString(new StringToBigDecimalTransformer());
        registry.registerWithReversedToString(new StringToBigIntegerTransformer());
        registry.registerWithReversedToString(new StringToBooleanTransformer());
        registry.registerWithReversedToString(new StringToByteTransformer());
        registry.registerWithReversedToString(new StringToCharacterTransformer());
        registry.registerWithReversedToString(new StringToDoubleTransformer());
        registry.registerWithReversedToString(new StringToFloatTransformer());
        registry.registerWithReversedToString(new StringToIntegerTransformer());
        registry.registerWithReversedToString(new StringToLongTransformer());
        registry.registerWithReversedToString(new StringToShortTransformer());
        registry.registerWithReversedToString(new StringToUuidTransformer());
    }
}
