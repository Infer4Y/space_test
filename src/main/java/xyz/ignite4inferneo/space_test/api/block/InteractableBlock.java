package xyz.ignite4inferneo.space_test.api.block;

import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;
import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Extended block interface with interaction capabilities
 */
public interface InteractableBlock extends Block {

    /**
     * Called when player right-clicks the block
     * @return true if interaction was handled (prevents block placement)
     */
    boolean onInteract(World world, int x, int y, int z, PlayerEntity player);

    /**
     * Can this block be interacted with?
     */
    default boolean canInteract() {
        return true;
    }

    /**
     * Get interaction range
     */
    default double getInteractionRange() {
        return 5.0;
    }
}

