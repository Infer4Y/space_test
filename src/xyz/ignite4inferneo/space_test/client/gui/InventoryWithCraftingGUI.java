package xyz.ignite4inferneo.space_test.client.gui;

import xyz.ignite4inferneo.space_test.common.crafting.CraftingSystem;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * FIXED: Enhanced inventory renderer with working 2x2 crafting grid
 */
public class InventoryWithCraftingGUI {

    private static final int SLOT_SIZE = 36;
    private static final int SLOT_PADDING = 4;

    // 2x2 crafting grid for inventory
    private final ItemStack[] craftingGrid2x2 = new ItemStack[4];
    private ItemStack craftingOutput2x2 = ItemStack.EMPTY;

    public InventoryWithCraftingGUI() {
        for (int i = 0; i < 4; i++) {
            craftingGrid2x2[i] = ItemStack.EMPTY;
        }
    }

    public void render(Graphics2D g, Inventory inventory, int screenWidth, int screenHeight) {
        int invWidth = Inventory.INVENTORY_COLS * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int invHeight = (Inventory.INVENTORY_ROWS + 1) * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 3 + 30;

        int startX = (screenWidth - invWidth) / 2;
        int startY = (screenHeight - invHeight) / 2;

        // Main background
        g.setColor(new Color(40, 40, 40, 240));
        g.fillRect(startX, startY, invWidth, invHeight);
        g.setColor(new Color(139, 139, 139));
        g.drawRect(startX, startY, invWidth, invHeight);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Inventory", startX + 10, startY + 20);

        // Main inventory
        int inventoryStartY = startY + 35;
        for (int row = 0; row < Inventory.INVENTORY_ROWS; row++) {
            for (int col = 0; col < Inventory.INVENTORY_COLS; col++) {
                int slot = Inventory.HOTBAR_SIZE + row * Inventory.INVENTORY_COLS + col;
                int slotX = startX + SLOT_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = inventoryStartY + row * (SLOT_SIZE + SLOT_PADDING);

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
        int hotbarY = inventoryStartY + Inventory.INVENTORY_ROWS * (SLOT_SIZE + SLOT_PADDING) + 10;
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            int slotX = startX + SLOT_PADDING + i * (SLOT_SIZE + SLOT_PADDING);

            if (i == inventory.getSelectedSlot()) {
                g.setColor(new Color(255, 255, 255, 100));
            } else {
                g.setColor(new Color(60, 60, 60, 200));
            }
            g.fillRect(slotX, hotbarY, SLOT_SIZE, SLOT_SIZE);
            g.setColor(i == inventory.getSelectedSlot() ? Color.WHITE : new Color(100, 100, 100));
            g.drawRect(slotX, hotbarY, SLOT_SIZE, SLOT_SIZE);

            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                InventoryRenderer.renderItemStackAt(g, stack, slotX + 2, hotbarY + 2, SLOT_SIZE - 4);
            }
        }

        // Render 2x2 crafting grid on the right side
        renderCraftingGrid2x2(g, startX + invWidth + 20, startY + 35);
    }

    private void renderCraftingGrid2x2(Graphics2D g, int startX, int startY) {
        int gridWidth = 2 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int gridHeight = 2 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2 + 60;

        // Background
        g.setColor(new Color(40, 40, 40, 240));
        g.fillRect(startX - 5, startY - 25, gridWidth + 10, gridHeight + 10);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Crafting", startX, startY - 10);

        // 2x2 Grid
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int slotX = startX + SLOT_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = startY + row * (SLOT_SIZE + SLOT_PADDING);

                g.setColor(new Color(60, 60, 60, 200));
                g.fillRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
                g.setColor(new Color(100, 100, 100));
                g.drawRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

                ItemStack stack = craftingGrid2x2[row * 2 + col];
                if (!stack.isEmpty()) {
                    InventoryRenderer.renderItemStackAt(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
                }
            }
        }

