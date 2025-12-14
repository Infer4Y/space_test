package xyz.ignite4inferneo.space_test.api.event;


import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Fired when a block is about to be placed
 */
public class BlockPlaceEvent extends Event {
    private final World world;
    private final int x, y, z;
    private final String blockId;

    public BlockPlaceEvent(World world, int x, int y, int z, String blockId) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = blockId;
    }

    public World getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getBlockId() { return blockId; }
}
