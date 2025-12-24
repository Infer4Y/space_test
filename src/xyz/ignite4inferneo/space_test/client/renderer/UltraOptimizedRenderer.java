package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.world.Chunk;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIXED: Ultra-optimized renderer without clipping issues
 *
 * Fixes:
 * - Proper near plane handling
 * - Fixed depth buffer precision
 * - Removed aggressive subdivision (was causing artifacts)
 * - Better quad clipping
 * - Fixed texture coordinate interpolation
 */
public class UltraOptimizedRenderer {
    private static final int RENDER_DISTANCE = 8;
    private static final double NEAR_PLANE = 0.1;

    private final World world;
    private final TextureAtlas textureAtlas;
    private final ThreadedChunkMesher mesher;

    // Direct pixel buffer
    private int[] pixels;
    private double[] zBuffer; // Back to double for better precision
    protected int width;
    protected int height;

    // Camera
    public double x = 0, y = 70, z = 0;
    public double yaw = 0;
    public double pitch = 0;

    // Precomputed camera values
    private double halfWidth, halfHeight, invTanHalfFov;
    private double fx, fy, fz;
    private double rx, rz;
    private double ux, uy, uz;

    // Frustum planes
    private final Frustum frustum = new Frustum();

    // Mesh cache
    private final ConcurrentHashMap<Long, ChunkMesh> meshCache = new ConcurrentHashMap<>(256);
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> meshingInProgress = ConcurrentHashMap.newKeySet();

    // Face rendering
    private final FastFaceList renderFaces = new FastFaceList(8192);
    private final List<ChunkRenderTask> chunkTasks = new ArrayList<>(256);

    // Stats
    private int chunksRendered = 0;
    private int chunksMeshing = 0;
    private int quadsRendered = 0;
    private int quadsCulled = 0;
    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 30000;

    private static class ChunkMesh {
        List<GreedyMesher.Quad> quads;
        long lastUsed;

        ChunkMesh(List<GreedyMesher.Quad> quads) {
            this.quads = quads;
            this.lastUsed = System.currentTimeMillis();
        }
    }

    private static class Frustum {
        private final double[][] planes = new double[6][4];

        void update(double x, double y, double z,
                    double fx, double fy, double fz,
                    double rx, double rz,
                    double ux, double uy, double uz,
                    double fov, double aspect) {

            double nearDist = NEAR_PLANE;
            double farDist = RENDER_DISTANCE * 16.0 + 16.0;

            double tanHalfFov = Math.tan(fov * 0.5);
            double halfVSide = tanHalfFov * nearDist;
            double halfHSide = halfVSide * aspect;

            // Near plane
            planes[0][0] = fx;
            planes[0][1] = fy;
            planes[0][2] = fz;
            planes[0][3] = -(fx * x + fy * y + fz * z + nearDist);

            // Far plane
            planes[1][0] = -fx;
            planes[1][1] = -fy;
            planes[1][2] = -fz;
            planes[1][3] = fx * x + fy * y + fz * z + farDist;

            // Left, right, top, bottom planes
            double lnx = ux * halfVSide + rx * halfHSide;
            double lny = uy * halfVSide;
            double lnz = uz * halfVSide + rz * halfHSide;
            double len = Math.sqrt(lnx*lnx + lny*lny + lnz*lnz);
            planes[2][0] = lnx / len;
            planes[2][1] = lny / len;
            planes[2][2] = lnz / len;
            planes[2][3] = -(planes[2][0] * x + planes[2][1] * y + planes[2][2] * z);

            double rnx = ux * halfVSide - rx * halfHSide;
            double rny = uy * halfVSide;
            double rnz = uz * halfVSide - rz * halfHSide;
            len = Math.sqrt(rnx*rnx + rny*rny + rnz*rnz);
            planes[3][0] = rnx / len;
            planes[3][1] = rny / len;
            planes[3][2] = rnz / len;
            planes[3][3] = -(planes[3][0] * x + planes[3][1] * y + planes[3][2] * z);

            double tnx = -ux * nearDist + fx * halfVSide;
            double tny = -uy * nearDist + fy * halfVSide;
            double tnz = -uz * nearDist + fz * halfVSide;
            len = Math.sqrt(tnx*tnx + tny*tny + tnz*tnz);
            planes[4][0] = tnx / len;
            planes[4][1] = tny / len;
            planes[4][2] = tnz / len;
            planes[4][3] = -(planes[4][0] * x + planes[4][1] * y + planes[4][2] * z);

            double bnx = ux * nearDist + fx * halfVSide;
            double bny = uy * nearDist + fy * halfVSide;
            double bnz = uz * nearDist + fz * halfVSide;
            len = Math.sqrt(bnx*bnx + bny*bny + bnz*bnz);
            planes[5][0] = bnx / len;
            planes[5][1] = bny / len;
            planes[5][2] = bnz / len;
            planes[5][3] = -(planes[5][0] * x + planes[5][1] * y + planes[5][2] * z);
        }

