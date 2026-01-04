package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.entity.*;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;
import xyz.ignite4inferneo.space_test.common.world.Chunk;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REBUILT: Perspective-correct texture mapper with proper depth interpolation
 *
 * Key improvements:
 * - True perspective-correct UV interpolation (no warping)
 * - Efficient scanline rasterization
 * - Proper depth testing
 * - Multi-threaded chunk meshing
 */
public class PerspectiveCorrectRenderer {
    private static final int RENDER_DISTANCE = 8;
    private static final double NEAR_PLANE = 0.1;

    private final World world;
    private final TextureAtlas textureAtlas;
    private final ThreadedChunkMesher mesher;

    private int[] pixels;
    private double[] zBuffer;
    protected int width;
    protected int height;

    public double x = 0, y = 70, z = 0;
    public double yaw = 0, pitch = 0;

    private double halfWidth, halfHeight, invTanHalfFov;
    private double fx, fy, fz, rx, rz, ux, uy, uz;

    private final Frustum frustum = new Frustum();
    private final ConcurrentHashMap<Long, ChunkMesh> meshCache = new ConcurrentHashMap<>(256);
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> meshingInProgress = ConcurrentHashMap.newKeySet();

    private final List<Face> renderFaces = new ArrayList<>(8192);
    private final List<ChunkRenderTask> chunkTasks = new ArrayList<>(256);
    private final List<EntitySprite> entitySprites = new ArrayList<>(256);

    private int chunksRendered = 0, chunksMeshing = 0;
    private int quadsRendered = 0, quadsCulled = 0;
    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 30000;

    /**
     * Face with perspective-correct vertex data
     */
    private static class Face {
        // Screen coordinates
        int[] x = new int[4];
        int[] y = new int[4];

        // Perspective-correct interpolation values (u/z, v/z, 1/z)
        double[] uOverZ = new double[4];
        double[] vOverZ = new double[4];
        double[] oneOverZ = new double[4];

        int texIndex;
        float brightness;
        double avgDepth;
    }

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

        void update(double x, double y, double z, double fx, double fy, double fz,
                    double rx, double rz, double ux, double uy, double uz, double fov, double aspect) {
            double nearDist = NEAR_PLANE;
            double farDist = RENDER_DISTANCE * 16.0 + 16.0;
            double tanHalfFov = Math.tan(fov * 0.5);
            double halfVSide = tanHalfFov * nearDist;
            double halfHSide = halfVSide * aspect;

            planes[0][0] = fx; planes[0][1] = fy; planes[0][2] = fz;
            planes[0][3] = -(fx * x + fy * y + fz * z + nearDist);
            planes[1][0] = -fx; planes[1][1] = -fy; planes[1][2] = -fz;
            planes[1][3] = fx * x + fy * y + fz * z + farDist;

            double lnx = ux * halfVSide + rx * halfHSide;
            double lny = uy * halfVSide;
            double lnz = uz * halfVSide + rz * halfHSide;
            double len = Math.sqrt(lnx*lnx + lny*lny + lnz*lnz);
            planes[2][0] = lnx / len; planes[2][1] = lny / len; planes[2][2] = lnz / len;
            planes[2][3] = -(planes[2][0] * x + planes[2][1] * y + planes[2][2] * z);

            double rnx = ux * halfVSide - rx * halfHSide;
            double rny = uy * halfVSide;
            double rnz = uz * halfVSide - rz * halfHSide;
            len = Math.sqrt(rnx*rnx + rny*rny + rnz*rnz);
            planes[3][0] = rnx / len; planes[3][1] = rny / len; planes[3][2] = rnz / len;
            planes[3][3] = -(planes[3][0] * x + planes[3][1] * y + planes[3][2] * z);

            double tnx = -ux * nearDist + fx * halfVSide;
            double tny = -uy * nearDist + fy * halfVSide;
            double tnz = -uz * nearDist + fz * halfVSide;
            len = Math.sqrt(tnx*tnx + tny*tny + tnz*tnz);
            planes[4][0] = tnx / len; planes[4][1] = tny / len; planes[4][2] = tnz / len;
            planes[4][3] = -(planes[4][0] * x + planes[4][1] * y + planes[4][2] * z);

            double bnx = ux * nearDist + fx * halfVSide;
            double bny = uy * nearDist + fy * halfVSide;
            double bnz = uz * nearDist + fz * halfVSide;
            len = Math.sqrt(bnx*bnx + bny*bny + bnz*bnz);
            planes[5][0] = bnx / len; planes[5][1] = bny / len; planes[5][2] = bnz / len;
            planes[5][3] = -(planes[5][0] * x + planes[5][1] * y + planes[5][2] * z);
        }

