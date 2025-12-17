package xyz.ignite4inferneo.space_test.common.inventory;

/**
 * Represents a stack of items (blocks) in inventory
 */
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack("", 0);

    private final String blockId;
    private final int count;
    private final int maxStackSize;

    public ItemStack(String blockId, int count) {
        this(blockId, count, 64);
    }

    public ItemStack(String blockId, int count, int maxStackSize) {
        this.blockId = blockId;
        this.count = count;
        this.maxStackSize = maxStackSize;
    }

    public String getBlockId() {
        return blockId;
    }

    public int getCount() {
        return count;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public boolean isEmpty() {
        return count <= 0 || blockId.isEmpty();
    }

    public ItemStack withCount(int newCount) {
        return new ItemStack(blockId, newCount, maxStackSize);
    }

    public ItemStack shrink(int amount) {
        return withCount(Math.max(0, count - amount));
    }

    public ItemStack grow(int amount) {
        return withCount(Math.min(maxStackSize, count + amount));
    }

    @Override
    public String toString() {
        if (isEmpty()) return "Empty";
        return blockId + " x" + count;
    }
}