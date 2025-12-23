package xyz.ignite4inferneo.space_test.common.block;

import xyz.ignite4inferneo.space_test.api.block.ContainerBlock;
import xyz.ignite4inferneo.space_test.api.block.GUIBlock;
import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;
import xyz.ignite4inferneo.space_test.common.world.World;

public class ChestBlock extends BaseBlock implements ContainerBlock, GUIBlock {

    public ChestBlock() {
        super(
                "space_test:chest",
                "Chest",
                new int[]{4, 4, 4, 4, 4, 4}, // Wood texture
                true,
                false,
                2.5f
        );
    }

    @Override
    public int getContainerSize() {
        return 27; // 3 rows of 9
    }

    @Override
    public String getContainerTitle() {
        return "Chest";
    }

    @Override
    public String getGUIType() {
        return "chest";
    }

    @Override
    public boolean onInteract(World world, int x, int y, int z, PlayerEntity player) {
        System.out.println("[Chest] Player " + player.getUsername() + " opened chest");
        // TODO: Open chest GUI with world-specific inventory
        return true;
    }
}
