package xyz.ignite4inferneo.space_test.api.block;

import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Base interface for all blocks in the game.
 * Mods implement this to create custom blocks.
 */
public interface Block {

    /**
     * Get the block's unique identifier (e.g., "minecraft:stone")
     */
    String getId();

    /**
     * Get the block's display name
     */
    String getName();

    /**
     * Get texture indices for each face
     * @return array of 6 texture indices [bottom, top, north, south, west, east]
     */
    int[] getTextureIndices();

    /**
     * Is this block solid (for collision)?
     */
    default boolean isSolid() {
        return true;
    }

    /**
     * Is this block transparent (for rendering)?
     */
    default boolean isTransparent() {
        return false;
    }

    /**
     * Can this block be broken?
     */
    default boolean isBreakable() {
        return true;
    }

    /**
     * Get light emission level (0-15)
     */
    default int getLightLevel() {
        return 0;
    }

    /**
     * Get hardness (affects break time)
     */
    default float getHardness() {
        return 1.0f;
    }

    /**
     * Called when block is placed
     */
    default void onPlace(World world, int x, int y, int z) {}

    /**
     * Called when block is broken
     */
    default void onBreak(World world, int x, int y, int z) {}

    /**
     * Called when a neighbor block changes
     */
    default void onNeighborChange(World world, int x, int y, int z) {}

    /**
     * Called every tick (if block needs updates)
     */
    default void onUpdate(World world, int x, int y, int z) {}
}