        boolean isChunkVisible(int chunkX, int chunkZ) {
            double minX = chunkX * 16.0, maxX = minX + 16.0;
            double minY = 0, maxY = 256;
            double minZ = chunkZ * 16.0, maxZ = minZ + 16.0;

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

    private static class EntitySprite {
        int screenX, screenY, width, height;
        double depth;
        java.awt.image.BufferedImage texture;
        Entity entity;
        float healthPercent;
        boolean showHealth;

        EntitySprite(int x, int y, int w, int h, double d, java.awt.image.BufferedImage tex, Entity e) {
            this.screenX = x; this.screenY = y;
            this.width = w; this.height = h;
            this.depth = d; this.texture = tex; this.entity = e;
            this.showHealth = false; this.healthPercent = 1.0f;
            if (e instanceof LivingEntity living) {
                this.healthPercent = living.getHealth() / living.getMaxHealth();
                this.showHealth = (e instanceof PlayerEntity) ||
                        (healthPercent < 1.0f && !(e instanceof ItemEntity));
            }
        }
    }

    private static class ChunkRenderTask implements Comparable<ChunkRenderTask> {
        int chunkX, chunkZ;
        long key;
        double weightedDistance;
        void set(int x, int z, long k, double dist) {
            this.chunkX = x; this.chunkZ = z; this.key = k; this.weightedDistance = dist;
        }
        @Override
        public int compareTo(ChunkRenderTask o) {
            return Double.compare(this.weightedDistance, o.weightedDistance);
        }
    }

    public PerspectiveCorrectRenderer(World world) {
        this(world, Runtime.getRuntime().availableProcessors());
    }

    public PerspectiveCorrectRenderer(World world, int threadCount) {
        this.world = world;
        this.textureAtlas = new TextureAtlas();
        this.mesher = new ThreadedChunkMesher(threadCount);
        System.out.println("[PerspectiveCorrectRenderer] Initialized with " + threadCount + " threads");
    }

    public TextureAtlas getTextureAtlas() { return textureAtlas; }

    public void setCanvasSize(java.awt.Dimension dimension) {
        setCanvasSize(dimension.width, dimension.height);
    }

    public void setCanvasSize(int w, int h) {
        if (width == w && height == h && pixels != null) return;
        width = w; height = h;
        halfWidth = w * 0.5; halfHeight = h * 0.5;
        double fov = Math.PI / 3.0;
        invTanHalfFov = 1.0 / Math.tan(fov * 0.5);
        pixels = new int[w * h];
        zBuffer = new double[w * h];
    }

    public int[] getPixels() { return pixels; }

    public void markChunkDirty(int chunkX, int chunkZ) {
        dirtyChunks.add(chunkKey(chunkX, chunkZ));
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

        Arrays.fill(pixels, 0xFF_6080A0); // Deep space sky gradient feel
        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);

        renderFaces.clear();
        entitySprites.clear();
        quadsRendered = 0;
        quadsCulled = 0;

        updateCameraVectors();
        double fov = Math.PI / 3.0;
        double aspect = (double) width / height;
        frustum.update(x, y, z, fx, fy, fz, rx, rz, ux, uy, uz, fov, aspect);

        int camChunkX = (int) Math.floor(x / 16.0);
        int camChunkZ = (int) Math.floor(z / 16.0);

        processDirtyChunks();
        collectAndSortChunks(camChunkX, camChunkZ);

        chunksRendered = 0;
        chunksMeshing = meshingInProgress.size();

        for (ChunkRenderTask task : chunkTasks) {
            if (renderChunk(task.chunkX, task.chunkZ, task.key)) {
                chunksRendered++;
            }
        }

        collectEntitySprites();

        // Sort faces back-to-front for correct transparency/overlaps (painter's algorithm)
        sortFaces();

        for (Face face : renderFaces) {
            renderPerspectiveCorrectQuad(face);
        }

        renderEntitySprites();

        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupOldMeshes();
            lastCleanupTime = now;
        }
    }

    private void updateCameraVectors() {
        double cosPitch = Math.cos(pitch), sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw), sinYaw = Math.sin(yaw);
        fx = sinYaw * cosPitch; fy = -sinPitch; fz = cosYaw * cosPitch;
        rx = cosYaw; rz = -sinYaw;
        ux = sinYaw * sinPitch; uy = cosPitch; uz = cosYaw * sinPitch;
    }

    private void collectAndSortChunks(int camChunkX, int camChunkZ) {
        chunkTasks.clear();
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue;
                int chunkX = camChunkX + dx, chunkZ = camChunkZ + dz;
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
                    if (chunk != null) startMeshing(chunk, chunkX, chunkZ, key);
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
            int chunkX = (int)(key >> 32), chunkZ = (int)(key & 0xFFFFFFFFL);
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
        CompletableFuture<ThreadedChunkMesher.MeshResult> future = mesher.submitChunk(chunk, chunkX, chunkZ, key);
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
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        for (GreedyMesher.Quad quad : mesh.quads) renderQuad(quad, baseX, baseZ);
        return true;
    }

    /**
     * Project quad to screen and setup perspective-correct interpolation
     */
    private void renderQuad(GreedyMesher.Quad quad, int baseX, int baseZ) {
        double wx = baseX + quad.x, wy = quad.y, wz = baseZ + quad.z;
        double[][] corners = new double[4][3];

        // Build world-space corners
        if (quad.axis == 1) { // Y face
            double oy = wy + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{wx, oy, wz};
            corners[1] = new double[]{wx + quad.w, oy, wz};
            corners[2] = new double[]{wx + quad.w, oy, wz + quad.h};
            corners[3] = new double[]{wx, oy, wz + quad.h};
        } else if (quad.axis == 2) { // Z face
            double oz = wz + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{wx, wy, oz};
            corners[1] = new double[]{wx + quad.w, wy, oz};
            corners[2] = new double[]{wx + quad.w, wy + quad.h, oz};
            corners[3] = new double[]{wx, wy + quad.h, oz};
        } else { // X face
            double ox = wx + (quad.dir > 0 ? 1 : 0);
            corners[0] = new double[]{ox, wy, wz};
            corners[1] = new double[]{ox, wy, wz + quad.w};
            corners[2] = new double[]{ox, wy + quad.h, wz + quad.w};
            corners[3] = new double[]{ox, wy + quad.h, wz};
        }

        Face face = new Face();
        int behindCount = 0;
        double sumCamZ = 0;

        for (int i = 0; i < 4; i++) {
            double dx = corners[i][0] - x;
            double dy = corners[i][1] - y;
            double dz = corners[i][2] - z;

            double camZ = dx * fx + dy * fy + dz * fz;
            sumCamZ += camZ;

            if (camZ < NEAR_PLANE) {
                behindCount++;
                camZ = NEAR_PLANE; // Clamp to prevent div-by-zero & clipping artifacts
            }

            double scale = invTanHalfFov / camZ;

            face.x[i] = (int) (halfWidth + (dx * rx + dz * rz) * scale * halfHeight);
            face.y[i] = (int) (halfHeight - (dx * ux + dy * uy + dz * uz) * scale * halfHeight);

            face.oneOverZ[i] = 1.0 / camZ;
        }

        if (behindCount == 4) {
            quadsCulled++;
            return;
        }

        // UV setup based on axis (fixed order to match vertex winding)
        double u0 = 0, v0 = 0, u1 = quad.w, v1 = quad.h;
        if (quad.axis != 1) { // X or Z axis → flip V for correct orientation
            v0 = quad.h; v1 = 0;
        }

        face.uOverZ[0] = u0 * face.oneOverZ[0];
        face.vOverZ[0] = v0 * face.oneOverZ[0];
        face.uOverZ[1] = u1 * face.oneOverZ[1];
        face.vOverZ[1] = v0 * face.oneOverZ[1];
        face.uOverZ[2] = u1 * face.oneOverZ[2];
        face.vOverZ[2] = v1 * face.oneOverZ[2];
        face.uOverZ[3] = u0 * face.oneOverZ[3];
        face.vOverZ[3] = v1 * face.oneOverZ[3];

        face.texIndex = quad.texIndex;
        face.brightness = quad.brightness;
        face.avgDepth = sumCamZ * 0.25;

        renderFaces.add(face);
        quadsRendered++;
    }

    private void renderPerspectiveCorrectQuad(Face face) {
        // Find bounding box
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, face.x[i]);
            maxX = Math.max(maxX, face.x[i]);
            minY = Math.min(minY, face.y[i]);
            maxY = Math.max(maxY, face.y[i]);
        }

        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);
        if (minY > maxY) return;

        int brightnessInt = Math.min(255, (int)(face.brightness * 256f));

        for (int sy = minY; sy <= maxY; sy++) {
            // Find left/right intersections
            double leftX = Double.POSITIVE_INFINITY, rightX = Double.NEGATIVE_INFINITY;
            double leftUZ = 0, leftVZ = 0, leftOZ = 0;
            double rightUZ = 0, rightVZ = 0, rightOZ = 0;

            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                int y1 = face.y[i], y2 = face.y[j];
                if ((y1 <= sy && y2 > sy) || (y2 <= sy && y1 > sy)) {
                    double t = (sy - y1) / (double)(y2 - y1);
                    double ix = face.x[i] + t * (face.x[j] - face.x[i]);

                    double uz = face.uOverZ[i] + t * (face.uOverZ[j] - face.uOverZ[i]);
                    double vz = face.vOverZ[i] + t * (face.vOverZ[j] - face.vOverZ[i]);
                    double oz = face.oneOverZ[i] + t * (face.oneOverZ[j] - face.oneOverZ[i]);

                    if (ix < leftX) {
                        leftX = ix; leftUZ = uz; leftVZ = vz; leftOZ = oz;
                    }
                    if (ix > rightX) {
                        rightX = ix; rightUZ = uz; rightVZ = vz; rightOZ = oz;
                    }
                }
            }

            if (leftX >= rightX) continue;

            int startX = Math.max(0, (int) Math.ceil(leftX));
            int endX   = Math.min(width - 1, (int) rightX);

            double invSpan = 1.0 / (rightX - leftX);
            int rowOffset = sy * width;

            for (int sx = startX; sx <= endX; sx++) {
                double t = (sx - leftX) * invSpan;

                double uOverZ = leftUZ + t * (rightUZ - leftUZ);
                double vOverZ = leftVZ + t * (rightVZ - leftVZ);
                double oneOverZ = leftOZ + t * (rightOZ - leftOZ);

                double depth = 1.0 / oneOverZ;
                int idx = rowOffset + sx;

                if (depth >= zBuffer[idx]) continue; // Depth test (closer = smaller zBuffer value? Wait - you use < )

                // Note: your zBuffer stores actual depth (smaller = closer), test with depth < zBuffer[idx]
                zBuffer[idx] = depth;

                double u = uOverZ * depth;
                double v = vOverZ * depth;

                int color = textureAtlas.sample(face.texIndex, u, v);

                int r = (((color >> 16) & 0xFF) * brightnessInt) >> 8;
                int g = (((color >> 8)  & 0xFF) * brightnessInt) >> 8;
                int b = ((color & 0xFF) * brightnessInt) >> 8;

                pixels[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    private void collectEntitySprites() {
        entitySprites.clear();
        Collection<Entity> entities = world.getEntityManager().getEntities();
        for (Entity entity : entities) {
            if (entity.isRemoved()) continue;
            double dx = entity.x - x, dy = entity.y + entity.height / 2 - y, dz = entity.z - z;
            double distSq = dx*dx + dy*dy + dz*dz;
            if (distSq > 100 * 100) continue;
            double camDepth = dx * fx + dy * fy + dz * fz;
            if (camDepth < NEAR_PLANE) continue;
            double scale = invTanHalfFov / camDepth;
            int screenX = (int)(halfWidth + (dx * rx + dz * rz) * scale * halfHeight);
            int screenY = (int)(halfHeight - (dx * ux + dy * uy + dz * uz) * scale * halfHeight);
            int entitySize = (int)(entity.height * scale * halfHeight);
            entitySize = Math.max(8, Math.min(entitySize, 200));
            if (screenX + entitySize < 0 || screenX - entitySize >= width) continue;
            if (screenY + entitySize < 0 || screenY - entitySize >= height) continue;
            java.awt.image.BufferedImage texture = getEntityTexture(entity);
            entitySprites.add(new EntitySprite(screenX, screenY, entitySize, entitySize, camDepth, texture, entity));
        }
        entitySprites.sort((a, b) -> Double.compare(b.depth, a.depth));
    }

    private java.awt.image.BufferedImage getEntityTexture(Entity entity) {
        if (entity instanceof PlayerEntity || entity instanceof MobEntity) {
            return MobTextureGenerator.getTexture(entity.getType());
        }
        return null;
    }

    private void renderEntitySprites() {
        for (EntitySprite sprite : entitySprites) {
            if (sprite.entity instanceof ItemEntity) {
                renderItemEntitySprite(sprite, (ItemEntity)sprite.entity);
            } else if (sprite.texture != null) {
                renderTexturedEntitySprite(sprite);
            }
        }
    }

    private void renderTexturedEntitySprite(EntitySprite sprite) {
        if (sprite.texture == null) return;
        int bodyWidth = sprite.width / 2, bodyHeight = sprite.height;
        drawTexturedRect(sprite.screenX - bodyWidth/2, sprite.screenY - bodyHeight/2,
                bodyWidth, bodyHeight, sprite.texture, sprite.depth);
        if (sprite.showHealth) {
            renderHealthBar(sprite.screenX, sprite.screenY - bodyHeight/2 - 10,
                    bodyWidth * 2, sprite.healthPercent, sprite.depth - 0.02);
        }
    }

    private void drawTexturedRect(int x, int y, int width, int height, java.awt.image.BufferedImage texture, double depth) {
        int minX = Math.max(0, x), maxX = Math.min(this.width - 1, x + width);
        int minY = Math.max(0, y), maxY = Math.min(this.height - 1, y + height);
        int texWidth = texture.getWidth(), texHeight = texture.getHeight();
        for (int sy = minY; sy < maxY; sy++) {
            int rowStart = sy * this.width;
            double v = (double)(sy - y) / height;
            int texY = Math.max(0, Math.min(texHeight - 1, (int)(v * texHeight)));
            for (int sx = minX; sx < maxX; sx++) {
                int idx = rowStart + sx;
                if (idx >= 0 && idx < pixels.length && depth < zBuffer[idx]) {
                    double u = (double)(sx - x) / width;
                    int texX = Math.max(0, Math.min(texWidth - 1, (int)(u * texWidth)));
                    int color = texture.getRGB(texX, texY);
                    int alpha = (color >> 24) & 0xFF;
                    if (alpha < 128) continue;
                    zBuffer[idx] = depth;
                    pixels[idx] = color & 0xFFFFFF;
                }
            }
        }
    }

    private void renderItemEntitySprite(EntitySprite sprite, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItemStack();
        if (stack.isEmpty()) return;
        int cubeSize = Math.max(8, sprite.width / 4);
        int color = getItemColor(stack.getBlockId());
        fillRectWithDepth(sprite.screenX - cubeSize/2, sprite.screenY - cubeSize/2,
                cubeSize, cubeSize, color, sprite.depth);
    }

    private void renderHealthBar(int x, int y, int width, float healthPercent, double depth) {
        int barHeight = 3;
        fillRectWithDepth(x - width/2, y, width, barHeight, 0x00000096, depth);
        int healthColor = healthPercent > 0.6f ? 0x00FF00 : healthPercent > 0.3f ? 0xFFFF00 : 0xFF0000;
        fillRectWithDepth(x - width/2, y, (int)(width * healthPercent), barHeight, healthColor, depth - 0.001);
    }

    private void fillRectWithDepth(int x, int y, int w, int h, int color, double depth) {
        int minX = Math.max(0, x), maxX = Math.min(width - 1, x + w);
        int minY = Math.max(0, y), maxY = Math.min(height - 1, y + h);
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 255;

        for (int sy = minY; sy < maxY; sy++) {
            int rowStart = sy * width;
            for (int sx = minX; sx < maxX; sx++) {
                int idx = rowStart + sx;
                if (idx >= 0 && idx < pixels.length && depth < zBuffer[idx]) {
                    zBuffer[idx] = depth;
                    if (a == 255) {
                        pixels[idx] = (r << 16) | (g << 8) | b;
                    } else {
                        int oldColor = pixels[idx];
                        int newR = (r * a + ((oldColor >> 16) & 0xFF) * (255 - a)) / 255;
                        int newG = (g * a + ((oldColor >> 8) & 0xFF) * (255 - a)) / 255;
                        int newB = (b * a + (oldColor & 0xFF) * (255 - a)) / 255;
                        pixels[idx] = (newR << 16) | (newG << 8) | newB;
                    }
                }
            }
        }
    }

    private int getItemColor(String blockId) {
        return switch(blockId) {
            case "space_test:stone" -> 0x808080;
            case "space_test:dirt" -> 0x8B4513;
            case "space_test:grass" -> 0x228B22;
            case "space_test:wood" -> 0xA0522D;
            default -> 0xC8C8C8;
        };
    }

    private void sortFaces() {
        renderFaces.sort(Comparator.comparingDouble((Face f) -> f.avgDepth).reversed()); // Far to near
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
            System.out.println("[PerspectiveCorrectRenderer] Cleaned up " + removed + " old meshes");
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
        System.out.println("[PerspectiveCorrectRenderer] Shutting down...");
        mesher.shutdown();
    }
}