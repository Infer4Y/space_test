package xyz.ignite4inferneo.space_test.common.crafting;

import java.util.*;

/**
 * FIXED: Complete crafting system with proper recipe matching
 */
public class CraftingSystem {

    private static final List<Recipe> recipes = new ArrayList<>();

    /**
     * Register a crafting recipe
     */
    public static void registerRecipe(Recipe recipe) {
        recipes.add(recipe);
        System.out.println("[Crafting] Registered recipe: " + recipe.getOutput() +
                " (requires: " + recipe.getIngredients() + ")");
    }

    /**
     * Find matching recipe for given ingredients
     * FIXED: Now properly checks if you have AT LEAST the required amounts
     */
    public static Recipe findRecipe(Map<String, Integer> available) {
        for (Recipe recipe : recipes) {
            if (recipe.matches(available)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Get all recipes
     */
    public static Collection<Recipe> getAllRecipes() {
        return recipes;
    }

    /**
     * Check if can craft with given ingredients
     */
    public static boolean canCraft(Map<String, Integer> available, String output) {
        Recipe recipe = findRecipeByOutput(output);
        return recipe != null && recipe.matches(available);
    }

    /**
     * Find recipe by output item
     */
    public static Recipe findRecipeByOutput(String output) {
        for (Recipe recipe : recipes) {
            if (recipe.getOutput().equals(output)) {
                return recipe;
            }
        }
        return null;
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
         * FIXED: Check if available ingredients match this recipe
         * Returns true if you have AT LEAST the required amounts
         */
        public boolean matches(Map<String, Integer> available) {
            // Check each required ingredient
            for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
                String item = entry.getKey();
                int required = entry.getValue();
                int has = available.getOrDefault(item, 0);

                if (has < required) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Get total items needed for this recipe
         */
        public int getTotalItemsNeeded() {
            return ingredients.values().stream().mapToInt(Integer::intValue).sum();
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