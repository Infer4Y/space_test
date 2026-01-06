package xyz.ignite4inferneo.space_test.client.gui;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.client.renderer.ItemIconRenderer;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;

/**
 * UPDATED: Renders inventory UI with 3D item icons
 */
public class InventoryRenderer {

    private static final int SLOT_SIZE = 36;
    private static final int SLOT_PADDING = 4;
    private static final int HOTBAR_Y_OFFSET = 10;

    public static void renderHotbar(Graphics2D g, Inventory inventory, int screenWidth, int screenHeight) {
        int hotbarWidth = Inventory.HOTBAR_SIZE * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING;
        int startX = (screenWidth - hotbarWidth) / 2;
        int startY = screenHeight - SLOT_SIZE - HOTBAR_Y_OFFSET - SLOT_PADDING * 2;

        // Background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(startX - 4, startY - 4, hotbarWidth + 8, SLOT_SIZE + 16);

        // Draw slots
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            int slotX = startX + i * (SLOT_SIZE + SLOT_PADDING);
            int slotY = startY;

            // Slot background
            if (i == inventory.getSelectedSlot()) {
                g.setColor(new Color(255, 255, 255, 100));
            } else {
                g.setColor(new Color(60, 60, 60, 200));
            }
            g.fillRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            // Border
            g.setColor(i == inventory.getSelectedSlot() ? Color.WHITE : new Color(139, 139, 139));
            g.drawRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            // Item (NEW: Using 3D renderer)
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                ItemIconRenderer.renderItemIcon(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
            }

            // Slot number
            g.setColor(new Color(255, 255, 255, 180));
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.drawString(String.valueOf(i + 1), slotX + 2, slotY + 12);
        }
    }

    public static void renderInventory(Graphics2D g, Inventory inventory, int screenWidth, int screenHeight) {
        int invWidth = Inventory.INVENTORY_COLS * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int invHeight = (Inventory.INVENTORY_ROWS + 1) * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 3 + 30;

        int startX = (screenWidth - invWidth) / 2;
        int startY = (screenHeight - invHeight) / 2;

        // Background
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
                    ItemIconRenderer.renderItemIcon(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
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
                ItemIconRenderer.renderItemIcon(g, stack, slotX + 2, hotbarY + 2, SLOT_SIZE - 4);
            }
        }
    }

    /**
     * UPDATED: Public method to render item stack (now uses 3D renderer)
     */
    public static void renderItemStackAt(Graphics2D g, ItemStack stack, int x, int y, int size) {
        ItemIconRenderer.renderItemIcon(g, stack, x, y, size);
    }
}