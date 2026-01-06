package xyz.ignite4inferneo.space_test.common;

import xyz.ignite4inferneo.space_test.api.item.Item;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.item.*;

/**
 * Registers all vanilla (base game) items
 * Called during game initialization
 */
public class VanillaItems {

    public static void register() {
        System.out.println("[VanillaItems] Registering vanilla items...");

        // ===== BUILDING BLOCKS (as items) =====
        registerBlockItem("space_test:stone", "Stone", 0);
        registerBlockItem("space_test:dirt", "Dirt", 1);
        registerBlockItem("space_test:grass", "Grass Block", 3);
        registerBlockItem("space_test:wood", "Oak Wood", 4);
        registerBlockItem("space_test:planks", "Oak Planks", 4);
        registerBlockItem("space_test:leaves", "Oak Leaves", 5);
        registerBlockItem("space_test:crafting_table", "Crafting Table", 4);
        registerBlockItem("space_test:furnace", "Furnace", 0);
        registerBlockItem("space_test:chest", "Chest", 4);

        // ===== TOOLS =====
        // Wooden tools
        Registries.ITEMS.register("space_test:wooden_pickaxe",
                new ToolItem("space_test:wooden_pickaxe", "Wooden Pickaxe", 10, ToolType.PICKAXE, ToolTier.WOOD));

        Registries.ITEMS.register("space_test:wooden_axe",
                new ToolItem("space_test:wooden_axe", "Wooden Axe", 11, ToolType.AXE, ToolTier.WOOD));

        Registries.ITEMS.register("space_test:wooden_shovel",
                new ToolItem("space_test:wooden_shovel", "Wooden Shovel", 12, ToolType.SHOVEL, ToolTier.WOOD));

        Registries.ITEMS.register("space_test:wooden_sword",
                new WeaponItem("space_test:wooden_sword", "Wooden Sword", 13, 4.0f, ToolTier.WOOD));

        // Stone tools
        Registries.ITEMS.register("space_test:stone_pickaxe",
                new ToolItem("space_test:stone_pickaxe", "Stone Pickaxe", 14, ToolType.PICKAXE, ToolTier.STONE));

        Registries.ITEMS.register("space_test:stone_axe",
                new ToolItem("space_test:stone_axe", "Stone Axe", 15, ToolType.AXE, ToolTier.STONE));

        Registries.ITEMS.register("space_test:stone_shovel",
                new ToolItem("space_test:stone_shovel", "Stone Shovel", 16, ToolType.SHOVEL, ToolTier.STONE));

        Registries.ITEMS.register("space_test:stone_sword",
                new WeaponItem("space_test:stone_sword", "Stone Sword", 17, 5.0f, ToolTier.STONE));

        // Iron tools
        Registries.ITEMS.register("space_test:iron_pickaxe",
                new ToolItem("space_test:iron_pickaxe", "Iron Pickaxe", 18, ToolType.PICKAXE, ToolTier.IRON));

        Registries.ITEMS.register("space_test:iron_axe",
                new ToolItem("space_test:iron_axe", "Iron Axe", 19, ToolType.AXE, ToolTier.IRON));

        Registries.ITEMS.register("space_test:iron_shovel",
                new ToolItem("space_test:iron_shovel", "Iron Shovel", 20, ToolType.SHOVEL, ToolTier.IRON));

        Registries.ITEMS.register("space_test:iron_sword",
                new WeaponItem("space_test:iron_sword", "Iron Sword", 21, 6.0f, ToolTier.IRON));

        // ===== MATERIALS =====
        Registries.ITEMS.register("space_test:stick",
                new BaseItem("space_test:stick", "Stick", 22));

        Registries.ITEMS.register("space_test:coal",
                new FuelItem("space_test:coal", "Coal", 23, 1600));

        Registries.ITEMS.register("space_test:iron_ingot",
                new BaseItem("space_test:iron_ingot", "Iron Ingot", 24));

        Registries.ITEMS.register("space_test:gold_ingot",
                new BaseItem("space_test:gold_ingot", "Gold Ingot", 25));

        Registries.ITEMS.register("space_test:diamond",
                new BaseItem("space_test:diamond", "Diamond", 26));

        // ===== FOOD =====
        Registries.ITEMS.register("space_test:apple",
                new FoodItem("space_test:apple", "Apple", 27, 4, 2.4f));

        Registries.ITEMS.register("space_test:bread",
                new FoodItem("space_test:bread", "Bread", 28, 5, 6.0f));

        Registries.ITEMS.register("space_test:cooked_porkchop",
                new FoodItem("space_test:cooked_porkchop", "Cooked Porkchop", 29, 8, 12.8f));

        Registries.ITEMS.register("space_test:raw_porkchop",
                new FoodItem("space_test:raw_porkchop", "Raw Porkchop", 30, 3, 1.8f));

        // ===== SPECIAL ITEMS =====
        Registries.ITEMS.register("space_test:torch",
                new BaseItem("space_test:torch", "Torch", 31, 64));

        Registries.ITEMS.register("space_test:bucket",
                new BaseItem("space_test:bucket", "Bucket", 32, 16));

        Registries.ITEMS.register("space_test:water_bucket",
                new BaseItem("space_test:water_bucket", "Water Bucket", 33, 1));

        System.out.println("[VanillaItems] Registered " + Registries.ITEMS.size() + " items");
    }

    /**
     * Helper to register block items (items that place blocks)
     */
    private static void registerBlockItem(String blockId, String name, int textureIndex) {
        Registries.ITEMS.register(blockId, new BlockItem(blockId, name, textureIndex, blockId));
    }
}