        // Arrow
        int arrowY = startY + SLOT_SIZE / 2 + 8;
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("↓", startX + gridWidth / 2 - 8, arrowY + 20);

        // Output slot
        int outputX = startX + (gridWidth - SLOT_SIZE) / 2;
        int outputY = startY + 2 * (SLOT_SIZE + SLOT_PADDING) + 25;

        g.setColor(new Color(100, 100, 80, 200));
        g.fillRect(outputX, outputY, SLOT_SIZE, SLOT_SIZE);
        g.setColor(Color.YELLOW);
        g.drawRect(outputX, outputY, SLOT_SIZE, SLOT_SIZE);

        if (!craftingOutput2x2.isEmpty()) {
            InventoryRenderer.renderItemStackAt(g, craftingOutput2x2, outputX + 2, outputY + 2, SLOT_SIZE - 4);
        }
    }

    public boolean handleCraftingClick(int screenWidth, int screenHeight, int mouseX, int mouseY,
                                       InventoryInteraction invInteraction, boolean rightClick) {
        int invWidth = Inventory.INVENTORY_COLS * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int invHeight = (Inventory.INVENTORY_ROWS + 1) * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 3 + 30;
        int startX = (screenWidth - invWidth) / 2 + invWidth + 20;
        int startY = (screenHeight - invHeight) / 2 + 35;

        // Check output slot FIRST (higher priority)
        if (isOutputSlot2x2Clicked(startX, startY, mouseX, mouseY)) {
            handleOutputClick(invInteraction);
            return true;
        }

        // Check crafting grid slots
        int craftingSlot = getCraftingSlot2x2(startX, startY, mouseX, mouseY);
        if (craftingSlot != -1) {
            handleCraftingSlotClick(craftingSlot, invInteraction, rightClick);
            updateCrafting2x2();
            return true;
        }

        return false;
    }

    private int getCraftingSlot2x2(int startX, int startY, int mouseX, int mouseY) {
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int slotX = startX + SLOT_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = startY + row * (SLOT_SIZE + SLOT_PADDING);

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    return row * 2 + col;
                }
            }
        }
        return -1;
    }

    private boolean isOutputSlot2x2Clicked(int startX, int startY, int mouseX, int mouseY) {
        int gridWidth = 2 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int outputX = startX + (gridWidth - SLOT_SIZE) / 2;
        int outputY = startY + 2 * (SLOT_SIZE + SLOT_PADDING) + 25;

        return mouseX >= outputX && mouseX < outputX + SLOT_SIZE &&
                mouseY >= outputY && mouseY < outputY + SLOT_SIZE;
    }

    private void handleCraftingSlotClick(int slot, InventoryInteraction invInteraction, boolean rightClick) {
        ItemStack heldStack = invInteraction.getHeldStack();
        ItemStack slotStack = craftingGrid2x2[slot];

        if (heldStack.isEmpty()) {
            if (!slotStack.isEmpty()) {
                if (rightClick && slotStack.getCount() > 1) {
                    int half = (slotStack.getCount() + 1) / 2;
                    invInteraction.setHeldStack(new ItemStack(slotStack.getBlockId(), half));
                    craftingGrid2x2[slot] = slotStack.withCount(slotStack.getCount() - half);
                } else {
                    invInteraction.setHeldStack(slotStack);
                    craftingGrid2x2[slot] = ItemStack.EMPTY;
                }
            }
        } else {
            if (slotStack.isEmpty()) {
                if (rightClick) {
                    craftingGrid2x2[slot] = new ItemStack(heldStack.getBlockId(), 1);
                    if (heldStack.getCount() > 1) {
                        invInteraction.setHeldStack(heldStack.withCount(heldStack.getCount() - 1));
                    } else {
                        invInteraction.setHeldStack(ItemStack.EMPTY);
                    }
                } else {
                    craftingGrid2x2[slot] = heldStack;
                    invInteraction.setHeldStack(ItemStack.EMPTY);
                }
            } else if (slotStack.getBlockId().equals(heldStack.getBlockId())) {
                if (rightClick) {
                    if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                        craftingGrid2x2[slot] = slotStack.withCount(slotStack.getCount() + 1);
                        if (heldStack.getCount() > 1) {
                            invInteraction.setHeldStack(heldStack.withCount(heldStack.getCount() - 1));
                        } else {
                            invInteraction.setHeldStack(ItemStack.EMPTY);
                        }
                    }
                } else {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    int toAdd = Math.min(space, heldStack.getCount());
                    craftingGrid2x2[slot] = slotStack.withCount(slotStack.getCount() + toAdd);
                    int remaining = heldStack.getCount() - toAdd;
                    if (remaining > 0) {
                        invInteraction.setHeldStack(heldStack.withCount(remaining));
                    } else {
                        invInteraction.setHeldStack(ItemStack.EMPTY);
                    }
                }
            } else {
                ItemStack temp = slotStack;
                craftingGrid2x2[slot] = heldStack;
                invInteraction.setHeldStack(temp);
            }
        }
    }

    /**
     * FIXED: Handle output slot click (craft item)
     */
    private void handleOutputClick(InventoryInteraction invInteraction) {
        if (craftingOutput2x2.isEmpty()) {
            return;
        }

        ItemStack heldStack = invInteraction.getHeldStack();

        // Can only take if hands empty or holding same item
        if (heldStack.isEmpty()) {
            // Pick up output
            invInteraction.setHeldStack(craftingOutput2x2.copy());
            consumeIngredients();
            updateCrafting2x2();
            System.out.println("[Crafting] Crafted: " + craftingOutput2x2);
        } else if (heldStack.getBlockId().equals(craftingOutput2x2.getBlockId())) {
            // Stack with held item
            int newCount = heldStack.getCount() + craftingOutput2x2.getCount();
            if (newCount <= heldStack.getMaxStackSize()) {
                invInteraction.setHeldStack(heldStack.withCount(newCount));
                consumeIngredients();
                updateCrafting2x2();
                System.out.println("[Crafting] Crafted: " + craftingOutput2x2);
            }
        }
    }

    /**
     * FIXED: Consume one of each ingredient from the crafting grid
     */
    private void consumeIngredients() {
        for (int i = 0; i < 4; i++) {
            if (!craftingGrid2x2[i].isEmpty()) {
                craftingGrid2x2[i] = craftingGrid2x2[i].shrink(1);
            }
        }
    }

    /**
     * FIXED: Update crafting output based on grid
     */
    private void updateCrafting2x2() {
        // Count ingredients in grid
        Map<String, Integer> ingredients = new HashMap<>();
        for (ItemStack stack : craftingGrid2x2) {
            if (!stack.isEmpty()) {
                String id = stack.getBlockId();
                ingredients.put(id, ingredients.getOrDefault(id, 0) + stack.getCount());
            }
        }

        // Find matching recipe
        CraftingSystem.Recipe recipe = CraftingSystem.findRecipe(ingredients);

        if (recipe != null) {
            craftingOutput2x2 = new ItemStack(recipe.getOutput(), recipe.getOutputCount());
            System.out.println("[Crafting] Recipe found: " + recipe.getOutput() +
                    " x" + recipe.getOutputCount());
        } else {
            craftingOutput2x2 = ItemStack.EMPTY;
        }
    }

    public void returnItemsToInventory(Inventory inventory) {
        for (int i = 0; i < 4; i++) {
            if (!craftingGrid2x2[i].isEmpty()) {
                inventory.addItem(craftingGrid2x2[i].getBlockId(), craftingGrid2x2[i].getCount());
                craftingGrid2x2[i] = ItemStack.EMPTY;
            }
        }
        craftingOutput2x2 = ItemStack.EMPTY;
    }

    public void clearGrid() {
        for (int i = 0; i < 4; i++) {
            craftingGrid2x2[i] = ItemStack.EMPTY;
        }
        craftingOutput2x2 = ItemStack.EMPTY;
    }
}