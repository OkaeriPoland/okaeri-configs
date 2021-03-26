package eu.okaeri.configs.bukkit.serdes.impl;

//public class ShapedRecipeSerializer implements ObjectSerializer<ShapedRecipe> {
//
//    @Override
//    public Class<? super ShapedRecipe> getType() {
//        return ShapedRecipe.class;
//    }
//
//    @Override
//    public void serialize(ShapedRecipe shapedRecipe, SerializationData data) {
//
//        try {
//            Class.forName("org.bukkit.NamespacedKey");
//            data.add("key", shapedRecipe.getKey().getKey());
//        } catch (ClassNotFoundException ignored) {}
//
//        data.add("ingredients", shapedRecipe.getIngredientMap().entrySet().stream()
//                    .map(entry -> entry.getKey() + "=" + entry.getValue().getType().name())
//                    .collect(Collectors.joining("\n")));
//        data.add("result", shapedRecipe.getResult());
//        data.addCollection("shape", Arrays.asList(shapedRecipe.getShape()), String.class);
//    }
//
//    @Override
//    public ShapedRecipe deserialize(DeserializationData data, GenericsDeclaration generics) {
//
//        Map<Character, String> ingredients = new HashMap<>();
//        String ingredientsList = data.get("ingredients", String.class);
//        String[] ingredientsLines = StringUtils.split(ingredientsList);
//
//        if (ingredientsLines == null) {
//            throw new IllegalArgumentException("ingredientsLines cannot be null");
//        }
//
//        for (String ingredientsLine : ingredientsLines) {
//            String[] ingredientEntry = StringUtils.split(ingredientsLine, '=');
//            if (ingredientEntry.length != 2) {
//                continue;
//            }
//            ingredients.put(ingredientEntry[0].charAt(0), ingredientEntry[1]);
//        }
//
//        ItemStack result = data.get("result", ItemStack.class);
//        List<String> shape = data.getAsList("shape", String.class);
//
//        ShapedRecipe recipe;
//        try {
//            Class.forName("org.bukkit.NamespacedKey");
//            String key = data.get("key", String.class);
//            NamespacedKey namespacedKey = new NamespacedKey(CorePlugin.getCorePlugin(), key);
//            recipe = new ShapedRecipe(namespacedKey, result);
//        } catch (ClassNotFoundException exception) {
//            recipe = new ShapedRecipe(result);
//        }
//
//        recipe.shape(shape.toArray(new String[0]));
//        for (Map.Entry<Character, String> entry : ingredients.entrySet()) {
//            Material material = Material.valueOf(entry.getValue());
//            recipe.setIngredient(entry.getKey(), material);
//        }
//
//        return recipe;
//    }
//}
