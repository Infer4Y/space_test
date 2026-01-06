package xyz.ignite4inferneo.space_test.client.gui;

import xyz.ignite4inferneo.space_test.common.entity.ItemEntity;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.awt.*;

/**
 * Handles inventory interaction (clicking, dragging, moving items)
 */
public class InventoryInteraction {
    private static final int SLOT_SIZE = 36;
    private static final int SLOT_PADDING = 4;

    // Dragging state
    private ItemStack heldStack = ItemStack.EMPTY;
    private int heldSlot = -1;
    private boolean isDragging = false;

    // Mouse position for rendering held item
    private int mouseX = 0;
    private int mouseY = 0;

    // For dropping items
    private World world;
    private double playerX, playerY, playerZ;

    public void setWorld(World world) {
        this.world = world;
    }

    public void setPlayerPosition(double x, double y, double z) {
        this.playerX = x;
        this.playerY = y;
        this.playerZ = z;
    }

    /**
     * Handle mouse click in inventory
     */
    public void handleClick(Inventory inventory, int screenWidth, int screenHeight,
                            int mouseX, int mouseY, boolean rightClick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        int slot = getSlotAt(screenWidth, screenHeight, mouseX, mouseY);

        if (slot == -1) {
            // Clicked outside inventory - drop held item
            if (!heldStack.isEmpty()) {
                dropItem(heldStack);
                heldStack = ItemStack.EMPTY;
                heldSlot = -1;
                isDragging = false;
            }
            return;
        }

        if (rightClick) {
            handleRightClick(inventory, slot);
        } else {
            handleLeftClick(inventory, slot);
        }
    }

