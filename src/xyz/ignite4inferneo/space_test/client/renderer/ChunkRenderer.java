package xyz.ignite4inferneo.space_test.client.renderer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;

public class ChunkRenderer {
    private static final int CHUNK_SIZE = 16;
    private static final int CHUNK_SHIFT = 4;
    private static final int RENDER_DISTANCE = 3; // Reduced for better FPS

    private final HashMap<Long, Chunk> chunks = new HashMap<>(); // Long key for speed

    private BufferedImage screenBuffer;
    private int[] pixels;
    private double[] zBuffer;
    private int width, height;

    // Camera
    public double x = 0, y = 10, z = 25;
    public double yaw = Math.PI;
    public double pitch = 0;

    private final Random rand = new Random(12345);

    // Textures
    private final int[][] texturePixels = new int[3][];
    private static final int TEXTURE_SIZE = 16;
    private static final int TEXTURE_SHIFT = 4;
    private static final int TEXTURE_MASK = 15;

    private double halfWidth, halfHeight;
    private double invTanHalfFov;

    // Pre-allocated arrays to avoid GC
    private final ArrayList<Face> faces = new ArrayList<>(2048);
    private final ArrayDeque<Face> facePool = new ArrayDeque<>(2048);

    private static class Chunk {
        final byte[][][] blocks = new byte[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        final int chunkX, chunkZ;

        Chunk(int cx, int cz) {
            chunkX = cx;
            chunkZ = cz;
        }
    }

    private static class Face {
        int[] x = new int[4];
        int[] y = new int[4];
        double[] d = new double[4];
        double[] uv = new double[8];
        int texIndex;
        float brightness;
        double avgDepth;
    }

    public ChunkRenderer() {
        for (int i = 0; i < 2048; i++) {
            facePool.add(new Face());
        }

        BufferedImage[] textures = new BufferedImage[3];
        textures[0] = TextureUtils.createBlockTexture("stone", TEXTURE_SIZE);
        textures[1] = TextureUtils.createBlockTexture("dirt", TEXTURE_SIZE);
        textures[2] = TextureUtils.createBlockTexture("grass", TEXTURE_SIZE);

        for (int i = 0; i < 3; i++) {
            texturePixels[i] = ((DataBufferInt) textures[i].getRaster().getDataBuffer()).getData();
        }

        generateChunksAroundCamera();
    }

    private static long chunkKey(int x, int z) {
        return ((long)x << 32) | (z & 0xFFFFFFFFL);
    }

    private void generateChunksAroundCamera() {
        int camChunkX = ((int) Math.floor(x)) >> CHUNK_SHIFT;
        int camChunkZ = ((int) Math.floor(z)) >> CHUNK_SHIFT;

        // Generate missing chunks
        for (int cx = camChunkX - RENDER_DISTANCE; cx <= camChunkX + RENDER_DISTANCE; cx++) {
            for (int cz = camChunkZ - RENDER_DISTANCE; cz <= camChunkZ + RENDER_DISTANCE; cz++) {
                long key = chunkKey(cx, cz);
                if (!chunks.containsKey(key)) {
                    chunks.put(key, generateChunk(cx, cz));
                }
            }
        }

        // Remove far chunks (but not every frame)
        if (rand.nextInt(60) == 0) { // Only every ~60 frames
            chunks.entrySet().removeIf(entry -> {
                Chunk c = entry.getValue();
                int dx = c.chunkX - camChunkX;
                int dz = c.chunkZ - camChunkZ;
                return Math.abs(dx) > RENDER_DISTANCE + 1 || Math.abs(dz) > RENDER_DISTANCE + 1;
            });
        }
    }

