package xyz.ignite4inferneo.space_test.client.gui;

import xyz.ignite4inferneo.space_test.common.crafting.CraftingSystem;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;

/**
 * FIXED: Crafting table GUI with properly aligned inventory
 */
public class CraftingTableGUI {

    private static final int SLOT_SIZE = 36;
    private static final int SLOT_PADDING = 4;

    private final ItemStack[] craftingGrid = new ItemStack[9];
    private ItemStack craftingOutput = ItemStack.EMPTY;

    public CraftingTableGUI() {
        for (int i = 0; i < 9; i++) {
            craftingGrid[i] = ItemStack.EMPTY;
        }
    }

    /**
     * FIXED: Render with properly centered components
     */
    public void render(Graphics2D g, Inventory inventory, int screenWidth, int screenHeight) {
        // Calculate inventory width for centering
        int invWidth = Inventory.INVENTORY_COLS * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;

        // Center everything based on inventory width
        int centerX = screenWidth / 2;

        // Render crafting table at top-center
        renderCraftingGrid(g, centerX, screenHeight);

        // Render inventory at bottom-center
        renderPlayerInventory(g, inventory, centerX, screenHeight);
    }

    /**
     * FIXED: Render crafting grid centered
     */
    private void renderCraftingGrid(Graphics2D g, int centerX, int screenHeight) {
        int gridSize = 3 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int outputWidth = SLOT_SIZE + 50; // Arrow + output slot
        int totalWidth = gridSize + outputWidth;

        int startX = centerX - totalWidth / 2;
        int startY = 80;

        // Background
        g.setColor(new Color(40, 40, 40, 240));
        g.fillRect(startX - 10, startY - 30, totalWidth + 20, gridSize + 40);

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
        int arrowX = startX + gridSize + 10;
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
     * FIXED: Render inventory perfectly centered
     */
    private void renderPlayerInventory(Graphics2D g, Inventory inventory, int centerX, int screenHeight) {
        int invWidth = Inventory.INVENTORY_COLS * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int invHeight = (Inventory.INVENTORY_ROWS + 1) * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2 + 20;

        int startX = centerX - invWidth / 2;
        int startY = screenHeight - invHeight - 20;

        // Background
        g.setColor(new Color(40, 40, 40, 240));
        g.fillRect(startX - 5, startY - 5, invWidth + 10, invHeight);

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
     * FIXED: Click detection with correct positions
     */
    public boolean handleClick(Inventory inventory, int screenWidth, int screenHeight,
                               int mouseX, int mouseY, boolean rightClick,
                               InventoryInteraction invInteraction) {

        int centerX = screenWidth / 2;
        int gridSize = 3 * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int outputWidth = SLOT_SIZE + 50;
        int totalWidth = gridSize + outputWidth;
        int startX = centerX - totalWidth / 2;
        int startY = 80;

        // Check crafting grid
        int gridSlot = getCraftingSlotAt(startX, startY, mouseX, mouseY);
        if (gridSlot != -1) {
            handleCraftingSlotClick(gridSlot, invInteraction, rightClick);
            updateCrafting();
            return true;
        }

        // Check output slot
        if (isOutputSlotClicked(startX, startY, gridSize, mouseX, mouseY)) {
            handleOutputClick(inventory, invInteraction);
            return true;
        }

        return false;
    }

    private int getCraftingSlotAt(int startX, int startY, int mouseX, int mouseY) {
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

    private boolean isOutputSlotClicked(int startX, int startY, int gridSize, int mouseX, int mouseY) {
        int arrowX = startX + gridSize + 10;
        int outputX = arrowX + 35;
        int outputY = startY + (gridSize - SLOT_SIZE) / 2;

        return mouseX >= outputX && mouseX < outputX + SLOT_SIZE &&
                mouseY >= outputY && mouseY < outputY + SLOT_SIZE;
    }

    private void handleCraftingSlotClick(int slot, InventoryInteraction invInteraction, boolean rightClick) {
        ItemStack heldStack = invInteraction.getHeldStack();
        ItemStack slotStack = craftingGrid[slot];

        if (heldStack.isEmpty()) {
            if (!slotStack.isEmpty()) {
                if (rightClick && slotStack.getCount() > 1) {
                    int half = (slotStack.getCount() + 1) / 2;
                    invInteraction.setHeldStack(new ItemStack(slotStack.getBlockId(), half));
                    craftingGrid[slot] = slotStack.withCount(slotStack.getCount() - half);
                } else {
                    invInteraction.setHeldStack(slotStack);
                    craftingGrid[slot] = ItemStack.EMPTY;
                }
            }
        } else {
            if (slotStack.isEmpty()) {
                if (rightClick) {
                    craftingGrid[slot] = new ItemStack(heldStack.getBlockId(), 1);
                    if (heldStack.getCount() > 1) {
                        invInteraction.setHeldStack(heldStack.withCount(heldStack.getCount() - 1));
                    } else {
                        invInteraction.setHeldStack(ItemStack.EMPTY);
                    }
                } else {
                    craftingGrid[slot] = heldStack;
                    invInteraction.setHeldStack(ItemStack.EMPTY);
                }
            } else if (slotStack.getBlockId().equals(heldStack.getBlockId())) {
                if (rightClick) {
                    if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                        craftingGrid[slot] = slotStack.withCount(slotStack.getCount() + 1);
                        if (heldStack.getCount() > 1) {
                            invInteraction.setHeldStack(heldStack.withCount(heldStack.getCount() - 1));
                        } else {
                            invInteraction.setHeldStack(ItemStack.EMPTY);
                        }
                    }
                } else {
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
                ItemStack temp = slotStack;
                craftingGrid[slot] = heldStack;
                invInteraction.setHeldStack(temp);
            }
        }
    }

    private void handleOutputClick(Inventory inventory, InventoryInteraction invInteraction) {
        if (craftingOutput.isEmpty()) return;

        ItemStack heldStack = invInteraction.getHeldStack();

        if (heldStack.isEmpty() ||
                (heldStack.getBlockId().equals(craftingOutput.getBlockId()) &&
                        heldStack.getCount() + craftingOutput.getCount() <= heldStack.getMaxStackSize())) {

            for (int i = 0; i < 9; i++) {
                if (!craftingGrid[i].isEmpty()) {
                    craftingGrid[i] = craftingGrid[i].shrink(1);
                }
            }

            System.out.println("[CraftingTable] Crafted: " + craftingOutput);
            updateCrafting();
        }
    }

    private void updateCrafting() {
        String[] grid = new String[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = !craftingGrid[i].isEmpty() ? craftingGrid[i].getBlockId() : null;
        }

        CraftingSystem.Recipe recipe = CraftingSystem.findRecipe(grid, 3);

        if (recipe != null) {
            craftingOutput = new ItemStack(recipe.getOutput(), recipe.getOutputCount());
        } else {
            craftingOutput = ItemStack.EMPTY;
        }
    }

    public void returnItemsToInventory() {
        // TODO: Return items when closing GUI
    }
}