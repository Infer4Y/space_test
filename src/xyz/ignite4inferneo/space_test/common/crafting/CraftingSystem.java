package xyz.ignite4inferneo.space_test.common.crafting;

import java.util.*;

/**
 * IMPROVED: Complete crafting system with shaped and shapeless recipes
 *
 * Features:
 * - Shaped recipes (pattern-based, like crafting tables)
 * - Shapeless recipes (order doesn't matter, like planks from wood)
 * - Priority system (shaped recipes checked first)
 * - 2x2 and 3x3 grid support
 */
public class CraftingSystem {

    private static final List<Recipe> recipes = new ArrayList<>();

    /**
     * Register a crafting recipe
     */
    public static void registerRecipe(Recipe recipe) {
        recipes.add(recipe);
        System.out.println("[Crafting] Registered " + recipe.getType() + " recipe: " +
                recipe.getOutput() + " (requires: " + recipe.getIngredients() + ")");
    }

    /**
     * Find matching recipe for given grid (checks shaped first, then shapeless)
     */
    public static Recipe findRecipe(String[] grid, int gridSize) {
        // Try shaped recipes first (more specific)
        for (Recipe recipe : recipes) {
            if (recipe.getType() == RecipeType.SHAPED && recipe.matchesGrid(grid, gridSize)) {
                return recipe;
            }
        }

        // Then try shapeless recipes
        Map<String, Integer> available = countItems(grid);
        for (Recipe recipe : recipes) {
            if (recipe.getType() == RecipeType.SHAPELESS && recipe.matches(available)) {
                return recipe;
            }
        }

        return null;
    }

