package eu.okaeri.configs.serdes.okaeri.indexedset;

import eu.okaeri.commons.indexedset.*;
import eu.okaeri.configs.annotation.TargetType;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerdesContext;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Serializes {@link IndexedSet}
 * <p>
 * Config field examples:
 * - {@code IndexedSet<String, Arena> arenas = IndexedSet.of(Arena::getKey, new Arena(...))}
 * - {@code @IndexedSetSpec(key = "name") IndexedSet<String, Arena> arenas = IndexedSet.of(Arena::getName, new Arena(...)}
 * <p>
 * Use {@link IndexedSetSpec} annotation to specify custom key field name (default: key).
 * This applies to both:
 * - fields resulting from the custom {@link ObjectSerializer} implementations
 * - fields resulting from the {@link eu.okaeri.configs.OkaeriConfig} subconfig serialization
 * <p>
 * {@link IndexedSet} default value is required to provide {@code keyFunction} matching the spec field name.
 * Inconsistency between spec field name and keyFunction may and will yield this serializer unusable.
 * <p>
 * The {@code keyFunction} provides low-overhead runtime behavior for the {@link IndexedSet}
 * while {@link IndexedSetSpec} key name is used in the serialization/deserialization process for:
 * - hiding the key field from the serialized output
 * - adding the key field before further deserialization
 */
public class IndexedSetSerializer implements ObjectSerializer<IndexedSet> {

    @Override
    public boolean supports(@NonNull Class<? super IndexedSet> type) {
        return IndexedSet.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(@NonNull IndexedSet set, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {

        Configurer configurer = data.getConfigurer();
        GenericsDeclaration valueType = generics.getSubtypeAtOrThrow(0);
        GenericsDeclaration keyType = generics.getSubtypeAtOrThrow(1);

        String keyFieldName = data.getContext().getAttachment(IndexedSetSpecData.class)
                .map(IndexedSetSpecData::getKey)
                .orElse("key");

        // add all set elements to the serialization data
        for (Object key : set.keySet()) {

            // validate that element key is viable for transformation to string
            if (!configurer.isToStringObject(key, keyType)) {
                throw new IllegalArgumentException("Cannot transform IndexedSet's key to string: " + key + " [" + key.getClass() + "]");
            }

            // get string key and the data from the set
            String strKey = configurer.resolveType(key, keyType, String.class, null, SerdesContext.of(configurer));
            Object value = set.get(key);

            // manually simplify the element for further manipulation
            value = configurer.simplify(value, valueType, SerdesContext.of(configurer), true);

            // if simplified to map, hide key field
            if (value instanceof Map) {
                ((Map<?, ?>) value).remove(keyFieldName);
            }

            // add the data to the serialization output
            data.add(strKey, value, GenericsDeclaration.of(value));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public IndexedSet deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        Configurer configurer = data.getConfigurer();
        GenericsDeclaration targetValueType = generics.getSubtypeAtOrThrow(0);

        String keyFieldName = data.getContext().getAttachment(IndexedSetSpecData.class)
                .map(IndexedSetSpecData::getKey)
                .orElse("key");

        // extract current set for the keyFunction
        AbstractIndexedSet abstractIndexedSet = (AbstractIndexedSet) data.getContext().getField().getStartingValue();
        if (abstractIndexedSet == null) abstractIndexedSet = (AbstractIndexedSet) data.getContext().getField().getValue();

        // copy keyFunction to the new set and clear the current data
        IndexedSet set = this.createSetCopy(abstractIndexedSet, generics, data);
        set.clear();

        // resolve all data entries to the set elements
        for (Map.Entry<String, Object> entry : data.asMap().entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            // resolve type of current value to map for mutations
            GenericsDeclaration objectMapType = GenericsDeclaration.of(Map.class, Arrays.asList(String.class, Object.class));
            Map<String, Object> objectMap = configurer.resolveType(value, GenericsDeclaration.of(value), Map.class, objectMapType, SerdesContext.of(configurer));

            // add key as data and transform to the target generic type
            objectMap.put(keyFieldName, key);
            Object result = configurer.resolveType(objectMap, objectMapType, targetValueType.getType(), targetValueType, SerdesContext.of(configurer));

            // add to the set
            set.add(result);
        }

        return set;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private IndexedSet createSetCopy(@NonNull AbstractIndexedSet set, @NonNull GenericsDeclaration generics, DeserializationData data) {

        // resolve real target type
        if (generics.getType() == IndexedSet.class) {
            Optional<TargetType> targetTypeOptional = data.getContext().getFieldAnnotation(TargetType.class);
            if (targetTypeOptional.isPresent()) {
                TargetType targetType = targetTypeOptional.get();
                return this.createSetCopy(set, GenericsDeclaration.of(targetType.value()), null);
            }
            return new IndexedLinkedHashSet(set);
        }

        // standard types
        if (generics.getType() == IndexedLinkedHashSet.class) {
            return new IndexedLinkedHashSet(set);
        }

        if (generics.getType() == IndexedConcurrentHashSet.class) {
            return new IndexedConcurrentHashSet(set);
        }

        if (generics.getType() == IndexedHashSet.class) {
            return new IndexedHashSet(set);
        }

        // custom types
        try {
            Constructor<?> constructor = generics.getType().getConstructor(AbstractIndexedSet.class);
            return (IndexedSet) constructor.newInstance(set);
        } catch (NoSuchMethodException ignored) {
        }

        throw new IllegalArgumentException("Cannot copy the set into " + generics.getType() + "! Unknown set type: " + generics);
    }
}
