package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.world.Chunk;

import java.util.ArrayList;
import java.util.List;

public class GreedyMesher {

    private static final int MAX_QUAD_SIZE = 4;

    // IMPORTANT: Use the correct air block ID
    private static final String AIR = "space_test:air";

    public static class Quad {
        public int x, y, z;
        public int w, h;
        public int axis;
        public int dir;
        public int texIndex;
        public float brightness;

        public Quad(int x, int y, int z, int w, int h, int axis, int dir, int texIndex, float brightness) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.axis = axis;
            this.dir = dir;
            this.texIndex = texIndex;
            this.brightness = brightness;
        }
    }

    public static List<Quad> mesh(Chunk chunk) {
        List<Quad> quads = new ArrayList<>();
        String[][][] blocks = chunk.getBlocks();

        meshYFaces(blocks, quads, -1);
        meshYFaces(blocks, quads, 1);
        meshZFaces(blocks, quads, -1);
        meshZFaces(blocks, quads, 1);
        meshXFaces(blocks, quads, -1);
        meshXFaces(blocks, quads, 1);

        return quads;
    }

    private static void meshYFaces(String[][][] blocks, List<Quad> quads, int dir) {
        boolean[][] mask = new boolean[Chunk.SIZE][Chunk.SIZE];
        int[][] texMask = new int[Chunk.SIZE][Chunk.SIZE];

        int faceIndex = dir > 0 ? 1 : 0;
        float brightness = dir > 0 ? 1.0f : 0.6f;

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    mask[x][z] = false;
                }
            }

            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    String block = blocks[x][y][z];
                    if (block.equals(AIR)) continue;

                    int checkY = y + dir;
                    if (checkY < 0 || checkY >= Chunk.HEIGHT || blocks[x][checkY][z].equals(AIR)) {
                        Block blockObj = Registries.BLOCKS.get(block);
                        if (blockObj != null) {
                            mask[x][z] = true;
                            texMask[x][z] = blockObj.getTextureIndices()[faceIndex];
                        }
                    }
                }
            }

            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; ) {
                    if (!mask[x][z]) {
                        z++;
                        continue;
                    }

                    int tex = texMask[x][z];

                    int w = 1;
                    while (x + w < Chunk.SIZE && w < MAX_QUAD_SIZE &&
                            mask[x + w][z] && texMask[x + w][z] == tex) {
                        w++;
                    }

                    int h = 1;
                    boolean done = false;
                    while (z + h < Chunk.SIZE && h < MAX_QUAD_SIZE && !done) {
                        for (int k = 0; k < w; k++) {
                            if (!mask[x + k][z + h] || texMask[x + k][z + h] != tex) {
                                done = true;
                                break;
                            }
                        }
                        if (!done) h++;
                    }

                    quads.add(new Quad(x, y, z, w, h, 1, dir, tex, brightness));

                    for (int i = 0; i < w; i++) {
                        for (int k = 0; k < h; k++) {
                            mask[x + i][z + k] = false;
                        }
                    }

                    z += h;
                }
            }
        }
    }

    private static void meshZFaces(String[][][] blocks, List<Quad> quads, int dir) {
        boolean[][] mask = new boolean[Chunk.SIZE][Chunk.HEIGHT];
        int[][] texMask = new int[Chunk.SIZE][Chunk.HEIGHT];

        int faceIndex = dir > 0 ? 3 : 2;
        float brightness = 0.8f;

        for (int z = 0; z < Chunk.SIZE; z++) {
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    mask[x][y] = false;
                }
            }

            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    String block = blocks[x][y][z];
                    if (block.equals(AIR)) continue;  // FIXED

                    int checkZ = z + dir;
                    if (checkZ < 0 || checkZ >= Chunk.SIZE || blocks[x][y][checkZ].equals(AIR)) {
                        Block blockObj = Registries.BLOCKS.get(block);
                        if (blockObj != null) {
                            mask[x][y] = true;
                            texMask[x][y] = blockObj.getTextureIndices()[faceIndex];
                        }
                    }
                }
            }

            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.HEIGHT; ) {
                    if (!mask[x][y]) {
                        y++;
                        continue;
                    }

                    int tex = texMask[x][y];

                    int w = 1;
                    while (x + w < Chunk.SIZE && w < MAX_QUAD_SIZE &&
                            mask[x + w][y] && texMask[x + w][y] == tex) {
                        w++;
                    }

                    int h = 1;
                    boolean done = false;
                    while (y + h < Chunk.HEIGHT && h < MAX_QUAD_SIZE && !done) {
                        for (int k = 0; k < w; k++) {
                            if (!mask[x + k][y + h] || texMask[x + k][y + h] != tex) {
                                done = true;
                                break;
                            }
                        }
                        if (!done) h++;
                    }

                    quads.add(new Quad(x, y, z, w, h, 2, dir, tex, brightness));

                    for (int i = 0; i < w; i++) {
                        for (int k = 0; k < h; k++) {
                            mask[x + i][y + k] = false;
                        }
                    }

                    y += h;
                }
            }
        }
    }

    private static void meshXFaces(String[][][] blocks, List<Quad> quads, int dir) {
        boolean[][] mask = new boolean[Chunk.SIZE][Chunk.HEIGHT];
        int[][] texMask = new int[Chunk.SIZE][Chunk.HEIGHT];

        int faceIndex = dir > 0 ? 5 : 4;
        float brightness = 0.8f;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    mask[z][y] = false;
                }
            }

            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    String block = blocks[x][y][z];
                    if (block.equals(AIR)) continue;  // FIXED

                    int checkX = x + dir;
                    if (checkX < 0 || checkX >= Chunk.SIZE || blocks[checkX][y][z].equals(AIR)) {
                        Block blockObj = Registries.BLOCKS.get(block);
                        if (blockObj != null) {
                            mask[z][y] = true;
                            texMask[z][y] = blockObj.getTextureIndices()[faceIndex];
                        }
                    }
                }
            }

            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int y = 0; y < Chunk.HEIGHT; ) {
                    if (!mask[z][y]) {
                        y++;
                        continue;
                    }

                    int tex = texMask[z][y];

                    int w = 1;
                    while (z + w < Chunk.SIZE && w < MAX_QUAD_SIZE &&
                            mask[z + w][y] && texMask[z + w][y] == tex) {
                        w++;
                    }

                    int h = 1;
                    boolean done = false;
                    while (y + h < Chunk.HEIGHT && h < MAX_QUAD_SIZE && !done) {
                        for (int k = 0; k < w; k++) {
                            if (!mask[z + k][y + h] || texMask[z + k][y + h] != tex) {
                                done = true;
                                break;
                            }
                        }
                        if (!done) h++;
                    }

                    quads.add(new Quad(x, y, z, w, h, 0, dir, tex, brightness));

                    for (int i = 0; i < w; i++) {
                        for (int k = 0; k < h; k++) {
                            mask[z + i][y + k] = false;
                        }
                    }

                    y += h;
                }
            }
        }
    }
}