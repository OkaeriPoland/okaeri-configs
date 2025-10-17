package eu.okaeri.configs.yaml.bukkit.serdes.serializer;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Not included in default {@link eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit}
 * due to plugin instance requirement. Register when needed as follows:
 *
 * <pre>
 * {@code registry.register(new SerdesBukkit());}
 * {@code registry.register(new ShapedRecipeSerializer(JavaPlugin.getPlugin(MyPlugin.class)));}
 * </pre>
 */
@RequiredArgsConstructor
public class ShapedRecipeSerializer implements ObjectSerializer<ShapedRecipe> {

    private static Boolean hasNamespacedKey = null;
    private static Boolean hasRecipeChoices = null;

    private final Plugin plugin;

    @Override
    public boolean supports(@NonNull Class<?> type) {
        return ShapedRecipe.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull ShapedRecipe shapedRecipe, @NonNull SerializationData data, @NonNull GenericsDeclaration generics) {

        // 1.8.8 does not have NamespacedKey
        if (hasNamespacedKey()) {
            data.add("key", shapedRecipe.getKey().getKey());
        }

        // okaeri-configs cannot store arrays
        List<String> shapeList = Arrays.asList(shapedRecipe.getShape());
        data.addCollection("shape", shapeList, String.class);

        // 1.13+ allows exact ItemStack as ingredient
        if (hasRecipeChoices()) {
            Map<String, List> choices = shapedRecipe.getChoiceMap().entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()),
                    entry -> {
                        if (entry.getValue() instanceof RecipeChoice.ExactChoice) {
                            List<ItemStack> stacks = ((RecipeChoice.ExactChoice) entry.getValue()).getChoices();
                            GenericsDeclaration listType = GenericsDeclaration.of(List.class, Collections.singletonList(ItemStack.class));
                            return (List) data.getConfigurer().simplifyCollection(stacks, listType, data.getContext(), true);
                        }
                        if (entry.getValue() instanceof RecipeChoice.MaterialChoice) {
                            List<Material> materials = ((RecipeChoice.MaterialChoice) entry.getValue()).getChoices();
                            GenericsDeclaration listType = GenericsDeclaration.of(List.class, Collections.singletonList(Material.class));
                            return (List) data.getConfigurer().simplifyCollection(materials, listType, data.getContext(), true);
                        }
                        throw new IllegalArgumentException("Unknown choice type in recipe: " + entry.getValue().getClass() + " [" + entry + "]");
                    },
                    (u, v) -> {
                        throw new IllegalStateException("Duplicate recipe key u=" + u + ", v=" + v);
                    },
                    LinkedHashMap::new
                ));
            data.addRaw("ingredients", choices);
        }
        // versions below allow only Material/MaterialData
        else {
            Map<Character, List<Material>> ingredientMap = shapedRecipe.getIngredientMap().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> Collections.singletonList(entry.getValue().getType()),
                    (u, v) -> {
                        throw new IllegalStateException("Duplicate recipe key u=" + u + ", v=" + v);
                    },
                    LinkedHashMap::new
                ));
            GenericsDeclaration valueType = GenericsDeclaration.of(List.class, Collections.singletonList(Material.class));
            data.addAsMap("ingredients", ingredientMap, GenericsDeclaration.of(Map.class, Arrays.asList(Character.class, valueType)));
        }

        // finally, the result!
        data.add("result", shapedRecipe.getResult(), ItemStack.class);
    }

    @Override
    public ShapedRecipe deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {

        ItemStack result = data.get("result", ItemStack.class);
        List<String> shape = data.getAsList("shape", String.class);

        ShapedRecipe recipe = hasNamespacedKey()
            ? new ShapedRecipe(new NamespacedKey(this.plugin, data.get("key", String.class)), result)
            : new ShapedRecipe(result);

        recipe.shape(shape.toArray(new String[0]));
        Map<Character, Object> ingredients = data.getAsMap("ingredients", Character.class, Object.class);

        for (Map.Entry<Character, Object> entry : ingredients.entrySet()) {

            if (!(entry.getValue() instanceof Collection)) {
                throw new IllegalArgumentException("Unknown recipe ingredient for " + entry.getKey() + ": " + entry.getValue() + " (" + entry.getValue().getClass() + ")");
            }

            List<?> list = (List<?>) entry.getValue();
            if (list.isEmpty()) {
                throw new IllegalArgumentException("Empty ingredients list for " + entry.getKey());
            }

            // 1.13+ recipe choices
            if (hasRecipeChoices()) {
                // string for enum Material (MaterialChoice)
                Object firstElement = list.get(0);
                if (firstElement instanceof String) {
                    recipe.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(list.stream()
                        .map(String.class::cast)
                        .map(Material::valueOf)
                        .collect(Collectors.toList())));
                }
                // map for ItemStack (ExactChoice)
                else if (firstElement instanceof Map) {
                    recipe.setIngredient(entry.getKey(), new RecipeChoice.ExactChoice(list.stream()
                        .map(map -> data.getConfigurer().resolveType(
                            map,
                            GenericsDeclaration.of(map),
                            ItemStack.class,
                            GenericsDeclaration.of(ItemStack.class),
                            data.getContext()
                        ))
                        .collect(Collectors.toList())));
                }
                // huh?
                else {
                    throw new IllegalArgumentException("Unknown recipe ingredient type for " + entry.getKey() + ": " + firstElement.getClass());
                }
            }
            // pre 1.13, just single material
            else {
                // not available in this version
                if (list.size() > 1) {
                    throw new IllegalArgumentException("Recipes with more than one Material are not allowed on this version: " + list);
                }
                // for the sake of compatibility serialization always uses list
                Object firstElement = list.get(0);
                if (firstElement instanceof String) {
                    Material material = Material.valueOf((String) firstElement);
                    recipe.setIngredient(entry.getKey(), material);
                }
                // huh?
                else {
                    throw new IllegalArgumentException("Unknown recipe ingredient type for " + entry.getKey() + ": " + firstElement.getClass());
                }
            }
        }

        return recipe;
    }

    private static boolean hasNamespacedKey() {
        if (hasNamespacedKey == null) {
            try {
                Class.forName("org.bukkit.NamespacedKey");
                hasNamespacedKey = true;
            } catch (ClassNotFoundException ignored) {
                hasNamespacedKey = false;
            }
        }
        return hasNamespacedKey;
    }

    private static boolean hasRecipeChoices() {
        if (hasRecipeChoices == null) {
            try {
                Class.forName("org.bukkit.inventory.RecipeChoice");
                hasRecipeChoices = true;
            } catch (ClassNotFoundException ignored) {
                hasRecipeChoices = false;
            }
        }
        return hasRecipeChoices;
    }
}
