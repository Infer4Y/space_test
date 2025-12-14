package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.world.Chunk;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized renderer - COMPLETE FIXED VERSION
 */
public class OptimizedRenderer {
    private static final int RENDER_DISTANCE = 8;

    private final World world;
    private BufferedImage screenBuffer;
    private int[] pixels;
    private double[] zBuffer;
    private int width, height;

    // Camera
    public double x = 0, y = 70, z = 0;
    public double yaw = 0;
    public double pitch = 0;

    // Texture atlas
    private final TextureAtlas textureAtlas;

    // Mesh cache
    private final Map<Long, ChunkMesh> meshCache = new ConcurrentHashMap<>();
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();

    // Pre-computed values
    private double halfWidth, halfHeight, invTanHalfFov;

    // Face pool for rendering
    private final ArrayList<RenderFace> renderFaces = new ArrayList<>(4096);
    private final ArrayDeque<RenderFace> facePool = new ArrayDeque<>(4096);

    private static class ChunkMesh {
        List<GreedyMesher.Quad> quads;
        long lastUsed;

        ChunkMesh(List<GreedyMesher.Quad> quads) {
            this.quads = quads;
            this.lastUsed = System.currentTimeMillis();
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

    public OptimizedRenderer(World world) {
        this.world = world;
        this.textureAtlas = new TextureAtlas();

        for (int i = 0; i < 4096; i++) {
            facePool.add(new RenderFace());
        }
    }

    public void setCanvasSize(Dimension dimension){
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

    public void move(double forward, double strafe, double vertical) {
        x += Math.sin(yaw) * forward;
        z += Math.cos(yaw) * forward;
        x += Math.cos(yaw) * strafe;
        z -= Math.sin(yaw) * strafe;
        y += vertical;
    }

    public void rotate(double deltaYaw, double deltaPitch) {
        yaw += deltaYaw;
        pitch = Math.max(-1.5, Math.min(1.5, pitch + deltaPitch));
    }

    public double[] getCameraPosition() {
        return new double[]{x, y, z};
    }

    public double[] getCameraDirection() {
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        return new double[]{
                sinYaw * cosPitch,
                -sinPitch,
                cosYaw * cosPitch
        };
    }

    public void markChunkDirty(int chunkX, int chunkZ) {
        dirtyChunks.add(chunkKey(chunkX, chunkZ));
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

        // Update dirty chunks
        for (Long key : dirtyChunks) {
            meshCache.remove(key);
        }
        dirtyChunks.clear();

        // Render all chunks in range (no frustum culling for now)
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = camChunkX + dx;
                int chunkZ = camChunkZ + dz;

                // Simple distance check
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue;

                renderChunk(chunkX, chunkZ, fx, fy, fz, rx, rz, ux, uy, uz);
            }
        }

        // Sort faces back to front
        renderFaces.sort((a, b) -> Double.compare(b.avgDepth, a.avgDepth));

        // Render all faces
        for (RenderFace face : renderFaces) {
            fillTexturedQuad(face);
        }

        // Clean old meshes
        if (meshCache.size() > 200) {
            long now = System.currentTimeMillis();
            meshCache.entrySet().removeIf(e -> now - e.getValue().lastUsed > 30000);
        }
    }

    private void renderChunk(int chunkX, int chunkZ, double fx, double fy, double fz,
                             double rx, double rz, double ux, double uy, double uz) {

        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null) return;

        long key = chunkKey(chunkX, chunkZ);
        ChunkMesh mesh = meshCache.get(key);

        if (mesh == null || chunk.isDirty()) {
            mesh = new ChunkMesh(GreedyMesher.mesh(chunk));
            meshCache.put(key, mesh);
            chunk.clearDirty();
        }

        mesh.lastUsed = System.currentTimeMillis();

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (GreedyMesher.Quad quad : mesh.quads) {
            renderQuad(quad, baseX, baseZ, fx, fy, fz, rx, rz, ux, uy, uz);
        }
    }

    // Replace renderQuad in OptimizedRenderer.java - fix UV coordinates for Y-axis

    private void renderQuad(GreedyMesher.Quad quad, int baseX, int baseZ,
                            double fx, double fy, double fz,
                            double rx, double rz, double ux, double uy, double uz) {

        double wx = baseX + quad.x;
        double wy = quad.y;
        double wz = baseZ + quad.z;

        double[][] corners = new double[4][3];

        if (quad.axis == 1) { // Y-axis (horizontal faces - top/bottom)
            double oy = wy + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{wx,          oy, wz};
            corners[1] = new double[]{wx + quad.w, oy, wz};
            corners[2] = new double[]{wx + quad.w, oy, wz + quad.h};
            corners[3] = new double[]{wx,          oy, wz + quad.h};

        } else if (quad.axis == 2) { // Z-axis (vertical faces - north/south)
            double oz = wz + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{wx,          wy,          oz};
            corners[1] = new double[]{wx + quad.w, wy,          oz};
            corners[2] = new double[]{wx + quad.w, wy + quad.h, oz};
            corners[3] = new double[]{wx,          wy + quad.h, oz};

        } else { // X-axis (vertical faces - west/east)
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

        for (int i = 0; i < 4; i++) {
            double dx = corners[i][0] - x;
            double dy = corners[i][1] - y;
            double dz = corners[i][2] - z;

            camZ[i] = dx * fx + dy * fy + dz * fz;
            if (camZ[i] <= 0.1) return;

            double scale = invTanHalfFov / camZ[i];
            sx[i] = (int) (halfWidth + (dx * rx + dz * rz) * scale * halfHeight);
            sy[i] = (int) (halfHeight - (dx * ux + dy * uy + dz * uz) * scale * halfHeight);
        }

        RenderFace face = facePool.poll();
        if (face == null) face = new RenderFace();

        System.arraycopy(sx, 0, face.x, 0, 4);
        System.arraycopy(sy, 0, face.y, 0, 4);
        System.arraycopy(camZ, 0, face.d, 0, 4);

        // FIXED: Different UV layout for Y-axis vs X/Z-axis
        if (quad.axis == 1) {
            // Y-axis (horizontal) - UVs are fine as-is
            face.uv[0] = 0;      face.uv[1] = 0;
            face.uv[2] = quad.w; face.uv[3] = 0;
            face.uv[4] = quad.w; face.uv[5] = quad.h;
            face.uv[6] = 0;      face.uv[7] = quad.h;
        } else {
            // X-axis and Z-axis (vertical) - flip V coordinate to fix upside-down textures
            face.uv[0] = 0;      face.uv[1] = quad.h;  // V flipped
            face.uv[2] = quad.w; face.uv[3] = quad.h;  // V flipped
            face.uv[4] = quad.w; face.uv[5] = 0;       // V flipped
            face.uv[6] = 0;      face.uv[7] = 0;       // V flipped
        }

        face.texIndex = quad.texIndex;
        face.brightness = quad.brightness;
        face.avgDepth = (camZ[0] + camZ[1] + camZ[2] + camZ[3]) * 0.25;

        renderFaces.add(face);
    }

    private void fillTexturedQuad(RenderFace face) {
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

            int minX = Math.max(0, (int) ex0);
            int maxX = Math.min(width - 1, (int) ex1);

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

    private static long chunkKey(int x, int z) {
        return ((long)x << 32) | (z & 0xFFFFFFFFL);
    }
}