    /**
     * Find recipe by ingredients (for shapeless matching)
     */
    public static Recipe findRecipe(Map<String, Integer> available) {
        // Only check shapeless recipes
        for (Recipe recipe : recipes) {
            if (recipe.getType() == RecipeType.SHAPELESS && recipe.matches(available)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Count items in grid
     */
    private static Map<String, Integer> countItems(String[] grid) {
        Map<String, Integer> counts = new HashMap<>();
        for (String item : grid) {
            if (item != null && !item.isEmpty() && !item.equals("")) {
                counts.put(item, counts.getOrDefault(item, 0) + 1);
            }
        }
        return counts;
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

        // For shaped recipes
        private final String[] pattern;
        private final int patternWidth;
        private final int patternHeight;

        // Shapeless recipe
        public Recipe(String output, int outputCount, Map<String, Integer> ingredients) {
            this(output, outputCount, ingredients, RecipeType.SHAPELESS, null, 0, 0);
        }

        // Shaped recipe
        public Recipe(String output, int outputCount, String[] pattern, int width, int height) {
            this.output = output;
            this.outputCount = outputCount;
            this.type = RecipeType.SHAPED;
            this.pattern = pattern;
            this.patternWidth = width;
            this.patternHeight = height;

            // Count ingredients from pattern
            this.ingredients = new HashMap<>();
            for (String item : pattern) {
                if (item != null && !item.isEmpty()) {
                    ingredients.put(item, ingredients.getOrDefault(item, 0) + 1);
                }
            }
        }

        // Full constructor
        private Recipe(String output, int outputCount, Map<String, Integer> ingredients,
                       RecipeType type, String[] pattern, int width, int height) {
            this.output = output;
            this.outputCount = outputCount;
            this.ingredients = new HashMap<>(ingredients);
            this.type = type;
            this.pattern = pattern;
            this.patternWidth = width;
            this.patternHeight = height;
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
         * Check if available ingredients match this recipe (shapeless)
         */
        public boolean matches(Map<String, Integer> available) {
            if (type == RecipeType.SHAPED) return false;

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
         * Check if grid matches this shaped recipe
         */
        public boolean matchesGrid(String[] grid, int gridSize) {
            if (type != RecipeType.SHAPED) return false;

            // Pattern must fit in grid
            if (patternWidth > gridSize || patternHeight > gridSize) {
                return false;
            }

            // Try all possible positions in the grid
            for (int offsetY = 0; offsetY <= gridSize - patternHeight; offsetY++) {
                for (int offsetX = 0; offsetX <= gridSize - patternWidth; offsetX++) {
                    if (matchesAtPosition(grid, gridSize, offsetX, offsetY)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Check if pattern matches at specific position in grid
         */
        private boolean matchesAtPosition(String[] grid, int gridSize, int offsetX, int offsetY) {
            // Check if pattern matches at this offset
            for (int py = 0; py < patternHeight; py++) {
                for (int px = 0; px < patternWidth; px++) {
                    int gridX = offsetX + px;
                    int gridY = offsetY + py;
                    int gridIndex = gridY * gridSize + gridX;
                    int patternIndex = py * patternWidth + px;

                    String gridItem = (gridIndex < grid.length) ? grid[gridIndex] : null;
                    String patternItem = pattern[patternIndex];

                    // Normalize empty strings
                    if (gridItem != null && gridItem.isEmpty()) gridItem = null;
                    if (patternItem != null && patternItem.isEmpty()) patternItem = null;

                    // Check match
                    if (patternItem == null) {
                        if (gridItem != null) return false; // Pattern expects empty, grid has item
                    } else {
                        if (!patternItem.equals(gridItem)) return false; // Mismatch
                    }
                }
            }

            // Check that rest of grid is empty
            for (int gy = 0; gy < gridSize; gy++) {
                for (int gx = 0; gx < gridSize; gx++) {
                    int gridIndex = gy * gridSize + gx;
                    String gridItem = (gridIndex < grid.length) ? grid[gridIndex] : null;

                    if (gridItem != null && !gridItem.isEmpty()) {
                        // Is this position within the pattern area?
                        int patternX = gx - offsetX;
                        int patternY = gy - offsetY;

                        if (patternX < 0 || patternX >= patternWidth ||
                                patternY < 0 || patternY >= patternHeight) {
                            return false; // Item outside pattern area
                        }
                    }
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
        SHAPED      // Grid pattern matters
    }

    /**
     * Builder for easy recipe creation
     */
    public static class RecipeBuilder {
        private String output;
        private int outputCount = 1;
        private Map<String, Integer> ingredients = new HashMap<>();
        private RecipeType type = RecipeType.SHAPELESS;
        private String[] pattern;
        private int patternWidth;
        private int patternHeight;

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

        /**
         * Set shaped pattern (2x2)
         * Example: pattern("AB", "CD") where A,B,C,D are item IDs or null
         */
        public RecipeBuilder pattern(String... rows) {
            this.type = RecipeType.SHAPED;
            this.patternHeight = rows.length;
            this.patternWidth = rows[0].length();

            // Convert string pattern to array
            this.pattern = new String[patternWidth * patternHeight];
            for (int y = 0; y < patternHeight; y++) {
                String row = rows[y];
                for (int x = 0; x < patternWidth; x++) {
                    char c = x < row.length() ? row.charAt(x) : ' ';
                    pattern[y * patternWidth + x] = c == ' ' ? null : String.valueOf(c);
                }
            }

            return this;
        }

        /**
         * Set shaped pattern with item mapping
         * Example:
         *   pattern("AAA", "A A", "AAA")
         *   key('A', "space_test:planks")
         */
        public RecipeBuilder pattern(Map<Character, String> key, String... rows) {
            this.type = RecipeType.SHAPED;
            this.patternHeight = rows.length;
            this.patternWidth = rows[0].length();

            this.pattern = new String[patternWidth * patternHeight];
            for (int y = 0; y < patternHeight; y++) {
                String row = rows[y];
                for (int x = 0; x < patternWidth; x++) {
                    char c = x < row.length() ? row.charAt(x) : ' ';
                    pattern[y * patternWidth + x] = (c == ' ') ? null : key.get(c);
                }
            }

            return this;
        }

        public Recipe build() {
            if (type == RecipeType.SHAPED) {
                return new Recipe(output, outputCount, pattern, patternWidth, patternHeight);
            } else {
                return new Recipe(output, outputCount, ingredients);
            }
        }

        public void register() {
            CraftingSystem.registerRecipe(build());
        }
    }
}