package xyz.ignite4inferneo.space_test.client.renderer;

/**
 * Cube geometry helper. Vertices represent a unit cube (1×1×1).
 * Face order matches the mesher mapping used in ChunkRenderer.
 */
public class Cube {
    // unit cube centered at origin? In mesher we offset to chunk coords and then shift by half-chunk
    public static final double[][] vertices = {
            {0,0,0}, {1,0,0}, {1,1,0}, {0,1,0},
            {0,0,1}, {1,0,1}, {1,1,1}, {0,1,1}
    };

    // faces (quads) using above indices
    public static final int[][] faces = {
            {0,1,2,3},
            {1,5,6,2},
            {5,4,7,6},
            {4,0,3,7},
            {3,2,6,7},
            {4,5,1,0}
    };

    // simple default UVs; mesher uses these
    public static double[][] getFaceUV(int face) {
        return new double[][]{{0,0},{1,0},{1,1},{0,1}};
    }
}
