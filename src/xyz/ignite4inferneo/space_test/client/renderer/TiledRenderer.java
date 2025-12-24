package xyz.ignite4inferneo.space_test.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Tiled rendering system - divides screen into tiles and renders in parallel
 *
 * Benefits:
 * - Better CPU cache usage
 * - Parallel rendering of screen regions
 * - Reduced contention on shared buffers
 * - More consistent frame times
 */
public class TiledRenderer {

    private final int tileSize;
    private final ExecutorService executor;
    private final List<RenderTile> tiles;

    public static class RenderTile {
        public final int x, y;           // Tile position in pixels
        public final int width, height;  // Tile size in pixels
        public final int startX, startY; // Pixel coordinates
        public final int endX, endY;     // Pixel coordinates (exclusive)

        public RenderTile(int x, int y, int width, int height, int screenWidth, int screenHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x * width;
            this.startY = y * height;
            this.endX = Math.min(startX + width, screenWidth);
            this.endY = Math.min(startY + height, screenHeight);
        }

        public boolean contains(int pixelX, int pixelY) {
            return pixelX >= startX && pixelX < endX &&
                    pixelY >= startY && pixelY < endY;
        }
    }

    /**
     * Create tiled renderer
     * @param tileSize Size of each tile (64, 128, 256 recommended)
     * @param threadCount Number of worker threads
     */
    public TiledRenderer(int tileSize, int threadCount) {
        this.tileSize = tileSize;
        this.tiles = new ArrayList<>();

        // Create thread pool
        this.executor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });

        System.out.println("[TiledRenderer] Initialized with tile size " + tileSize +
                " and " + threadCount + " threads");
    }

    /**
     * Update tiles for new screen size
     */
    public void updateTiles(int screenWidth, int screenHeight) {
        tiles.clear();

        int tilesX = (screenWidth + tileSize - 1) / tileSize;
        int tilesY = (screenHeight + tileSize - 1) / tileSize;

        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                tiles.add(new RenderTile(tx, ty, tileSize, tileSize, screenWidth, screenHeight));
            }
        }

        System.out.println("[TiledRenderer] Created " + tiles.size() + " tiles (" +
                tilesX + "x" + tilesY + ")");
    }

    /**
     * Render all tiles in parallel
     * @param renderTask Task to run for each tile
     */
    public void renderTiles(TileRenderTask renderTask) throws InterruptedException, ExecutionException {
        List<Future<?>> futures = new ArrayList<>();

        // Submit all tiles to thread pool
        for (RenderTile tile : tiles) {
            Future<?> future = executor.submit(() -> {
                try {
                    renderTask.renderTile(tile);
                } catch (Exception e) {
                    System.err.println("[TiledRenderer] Error rendering tile: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // Wait for all tiles to complete
        for (Future<?> future : futures) {
            future.get();
        }
    }

    /**
     * Render tiles with priority ordering (useful for progressive rendering)
     * Center tiles render first for better perceived performance
     */
    public void renderTilesPrioritized(TileRenderTask renderTask, int centerX, int centerY)
            throws InterruptedException, ExecutionException {

        // Sort tiles by distance from center
        List<RenderTile> sortedTiles = new ArrayList<>(tiles);
        sortedTiles.sort((a, b) -> {
            int aCenterX = a.startX + a.width / 2;
            int aCenterY = a.startY + a.height / 2;
            int bCenterX = b.startX + b.width / 2;
            int bCenterY = b.startY + b.height / 2;

            int distA = (aCenterX - centerX) * (aCenterX - centerX) +
                    (aCenterY - centerY) * (aCenterY - centerY);
            int distB = (bCenterX - centerX) * (bCenterX - centerX) +
                    (bCenterY - centerY) * (bCenterY - centerY);

            return Integer.compare(distA, distB);
        });

        List<Future<?>> futures = new ArrayList<>();

        // Render center tiles first
        for (RenderTile tile : sortedTiles) {
            Future<?> future = executor.submit(() -> {
                try {
                    renderTask.renderTile(tile);
                } catch (Exception e) {
                    System.err.println("[TiledRenderer] Error rendering tile: " + e.getMessage());
                }
            });
            futures.add(future);
        }

        // Wait for all tiles
        for (Future<?> future : futures) {
            future.get();
        }
    }

    /**
     * Get tile at pixel position
     */
    public RenderTile getTileAt(int pixelX, int pixelY) {
        for (RenderTile tile : tiles) {
            if (tile.contains(pixelX, pixelY)) {
                return tile;
            }
        }
        return null;
    }

    /**
     * Get all tiles
     */
    public List<RenderTile> getTiles() {
        return tiles;
    }

    /**
     * Shutdown thread pool
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    /**
     * Interface for tile rendering tasks
     */
    @FunctionalInterface
    public interface TileRenderTask {
        void renderTile(RenderTile tile);
    }
}