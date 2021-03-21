package eu.okaeri.configs.transformer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.schema.GenericsPair;

import java.util.Arrays;
import java.util.List;

public interface ObjectTransformer<S, D> {

    GenericsPair getPair();

    D transform(S data);

    default GenericsPair generics(GenericsDeclaration from, GenericsDeclaration to) {
        return new GenericsPair(from, to);
    }

    default GenericsPair genericsPair(Class<?> from, Class<?> to) {
        return new GenericsPair(this.declaration(from), this.declaration(to));
    }

    default GenericsDeclaration declaration(Class<?> type) {
        return new GenericsDeclaration(type);
    }

    default GenericsDeclaration declaration(Class<?> type, List<GenericsDeclaration> subtypes) {
        return new GenericsDeclaration(type, subtypes);
    }

    default GenericsDeclaration declaration(Class<?> type, GenericsDeclaration... subtypes) {
        return new GenericsDeclaration(type, Arrays.asList(subtypes));
    }
}