        boolean isChunkVisible(int chunkX, int chunkZ) {
            double minX = chunkX * 16.0;
            double maxX = minX + 16.0;
            double minY = 0;
            double maxY = 256;
            double minZ = chunkZ * 16.0;
            double maxZ = minZ + 16.0;

            for (int i = 0; i < 6; i++) {
                double px = planes[i][0] > 0 ? maxX : minX;
                double py = planes[i][1] > 0 ? maxY : minY;
                double pz = planes[i][2] > 0 ? maxZ : minZ;

                double dist = planes[i][0] * px + planes[i][1] * py + planes[i][2] * pz + planes[i][3];
                if (dist < 0) return false;
            }
            return true;
        }
    }

    private static class FastFaceList {
        private static class Face {
            int[] x = new int[4];
            int[] y = new int[4];
            double[] d = new double[4];
            double[] uv = new double[8];
            int texIndex;
            float brightness;
            double avgDepth;
        }

        final Face[] faces;
        private int count = 0;

        FastFaceList(int capacity) {
            faces = new Face[capacity];
            for (int i = 0; i < capacity; i++) {
                faces[i] = new Face();
            }
        }

        Face add() {
            if (count >= faces.length) return null;
            return faces[count++];
        }

        void clear() {
            count = 0;
        }

        Face get(int i) {
            return faces[i];
        }

        int size() {
            return count;
        }
    }

    private static class ChunkRenderTask implements Comparable<ChunkRenderTask> {
        int chunkX, chunkZ;
        long key;
        double weightedDistance;

        void set(int x, int z, long k, double dist) {
            this.chunkX = x;
            this.chunkZ = z;
            this.key = k;
            this.weightedDistance = dist;
        }

        @Override
        public int compareTo(ChunkRenderTask o) {
            return Double.compare(this.weightedDistance, o.weightedDistance);
        }
    }

    public UltraOptimizedRenderer(World world) {
        this(world, Runtime.getRuntime().availableProcessors());
    }

    public UltraOptimizedRenderer(World world, int threadCount) {
        this.world = world;
        this.textureAtlas = new TextureAtlas();
        this.mesher = new ThreadedChunkMesher(threadCount);

        System.out.println("[UltraOptimizedRenderer] Initialized with " + threadCount + " threads");
    }

    public void setCanvasSize(Dimension dimension) {
        setCanvasSize(dimension.width, dimension.height);
    }

    public void setCanvasSize(int w, int h) {
        if (width == w && height == h && pixels != null) return;

        width = w;
        height = h;
        halfWidth = w * 0.5;
        halfHeight = h * 0.5;

        double fov = Math.PI / 3.0;
        invTanHalfFov = 1.0 / Math.tan(fov * 0.5);

        pixels = new int[w * h];
        zBuffer = new double[w * h];
    }

    public int[] getPixels() {
        return pixels;
    }

    public void markChunkDirty(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        dirtyChunks.add(key);
    }

