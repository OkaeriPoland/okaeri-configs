package eu.okaeri.configs.serdes.standard;

import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.TransformerRegistry;

public class StandardSerdes implements OkaeriSerdesPack {

    @Override
    public void register(TransformerRegistry registry) {

        // some magic
        registry.register(new ObjectToStringTransformer());
        registry.register(new StringToStringTransformer());

        // standard types
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