    private Chunk generateChunk(int cx, int cz) {
        Chunk chunk = new Chunk(cx, cz);
        int baseX = cx << CHUNK_SHIFT;
        int baseZ = cz << CHUNK_SHIFT;

        for (int bx = 0; bx < CHUNK_SIZE; bx++) {
            int worldX = baseX + bx;
            for (int bz = 0; bz < CHUNK_SIZE; bz++) {
                int worldZ = baseZ + bz;

                double noise = Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1);
                int groundHeight = 5 + (int)(noise * 3);

                for (int by = 0; by < groundHeight; by++) {
                    chunk.blocks[bx][by][bz] = (byte)((by < groundHeight - 2) ? 1 : 2);
                }
                if (groundHeight < CHUNK_SIZE && rand.nextFloat() < 0.3f) {
                    chunk.blocks[bx][groundHeight][bz] = 3;
                }
            }
        }

        return chunk;
    }

    public void setScreenSize(int w, int h) {
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

    public BufferedImage getScreen() {
        return screenBuffer;
    }

    public void move(double forward, double strafe, double vertical) {
        x += Math.sin(yaw) * forward;
        z += Math.cos(yaw) * forward;
        x += Math.cos(yaw) * strafe;
        z -= Math.sin(yaw) * strafe;
        y += vertical;

        generateChunksAroundCamera();
    }

    public void rotate(double deltaYaw, double deltaPitch) {
        yaw += deltaYaw;
        pitch = Math.max(-1.47, Math.min(1.47, pitch + deltaPitch));
    }

    public void render() {
        if (screenBuffer == null) return;

        // Fast clear
        Arrays.fill(pixels, 0x87CEEB);
        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);

        // Return faces to pool
        for (int i = 0; i < faces.size(); i++) {
            facePool.add(faces.get(i));
        }
        faces.clear();

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

        // Single-threaded chunk processing (faster for this workload)
        int camChunkX = ((int) Math.floor(x)) >> CHUNK_SHIFT;
        int camChunkZ = ((int) Math.floor(z)) >> CHUNK_SHIFT;

        // Render chunks in distance order (near to far for early rejection)
        for (int dist = 0; dist <= RENDER_DISTANCE; dist++) {
            for (int dx = -dist; dx <= dist; dx++) {
                for (int dz = -dist; dz <= dist; dz++) {
                    if (Math.abs(dx) != dist && Math.abs(dz) != dist) continue; // Only ring

                    long key = chunkKey(camChunkX + dx, camChunkZ + dz);
                    Chunk chunk = chunks.get(key);
                    if (chunk == null) continue;

                    // Quick frustum cull
                    double chunkCenterX = (chunk.chunkX << CHUNK_SHIFT) + 8;
                    double chunkCenterZ = (chunk.chunkZ << CHUNK_SHIFT) + 8;
                    double cdx = chunkCenterX - x;
                    double cdz = chunkCenterZ - z;

                    if (cdx * cdx + cdz * cdz > 3600) continue; // 60 block radius

                    renderChunk(chunk, fx, fy, fz, rx, rz, ux, uy, uz);
                }
            }
        }

        // Sort back to front
        faces.sort((a, b) -> Double.compare(b.avgDepth, a.avgDepth));

        // Render faces
        for (int i = 0; i < faces.size(); i++) {
            fillTexturedQuad(faces.get(i));
        }
    }