    /**
     * Left click: Pick up entire stack or swap/stack
     */
    private void handleLeftClick(Inventory inventory, int slot) {
        ItemStack slotStack = inventory.getStack(slot);

        if (heldStack.isEmpty()) {
            // Pick up from slot
            if (!slotStack.isEmpty()) {
                heldStack = slotStack;
                heldSlot = slot;
                inventory.setStack(slot, ItemStack.EMPTY);
                isDragging = true;
            }
        } else {
            // Placing or swapping
            if (slotStack.isEmpty()) {
                // Place entire held stack
                inventory.setStack(slot, heldStack);
                heldStack = ItemStack.EMPTY;
                heldSlot = -1;
                isDragging = false;
            } else if (slotStack.getBlockId().equals(heldStack.getBlockId())) {
                // Try to stack
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, heldStack.getCount());
                    inventory.setStack(slot, slotStack.withCount(slotStack.getCount() + toAdd));

                    int remaining = heldStack.getCount() - toAdd;
                    if (remaining <= 0) {
                        heldStack = ItemStack.EMPTY;
                        heldSlot = -1;
                        isDragging = false;
                    } else {
                        heldStack = heldStack.withCount(remaining);
                    }
                }
            } else {
                // Swap stacks
                ItemStack temp = slotStack;
                inventory.setStack(slot, heldStack);
                heldStack = temp;
                heldSlot = slot;
            }
        }
    }

    /**
     * Right click: Split stack or place one item
     */
    private void handleRightClick(Inventory inventory, int slot) {
        ItemStack slotStack = inventory.getStack(slot);

        if (heldStack.isEmpty()) {
            // Pick up half
            if (!slotStack.isEmpty() && slotStack.getCount() > 1) {
                int halfCount = (slotStack.getCount() + 1) / 2;
                heldStack = new ItemStack(slotStack.getBlockId(), halfCount);
                inventory.setStack(slot, slotStack.withCount(slotStack.getCount() - halfCount));
                heldSlot = slot;
                isDragging = true;
            } else if (!slotStack.isEmpty() && slotStack.getCount() == 1) {
                // Pick up single item
                heldStack = slotStack;
                heldSlot = slot;
                inventory.setStack(slot, ItemStack.EMPTY);
                isDragging = true;
            }
        } else {
            // Place one item
            if (slotStack.isEmpty()) {
                // Place single item in empty slot
                inventory.setStack(slot, new ItemStack(heldStack.getBlockId(), 1));
                heldStack = heldStack.withCount(heldStack.getCount() - 1);
                if (heldStack.getCount() <= 0) {
                    heldStack = ItemStack.EMPTY;
                    heldSlot = -1;
                    isDragging = false;
                }
            } else if (slotStack.getBlockId().equals(heldStack.getBlockId())) {
                // Add one to existing stack
                if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                    inventory.setStack(slot, slotStack.withCount(slotStack.getCount() + 1));
                    heldStack = heldStack.withCount(heldStack.getCount() - 1);
                    if (heldStack.getCount() <= 0) {
                        heldStack = ItemStack.EMPTY;
                        heldSlot = -1;
                        isDragging = false;
                    }
                }
            }
        }
    }

    /**
     * Update mouse position for rendering
     */
    public void updateMousePosition(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    /**
     * Get slot at mouse position
     * Returns -1 if no slot at position
     */
    private int getSlotAt(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        int invWidth = Inventory.INVENTORY_COLS * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 2;
        int invHeight = (Inventory.INVENTORY_ROWS + 1) * (SLOT_SIZE + SLOT_PADDING) + SLOT_PADDING * 3 + 30;

        int startX = (screenWidth - invWidth) / 2;
        int startY = (screenHeight - invHeight) / 2;

        int inventoryStartY = startY + 35;

        // Check main inventory slots
        for (int row = 0; row < Inventory.INVENTORY_ROWS; row++) {
            for (int col = 0; col < Inventory.INVENTORY_COLS; col++) {
                int slotX = startX + SLOT_PADDING + col * (SLOT_SIZE + SLOT_PADDING);
                int slotY = inventoryStartY + row * (SLOT_SIZE + SLOT_PADDING);

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    return Inventory.HOTBAR_SIZE + row * Inventory.INVENTORY_COLS + col;
                }
            }
        }

        // Check hotbar slots
        int hotbarY = inventoryStartY + Inventory.INVENTORY_ROWS * (SLOT_SIZE + SLOT_PADDING) + 10;
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            int slotX = startX + SLOT_PADDING + i * (SLOT_SIZE + SLOT_PADDING);

            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                    mouseY >= hotbarY && mouseY < hotbarY + SLOT_SIZE) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Render the held item following the cursor
     */
    public void renderHeldItem(Graphics2D g) {
        if (heldStack.isEmpty()) return;

        // Render semi-transparent item at cursor
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));

        int renderX = mouseX - SLOT_SIZE / 2;
        int renderY = mouseY - SLOT_SIZE / 2;

        // Draw background
        g.setColor(new Color(60, 60, 60, 200));
        g.fillRect(renderX, renderY, SLOT_SIZE, SLOT_SIZE);
        g.setColor(Color.WHITE);
        g.drawRect(renderX, renderY, SLOT_SIZE, SLOT_SIZE);

        // Draw item
        InventoryRenderer.renderItemStackAt(g, heldStack, renderX + 2, renderY + 2, SLOT_SIZE - 4);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    /**
     * Drop item into world as entity
     */
    private void dropItem(ItemStack stack) {
        if (world == null || stack.isEmpty()) return;

        // Calculate drop position (in front of player)
        double dropX = playerX;
        double dropY = playerY + 1.0; // Above player
        double dropZ = playerZ;

        ItemEntity itemEntity = new ItemEntity(world, dropX, dropY, dropZ, stack);
        world.getEntityManager().addEntity(itemEntity);

        System.out.println("[Inventory] Dropped " + stack);
    }

    /**
     * Check if currently dragging an item
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Get held item stack
     */
    public ItemStack getHeldStack() {
        return heldStack;
    }

    /**
     * Set held item stack (for crafting grid interaction)
     */
    public void setHeldStack(ItemStack stack) {
        this.heldStack = stack;
        this.isDragging = !stack.isEmpty();
    }

    /**
     * Reset interaction state
     */
    public void reset() {
        heldStack = ItemStack.EMPTY;
        heldSlot = -1;
        isDragging = false;
    }

    /**
     * Return held item to inventory (when closing GUI)
     */
    public void returnHeldItem(Inventory inventory) {
        if (!heldStack.isEmpty()) {
            // Try to put back in original slot
            if (heldSlot != -1 && inventory.getStack(heldSlot).isEmpty()) {
                inventory.setStack(heldSlot, heldStack);
            } else {
                // Otherwise add to inventory or drop
                if (!inventory.addItem(heldStack.getBlockId(), heldStack.getCount())) {
                    // Inventory full, drop it
                    dropItem(heldStack);
                }
            }
            reset();
        }
    }
}