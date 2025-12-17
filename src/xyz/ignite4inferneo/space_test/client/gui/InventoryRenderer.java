package xyz.ignite4inferneo.space_test.client.gui;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;

/**
 * Renders inventory UI
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

            // Item
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                renderItemStack(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
            }

            // Slot number
            g.setColor(Color.WHITE);
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
                    renderItemStack(g, stack, slotX + 2, slotY + 2, SLOT_SIZE - 4);
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
                renderItemStack(g, stack, slotX + 2, hotbarY + 2, SLOT_SIZE - 4);
            }
        }
    }

    private static void renderItemStack(Graphics2D g, ItemStack stack, int x, int y, int size) {
        renderItemStackAt(g, stack, x, y, size);
    }

    /**
     * Public method to render item stack at position (used by InventoryInteraction)
     */
    public static void renderItemStackAt(Graphics2D g, ItemStack stack, int x, int y, int size) {
        Block block = Registries.BLOCKS.get(stack.getBlockId());
        if (block == null) return;

        Color color = getBlockColor(stack.getBlockId());
        g.setColor(color);
        g.fillRect(x, y, size, size);
        g.setColor(color.darker());
        g.drawRect(x, y, size, size);

        if (stack.getCount() > 1) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            String countStr = String.valueOf(stack.getCount());
            int textWidth = g.getFontMetrics().stringWidth(countStr);
            g.drawString(countStr, x + size - textWidth - 2, y + size - 2);
        }
    }

    private static Color getBlockColor(String blockId) {
        return switch (blockId) {
            case "space_test:stone" -> new Color(128, 128, 128);
            case "space_test:dirt" -> new Color(139, 69, 19);
            case "space_test:grass" -> new Color(34, 139, 34);
            case "space_test:wood" -> new Color(160, 82, 45);
            case "space_test:leaves" -> new Color(0, 128, 0);
            default -> new Color(200, 200, 200);
        };
    }
}