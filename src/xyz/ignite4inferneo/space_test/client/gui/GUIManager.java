package xyz.ignite4inferneo.space_test.client.gui;

import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;

/**
 * Central GUI manager handles all open GUIs
 */
public class GUIManager {

    public enum GUIType {
        NONE,
        INVENTORY,
        CRAFTING_TABLE,
        FURNACE,
        CHEST
    }

    private GUIType currentGUI = GUIType.NONE;
    private InventoryInteraction inventoryInteraction;
    private CraftingTableGUI craftingTableGUI;
    private InventoryWithCraftingGUI inventoryWithCraftingGUI;

    // GUI block position (for range check)
    private int guiBlockX, guiBlockY, guiBlockZ;

    public GUIManager() {
        this.inventoryInteraction = new InventoryInteraction();
        this.craftingTableGUI = new CraftingTableGUI();
        this.inventoryWithCraftingGUI = new InventoryWithCraftingGUI();
    }

    /**
     * Open a GUI
     */
    public void openGUI(GUIType type) {
        closeGUI(); // Close any open GUI first
        currentGUI = type;
        System.out.println("[GUIManager] Opened " + type);
    }

    /**
     * Open GUI at block position
     */
    public void openGUI(GUIType type, int x, int y, int z) {
        openGUI(type);
        this.guiBlockX = x;
        this.guiBlockY = y;
        this.guiBlockZ = z;
    }

    /**
     * Close current GUI
     */
    public void closeGUI() {
        if (currentGUI != GUIType.NONE) {
            System.out.println("[GUIManager] Closed " + currentGUI);

            // Clear crafting grid when closing
            if (currentGUI == GUIType.CRAFTING_TABLE) {
                craftingTableGUI.returnItemsToInventory();
            } else if (currentGUI == GUIType.INVENTORY) {
                // Return items from 2x2 crafting grid
                // Will be handled when returnHeldItems is called
            }
        }
        currentGUI = GUIType.NONE;
    }

    /**
     * Render current GUI
     */
    public void render(Graphics2D g, Inventory inventory, int screenWidth, int screenHeight) {
        switch (currentGUI) {
            case INVENTORY:
                inventoryWithCraftingGUI.render(g, inventory, screenWidth, screenHeight);
                inventoryInteraction.renderHeldItem(g);
                break;

            case CRAFTING_TABLE:
                craftingTableGUI.render(g, inventory, screenWidth, screenHeight);
                inventoryInteraction.renderHeldItem(g);
                break;

            case FURNACE:
                // TODO: Furnace GUI
                InventoryRenderer.renderInventory(g, inventory, screenWidth, screenHeight);
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.drawString("FURNACE GUI (TODO)", screenWidth/2 - 100, 50);
                break;

            case CHEST:
                // TODO: Chest GUI
                InventoryRenderer.renderInventory(g, inventory, screenWidth, screenHeight);
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.drawString("CHEST GUI (TODO)", screenWidth/2 - 100, 50);
                break;

            default:
                break;
        }
    }

    /**
     * Handle mouse click in GUI
     */
    public void handleClick(Inventory inventory, int screenWidth, int screenHeight,
                            int mouseX, int mouseY, boolean rightClick) {
        switch (currentGUI) {
            case INVENTORY:
                // Try 2x2 crafting first
                if (!inventoryWithCraftingGUI.handleCraftingClick(screenWidth, screenHeight,
                        mouseX, mouseY, inventoryInteraction, rightClick)) {
                    // If not in crafting area, handle inventory
                    inventoryInteraction.handleClick(inventory, screenWidth, screenHeight,
                            mouseX, mouseY, rightClick);
                }
                break;

            case CRAFTING_TABLE:
                // Try crafting GUI first
                if (!craftingTableGUI.handleClick(inventory, screenWidth, screenHeight,
                        mouseX, mouseY, rightClick, inventoryInteraction)) {
                    // If not handled by crafting GUI, try inventory
                    inventoryInteraction.handleClick(inventory, screenWidth, screenHeight,
                            mouseX, mouseY, rightClick);
                }
                break;

            default:
                break;
        }
    }

    /**
     * Update mouse position
     */
    public void updateMousePosition(int mouseX, int mouseY) {
        inventoryInteraction.updateMousePosition(mouseX, mouseY);
    }

    /**
     * Check if any GUI is open
     */
    public boolean hasOpenGUI() {
        return currentGUI != GUIType.NONE;
    }

    /**
     * Get current GUI type
     */
    public GUIType getCurrentGUI() {
        return currentGUI;
    }

    /**
     * Get GUI block position
     */
    public int[] getGUIBlockPosition() {
        return new int[]{guiBlockX, guiBlockY, guiBlockZ};
    }

    /**
     * Check if player is in range of GUI block
     */
    public boolean isInRangeOfGUIBlock(double playerX, double playerY, double playerZ) {
        if (currentGUI == GUIType.NONE || currentGUI == GUIType.INVENTORY) {
            return true; // Inventory always accessible
        }

        double dx = playerX - (guiBlockX + 0.5);
        double dy = playerY - (guiBlockY + 0.5);
        double dz = playerZ - (guiBlockZ + 0.5);
        double distSq = dx*dx + dy*dy + dz*dz;

        return distSq <= 25.0; // Max 5 blocks
    }

    /**
     * Return held items when closing
     */
    public void returnHeldItems(Inventory inventory) {
        inventoryInteraction.returnHeldItem(inventory);

        if (currentGUI == GUIType.CRAFTING_TABLE) {
            craftingTableGUI.returnItemsToInventory();
        } else if (currentGUI == GUIType.INVENTORY) {
            inventoryWithCraftingGUI.returnItemsToInventory(inventory);
        }
    }

    public InventoryInteraction getInventoryInteraction() {
        return inventoryInteraction;
    }
}