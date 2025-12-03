package eu.okaeri.configs.serdes.okaeri.material;

import eu.okaeri.commons.bukkit.material.TagMaterialSet;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.HashSet;
import java.util.List;

public class TagMaterialSetSerializer implements ObjectSerializer<TagMaterialSet> {

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return TagMaterialSet.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull TagMaterialSet set, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {
        data.setCollection("tags", set.getTags(), Tag.class);
        data.setCollection("materials", set.getMaterials(), Material.class);
    }

    @Override
    public TagMaterialSet deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        List<Tag> tags = data.getAsList("tags", Tag.class);
        List<Material> materials = data.getAsList("materials", Material.class);

        return new TagMaterialSet(
            (tags == null) ? new HashSet<>() : new HashSet<>(tags),
            (materials == null) ? new HashSet<>() : new HashSet<>(materials)
        );
    }
}
