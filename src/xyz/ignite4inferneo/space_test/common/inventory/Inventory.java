package xyz.ignite4inferneo.space_test.common.inventory;

/**
 * Enhanced player inventory with hotbar and main storage
 */
public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int INVENTORY_ROWS = 3;
    public static final int INVENTORY_COLS = 9;
    public static final int INVENTORY_SIZE = INVENTORY_ROWS * INVENTORY_COLS;
    public static final int TOTAL_SIZE = HOTBAR_SIZE + INVENTORY_SIZE;

    private final ItemStack[] slots = new ItemStack[TOTAL_SIZE];
    private int selectedHotbarSlot = 0;

    public Inventory() {
        for (int i = 0; i < TOTAL_SIZE; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= TOTAL_SIZE) return ItemStack.EMPTY;
        return slots[slot];
    }

    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= TOTAL_SIZE) return;
        slots[slot] = stack;
    }

    public int getSelectedSlot() {
        return selectedHotbarSlot;
    }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            selectedHotbarSlot = slot;
        }
    }

    public void scrollHotbar(int direction) {
        selectedHotbarSlot += direction;
        if (selectedHotbarSlot < 0) selectedHotbarSlot = HOTBAR_SIZE - 1;
        if (selectedHotbarSlot >= HOTBAR_SIZE) selectedHotbarSlot = 0;
    }

    public ItemStack getSelectedStack() {
        return getStack(selectedHotbarSlot);
    }

    public void swapSlots(int slot1, int slot2) {
        if (slot1 < 0 || slot1 >= TOTAL_SIZE || slot2 < 0 || slot2 >= TOTAL_SIZE) return;
        ItemStack temp = slots[slot1];
        slots[slot1] = slots[slot2];
        slots[slot2] = temp;
    }

    public boolean addItem(String blockId, int count) {
        if (count <= 0) return true;

        // Try stacking in hotbar first
        count = tryStackInRange(blockId, count, 0, HOTBAR_SIZE);
        if (count <= 0) return true;

        // Then main inventory
        count = tryStackInRange(blockId, count, HOTBAR_SIZE, TOTAL_SIZE);
        if (count <= 0) return true;

        // Fill empty slots
        while (count > 0) {
            int emptySlot = findEmptySlot();
            if (emptySlot == -1) return false;

            int toAdd = Math.min(64, count);
            slots[emptySlot] = new ItemStack(blockId, toAdd);
            count -= toAdd;
        }

        return true;
    }

    private int tryStackInRange(String blockId, int count, int start, int end) {
        for (int i = start; i < end && count > 0; i++) {
            ItemStack stack = slots[i];
            if (!stack.isEmpty() && stack.getBlockId().equals(blockId)) {
                int space = stack.getMaxStackSize() - stack.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, count);
                    slots[i] = new ItemStack(blockId, stack.getCount() + toAdd);
                    count -= toAdd;
                }
            }
        }
        return count;
    }

    public boolean removeItem(String blockId, int count) {
        int remaining = count;

        for (int i = 0; i < TOTAL_SIZE && remaining > 0; i++) {
            ItemStack stack = slots[i];
            if (!stack.isEmpty() && stack.getBlockId().equals(blockId)) {
                int toRemove = Math.min(stack.getCount(), remaining);
                if (stack.getCount() <= toRemove) {
                    slots[i] = ItemStack.EMPTY;
                } else {
                    slots[i] = new ItemStack(blockId, stack.getCount() - toRemove);
                }
                remaining -= toRemove;
            }
        }

        return remaining == 0;
    }

    public void decreaseStack(int slot, int amount) {
        if (slot < 0 || slot >= TOTAL_SIZE) return;
        ItemStack stack = slots[slot];
        if (stack.isEmpty()) return;

        if (stack.getCount() <= amount) {
            slots[slot] = ItemStack.EMPTY;
        } else {
            slots[slot] = stack.shrink(amount);
        }
    }

    private int findEmptySlot() {
        for (int i = 0; i < TOTAL_SIZE; i++) {
            if (slots[i].isEmpty()) return i;
        }
        return -1;
    }

    public int findItem(String blockId) {
        for (int i = 0; i < TOTAL_SIZE; i++) {
            if (!slots[i].isEmpty() && slots[i].getBlockId().equals(blockId)) {
                return i;
            }
        }
        return -1;
    }

    public int countItem(String blockId) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && stack.getBlockId().equals(blockId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public boolean hasItem(String blockId, int count) {
        return countItem(blockId) >= count;
    }

    public boolean isFull() {
        return findEmptySlot() == -1;
    }

    public boolean hasRoom(String blockId, int count) {
        int space = 0;
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                space += 64;
            } else if (stack.getBlockId().equals(blockId)) {
                space += stack.getMaxStackSize() - stack.getCount();
            }
        }
        return space >= count;
    }

    public void clear() {
        for (int i = 0; i < TOTAL_SIZE; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack[] getSlots() {
        return slots;
    }

    public int getEmptySlots() {
        int count = 0;
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) count++;
        }
        return count;
    }
}