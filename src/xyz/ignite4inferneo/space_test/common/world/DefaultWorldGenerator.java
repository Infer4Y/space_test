package xyz.ignite4inferneo.space_test.common.world;

import java.util.Random;

/**
 * Fixed world generator - proper terrain layers
 */
public class DefaultWorldGenerator implements IWorldGenerator {

    private final long seed;

    public DefaultWorldGenerator(long seed) {
        this.seed = seed;
    }

    @Override
    public void generateChunk(World world, Chunk chunk) {
        int baseX = chunk.getChunkX() * Chunk.SIZE;
        int baseZ = chunk.getChunkZ() * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            int worldX = baseX + x;

            for (int z = 0; z < Chunk.SIZE; z++) {
                int worldZ = baseZ + z;

                // Simple terrain height using sine waves
                double noise = Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1);
                int groundHeight = 58 + (int)(noise * 4); // Height 54-62

                // Generate terrain layers FROM BOTTOM UP
                for (int y = 0; y <= groundHeight; y++) {
                    if (y == 0) {
                        // Bedrock at bottom
                        chunk.setBlock(x, y, z, "space_test:stone");
                    } else if (y < groundHeight - 3) {
                        // Deep stone
                        chunk.setBlock(x, y, z, "space_test:stone");
                    } else if (y < groundHeight) {
                        // Dirt layer (2-3 blocks)
                        chunk.setBlock(x, y, z, "space_test:dirt");
                    } else {
                        // Grass on top
                        chunk.setBlock(x, y, z, "space_test:grass");
                    }
                }

                // Random trees
                if (groundHeight < Chunk.HEIGHT - 8) {
                    Random r = new Random(seed + worldX * 374761393L + worldZ * 668265263L);
                    if (r.nextFloat() < 0.015f) { // 1.5% chance - less trees
                        generateTree(chunk, x, groundHeight + 1, z, r);
                    }
                }
            }
        }

        chunk.clearDirty();
    }

    private void generateTree(Chunk chunk, int x, int y, int z, Random r) {
        int treeHeight = 4 + r.nextInt(3);

        // Trunk
        for (int ty = 0; ty < treeHeight; ty++) {
            if (y + ty < Chunk.HEIGHT) {
                chunk.setBlock(x, y + ty, z, "space_test:wood");
            }
        }

        // Leaves (smaller canopy to reduce block count)
        int leafY = y + treeHeight - 1;
        for (int lx = -1; lx <= 1; lx++) {
            for (int lz = -1; lz <= 1; lz++) {
                for (int ly = 0; ly < 2; ly++) {
                    // Skip center trunk
                    if (lx == 0 && lz == 0 && ly == 0) continue;

                    int leafX = x + lx;
                    int leafZ = z + lz;
                    int finalY = leafY + ly;

                    if (leafX >= 0 && leafX < Chunk.SIZE &&
                            leafZ >= 0 && leafZ < Chunk.SIZE &&
                            finalY < Chunk.HEIGHT) {
                        chunk.setBlock(leafX, finalY, leafZ, "space_test:leaves");
                    }
                }
            }
        }
    }
}