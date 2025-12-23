package xyz.ignite4inferneo.space_test.client.gui;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.crafting.CraftingSystem;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Crafting GUI renderer
 */
public class CraftingGUI {

    private static final int SLOT_SIZE = 36;
    private static final int SLOT_PADDING = 4;

    // 2x2 crafting grid (in inventory)
    private static final ItemStack[] craftingGrid2x2 = new ItemStack[4];

    // 3x3 crafting grid (crafting table)
    private static final ItemStack[] craftingGrid3x3 = new ItemStack[9];

    // Current crafting output
    private static ItemStack craftingOutput = ItemStack.EMPTY;

    static {
        // Initialize grids
        for (int i = 0; i < 4; i++) {
            craftingGrid2x2[i] = ItemStack.EMPTY;
        }
        for (int i = 0; i < 9; i++) {
            craftingGrid3x3[i] = ItemStack.EMPTY;
        }
    }

    /**
     * Render 2x2 crafting grid (in inventory)
     */
    public static void render2x2Grid(Graphics2D g, int screenWidth, int screenHeight) {
        int invWidth = Inventory.INVENTORY_COLS * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int startX = (screenWidth - invWidth) / 2 + invWidth + 20; // To the right of inventory
        int startY = (screenHeight / 2) - 100;

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Crafting", startX, startY);

        startY += 20;

        // Background
        int gridWidth = 2 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int gridHeight = 2 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2 + 60; // Extra for output
        g.setColor(new Color(40, 40, 40, 240));
        g.fillRect(startX - 5, startY - 5, gridWidth + 10, gridHeight + 10);

        // Crafting grid (2x2)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int slotX = startX + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = startY + row * (SLOT_SIZE + SLOT_PADDING);

                g.setColor(new Color(60, 60, 60, 200));
                g.fillRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
                g.setColor(new Color(100, 100, 100));
                g.drawRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

                // Render item
                ItemStack stack = craftingGrid2x2[row * 2 + col];
                if (!stack.isEmpty()) {
                    InventoryRenderer.renderItemStackAt(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
                }
            }
        }

        // Arrow
        int arrowY = startY + SLOT_SIZE / 2;
        g.setColor(Color.WHITE);
        g.drawString("→", startX + gridWidth / 2 - 10, arrowY + 25);

        // Output slot
        int outputX = startX + (gridWidth - SLOT_SIZE) / 2;
        int outputY = startY + 2 * (SLOT_SIZE + SLOT_PADDING) + 20;

        g.setColor(new Color(80, 80, 80, 200));
        g.fillRect(outputX, outputY, SLOT_SIZE, SLOT_SIZE);
        g.setColor(Color.WHITE);
        g.drawRect(outputX, outputY, SLOT_SIZE, SLOT_SIZE);

        if (!craftingOutput.isEmpty()) {
            InventoryRenderer.renderItemStackAt(g, craftingOutput, outputX + 2, outputY + 2, SLOT_SIZE - 4);
        }
    }

    /**
     * Render 3x3 crafting grid (crafting table)
     */
    public static void render3x3Grid(Graphics2D g, int screenWidth, int screenHeight) {
        int startX = (screenWidth / 2) - 150;
        int startY = (screenHeight / 2) - 120;

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Crafting Table", startX, startY);

        startY += 25;

        // Background
        int gridWidth = 3 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int gridHeight = 3 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2 + 60;
        g.setColor(new Color(40, 40, 40, 240));
        g.fillRect(startX - 5, startY - 5, gridWidth + 80, gridHeight + 10);

        // Crafting grid (3x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = startX + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = startY + row * (SLOT_SIZE + SLOT_PADDING);

                g.setColor(new Color(60, 60, 60, 200));
                g.fillRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
                g.setColor(new Color(100, 100, 100));
                g.drawRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

                // Render item
                ItemStack stack = craftingGrid3x3[row * 3 + col];
                if (!stack.isEmpty()) {
                    InventoryRenderer.renderItemStackAt(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
                }
            }
        }

        // Arrow
        int arrowX = startX + gridWidth + 10;
        int arrowY = startY + gridHeight / 2 - 10;
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("→", arrowX, arrowY);

        // Output slot
        int outputX = arrowX + 30;
        int outputY = startY + (gridHeight - SLOT_SIZE) / 2;

        g.setColor(new Color(80, 80, 80, 200));
        g.fillRect(outputX, outputY, SLOT_SIZE, SLOT_SIZE);
        g.setColor(Color.WHITE);
        g.drawRect(outputX, outputY, SLOT_SIZE, SLOT_SIZE);

        if (!craftingOutput.isEmpty()) {
            InventoryRenderer.renderItemStackAt(g, craftingOutput, outputX + 2, outputY + 2, SLOT_SIZE - 4);
        }
    }

    /**
     * Update crafting output based on grid contents
     */
    public static void updateCrafting(boolean use3x3) {
        ItemStack[] grid = use3x3 ? craftingGrid3x3 : craftingGrid2x2;

        // Count ingredients
        Map<String, Integer> ingredients = new HashMap<>();
        for (ItemStack stack : grid) {
            if (!stack.isEmpty()) {
                String id = stack.getBlockId();
                ingredients.put(id, ingredients.getOrDefault(id, 0) + stack.getCount());
            }
        }

        // Find matching recipe
        CraftingSystem.Recipe recipe = CraftingSystem.findRecipe(ingredients);

        if (recipe != null) {
            craftingOutput = new ItemStack(recipe.getOutput(), recipe.getOutputCount());
        } else {
            craftingOutput = ItemStack.EMPTY;
        }
    }

    /**
     * Handle click in crafting grid
     */
    public static boolean handleCraftingClick(int mouseX, int mouseY, boolean use3x3, int screenWidth, int screenHeight) {
        // TODO: Implement click handling for adding/removing items from crafting grid
        return false;
    }

    /**
     * Craft item (consume ingredients and give output)
     */
    public static ItemStack craftItem(Inventory playerInventory, boolean use3x3) {
        if (craftingOutput.isEmpty()) return ItemStack.EMPTY;

        ItemStack[] grid = use3x3 ? craftingGrid3x3 : craftingGrid2x2;

        // Count ingredients
        Map<String, Integer> ingredients = new HashMap<>();
        for (ItemStack stack : grid) {
            if (!stack.isEmpty()) {
                String id = stack.getBlockId();
                ingredients.put(id, ingredients.getOrDefault(id, 0) + stack.getCount());
            }
        }

        // Find and consume recipe
        CraftingSystem.Recipe recipe = CraftingSystem.findRecipe(ingredients);
        if (recipe != null && recipe.matches(ingredients)) {
            // Consume ingredients from grid
            for (int i = 0; i < grid.length; i++) {
                if (!grid[i].isEmpty()) {
                    grid[i] = grid[i].shrink(1);
                }
            }

            // Update crafting
            updateCrafting(use3x3);

            // Return output
            return new ItemStack(recipe.getOutput(), recipe.getOutputCount());
        }

        return ItemStack.EMPTY;
    }

    /**
     * Clear crafting grid
     */
    public static void clearGrid(boolean use3x3) {
        ItemStack[] grid = use3x3 ? craftingGrid3x3 : craftingGrid2x2;
        for (int i = 0; i < grid.length; i++) {
            grid[i] = ItemStack.EMPTY;
        }
        craftingOutput = ItemStack.EMPTY;
    }

    public static ItemStack[] getGrid2x2() {
        return craftingGrid2x2;
    }

    public static ItemStack[] getGrid3x3() {
        return craftingGrid3x3;
    }

    public static ItemStack getCraftingOutput() {
        return craftingOutput;
    }
}