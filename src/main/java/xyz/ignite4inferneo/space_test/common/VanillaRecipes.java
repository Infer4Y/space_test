package xyz.ignite4inferneo.space_test.common;

import xyz.ignite4inferneo.space_test.common.crafting.CraftingSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Register all vanilla crafting recipes with proper shaped/shapeless support
 */
public class VanillaRecipes {

    public static void register() {
        System.out.println("[VanillaRecipes] Registering crafting recipes...");

        // ===== SHAPELESS RECIPES =====

        // Planks from wood (shapeless - 1 wood = 4 planks)
        new CraftingSystem.RecipeBuilder()
                .output("space_test:planks")
                .outputCount(4)
                .ingredient("space_test:wood", 1)
                .type(CraftingSystem.RecipeType.SHAPELESS)
                .register();

        // Sticks from planks (shapeless - 2 planks = 4 sticks)
        new CraftingSystem.RecipeBuilder()
                .output("space_test:stick")
                .outputCount(4)
                .ingredient("space_test:planks", 2)
                .type(CraftingSystem.RecipeType.SHAPELESS)
                .register();

        // ===== SHAPED RECIPES (2x2 - can be made in inventory) =====

        // Crafting table (2x2 pattern)
        Map<Character, String> craftingTableKey = new HashMap<>();
        craftingTableKey.put('P', "space_test:planks");

        new CraftingSystem.RecipeBuilder()
                .output("space_test:crafting_table")
                .outputCount(1)
                .pattern(craftingTableKey,
                        "PP",
                        "PP")
                .register();

        // ===== SHAPED RECIPES (3x3 - requires crafting table) =====

        // Wooden pickaxe
        Map<Character, String> woodenPickaxeKey = new HashMap<>();
        woodenPickaxeKey.put('P', "space_test:planks");
        woodenPickaxeKey.put('S', "space_test:stick");

        new CraftingSystem.RecipeBuilder()
                .output("space_test:wooden_pickaxe")
                .outputCount(1)
                .pattern(woodenPickaxeKey,
                        "PPP",
                        " S ",
                        " S ")
                .register();

        // Wooden axe
        new CraftingSystem.RecipeBuilder()
                .output("space_test:wooden_axe")
                .outputCount(1)
                .pattern(woodenPickaxeKey,
                        "PP ",
                        "PS ",
                        " S ")
                .register();

        // Wooden shovel
        new CraftingSystem.RecipeBuilder()
                .output("space_test:wooden_shovel")
                .outputCount(1)
                .pattern(woodenPickaxeKey,
                        " P ",
                        " S ",
                        " S ")
                .register();

        // Stone pickaxe
        Map<Character, String> stonePickaxeKey = new HashMap<>();
        stonePickaxeKey.put('C', "space_test:stone");
        stonePickaxeKey.put('S', "space_test:stick");

        new CraftingSystem.RecipeBuilder()
                .output("space_test:stone_pickaxe")
                .outputCount(1)
                .pattern(stonePickaxeKey,
                        "CCC",
                        " S ",
                        " S ")
                .register();

        // Stone axe
        new CraftingSystem.RecipeBuilder()
                .output("space_test:stone_axe")
                .outputCount(1)
                .pattern(stonePickaxeKey,
                        "CC ",
                        "CS ",
                        " S ")
                .register();

        // Stone shovel
        new CraftingSystem.RecipeBuilder()
                .output("space_test:stone_shovel")
                .outputCount(1)
                .pattern(stonePickaxeKey,
                        " C ",
                        " S ",
                        " S ")
                .register();

        // Furnace (8 stone in ring)
        Map<Character, String> furnaceKey = new HashMap<>();
        furnaceKey.put('S', "space_test:stone");

        new CraftingSystem.RecipeBuilder()
                .output("space_test:furnace")
                .outputCount(1)
                .pattern(furnaceKey,
                        "SSS",
                        "S S",
                        "SSS")
                .register();

        // Chest (8 planks in ring)
        Map<Character, String> chestKey = new HashMap<>();
        chestKey.put('P', "space_test:planks");

        new CraftingSystem.RecipeBuilder()
                .output("space_test:chest")
                .outputCount(1)
                .pattern(chestKey,
                        "PPP",
                        "P P",
                        "PPP")
                .register();

        // Torch (stick + coal)
        Map<Character, String> torchKey = new HashMap<>();
        torchKey.put('C', "space_test:coal");
        torchKey.put('S', "space_test:stick");

        new CraftingSystem.RecipeBuilder()
                .output("space_test:torch")
                .outputCount(4)
                .pattern(torchKey,
                        " C ",
                        " S ",
                        "   ")
                .register();

        System.out.println("[VanillaRecipes] Registered " +
                CraftingSystem.getAllRecipes().size() + " recipes");
    }
}