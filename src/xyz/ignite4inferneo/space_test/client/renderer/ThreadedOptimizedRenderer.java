package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.world.Chunk;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-threaded optimized renderer with async chunk meshing
 */
public class ThreadedOptimizedRenderer {
    private static final int RENDER_DISTANCE = 8;

    private final World world;
    private BufferedImage screenBuffer;
    private int[] pixels;
    private double[] zBuffer;
    protected int width;
    protected int height;

    // Camera
    public double x = 0, y = 70, z = 0;
    public double yaw = 0;
    public double pitch = 0;

    // Texture atlas
    private final TextureAtlas textureAtlas;

    // Multi-threaded mesher
    private final ThreadedChunkMesher mesher;

    // Mesh cache (thread-safe)
    private final ConcurrentHashMap<Long, ChunkMesh> meshCache = new ConcurrentHashMap<>();
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> meshingInProgress = ConcurrentHashMap.newKeySet();

    // Pre-computed values
    private double halfWidth, halfHeight, invTanHalfFov;

    // Face pool for rendering
    private final ArrayList<RenderFace> renderFaces = new ArrayList<>(8192);
    private final ArrayDeque<RenderFace> facePool = new ArrayDeque<>(8192);

    // Stats
    private int chunksRendered = 0;
    private int chunksMeshing = 0;
    private int quadsSubdivided = 0;
    private int quadsCulled = 0;
    private int quadsRendered = 0;

    private static class ChunkMesh {
        List<GreedyMesher.Quad> quads;
        long lastUsed;
        boolean ready; // Mesh is ready to render

        ChunkMesh(List<GreedyMesher.Quad> quads) {
            this.quads = quads;
            this.lastUsed = System.currentTimeMillis();
            this.ready = true;
        }
    }

    private static class RenderFace {
        int[] x = new int[4];
        int[] y = new int[4];
        double[] d = new double[4];
        double[] uv = new double[8];
        int texIndex;
        float brightness;
        double avgDepth;
    }

    public ThreadedOptimizedRenderer(World world) {
        this(world, Runtime.getRuntime().availableProcessors());
    }

    public ThreadedOptimizedRenderer(World world, int threadCount) {
        this.world = world;
        this.textureAtlas = new TextureAtlas();
        this.mesher = new ThreadedChunkMesher(threadCount);

        // Pre-populate face pool
        for (int i = 0; i < 8192; i++) {
            facePool.add(new RenderFace());
        }

        System.out.println("[ThreadedRenderer] Initialized with " + threadCount + " meshing threads");
    }

