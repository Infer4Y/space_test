package xyz.ignite4inferneo.space_test.common.world;

import java.util.Random;

/**
 * Default world generator with simple terrain
 */
public class DefaultWorldGenerator implements IWorldGenerator {

    private final long seed;
    private final Random random;

    public DefaultWorldGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
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
                int groundHeight = 5 + (int)(noise * 3)+50;

                // Generate terrain layers
                for (int y = 0; y < groundHeight; y++) {
                    if (y < groundHeight - 3) {
                        chunk.setBlock(x, y, z, "space_test:stone");
                    } else if (y < groundHeight - 1) {
                        chunk.setBlock(x, y, z, "space_test:dirt");
                    } else {
                        chunk.setBlock(x, y, z, "space_test:grass");
                    }
                }

                // Random trees/decorations
                if (groundHeight > 0 && groundHeight < Chunk.HEIGHT - 5) {
                    Random r = new Random(seed + worldX * 374761393L + worldZ * 668265263L);
                    if (r.nextFloat() < 0.02f) { // 2% chance
                        // Simple tree
                        int treeHeight = 4 + r.nextInt(3);
                        for (int ty = 0; ty < treeHeight; ty++) {
                            chunk.setBlock(x, groundHeight + ty, z, "space_test:wood");
                        }
                        // Leaves
                        for (int lx = -2; lx <= 2; lx++) {
                            for (int lz = -2; lz <= 2; lz++) {
                                for (int ly = 0; ly < 3; ly++) {
                                    if (lx == 0 && lz == 0 && ly < 2) continue;
                                    int leafX = x + lx;
                                    int leafZ = z + lz;
                                    int leafY = groundHeight + treeHeight - 1 + ly;
                                    if (leafX >= 0 && leafX < Chunk.SIZE &&
                                            leafZ >= 0 && leafZ < Chunk.SIZE &&
                                            leafY < Chunk.HEIGHT) {
                                        chunk.setBlock(leafX, leafY, leafZ, "space_test:leaves");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        chunk.clearDirty();
    }
}
