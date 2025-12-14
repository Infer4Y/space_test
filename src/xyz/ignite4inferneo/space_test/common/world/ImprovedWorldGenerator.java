package xyz.ignite4inferneo.space_test.common.world;

import java.util.Random;

/**
 * Advanced terrain generator with:
 * - Mountains using ridged noise
 * - Valleys and rolling hills
 * - River-like carvings
 * - Better distribution of terrain features
 */
public class ImprovedWorldGenerator implements IWorldGenerator {

    private final long seed;
    private final PerlinNoise continentalnessNoise;  // Large-scale terrain
    private final PerlinNoise erosionNoise;          // Valleys and rivers
    private final PerlinNoise peaksNoise;            // Mountain peaks
    private final PerlinNoise detailNoise;           // Small details

    // Terrain parameters
    private static final int SEA_LEVEL = 62;
    private static final int MIN_HEIGHT = 40;
    private static final int MAX_HEIGHT = 120;

    public ImprovedWorldGenerator(long seed) {
        this.seed = seed;
        this.continentalnessNoise = new PerlinNoise(seed);
        this.erosionNoise = new PerlinNoise(seed + 1);
        this.peaksNoise = new PerlinNoise(seed + 2);
        this.detailNoise = new PerlinNoise(seed + 3);
    }

    @Override
    public void generateChunk(World world, Chunk chunk) {
        int baseX = chunk.getChunkX() * Chunk.SIZE;
        int baseZ = chunk.getChunkZ() * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int worldX = baseX + x;
                int worldZ = baseZ + z;

                int height = calculateTerrainHeight(worldX, worldZ);
                generateColumn(chunk, x, z, height, worldX, worldZ);
            }
        }

        chunk.clearDirty();
    }

    private int calculateTerrainHeight(int x, int z) {
        double scale = 0.003; // Smaller = bigger features
        double riverScale = 0.008; // Rivers

        // 1. Continental shape (large rolling hills)
        double continental = continentalnessNoise.octaveNoise(
                x * scale * 0.5, z * scale * 0.5, 4, 0.5
        );

        // 2. Mountain peaks (ridged noise for dramatic mountains)
        double peaks = peaksNoise.ridgedNoise(
                x * scale * 2.0, z * scale * 2.0, 5, 0.5
        );

        // 3. Erosion (valleys and river carving)
        double erosion = erosionNoise.octaveNoise(
                x * riverScale, z * riverScale, 4, 0.6
        );

        // Create river valleys where erosion is near zero
        double riverCarving = Math.abs(erosion);
        riverCarving = Math.pow(riverCarving, 0.5); // Make rivers wider

        // 4. Fine detail
        double detail = detailNoise.noise(x * scale * 8, z * scale * 8) * 0.1;

        // Combine noises
        // Continental provides base height
        double baseHeight = continental * 0.6;

        // Add mountains where continental is high
        double mountainInfluence = Math.max(0, continental * 0.8);
        double mountainHeight = peaks * mountainInfluence;

        // Apply river carving (deeper valleys in low areas)
        double carveAmount = 1.0 - riverCarving;
        carveAmount = Math.pow(carveAmount, 3.0); // Sharp river cuts

        // Combine everything
        double finalNoise = baseHeight + mountainHeight * 0.8 - carveAmount * 0.3 + detail;

        // Map to world height
        int height = (int) (SEA_LEVEL + finalNoise * (MAX_HEIGHT - MIN_HEIGHT));

        // Clamp
        return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
    }

    private void generateColumn(Chunk chunk, int x, int z, int height, int worldX, int worldZ) {
        // Generate terrain layers
        for (int y = 0; y <= height; y++) {
            if (y == 0) {
                // Bedrock
                chunk.setBlock(x, y, z, "space_test:stone");
            } else if (y < height - 4) {
                // Deep stone
                chunk.setBlock(x, y, z, "space_test:stone");
            } else if (y < height) {
                // Dirt layer
                chunk.setBlock(x, y, z, "space_test:dirt");
            } else {
                // Surface block
                if (height < SEA_LEVEL - 1) {
                    // Sand near water (future)
                    chunk.setBlock(x, y, z, "space_test:dirt");
                } else {
                    // Grass
                    chunk.setBlock(x, y, z, "space_test:grass");
                }
            }
        }

        // Trees on grass above sea level
        if (height >= SEA_LEVEL && height < Chunk.HEIGHT - 8) {
            Random r = new Random(seed + worldX * 374761393L + worldZ * 668265263L);

            // More trees in valleys, fewer on mountain peaks
            double treeDensity = 0.02; // Base 2% chance

            if (r.nextFloat() < treeDensity) {
                generateTree(chunk, x, height + 1, z, r);
            }
        }
    }

    private void generateTree(Chunk chunk, int x, int baseY, int z, Random r) {
        int treeHeight = 4 + r.nextInt(3);

        // Trunk
        for (int y = 0; y < treeHeight; y++) {
            int worldY = baseY + y;
            if (worldY < Chunk.HEIGHT) {
                chunk.setBlock(x, worldY, z, "space_test:wood");
            }
        }

        // Leaves (compact canopy)
        int leafStartY = baseY + treeHeight - 2;
        for (int lx = -1; lx <= 1; lx++) {
            for (int lz = -1; lz <= 1; lz++) {
                for (int ly = 0; ly < 3; ly++) {
                    // Skip trunk center on lower levels
                    if (lx == 0 && lz == 0 && ly < 2) continue;

                    int leafX = x + lx;
                    int leafZ = z + lz;
                    int leafY = leafStartY + ly;

                    if (leafX >= 0 && leafX < Chunk.SIZE &&
                            leafZ >= 0 && leafZ < Chunk.SIZE &&
                            leafY < Chunk.HEIGHT) {
                        chunk.setBlock(leafX, leafY, leafZ, "space_test:leaves");
                    }
                }
            }
        }
    }

    @Override
    public int[] getSpawnPosition() {
        // Spawn at a reasonable height
        return new int[]{0, 70, 0};
    }
}