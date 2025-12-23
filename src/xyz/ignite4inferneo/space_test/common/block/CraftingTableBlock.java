package xyz.ignite4inferneo.space_test.common.block;

import xyz.ignite4inferneo.space_test.api.block.GUIBlock;
import xyz.ignite4inferneo.space_test.api.block.ContainerBlock;
import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;
import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Crafting Table - opens 3x3 crafting GUI
 */
public class CraftingTableBlock extends BaseBlock implements GUIBlock {

    public CraftingTableBlock() {
        super(
                "space_test:crafting_table",
                "Crafting Table",
                new int[]{4, 4, 4, 4, 4, 4}, // Wood texture
                true,
                false,
                2.5f
        );
    }

    @Override
    public String getGUIType() {
        return "crafting_table";
    }

    @Override
    public boolean onInteract(World world, int x, int y, int z, PlayerEntity player) {
        // Open crafting GUI
        System.out.println("[CraftingTable] Player " + player.getUsername() + " opened crafting table");
        // TODO: Open crafting GUI
        return true;
    }
}
