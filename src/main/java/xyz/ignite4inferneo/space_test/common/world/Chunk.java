package xyz.ignite4inferneo.space_test.common.world;

import java.lang.reflect.Array;

/**
 * Represents a 16x256x16 chunk of the world.
 * Stores block IDs as strings for flexibility with modded blocks.
 */
public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 256;

    private final int chunkX;
    private final int chunkZ;
    private final String[][][] blocks;
    private boolean dirty = true;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new String[SIZE][HEIGHT][SIZE];



        // Initialize with air
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    blocks[x][y][z] = "space_test:air";
                }
            }
        }
    }

    public String getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) {
            return "space_test:air";
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, String blockId) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) {
            return;
        }
        blocks[x][y][z] = blockId;
        markDirty();
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    /**
     * Get the raw block array (for rendering optimization)
     */
    public String[][][] getBlocks() {
        return blocks;
    }
}