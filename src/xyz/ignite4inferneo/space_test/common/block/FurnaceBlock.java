package xyz.ignite4inferneo.space_test.common.block;

import xyz.ignite4inferneo.space_test.api.block.GUIBlock;
import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;
import xyz.ignite4inferneo.space_test.common.world.World;
/**
 * Furnace - smelts items
 */
public class FurnaceBlock extends BaseBlock implements GUIBlock {

    public FurnaceBlock() {
        super(
                "space_test:furnace",
                "Furnace",
                new int[]{0, 0, 0, 0, 0, 0}, // Stone texture
                true,
                false,
                3.5f
        );
    }

    @Override
    public String getGUIType() {
        return "furnace";
    }

    @Override
    public boolean onInteract(World world, int x, int y, int z, PlayerEntity player) {
        System.out.println("[Furnace] Player " + player.getUsername() + " opened furnace");
        // TODO: Open furnace GUI
        return true;
    }
}
