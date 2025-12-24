package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.world.World;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Adapter for UltraOptimizedRenderer that provides BufferedImage compatibility
 * with the existing Window class
 */
public class RendererAdapter {
    private final UltraOptimizedRenderer renderer;
    private BufferedImage screenBuffer;
    private int[] bufferPixels;
    private int currentWidth = 0;
    private int currentHeight = 0;

    public RendererAdapter(World world) {
        this.renderer = new UltraOptimizedRenderer(world, 1);
    }

    public RendererAdapter(World world, int threadCount) {
        this.renderer = new UltraOptimizedRenderer(world, threadCount);
    }

    /**
     * Set canvas size and create BufferedImage wrapper
     */
    public void setCanvasSize(Dimension dimension) {
        setCanvasSize(dimension.width, dimension.height);
    }

    public void setCanvasSize(int w, int h) {
        if (currentWidth != w || currentHeight != h) {
            // Update renderer
            renderer.setCanvasSize(w, h);

            // Create BufferedImage wrapper
            screenBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            bufferPixels = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();

            currentWidth = w;
            currentHeight = h;
        }
    }

    /**
     * Render frame - copies pixels to BufferedImage
     */
    public void render() {
        renderer.render();

        // Fast copy from renderer to BufferedImage
        int[] rendererPixels = renderer.getPixels();
        if (rendererPixels != null && bufferPixels != null) {
            System.arraycopy(rendererPixels, 0, bufferPixels, 0,
                    Math.min(rendererPixels.length, bufferPixels.length));
        }
    }

    /**
     * Get BufferedImage for display (compatible with Window class)
     */
    public BufferedImage getScreenBuffer() {
        return screenBuffer;
    }

    // Delegate camera properties
    public double x, y, z;
    public double yaw, pitch;

    public void markChunkDirty(int chunkX, int chunkZ) {
        renderer.markChunkDirty(chunkX, chunkZ);
    }

    public void preloadChunksAround(double x, double z, int radius) {
        renderer.preloadChunksAround(x, z, radius);
    }

    // Stats
    public int getChunksRendered() { return renderer.getChunksRendered(); }
    public int getChunksMeshing() { return renderer.getChunksMeshing(); }
    public int getCachedMeshCount() { return renderer.getCachedMeshCount(); }
    public int getQuadsRendered() { return renderer.getQuadsRendered(); }
    public int getQuadsCulled() { return renderer.getQuadsCulled(); }

    public void shutdown() {
        renderer.shutdown();
    }

    /**
     * Sync camera values before render
     */
    public void syncCamera() {
        renderer.x = this.x;
        renderer.y = this.y;
        renderer.z = this.z;
        renderer.yaw = this.yaw;
        renderer.pitch = this.pitch;
    }
}