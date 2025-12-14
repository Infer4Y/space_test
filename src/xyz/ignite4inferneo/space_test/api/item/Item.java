package xyz.ignite4inferneo.space_test.api.item;

import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Base interface for all items in the game.
 */
public interface Item {

    /**
     * Get the item's unique identifier
     */
    String getId();

    /**
     * Get the item's display name
     */
    String getName();

    /**
     * Get maximum stack size
     */
    default int getMaxStackSize() {
        return 64;
    }

    /**
     * Get texture index for inventory rendering
     */
    int getTextureIndex();

    /**
     * Called when item is used (right-click)
     */
    default boolean onUse(World world, int x, int y, int z) {
        return false;
    }

    /**
     * Is this item a block item (can be placed)?
     */
    default boolean isBlockItem() {
        return false;
    }

    /**
     * Get the block ID if this is a block item
     */
    default String getBlockId() {
        return null;
    }
}
