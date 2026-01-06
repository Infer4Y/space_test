package xyz.ignite4inferneo.space_test.common.world;

/**
 * Perlin noise generator for natural terrain generation.
 * Based on Ken Perlin's improved noise algorithm.
 */
public class PerlinNoise {
    private final int[] permutation;

    public PerlinNoise(long seed) {
        // Generate permutation table from seed
        permutation = new int[512];
        int[] p = new int[256];

        // Initialize with sequential values
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        // Shuffle using seed
        java.util.Random rand = new java.util.Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }

        // Duplicate for overflow
        for (int i = 0; i < 256; i++) {
            permutation[i] = permutation[i + 256] = p[i];
        }
    }

    /**
     * Get 2D Perlin noise value at coordinates
     * @return value between -1 and 1
     */
    public double noise(double x, double y) {
        // Find unit grid cell containing point
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        // Get relative coordinates within cell
        x -= Math.floor(x);
        y -= Math.floor(y);

        // Compute fade curves
        double u = fade(x);
        double v = fade(y);

        // Hash coordinates of 4 corners
        int A = permutation[X] + Y;
        int B = permutation[X + 1] + Y;

        // Blend results from 4 corners
        return lerp(v,
                lerp(u, grad(permutation[A], x, y),
                        grad(permutation[B], x - 1, y)),
                lerp(u, grad(permutation[A + 1], x, y - 1),
                        grad(permutation[B + 1], x - 1, y - 1))
        );
    }

    /**
     * Octave noise - multiple layers for more detail
     */
    public double octaveNoise(double x, double y, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    /**
     * Ridged noise - creates ridge-like features (good for mountains)
     */
    public double ridgedNoise(double x, double y, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            double n = Math.abs(noise(x * frequency, y * frequency));
            n = 1.0 - n; // Invert
            n = n * n; // Sharpen ridges
            total += n * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    private double fade(double t) {
        // 6t^5 - 15t^4 + 10t^3
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y) {
        // Convert low 2 bits of hash to gradient direction
        int h = hash & 3;
        double u = h < 2 ? x : y;
        double v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}