package xyz.ignite4inferneo.space_test.api.block;

import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;
import xyz.ignite4inferneo.space_test.common.world.World; /**
 * Block with custom GUI (crafting table, furnace, etc.)
 */
public interface GUIBlock extends InteractableBlock {

    /**
     * Get GUI type identifier
     */
    String getGUIType();

    @Override
    default boolean onInteract(World world, int x, int y, int z, PlayerEntity player) {
        // Open GUI for this block
        // TODO: Implement GUI system
        System.out.println("Opening " + getGUIType() + " GUI at " + x + ", " + y + ", " + z);
        return true;
    }
}