    public void preloadChunksAround(double x, double z, int radius) {
        int centerChunkX = (int) Math.floor(x) >> 4;
        int centerChunkZ = (int) Math.floor(z) >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                long key = chunkKey(chunkX, chunkZ);

                Chunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk != null && !meshingInProgress.contains(key)) {
                    startMeshing(chunk, chunkX, chunkZ, key);
                }
            }
        }
    }

    public void render() {
        if (pixels == null) return;

        // Clear buffers
        Arrays.fill(pixels, 0x87CEEB);
        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);

        renderFaces.clear();
        quadsRendered = 0;
        quadsCulled = 0;

        // Update camera
        updateCameraVectors();

        double fov = Math.PI / 3.0;
        double aspect = (double) width / height;
        frustum.update(x, y, z, fx, fy, fz, rx, rz, ux, uy, uz, fov, aspect);

        int camChunkX = (int) Math.floor(x) >> 4;
        int camChunkZ = (int) Math.floor(z) >> 4;

        processDirtyChunks();
        collectAndSortChunks(camChunkX, camChunkZ);

        chunksRendered = 0;
        chunksMeshing = meshingInProgress.size();

        for (int i = 0; i < chunkTasks.size(); i++) {
            ChunkRenderTask task = chunkTasks.get(i);
            if (renderChunk(task.chunkX, task.chunkZ, task.key)) {
                chunksRendered++;
            }
        }

        // Sort faces back to front
        sortFaces();

        // Render all faces
        for (int i = 0; i < renderFaces.size(); i++) {
            fillTexturedQuad(renderFaces.get(i));
        }

        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupOldMeshes();
            lastCleanupTime = now;
        }
    }

    private void updateCameraVectors() {
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        fx = sinYaw * cosPitch;
        fy = -sinPitch;
        fz = cosYaw * cosPitch;
        rx = cosYaw;
        rz = -sinYaw;
        ux = sinYaw * sinPitch;
        uy = cosPitch;
        uz = cosYaw * sinPitch;
    }

    private void collectAndSortChunks(int camChunkX, int camChunkZ) {
        chunkTasks.clear();

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue;

                int chunkX = camChunkX + dx;
                int chunkZ = camChunkZ + dz;

                if (!frustum.isChunkVisible(chunkX, chunkZ)) continue;

                long key = chunkKey(chunkX, chunkZ);

                double centerX = (chunkX * 16.0 + 8.0) - x;
                double centerZ = (chunkZ * 16.0 + 8.0) - z;
                double dist = Math.sqrt(centerX * centerX + centerZ * centerZ);

                double dot = (centerX * fx + centerZ * fz) / (dist + 0.001);
                double weightedDist = dist * (1.5 - dot * 0.5);

                ChunkRenderTask task = new ChunkRenderTask();
                task.set(chunkX, chunkZ, key, weightedDist);
                chunkTasks.add(task);

                if (!meshCache.containsKey(key) && !meshingInProgress.contains(key)) {
                    Chunk chunk = world.getChunk(chunkX, chunkZ);
                    if (chunk != null) {
                        startMeshing(chunk, chunkX, chunkZ, key);
                    }
                }
            }
        }

        Collections.sort(chunkTasks);
    }

    private void processDirtyChunks() {
        if (dirtyChunks.isEmpty()) return;

        Iterator<Long> it = dirtyChunks.iterator();
        int processed = 0;
        while (it.hasNext() && processed < 16) {
            Long key = it.next();
            int chunkX = (int)(key >> 32);
            int chunkZ = (int)(key & 0xFFFFFFFFL);

            Chunk chunk = world.getChunk(chunkX, chunkZ);
            if (chunk != null && !meshingInProgress.contains(key)) {
                startMeshing(chunk, chunkX, chunkZ, key);
                it.remove();
                processed++;
            }
        }
    }

    private void startMeshing(Chunk chunk, int chunkX, int chunkZ, long key) {
        meshingInProgress.add(key);

        CompletableFuture<ThreadedChunkMesher.MeshResult> future =
                mesher.submitChunk(chunk, chunkX, chunkZ, key);

        future.thenAccept(result -> {
            ChunkMesh mesh = new ChunkMesh(result.quads);
            meshCache.put(result.key, mesh);
            chunk.clearDirty();
            meshingInProgress.remove(result.key);
            dirtyChunks.remove(result.key);
        });
    }

    private boolean renderChunk(int chunkX, int chunkZ, long key) {
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null) return false;

        ChunkMesh mesh = meshCache.get(key);
        if (mesh == null) return false;

        mesh.lastUsed = System.currentTimeMillis();

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (GreedyMesher.Quad quad : mesh.quads) {
            renderQuad(quad, baseX, baseZ);
        }

        return true;
    }

    /**
     * FIXED: Render quad without subdivision artifacts
     */
    private void renderQuad(GreedyMesher.Quad quad, int baseX, int baseZ) {
        double wx = baseX + quad.x;
        double wy = quad.y;
        double wz = baseZ + quad.z;

        // Calculate corners
        double[][] corners = new double[4][3];

        if (quad.axis == 1) {
            double oy = wy + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{wx,          oy, wz};
            corners[1] = new double[]{wx + quad.w, oy, wz};
            corners[2] = new double[]{wx + quad.w, oy, wz + quad.h};
            corners[3] = new double[]{wx,          oy, wz + quad.h};
        } else if (quad.axis == 2) {
            double oz = wz + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{wx,          wy,          oz};
            corners[1] = new double[]{wx + quad.w, wy,          oz};
            corners[2] = new double[]{wx + quad.w, wy + quad.h, oz};
            corners[3] = new double[]{wx,          wy + quad.h, oz};
        } else {
            double ox = wx + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{ox, wy,          wz};
            corners[1] = new double[]{ox, wy,          wz + quad.w};
            corners[2] = new double[]{ox, wy + quad.h, wz + quad.w};
            corners[3] = new double[]{ox, wy + quad.h, wz};
        }

        // Project corners
        double[] camZ = new double[4];
        int[] sx = new int[4];
        int[] sy = new int[4];

        int behindCount = 0;
        for (int i = 0; i < 4; i++) {
            double dx = corners[i][0] - x;
            double dy = corners[i][1] - y;
            double dz = corners[i][2] - z;

            camZ[i] = dx * fx + dy * fy + dz * fz;

            if (camZ[i] < NEAR_PLANE) {
                behindCount++;
                camZ[i] = NEAR_PLANE; // Clamp to near plane
            }
        }

        // Skip if entirely behind camera
        if (behindCount == 4) {
            quadsCulled++;
            return;
        }

        // Project to screen
        for (int i = 0; i < 4; i++) {
            double dx = corners[i][0] - x;
            double dy = corners[i][1] - y;
            double dz = corners[i][2] - z;

            double scale = invTanHalfFov / camZ[i];
            sx[i] = (int)(halfWidth + (dx * rx + dz * rz) * scale * halfHeight);
            sy[i] = (int)(halfHeight - (dx * ux + dy * uy + dz * uz) * scale * halfHeight);
        }

        FastFaceList.Face face = renderFaces.add();
        if (face == null) return;

        System.arraycopy(sx, 0, face.x, 0, 4);
        System.arraycopy(sy, 0, face.y, 0, 4);
        System.arraycopy(camZ, 0, face.d, 0, 4);

        // Set UVs
        if (quad.axis == 1) {
            face.uv[0] = 0;      face.uv[1] = 0;
            face.uv[2] = quad.w; face.uv[3] = 0;
            face.uv[4] = quad.w; face.uv[5] = quad.h;
            face.uv[6] = 0;      face.uv[7] = quad.h;
        } else {
            face.uv[0] = 0;      face.uv[1] = quad.h;
            face.uv[2] = quad.w; face.uv[3] = quad.h;
            face.uv[4] = quad.w; face.uv[5] = 0;
            face.uv[6] = 0;      face.uv[7] = 0;
        }

        face.texIndex = quad.texIndex;
        face.brightness = quad.brightness;
        face.avgDepth = (camZ[0] + camZ[1] + camZ[2] + camZ[3]) * 0.25;

        quadsRendered++;
    }

    private void sortFaces() {
        int n = renderFaces.size();
        for (int i = 1; i < n; i++) {
            FastFaceList.Face key = renderFaces.get(i);
            double keyDepth = key.avgDepth;
            int j = i - 1;

            while (j >= 0 && renderFaces.get(j).avgDepth < keyDepth) {
                j--;
            }

            if (j + 1 != i) {
                FastFaceList.Face temp = renderFaces.faces[i];
                System.arraycopy(renderFaces.faces, j + 1, renderFaces.faces, j + 2, i - j - 1);
                renderFaces.faces[j + 1] = temp;
            }
        }
    }

    /**
     * FIXED: Proper texture interpolation
     */
    private void fillTexturedQuad(FastFaceList.Face face) {
        int minY = Math.min(Math.min(face.y[0], face.y[1]), Math.min(face.y[2], face.y[3]));
        int maxY = Math.max(Math.max(face.y[0], face.y[1]), Math.max(face.y[2], face.y[3]));

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
                    double t = (sy - face.y[i]) / (double)(face.y[j] - face.y[i]);
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

            int minX = Math.max(0, (int)ex0);
            int maxX = Math.min(width - 1, (int)ex1);

            double spanWidth = ex1 - ex0;
            if (spanWidth < 0.001) continue;

            double invSpanWidth = 1.0 / spanWidth;
            int rowStart = sy * width;

            for (int sx = minX; sx <= maxX; sx++) {
                double t = (sx - ex0) * invSpanWidth;
                double invD = eInvD0 + t * (eInvD1 - eInvD0);
                double depth = 1.0 / invD;

                int idx = rowStart + sx;
                if (depth < zBuffer[idx]) {
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

    private void cleanupOldMeshes() {
        if (meshCache.size() < 300) return;

        long now = System.currentTimeMillis();
        int removed = 0;

        Iterator<Map.Entry<Long, ChunkMesh>> it = meshCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, ChunkMesh> entry = it.next();
            if (now - entry.getValue().lastUsed > 60000) {
                it.remove();
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("[UltraOptimizedRenderer] Cleaned up " + removed + " old meshes");
        }
    }

    private static long chunkKey(int x, int z) {
        return ((long)x << 32) | (z & 0xFFFFFFFFL);
    }

    public int getChunksRendered() { return chunksRendered; }
    public int getChunksMeshing() { return chunksMeshing; }
    public int getCachedMeshCount() { return meshCache.size(); }
    public int getQuadsRendered() { return quadsRendered; }
    public int getQuadsCulled() { return quadsCulled; }

    public void shutdown() {
        mesher.shutdown();
    }
}