    private void renderChunk(Chunk chunk, double fx, double fy, double fz,
                             double rx, double rz, double ux, double uy, double uz) {

        int baseX = chunk.chunkX << CHUNK_SHIFT;
        int baseZ = chunk.chunkZ << CHUNK_SHIFT;

        for (int bx = 0; bx < CHUNK_SIZE; bx++) {
            for (int by = 0; by < CHUNK_SIZE; by++) {
                for (int bz = 0; bz < CHUNK_SIZE; bz++) {
                    byte block = chunk.blocks[bx][by][bz];
                    if (block == 0) continue;

                    // Inline face culling for speed
                    if ((by == 0 || chunk.blocks[bx][by-1][bz] == 0))
                        collectFace(chunk, bx, by, bz, 0, -1, 0, block, fx, fy, fz, rx, rz, ux, uy, uz, baseX, baseZ);
                    if ((by == CHUNK_SIZE-1 || chunk.blocks[bx][by+1][bz] == 0))
                        collectFace(chunk, bx, by, bz, 0, 1, 0, block, fx, fy, fz, rx, rz, ux, uy, uz, baseX, baseZ);
                    if ((bx == 0 || chunk.blocks[bx-1][by][bz] == 0))
                        collectFace(chunk, bx, by, bz, -1, 0, 0, block, fx, fy, fz, rx, rz, ux, uy, uz, baseX, baseZ);
                    if ((bx == CHUNK_SIZE-1 || chunk.blocks[bx+1][by][bz] == 0))
                        collectFace(chunk, bx, by, bz, 1, 0, 0, block, fx, fy, fz, rx, rz, ux, uy, uz, baseX, baseZ);
                    if ((bz == 0 || chunk.blocks[bx][by][bz-1] == 0))
                        collectFace(chunk, bx, by, bz, 0, 0, -1, block, fx, fy, fz, rx, rz, ux, uy, uz, baseX, baseZ);
                    if ((bz == CHUNK_SIZE-1 || chunk.blocks[bx][by][bz+1] == 0))
                        collectFace(chunk, bx, by, bz, 0, 0, 1, block, fx, fy, fz, rx, rz, ux, uy, uz, baseX, baseZ);
                }
            }
        }
    }

