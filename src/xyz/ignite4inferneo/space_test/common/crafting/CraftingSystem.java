package xyz.ignite4inferneo.space_test.common.crafting;

import java.util.*;

/**
 * Complete crafting system with recipes and pattern matching
 */
public class CraftingSystem {

    private static final Map<String, Recipe> recipes = new HashMap<>();

    /**
     * Register a crafting recipe
     */
    public static void registerRecipe(Recipe recipe) {
        recipes.put(recipe.getOutput(), recipe);
        System.out.println("[Crafting] Registered recipe: " + recipe.getOutput());
    }

    /**
     * Find matching recipe for given ingredients
     */
    public static Recipe findRecipe(Map<String, Integer> ingredients) {
        for (Recipe recipe : recipes.values()) {
            if (recipe.matches(ingredients)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Find recipe by output item
     */
    public static Recipe getRecipe(String output) {
        return recipes.get(output);
    }

    /**
     * Get all recipes
     */
    public static Collection<Recipe> getAllRecipes() {
        return recipes.values();
    }

    /**
     * Check if can craft with given ingredients
     */
    public static boolean canCraft(Map<String, Integer> ingredients, String output) {
        Recipe recipe = recipes.get(output);
        return recipe != null && recipe.matches(ingredients);
    }

    public static class Recipe {
        private final String output;
        private final int outputCount;
        private final Map<String, Integer> ingredients;
        private final RecipeType type;

        public Recipe(String output, int outputCount, Map<String, Integer> ingredients) {
            this(output, outputCount, ingredients, RecipeType.SHAPELESS);
        }

        public Recipe(String output, int outputCount, Map<String, Integer> ingredients, RecipeType type) {
            this.output = output;
            this.outputCount = outputCount;
            this.ingredients = new HashMap<>(ingredients);
            this.type = type;
        }

        public String getOutput() {
            return output;
        }

        public int getOutputCount() {
            return outputCount;
        }

        public Map<String, Integer> getIngredients() {
            return new HashMap<>(ingredients);
        }

        public RecipeType getType() {
            return type;
        }

        /**
         * Check if available ingredients match this recipe
         */
        public boolean matches(Map<String, Integer> available) {
            for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
                int required = entry.getValue();
                int has = available.getOrDefault(entry.getKey(), 0);
                if (has < required) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Consume ingredients from inventory
         */
        public void consumeIngredients(Map<String, Integer> available) {
            for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
                String item = entry.getKey();
                int required = entry.getValue();
                int has = available.get(item);
                available.put(item, has - required);
            }
        }
    }

    public enum RecipeType {
        SHAPELESS,  // Order doesn't matter
        SHAPED      // Grid pattern matters (3x3 crafting)
    }

    /**
     * Builder for easy recipe creation
     */
    public static class RecipeBuilder {
        private String output;
        private int outputCount = 1;
        private Map<String, Integer> ingredients = new HashMap<>();
        private RecipeType type = RecipeType.SHAPELESS;

        public RecipeBuilder output(String item) {
            this.output = item;
            return this;
        }

        public RecipeBuilder outputCount(int count) {
            this.outputCount = count;
            return this;
        }

        public RecipeBuilder ingredient(String item, int count) {
            ingredients.put(item, count);
            return this;
        }

        public RecipeBuilder type(RecipeType type) {
            this.type = type;
            return this;
        }

        public Recipe build() {
            return new Recipe(output, outputCount, ingredients, type);
        }

        public void register() {
            CraftingSystem.registerRecipe(build());
        }
    }
}