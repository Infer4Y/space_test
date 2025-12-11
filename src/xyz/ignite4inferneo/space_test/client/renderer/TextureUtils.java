package xyz.ignite4inferneo.space_test.client.renderer;

import java.awt.image.BufferedImage;
import java.util.Random;

public class TextureUtils {

    private static final Random rand = new Random();

    public static BufferedImage createRandomNoise(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                int rgb = ((int)(Math.random()*256) << 16) |
                        ((int)(Math.random()*256) << 8) |
                        ((int)(Math.random()*256));
                img.setRGB(x, y, 0xFF000000 | rgb);
            }
        return img;
    }

    public static BufferedImage createBlockTexture(String type, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = switch(type.toLowerCase()) {
                    case "grass" -> generateGrassPixel(x, y, size);
                    case "dirt" -> generateDirtPixel(x, y, size);
                    case "stone" -> generateStonePixel(x, y, size);
                    default -> 0xFFFFFF;
                };
                img.setRGB(x, y, 0xFF000000 | color);
            }
        }
        return img;
    }

    private static int generateGrassPixel(int x, int y, int size) {
        // Rich blue/purple grass with varied patterns
        int baseR = 30;
        int baseG = 70;
        int baseB = 180;

        // Multi-octave noise for natural variation
        double noise = perlinNoise(x / 4.0, y / 4.0) * 0.5
                + perlinNoise(x / 2.0, y / 2.0) * 0.3
                + perlinNoise(x, y) * 0.2;

        int r = baseR + (int)(noise * 40);
        int g = baseG + (int)(noise * 60);
        int b = baseB + (int)(noise * 50);

        // Add blade-like streaks
        if ((x + y * 3) % 5 < 2 && rand.nextFloat() < 0.3f) {
            r += 20;
            g += 30;
            b += 15;
        }

        // Random bright highlights (dewdrops/sparkle)
        if (rand.nextFloat() < 0.05f) {
            r = Math.min(255, r + 80);
            g = Math.min(255, g + 100);
            b = Math.min(255, b + 60);
        }

        // Darker patches for depth
        if (((x / 3) + (y / 3)) % 2 == 0 && rand.nextFloat() < 0.4f) {
            r = Math.max(0, r - 25);
            g = Math.max(0, g - 35);
            b = Math.max(0, b - 40);
        }

        return clampRGB(r, g, b);
    }

    private static int generateDirtPixel(int x, int y, int size) {
        // Rich brown earth with varied texture
        int baseR = 110;
        int baseG = 70;
        int baseB = 40;

        // Layered noise for soil texture
        double noise = perlinNoise(x / 3.0, y / 3.0) * 0.6
                + perlinNoise(x, y) * 0.4;

        int r = baseR + (int)(noise * 50);
        int g = baseG + (int)(noise * 40);
        int b = baseB + (int)(noise * 30);

        // Add small pebbles/particles
        if (rand.nextFloat() < 0.15f) {
            int pebble = rand.nextInt(40) - 20;
            r += pebble;
            g += pebble / 2;
            b += pebble / 3;
        }

        // Darker clumps
        if (((x / 2) ^ (y / 2)) % 3 == 0) {
            r = Math.max(0, r - 20);
            g = Math.max(0, g - 15);
            b = Math.max(0, b - 10);
        }

        // Occasional bright minerals
        if (rand.nextFloat() < 0.08f) {
            r = Math.min(255, r + 40);
            g = Math.min(255, g + 35);
            b = Math.min(255, b + 25);
        }

        return clampRGB(r, g, b);
    }

    private static int generateStonePixel(int x, int y, int size) {
        // Detailed gray stone with crystal veins
        int baseGray = 100;

        // Multi-frequency noise for rock texture
        double noise = perlinNoise(x / 5.0, y / 5.0) * 0.5
                + perlinNoise(x / 2.0, y / 2.0) * 0.3
                + perlinNoise(x, y) * 0.2;

        int gray = baseGray + (int)(noise * 60);

        // Add crystalline veins (diagonal patterns)
        if (Math.abs((x - y) % 7) < 2 && rand.nextFloat() < 0.3f) {
            gray = Math.min(255, gray + 50);
        }

        // Random bright crystal specks
        if (rand.nextFloat() < 0.1f) {
            gray = Math.min(255, gray + 70);
        }

        // Cracks and darker spots
        if ((x + y * 2) % 8 == 0 || (x * 2 + y) % 9 == 0) {
            gray = Math.max(0, gray - 40);
        }

        // Very subtle color tint for interest
        int r = gray;
        int g = Math.max(0, gray - 5);
        int b = Math.max(0, gray - 8);

        return clampRGB(r, g, b);
    }

    // Simple Perlin-like noise using interpolated random values
    private static double perlinNoise(double x, double y) {
        int xi = (int)Math.floor(x);
        int yi = (int)Math.floor(y);

        double xf = x - xi;
        double yf = y - yi;

        // Sample corners
        double n00 = randomGradient(xi, yi, xf, yf);
        double n10 = randomGradient(xi + 1, yi, xf - 1, yf);
        double n01 = randomGradient(xi, yi + 1, xf, yf - 1);
        double n11 = randomGradient(xi + 1, yi + 1, xf - 1, yf - 1);

        // Smoothstep interpolation
        double u = smoothstep(xf);
        double v = smoothstep(yf);

        double nx0 = lerp(n00, n10, u);
        double nx1 = lerp(n01, n11, u);

        return lerp(nx0, nx1, v);
    }

    private static double randomGradient(int ix, int iy, double x, double y) {
        // Pseudo-random gradient based on integer coordinates
        int hash = (ix * 374761393 + iy * 668265263) & 0x7FFFFFFF;
        double angle = (hash % 628318) / 100000.0; // ~2*PI
        return x * Math.cos(angle) + y * Math.sin(angle);
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static int clampRGB(int r, int g, int b) {
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }
}