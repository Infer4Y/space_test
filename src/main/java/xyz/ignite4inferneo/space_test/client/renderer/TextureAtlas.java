package xyz.ignite4inferneo.space_test.client.renderer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashMap;
import java.util.Map;

/**
 * Texture atlas for efficient texture sampling
 */
public class TextureAtlas {
    private static final int TEXTURE_SIZE = 16;
    private static final int TEXTURE_SHIFT = 4;
    private static final int TEXTURE_MASK = 15;

    private final Map<Integer, int[]> textures = new HashMap<>();
    private int nextIndex = 0;

    public TextureAtlas() {
        // Register default textures
        registerTexture(TextureUtils.createBlockTexture("stone", TEXTURE_SIZE));
        registerTexture(TextureUtils.createBlockTexture("dirt", TEXTURE_SIZE));
        registerTexture(TextureUtils.createBlockTexture("grass_side", TEXTURE_SIZE));
        registerTexture(TextureUtils.createBlockTexture("grass", TEXTURE_SIZE));
        registerTexture(TextureUtils.createBlockTexture("wood", TEXTURE_SIZE));
        registerTexture(TextureUtils.createBlockTexture("leaves", TEXTURE_SIZE));
    }

    /**
     * Register a new texture and return its index
     */
    public int registerTexture(BufferedImage texture) {
        int[] pixels = ((DataBufferInt) texture.getRaster().getDataBuffer()).getData();
        int index = nextIndex++;
        textures.put(index, pixels);
        return index;
    }

    /**
     * Sample a texture with wrapping UVs
     */
    public int sample(int texIndex, double u, double v) {
        int[] pixels = textures.get(texIndex);
        if (pixels == null) return 0xFF00FF; // Magenta for missing texture

        // IMPORTANT: Wrap UVs using modulo
        // This ensures textures tile correctly across merged quads
        u = u - Math.floor(u);  // Keep fractional part (0.0 to 1.0)
        v = v - Math.floor(v);

        int texX = ((int)(u * TEXTURE_SIZE)) & TEXTURE_MASK;
        int texY = ((int)(v * TEXTURE_SIZE)) & TEXTURE_MASK;

        return pixels[(texY << TEXTURE_SHIFT) + texX];
    }

    /**
     * Get number of registered textures
     */
    public int getTextureCount() {
        return nextIndex;
    }
}