    /**
     * Pre-generate and mesh chunks around a position (useful for spawn)
     */
    public void preloadChunksAround(double x, double z, int radius) {
        int centerChunkX = (int) Math.floor(x) >> 4;
        int centerChunkZ = (int) Math.floor(z) >> 4;

        System.out.println("[ThreadedRenderer] Preloading " + (radius * 2 + 1) * (radius * 2 + 1) + " chunks...");

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                long key = chunkKey(chunkX, chunkZ);

                // Generate the chunk (this happens immediately)
                Chunk chunk = world.getChunk(chunkX, chunkZ);

                // Start meshing it
                if (chunk != null && !meshingInProgress.contains(key)) {
                    startMeshing(chunk, chunkX, chunkZ, key);
                }
            }
        }

        System.out.println("[ThreadedRenderer] Preload initiated, meshing in background...");
    }

    public void setCanvasSize(Dimension dimension) {
        setCanvasSize(dimension.width, dimension.height);
    }

    public void setCanvasSize(int w, int h) {
        if (width == w && height == h && screenBuffer != null) return;

        width = w;
        height = h;
        halfWidth = w * 0.5;
        halfHeight = h * 0.5;

        double fov = Math.PI / 3.0;
        invTanHalfFov = 1.0 / Math.tan(fov * 0.5);

        screenBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();
        zBuffer = new double[w * h];
    }

    public BufferedImage getScreenBuffer() {
        return screenBuffer;
    }

    public void markChunkDirty(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        dirtyChunks.add(key);
        // DON'T remove old mesh immediately - keep it until new one is ready
        // This prevents flickering during rebuild
    }

    public void render() {
        if (screenBuffer == null) return;

        // Clear buffers
        Arrays.fill(pixels, 0x87CEEB);
        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);

        // Return faces to pool
        for (RenderFace face : renderFaces) {
            facePool.add(face);
        }
        renderFaces.clear();

        // Reset stats
        quadsSubdivided = 0;
        quadsCulled = 0;
        quadsRendered = 0;

        // Pre-compute camera matrix
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        double fx = sinYaw * cosPitch;
        double fy = -sinPitch;
        double fz = cosYaw * cosPitch;
        double rx = cosYaw;
        double rz = -sinYaw;
        double ux = sinYaw * sinPitch;
        double uy = cosPitch;
        double uz = cosYaw * sinPitch;

        int camChunkX = (int) Math.floor(x) >> 4;
        int camChunkZ = (int) Math.floor(z) >> 4;

        // Process dirty chunks (start meshing asynchronously)
        processDirtyChunks();

        // Collect chunks to render (prioritize by distance)
        List<ChunkRenderTask> renderTasks = new ArrayList<>();
        List<ChunkRenderTask> needsMeshing = new ArrayList<>();

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = camChunkX + dx;
                int chunkZ = camChunkZ + dz;

                // Distance check
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue;

                long key = chunkKey(chunkX, chunkZ);
                double distSq = dx * dx + dz * dz;

                ChunkRenderTask task = new ChunkRenderTask(chunkX, chunkZ, key, distSq);
                renderTasks.add(task);

                // Check if this chunk needs meshing
                if (!meshCache.containsKey(key) && !meshingInProgress.contains(key)) {
                    needsMeshing.add(task);
                }
            }
        }

        // Sort chunks needing meshing by distance (closest first)
        if (!needsMeshing.isEmpty()) {
            needsMeshing.sort(Comparator.comparingDouble(t -> t.distanceSq));

            // Start meshing closest chunks first (limit to prevent overload)
            int meshingStarted = 0;
            int maxNewMeshes = 16; // Limit new meshes per frame

            for (ChunkRenderTask task : needsMeshing) {
                if (meshingStarted >= maxNewMeshes) break;

                Chunk chunk = world.getChunk(task.chunkX, task.chunkZ);
                if (chunk != null && !meshingInProgress.contains(task.key)) {
                    startMeshing(chunk, task.chunkX, task.chunkZ, task.key);
                    meshingStarted++;
                }
            }
        }

        // Sort by distance (closest first)
        renderTasks.sort(Comparator.comparingDouble(t -> t.distanceSq));

        // Render all visible chunks
        chunksRendered = 0;
        chunksMeshing = meshingInProgress.size();

        for (ChunkRenderTask task : renderTasks) {
            if (renderChunk(task.chunkX, task.chunkZ, task.key, fx, fy, fz, rx, rz, ux, uy, uz)) {
                chunksRendered++;
            }
        }

        // Sort faces back to front
        renderFaces.sort((a, b) -> Double.compare(b.avgDepth, a.avgDepth));

        // Render all faces
        for (RenderFace face : renderFaces) {
            fillTexturedQuad(face);
        }

        // Clean old meshes periodically
        if (meshCache.size() > 400) {
            cleanOldMeshes();
        }
    }

    private static class ChunkRenderTask {
        int chunkX, chunkZ;
        long key;
        double distanceSq;

        ChunkRenderTask(int x, int z, long key, double dist) {
            this.chunkX = x;
            this.chunkZ = z;
            this.key = key;
            this.distanceSq = dist;
        }
    }

    private void processDirtyChunks() {
        // Start meshing for dirty chunks
        Iterator<Long> it = dirtyChunks.iterator();
        while (it.hasNext()) {
            Long key = it.next();
            int chunkX = (int)(key >> 32);
            int chunkZ = (int)(key & 0xFFFFFFFFL);

            Chunk chunk = world.getChunk(chunkX, chunkZ);
            if (chunk != null && !meshingInProgress.contains(key)) {
                startMeshing(chunk, chunkX, chunkZ, key);
                it.remove(); // Remove as we process it
            }
        }
    }

    private void startMeshing(Chunk chunk, int chunkX, int chunkZ, long key) {
        meshingInProgress.add(key);

        CompletableFuture<ThreadedChunkMesher.MeshResult> future =
                mesher.submitChunk(chunk, chunkX, chunkZ, key);

        future.thenAccept(result -> {
            // Replace old mesh with new one (smooth transition)
            ChunkMesh mesh = new ChunkMesh(result.quads);
            meshCache.put(result.key, mesh);
            chunk.clearDirty();
            meshingInProgress.remove(result.key);
            dirtyChunks.remove(result.key); // Remove from dirty set once complete
        });
    }

    private boolean renderChunk(int chunkX, int chunkZ, long key,
                                double fx, double fy, double fz,
                                double rx, double rz, double ux, double uy, double uz) {

        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null) return false;

        ChunkMesh mesh = meshCache.get(key);

        // If no mesh exists, it's being meshed or will be meshed soon
        if (mesh == null) {
            return false; // Can't render yet
        }

        // Mesh is ready - render it
        mesh.lastUsed = System.currentTimeMillis();

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (GreedyMesher.Quad quad : mesh.quads) {
            renderQuad(quad, baseX, baseZ, fx, fy, fz, rx, rz, ux, uy, uz);
        }

        return true;
    }

    private void renderQuad(GreedyMesher.Quad quad, int baseX, int baseZ,
                            double fx, double fy, double fz,
                            double rx, double rz, double ux, double uy, double uz) {

        double wx = baseX + quad.x;
        double wy = quad.y;
        double wz = baseZ + quad.z;

        // For large quads, subdivide to prevent culling visible parts
        // Subdivide more aggressively for quads that might span the view frustum
        boolean needsSubdivision = false;

        // Calculate quad center and size
        double centerX = wx + quad.w * 0.5;
        double centerY = wy + (quad.axis == 1 ? 0 : quad.h * 0.5);
        double centerZ = wz + (quad.axis == 1 ? quad.h * 0.5 : (quad.axis == 2 ? 0 : quad.w * 0.5));

        double dx = centerX - x;
        double dy = centerY - y;
        double dz = centerZ - z;
        double distSq = dx*dx + dy*dy + dz*dz;
        double dist = Math.sqrt(distSq);

        // Subdivide based on size and distance
        // Larger quads need subdivision even when further away
        int maxDim = Math.max(quad.w, quad.h);

        if (maxDim >= 8) {
            // Very large quads: subdivide if within 30 blocks
            needsSubdivision = dist < 30.0;
        } else if (maxDim >= 4) {
            // Medium quads: subdivide if within 15 blocks
            needsSubdivision = dist < 15.0;
        } else if (maxDim >= 2) {
            // Small quads: subdivide if within 8 blocks
            needsSubdivision = dist < 8.0;
        }

        if (needsSubdivision) {
            // Subdivide into smaller quads
            subdivideAndRenderQuad(quad, baseX, baseZ, fx, fy, fz, rx, rz, ux, uy, uz);
            return;
        }

        // Normal rendering for small quads or distant quads
        renderQuadDirect(quad, wx, wy, wz, fx, fy, fz, rx, rz, ux, uy, uz);
    }

    private void subdivideAndRenderQuad(GreedyMesher.Quad quad, int baseX, int baseZ,
                                        double fx, double fy, double fz,
                                        double rx, double rz, double ux, double uy, double uz) {
        quadsSubdivided++;

        // Determine subdivision level based on quad size
        int divisions = 2; // Default 2x2 subdivision
        int maxDim = Math.max(quad.w, quad.h);

        if (maxDim >= 8) {
            divisions = 4; // 4x4 for very large quads
        } else if (maxDim >= 4) {
            divisions = 2; // 2x2 for medium quads
        }

        int subW = Math.max(1, quad.w / divisions);
        int subH = Math.max(1, quad.h / divisions);

        for (int sx = 0; sx < quad.w; sx += subW) {
            for (int sy = 0; sy < quad.h; sy += subH) {
                int actualW = Math.min(subW, quad.w - sx);
                int actualH = Math.min(subH, quad.h - sy);

                // Calculate sub-quad position based on axis
                int subX, subY, subZ;
                if (quad.axis == 1) { // Y-axis (horizontal)
                    subX = quad.x + sx;
                    subY = quad.y;
                    subZ = quad.z + sy;
                } else if (quad.axis == 2) { // Z-axis (vertical north/south)
                    subX = quad.x + sx;
                    subY = quad.y + sy;
                    subZ = quad.z;
                } else { // X-axis (vertical west/east)
                    subX = quad.x;
                    subY = quad.y + sy;
                    subZ = quad.z + sx;
                }

                GreedyMesher.Quad subQuad = new GreedyMesher.Quad(
                        subX, subY, subZ,
                        actualW,
                        actualH,
                        quad.axis,
                        quad.dir,
                        quad.texIndex,
                        quad.brightness
                );

                double wx = baseX + subQuad.x;
                double wy = subQuad.y;
                double wz = baseZ + subQuad.z;

                renderQuadDirect(subQuad, wx, wy, wz, fx, fy, fz, rx, rz, ux, uy, uz);
            }
        }
    }

    private void renderQuadDirect(GreedyMesher.Quad quad, double wx, double wy, double wz,
                                  double fx, double fy, double fz,
                                  double rx, double rz, double ux, double uy, double uz) {

        double[][] corners = new double[4][3];

        if (quad.axis == 1) { // Y-axis
            double oy = wy + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{wx, oy, wz};
            corners[1] = new double[]{wx + quad.w, oy, wz};
            corners[2] = new double[]{wx + quad.w, oy, wz + quad.h};
            corners[3] = new double[]{wx, oy, wz + quad.h};
        } else if (quad.axis == 2) { // Z-axis
            double oz = wz + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{wx, wy, oz};
            corners[1] = new double[]{wx + quad.w, wy, oz};
            corners[2] = new double[]{wx + quad.w, wy + quad.h, oz};
            corners[3] = new double[]{wx, wy + quad.h, oz};
        } else { // X-axis
            double ox = wx + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{ox, wy, wz};
            corners[1] = new double[]{ox, wy, wz + quad.w};
            corners[2] = new double[]{ox, wy + quad.h, wz + quad.w};
            corners[3] = new double[]{ox, wy + quad.h, wz};
        }

        // Project corners
        double[] camZ = new double[4];
        int[] sx = new int[4];
        int[] sy = new int[4];

        // Check if any corners are behind camera
        int behindCount = 0;
        double minDepth = Double.POSITIVE_INFINITY;
        double maxDepth = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < 4; i++) {
            double dx = corners[i][0] - x;
            double dy = corners[i][1] - y;
            double dz = corners[i][2] - z;

            camZ[i] = dx * fx + dy * fy + dz * fz;
            if (camZ[i] <= 0.01) {
                behindCount++;
            } else {
                minDepth = Math.min(minDepth, camZ[i]);
                maxDepth = Math.max(maxDepth, camZ[i]);
            }
        }

        // Skip if entire quad is behind camera
        if (behindCount == 4) return;

        // Skip if depth variance is too extreme (prevents stretching artifacts)
        if (maxDepth > 0 && minDepth > 0 && maxDepth / minDepth > 100.0) return;

        // Project corners (clamp to near plane)
        for (int i = 0; i < 4; i++) {
            double dx = corners[i][0] - x;
            double dy = corners[i][1] - y;
            double dz = corners[i][2] - z;

            // Clamp depth to near plane
            double depth = Math.max(0.01, camZ[i]);

            double scale = invTanHalfFov / depth;
            sx[i] = (int) (halfWidth + (dx * rx + dz * rz) * scale * halfHeight);
            sy[i] = (int) (halfHeight - (dx * ux + dy * uy + dz * uz) * scale * halfHeight);
        }

        RenderFace face = facePool.poll();
        if (face == null) face = new RenderFace();

        System.arraycopy(sx, 0, face.x, 0, 4);
        System.arraycopy(sy, 0, face.y, 0, 4);
        System.arraycopy(camZ, 0, face.d, 0, 4);

        if (quad.axis == 1) {
            face.uv[0] = 0; face.uv[1] = 0;
            face.uv[2] = quad.w; face.uv[3] = 0;
            face.uv[4] = quad.w; face.uv[5] = quad.h;
            face.uv[6] = 0; face.uv[7] = quad.h;
        } else {
            face.uv[0] = 0; face.uv[1] = quad.h;
            face.uv[2] = quad.w; face.uv[3] = quad.h;
            face.uv[4] = quad.w; face.uv[5] = 0;
            face.uv[6] = 0; face.uv[7] = 0;
        }

        face.texIndex = quad.texIndex;
        face.brightness = quad.brightness;

        // Use minimum depth for sorting (helps with near clipping)
        minDepth = Math.min(Math.min(camZ[0], camZ[1]), Math.min(camZ[2], camZ[3]));
        face.avgDepth = Math.max(0.01, minDepth);

        renderFaces.add(face);
        quadsRendered++;
    }

    private void fillTexturedQuad(RenderFace face) {
        // Screen-space bounds check
        int minX = Math.min(Math.min(face.x[0], face.x[1]), Math.min(face.x[2], face.x[3]));
        int maxX = Math.max(Math.max(face.x[0], face.x[1]), Math.max(face.x[2], face.x[3]));
        int minY = Math.min(Math.min(face.y[0], face.y[1]), Math.min(face.y[2], face.y[3]));
        int maxY = Math.max(Math.max(face.y[0], face.y[1]), Math.max(face.y[2], face.y[3]));

        // Only skip if COMPLETELY off-screen with no overlap
        if (maxX < -width || minX > width * 2 || maxY < -height || minY > height * 2) {
            return;
        }

        // Skip only if truly absurd (prevents crashes, not visual issues)
        int faceWidth = maxX - minX;
        int faceHeight = maxY - minY;
        if (faceWidth > width * 20 || faceHeight > height * 20) {
            return;
        }

        // Clamp to screen bounds for rendering
        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);

        double invD0 = 1.0 / face.d[0], invD1 = 1.0 / face.d[1];
        double invD2 = 1.0 / face.d[2], invD3 = 1.0 / face.d[3];

        double uD0 = face.uv[0] * invD0, uD1 = face.uv[2] * invD1;
        double uD2 = face.uv[4] * invD2, uD3 = face.uv[6] * invD3;
        double vD0 = face.uv[1] * invD0, vD1 = face.uv[3] * invD1;
        double vD2 = face.uv[5] * invD2, vD3 = face.uv[7] * invD3;

        int brightnessInt = (int)(face.brightness * 256);

        for (int sy = minY; sy <= maxY; sy++) {
            double ex0 = Double.POSITIVE_INFINITY, ex1 = Double.NEGATIVE_INFINITY;
            double eInvD0 = 0, eInvD1 = 0, eUD0 = 0, eUD1 = 0, eVD0 = 0, eVD1 = 0;

            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                if ((face.y[i] <= sy && face.y[j] > sy) || (face.y[j] <= sy && face.y[i] > sy)) {
                    double t = (sy - face.y[i]) / (double) (face.y[j] - face.y[i]);
                    double ex = face.x[i] + t * (face.x[j] - face.x[i]);

                    double eInvD, eUD, eVD;
                    if (i == 0) {
                        eInvD = invD0 + t * (invD1 - invD0);
                        eUD = uD0 + t * (uD1 - uD0);
                        eVD = vD0 + t * (vD1 - vD0);
                    } else if (i == 1) {
                        eInvD = invD1 + t * (invD2 - invD1);
                        eUD = uD1 + t * (uD2 - uD1);
                        eVD = vD1 + t * (vD2 - vD1);
                    } else if (i == 2) {
                        eInvD = invD2 + t * (invD3 - invD2);
                        eUD = uD2 + t * (uD3 - uD2);
                        eVD = vD2 + t * (vD3 - vD2);
                    } else {
                        eInvD = invD3 + t * (invD0 - invD3);
                        eUD = uD3 + t * (uD0 - uD3);
                        eVD = vD3 + t * (vD0 - vD3);
                    }

                    if (ex < ex0) {
                        ex0 = ex; eInvD0 = eInvD; eUD0 = eUD; eVD0 = eVD;
                    }
                    if (ex > ex1) {
                        ex1 = ex; eInvD1 = eInvD; eUD1 = eUD; eVD1 = eVD;
                    }
                }
            }

            minX = Math.max(0, (int) ex0);
            maxX = Math.min(width - 1, (int) ex1);

            // Skip scanline if completely off-screen
            if (minX > maxX) continue;

            double spanWidth = ex1 - ex0;
            if (spanWidth < 0.001) continue;

            double invSpanWidth = 1.0 / spanWidth;
            int rowStart = sy * width;

            for (int sx = minX; sx <= maxX; sx++) {
                double t = (sx - ex0) * invSpanWidth;
                double invD = eInvD0 + t * (eInvD1 - eInvD0);
                double depth = 1.0 / invD;

                int idx = rowStart + sx;
                // Use small epsilon for depth comparison to reduce z-fighting
                if (depth < zBuffer[idx] + 0.001) {
                    zBuffer[idx] = depth;

                    double u = (eUD0 + t * (eUD1 - eUD0)) * depth;
                    double v = (eVD0 + t * (eVD1 - eVD0)) * depth;

                    int color = textureAtlas.sample(face.texIndex, u, v);

                    int r = (((color >> 16) & 0xFF) * brightnessInt) >> 8;
                    int g = (((color >> 8) & 0xFF) * brightnessInt) >> 8;
                    int b = ((color & 0xFF) * brightnessInt) >> 8;

                    pixels[idx] = (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    private void cleanOldMeshes() {
        long now = System.currentTimeMillis();
        meshCache.entrySet().removeIf(e -> now - e.getValue().lastUsed > 30000);
    }

    private static long chunkKey(int x, int z) {
        return ((long)x << 32) | (z & 0xFFFFFFFFL);
    }

    public int getChunksRendered() {

        return chunksRendered;
    }

    public int getChunksMeshing() {
        return chunksMeshing;
    }

    public int getCachedMeshCount() {
        return meshCache.size();
    }

    public int getQuadsSubdivided() {
        return quadsSubdivided;
    }

    public int getQuadsRendered() {
        return quadsRendered;
    }

    public int getQuadsCulled() {
        return quadsCulled;
    }

    public void shutdown() {
        mesher.shutdown();
    }
}