package xyz.ignite4inferneo.space_test.common;

import xyz.ignite4inferneo.space_test.common.crafting.CraftingSystem;

/**
 * Register all vanilla crafting recipes
 */
public class VanillaRecipes {

    public static void register() {
        System.out.println("[VanillaRecipes] Registering crafting recipes...");

        // Planks from wood
        new CraftingSystem.RecipeBuilder()
                .output("space_test:planks")
                .outputCount(4)
                .ingredient("space_test:wood", 1)
                .register();

        // Sticks from planks
        new CraftingSystem.RecipeBuilder()
                .output("space_test:stick")
                .outputCount(4)
                .ingredient("space_test:planks", 2)
                .register();

        // Crafting table from planks
        new CraftingSystem.RecipeBuilder()
                .output("space_test:crafting_table")
                .outputCount(1)
                .ingredient("space_test:planks", 4)
                .register();

        // Wooden pickaxe
        new CraftingSystem.RecipeBuilder()
                .output("space_test:wooden_pickaxe")
                .outputCount(1)
                .ingredient("space_test:planks", 3)
                .ingredient("space_test:stick", 2)
                .register();

        // Wooden axe
        new CraftingSystem.RecipeBuilder()
                .output("space_test:wooden_axe")
                .outputCount(1)
                .ingredient("space_test:planks", 3)
                .ingredient("space_test:stick", 2)
                .register();

        // Wooden shovel
        new CraftingSystem.RecipeBuilder()
                .output("space_test:wooden_shovel")
                .outputCount(1)
                .ingredient("space_test:planks", 1)
                .ingredient("space_test:stick", 2)
                .register();

        // Stone pickaxe
        new CraftingSystem.RecipeBuilder()
                .output("space_test:stone_pickaxe")
                .outputCount(1)
                .ingredient("space_test:stone", 3)
                .ingredient("space_test:stick", 2)
                .register();

        // Furnace
        new CraftingSystem.RecipeBuilder()
                .output("space_test:furnace")
                .outputCount(1)
                .ingredient("space_test:stone", 8)
                .register();

        // Chest
        new CraftingSystem.RecipeBuilder()
                .output("space_test:chest")
                .outputCount(1)
                .ingredient("space_test:planks", 8)
                .register();

        // Torch (from stick and coal/charcoal)
        new CraftingSystem.RecipeBuilder()
                .output("space_test:torch")
                .outputCount(4)
                .ingredient("space_test:stick", 1)
                .ingredient("space_test:coal", 1)
                .register();

        System.out.println("[VanillaRecipes] Registered " +
                CraftingSystem.getAllRecipes().size() + " recipes");
    }
}