    private void collectFace(Chunk chunk, int bx, int by, int bz, int nx, int ny, int nz, byte blockType,
                             double fx, double fy, double fz, double rx, double rz,
                             double ux, double uy, double uz, int baseX, int baseZ) {

        double blockX = baseX + bx;
        double blockY = by;
        double blockZ = baseZ + bz;

        // Backface culling
        double fcx = blockX + 0.5 + nx * 0.5;
        double fcy = blockY + 0.5 + ny * 0.5;
        double fcz = blockZ + 0.5 + nz * 0.5;

        double tcx = fcx - x;
        double tcy = fcy - y;
        double tcz = fcz - z;

        if (tcx * nx + tcy * ny + tcz * nz >= 0) return;

        int texIndex = blockType - 1;
        float brightness = (ny == 1) ? 1.0f : ((ny == -1) ? 0.6f : 0.8f);

        // Setup corners
        double cx0, cy0, cz0, cx1, cy1, cz1, cx2, cy2, cz2, cx3, cy3, cz3;

        if (ny != 0) {
            double offsetY = blockY + (ny > 0 ? 1 : 0);
            cx0 = blockX;     cy0 = offsetY; cz0 = blockZ;
            cx1 = blockX + 1; cy1 = offsetY; cz1 = blockZ;
            cx2 = blockX + 1; cy2 = offsetY; cz2 = blockZ + 1;
            cx3 = blockX;     cy3 = offsetY; cz3 = blockZ + 1;
        } else if (nx != 0) {
            double offsetX = blockX + (nx > 0 ? 1 : 0);
            cx0 = offsetX; cy0 = blockY;     cz0 = blockZ;
            cx1 = offsetX; cy1 = blockY + 1; cz1 = blockZ;
            cx2 = offsetX; cy2 = blockY + 1; cz2 = blockZ + 1;
            cx3 = offsetX; cy3 = blockY;     cz3 = blockZ + 1;
        } else {
            double offsetZ = blockZ + (nz > 0 ? 1 : 0);
            cx0 = blockX;     cy0 = blockY;     cz0 = offsetZ;
            cx1 = blockX + 1; cy1 = blockY;     cz1 = offsetZ;
            cx2 = blockX + 1; cy2 = blockY + 1; cz2 = offsetZ;
            cx3 = blockX;     cy3 = blockY + 1; cz3 = offsetZ;
        }

        // Project all corners inline
        double dx0 = cx0 - x, dy0 = cy0 - y, dz0 = cz0 - z;
        double camZ0 = dx0 * fx + dy0 * fy + dz0 * fz;
        if (camZ0 <= 0.1) return;

        double dx1 = cx1 - x, dy1 = cy1 - y, dz1 = cz1 - z;
        double camZ1 = dx1 * fx + dy1 * fy + dz1 * fz;
        if (camZ1 <= 0.1) return;

        double dx2 = cx2 - x, dy2 = cy2 - y, dz2 = cz2 - z;
        double camZ2 = dx2 * fx + dy2 * fy + dz2 * fz;
        if (camZ2 <= 0.1) return;

        double dx3 = cx3 - x, dy3 = cy3 - y, dz3 = cz3 - z;
        double camZ3 = dx3 * fx + dy3 * fy + dz3 * fz;
        if (camZ3 <= 0.1) return;

        double scale0 = invTanHalfFov / camZ0;
        double scale1 = invTanHalfFov / camZ1;
        double scale2 = invTanHalfFov / camZ2;
        double scale3 = invTanHalfFov / camZ3;

        int sx0 = (int) (halfWidth + (dx0 * rx + dz0 * rz) * scale0 * halfHeight);
        int sy0 = (int) (halfHeight - (dx0 * ux + dy0 * uy + dz0 * uz) * scale0 * halfHeight);

        int sx1 = (int) (halfWidth + (dx1 * rx + dz1 * rz) * scale1 * halfHeight);
        int sy1 = (int) (halfHeight - (dx1 * ux + dy1 * uy + dz1 * uz) * scale1 * halfHeight);

        int sx2 = (int) (halfWidth + (dx2 * rx + dz2 * rz) * scale2 * halfHeight);
        int sy2 = (int) (halfHeight - (dx2 * ux + dy2 * uy + dz2 * uz) * scale2 * halfHeight);

        int sx3 = (int) (halfWidth + (dx3 * rx + dz3 * rz) * scale3 * halfHeight);
        int sy3 = (int) (halfHeight - (dx3 * ux + dy3 * uy + dz3 * uz) * scale3 * halfHeight);

        Face face = facePool.poll();
        if (face == null) face = new Face();

        face.x[0] = sx0; face.x[1] = sx1; face.x[2] = sx2; face.x[3] = sx3;
        face.y[0] = sy0; face.y[1] = sy1; face.y[2] = sy2; face.y[3] = sy3;
        face.d[0] = camZ0; face.d[1] = camZ1; face.d[2] = camZ2; face.d[3] = camZ3;

        // Set UVs based on face normal
        if (ny != 0) {
            face.uv[0] = 0; face.uv[1] = 0; face.uv[2] = 1; face.uv[3] = 0;
            face.uv[4] = 1; face.uv[5] = 1; face.uv[6] = 0; face.uv[7] = 1;
        } else if (nx != 0) {
            face.uv[0] = 0; face.uv[1] = 1; face.uv[2] = 0; face.uv[3] = 0;
            face.uv[4] = 1; face.uv[5] = 0; face.uv[6] = 1; face.uv[7] = 1;
        } else {
            face.uv[0] = 0; face.uv[1] = 1; face.uv[2] = 1; face.uv[3] = 1;
            face.uv[4] = 1; face.uv[5] = 0; face.uv[6] = 0; face.uv[7] = 0;
        }

        face.texIndex = texIndex;
        face.brightness = brightness;
        face.avgDepth = (camZ0 + camZ1 + camZ2 + camZ3) * 0.25;

        faces.add(face);
    }

    private void fillTexturedQuad(Face face) {
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

        int[] texPixels = texturePixels[face.texIndex];
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

                    int texX = ((int) (u * TEXTURE_SIZE)) & TEXTURE_MASK;
                    int texY = ((int) (v * TEXTURE_SIZE)) & TEXTURE_MASK;

                    int color = texPixels[(texY << TEXTURE_SHIFT) + texX];

                    int r = (((color >> 16) & 0xFF) * brightnessInt) >> 8;
                    int g = (((color >> 8) & 0xFF) * brightnessInt) >> 8;
                    int b = ((color & 0xFF) * brightnessInt) >> 8;

                    pixels[idx] = (r << 16) | (g << 8) | b;
                }
            }
        }
    }
}