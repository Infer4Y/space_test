package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.world.Chunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple non-greedy mesher that generates one quad per face.
 * Use this if greedy meshing has bugs - it's slower but always works correctly.
 */
public class SimpleChunkMesher {

    /**
     * Generate simple mesh (one 1x1 quad per visible face)
     */
    public static List<GreedyMesher.Quad> mesh(Chunk chunk) {
        List<GreedyMesher.Quad> quads = new ArrayList<>();
        String[][][] blocks = chunk.getBlocks();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    String block = blocks[x][y][z];
                    if (block.equals("space_test:air")) continue;

                    Block blockObj = Registries.BLOCKS.get(block);
                    if (blockObj == null) continue;

                    int[] texIndices = blockObj.getTextureIndices();

                    // Check each face and add if exposed
                    // Bottom face (Y-)
                    if (isAir(blocks, x, y - 1, z)) {
                        quads.add(new GreedyMesher.Quad(x, y, z, 1, 1, 1, -1, texIndices[0], 0.6f));
                    }

                    // Top face (Y+)
                    if (isAir(blocks, x, y + 1, z)) {
                        quads.add(new GreedyMesher.Quad(x, y, z, 1, 1, 1, 1, texIndices[1], 1.0f));
                    }

                    // North face (Z-)
                    if (isAir(blocks, x, y, z - 1)) {
                        quads.add(new GreedyMesher.Quad(x, y, z, 1, 1, 2, -1, texIndices[2], 0.8f));
                    }

                    // South face (Z+)
                    if (isAir(blocks, x, y, z + 1)) {
                        quads.add(new GreedyMesher.Quad(x, y, z, 1, 1, 2, 1, texIndices[3], 0.8f));
                    }

                    // West face (X-)
                    if (isAir(blocks, x - 1, y, z)) {
                        quads.add(new GreedyMesher.Quad(x, y, z, 1, 1, 0, -1, texIndices[4], 0.8f));
                    }

                    // East face (X+)
                    if (isAir(blocks, x + 1, y, z)) {
                        quads.add(new GreedyMesher.Quad(x, y, z, 1, 1, 0, 1, texIndices[5], 0.8f));
                    }
                }
            }
        }

        return quads;
    }

    private static boolean isAir(String[][][] blocks, int x, int y, int z) {
        if (x < 0 || x >= Chunk.SIZE ||
                y < 0 || y >= Chunk.HEIGHT ||
                z < 0 || z >= Chunk.SIZE) {
            return true; // Treat out of bounds as air
        }
        return blocks[x][y][z].equals("space_test:air");
    }
}