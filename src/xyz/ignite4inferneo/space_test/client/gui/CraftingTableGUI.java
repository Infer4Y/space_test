package xyz.ignite4inferneo.space_test.client.gui;

import xyz.ignite4inferneo.space_test.common.crafting.CraftingSystem;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Full crafting table GUI with 3x3 grid
 */
public class CraftingTableGUI {

    private static final int SLOT_SIZE = 36;
    private static final int SLOT_PADDING = 4;

    // 3x3 crafting grid
    private final ItemStack[] craftingGrid = new ItemStack[9];
    private ItemStack craftingOutput = ItemStack.EMPTY;

    public CraftingTableGUI() {
        for (int i = 0; i < 9; i++) {
            craftingGrid[i] = ItemStack.EMPTY;
        }
    }

    /**
     * Render crafting table GUI
     */
    public void render(Graphics2D g, Inventory inventory, int screenWidth, int screenHeight) {
        // Render player inventory at bottom
        renderPlayerInventory(g, inventory, screenWidth, screenHeight);

        // Render crafting grid at top
        renderCraftingGrid(g, screenWidth, screenHeight);
    }

    /**
     * Render player inventory (bottom half)
     */
    private void renderPlayerInventory(Graphics2D g, Inventory inventory, int screenWidth, int screenHeight) {
        int invWidth = Inventory.INVENTORY_COLS * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int startX = (screenWidth - invWidth) / 2;
        int startY = screenHeight - 200;

        // Background
        g.setColor(new Color(40, 40, 40, 240));
        int totalHeight = (Inventory.INVENTORY_ROWS + 1) * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2 + 20;
        g.fillRect(startX - 5, startY - 5, invWidth + 10, totalHeight);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Inventory", startX, startY - 10);

        // Main inventory (3 rows)
        for (int row = 0; row < Inventory.INVENTORY_ROWS; row++) {
            for (int col = 0; col < Inventory.INVENTORY_COLS; col++) {
                int slot = Inventory.HOTBAR_SIZE + row * Inventory.INVENTORY_COLS + col;
                int slotX = startX + SLOT_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = startY + row * (SLOT_SIZE + SLOT_PADDING);

                g.setColor(new Color(60, 60, 60, 200));
                g.fillRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
                g.setColor(new Color(100, 100, 100));
                g.drawRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

                ItemStack stack = inventory.getStack(slot);
                if (!stack.isEmpty()) {
                    InventoryRenderer.renderItemStackAt(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
                }
            }
        }

        // Hotbar
        int hotbarY = startY + Inventory.INVENTORY_ROWS * (SLOT_SIZE + SLOT_PADDING) + 10;
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            int slotX = startX + SLOT_PADDING + i * (SLOT_SIZE + SLOT_PADDING);

            boolean selected = (i == inventory.getSelectedSlot());
            g.setColor(selected ? new Color(255, 255, 255, 100) : new Color(60, 60, 60, 200));
            g.fillRect(slotX, hotbarY, SLOT_SIZE, SLOT_SIZE);
            g.setColor(selected ? Color.WHITE : new Color(100, 100, 100));
            g.drawRect(slotX, hotbarY, SLOT_SIZE, SLOT_SIZE);

            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                InventoryRenderer.renderItemStackAt(g, stack, slotX + 2, hotbarY + 2, SLOT_SIZE - 4);
            }
        }
    }

    /**
     * Render 3x3 crafting grid (top)
     */
    private void renderCraftingGrid(Graphics2D g, int screenWidth, int screenHeight) {
        int gridSize = 3 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int startX = (screenWidth - gridSize - 100) / 2;
        int startY = 80;

        // Background
        g.setColor(new Color(40, 40, 40, 240));
        g.fillRect(startX - 10, startY - 30, gridSize + 120, gridSize + 40);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Crafting Table", startX, startY - 10);

        // 3x3 Grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = startX + SLOT_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = startY + row * (SLOT_SIZE + SLOT_PADDING);

                g.setColor(new Color(60, 60, 60, 200));
                g.fillRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
                g.setColor(new Color(100, 100, 100));
                g.drawRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

                ItemStack stack = craftingGrid[row * 3 + col];
                if (!stack.isEmpty()) {
                    InventoryRenderer.renderItemStackAt(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
                }
            }
        }

        // Arrow
        int arrowX = startX + gridSize + 5;
        int arrowY = startY + gridSize / 2;
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("→", arrowX, arrowY);

        // Output slot
        int outputX = arrowX + 35;
        int outputY = startY + (gridSize - SLOT_SIZE) / 2;

        g.setColor(new Color(100, 100, 80, 200));
        g.fillRect(outputX, outputY, SLOT_SIZE, SLOT_SIZE);
        g.setColor(Color.YELLOW);
        g.drawRect(outputX, outputY, SLOT_SIZE, SLOT_SIZE);

        if (!craftingOutput.isEmpty()) {
            InventoryRenderer.renderItemStackAt(g, craftingOutput, outputX + 2, outputY + 2, SLOT_SIZE - 4);
        }
    }

    /**
     * Handle click in crafting GUI
     */
    public boolean handleClick(Inventory inventory, int screenWidth, int screenHeight,
                               int mouseX, int mouseY, boolean rightClick,
                               InventoryInteraction invInteraction) {

        // Check crafting grid slots
        int gridSlot = getCraftingSlotAt(screenWidth, screenHeight, mouseX, mouseY);
        if (gridSlot != -1) {
            handleCraftingSlotClick(gridSlot, invInteraction, rightClick);
            updateCrafting();
            return true;
        }

        // Check output slot
        if (isOutputSlotClicked(screenWidth, screenHeight, mouseX, mouseY)) {
            handleOutputClick(inventory, invInteraction);
            return true;
        }

        return false; // Not handled, try inventory
    }

    /**
     * Get crafting slot at mouse position
     */
    private int getCraftingSlotAt(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        int gridSize = 3 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int startX = (screenWidth - gridSize - 100) / 2;
        int startY = 80;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = startX + SLOT_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = startY + row * (SLOT_SIZE + SLOT_PADDING);

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    return row * 3 + col;
                }
            }
        }
        return -1;
    }

    /**
     * Check if output slot clicked
     */
    private boolean isOutputSlotClicked(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        int gridSize = 3 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int startX = (screenWidth - gridSize - 100) / 2;
        int startY = 80;
        int arrowX = startX + gridSize + 5;
        int outputX = arrowX + 35;
        int outputY = startY + (gridSize - SLOT_SIZE) / 2;

        return mouseX >= outputX && mouseX < outputX + SLOT_SIZE &&
                mouseY >= outputY && mouseY < outputY + SLOT_SIZE;
    }

    /**
     * Handle click on crafting grid slot
     */
    private void handleCraftingSlotClick(int slot, InventoryInteraction invInteraction, boolean rightClick) {
        ItemStack heldStack = invInteraction.getHeldStack();
        ItemStack slotStack = craftingGrid[slot];

        if (heldStack.isEmpty()) {
            // Pick up from slot
            if (!slotStack.isEmpty()) {
                if (rightClick && slotStack.getCount() > 1) {
                    // Take half
                    int half = (slotStack.getCount() + 1) / 2;
                    invInteraction.setHeldStack(new ItemStack(slotStack.getBlockId(), half));
                    craftingGrid[slot] = slotStack.withCount(slotStack.getCount() - half);
                } else {
                    // Take all
                    invInteraction.setHeldStack(slotStack);
                    craftingGrid[slot] = ItemStack.EMPTY;
                }
            }
        } else {
            // Place in slot
            if (slotStack.isEmpty()) {
                if (rightClick) {
                    // Place one
                    craftingGrid[slot] = new ItemStack(heldStack.getBlockId(), 1);
                    if (heldStack.getCount() > 1) {
                        invInteraction.setHeldStack(heldStack.withCount(heldStack.getCount() - 1));
                    } else {
                        invInteraction.setHeldStack(ItemStack.EMPTY);
                    }
                } else {
                    // Place all
                    craftingGrid[slot] = heldStack;
                    invInteraction.setHeldStack(ItemStack.EMPTY);
                }
            } else if (slotStack.getBlockId().equals(heldStack.getBlockId())) {
                // Stack
                if (rightClick) {
                    // Add one
                    if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                        craftingGrid[slot] = slotStack.withCount(slotStack.getCount() + 1);
                        if (heldStack.getCount() > 1) {
                            invInteraction.setHeldStack(heldStack.withCount(heldStack.getCount() - 1));
                        } else {
                            invInteraction.setHeldStack(ItemStack.EMPTY);
                        }
                    }
                } else {
                    // Add as many as possible
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    int toAdd = Math.min(space, heldStack.getCount());
                    craftingGrid[slot] = slotStack.withCount(slotStack.getCount() + toAdd);
                    int remaining = heldStack.getCount() - toAdd;
                    if (remaining > 0) {
                        invInteraction.setHeldStack(heldStack.withCount(remaining));
                    } else {
                        invInteraction.setHeldStack(ItemStack.EMPTY);
                    }
                }
            } else {
                // Different items - swap
                ItemStack temp = slotStack;
                craftingGrid[slot] = heldStack;
                invInteraction.setHeldStack(temp);
            }
        }
    }

    /**
     * Handle click on output slot (craft item)
     */
    private void handleOutputClick(Inventory inventory, InventoryInteraction invInteraction) {
        if (craftingOutput.isEmpty()) return;

        ItemStack heldStack = invInteraction.getHeldStack();

        // Can only take output if hands are empty or holding same item
        if (heldStack.isEmpty() ||
                (heldStack.getBlockId().equals(craftingOutput.getBlockId()) &&
                        heldStack.getCount() + craftingOutput.getCount() <= heldStack.getMaxStackSize())) {

            // Consume ingredients
            for (int i = 0; i < 9; i++) {
                if (!craftingGrid[i].isEmpty()) {
                    craftingGrid[i] = craftingGrid[i].shrink(1);
                }
            }

            // Give output to player
            // This will be handled by the inventory interaction system
            System.out.println("[CraftingTable] Crafted: " + craftingOutput);

            updateCrafting();
        }
    }

    /**
     * Update crafting output based on grid
     */
    private void updateCrafting() {
        // Convert ItemStack array to String array for recipe matching
        String[] grid = new String[9];
        for (int i = 0; i < 9; i++) {
            if (!craftingGrid[i].isEmpty()) {
                grid[i] = craftingGrid[i].getBlockId();
            } else {
                grid[i] = null;
            }
        }

        // Find matching recipe (checks shaped first, then shapeless)
        CraftingSystem.Recipe recipe = CraftingSystem.findRecipe(grid, 3);

        if (recipe != null) {
            craftingOutput = new ItemStack(recipe.getOutput(), recipe.getOutputCount());
            System.out.println("[Crafting] Recipe found: " + recipe.getOutput() +
                    " x" + recipe.getOutputCount() + " (" + recipe.getType() + ")");
        } else {
            craftingOutput = ItemStack.EMPTY;
        }
    }

    /**
     * Return all items from crafting grid to inventory
     */
    public void returnItemsToInventory() {
        // TODO: Return items when closing GUI
    }
}