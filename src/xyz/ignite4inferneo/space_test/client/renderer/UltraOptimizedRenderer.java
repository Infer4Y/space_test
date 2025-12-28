package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.entity.*;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;
import xyz.ignite4inferneo.space_test.common.world.Chunk;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * COMPLETE: Ultra-optimized renderer WITH tiled rendering AND entity rendering
 *
 * Features:
 * - Parallel tile rendering for better CPU cache usage
 * - Threaded chunk meshing
 * - Integrated entity rendering with depth testing
 * - Frustum culling
 * - Greedy meshing
 * - Proper depth sorting
 * - Fixed texture interpolation
 */
public class UltraOptimizedRenderer {
    private static final int RENDER_DISTANCE = 8;
    private static final double NEAR_PLANE = 0.1;

    private final World world;
    private final TextureAtlas textureAtlas;
    private final ThreadedChunkMesher mesher;

    // Direct pixel buffer
    private int[] pixels;
    private double[] zBuffer;
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
    private final List<EntitySprite> entitySprites = new ArrayList<>(256);

    // Tiled rendering
    private TiledRenderer tiledRenderer;
    private static final int TILE_SIZE = 128; // 128x128 pixel tiles

    // Entity colors
    private static final Map<String, Integer> ENTITY_COLORS = new HashMap<>();
    static {
        ENTITY_COLORS.put("player", 0x6496FF);
        ENTITY_COLORS.put("zombie", 0x649664);
        ENTITY_COLORS.put("pig", 0xFFB4B4);
        ENTITY_COLORS.put("item", 0xFFFF64);
    }

    // Stats
    private int chunksRendered = 0;
    private int chunksMeshing = 0;
    private int quadsRendered = 0;
    private int quadsCulled = 0;
    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 30000;

    public TextureAtlas getTextureAtlas() {
        return textureAtlas;
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

    /**
     * Entity sprite for billboarded rendering
     */
    private static class EntitySprite {
        int screenX, screenY;
        int width, height;
        double depth;
        int color;
        Entity entity;

        // Health bar
        float healthPercent;
        boolean showHealth;

        EntitySprite(int x, int y, int w, int h, double d, int c, Entity e) {
            this.screenX = x;
            this.screenY = y;
            this.width = w;
            this.height = h;
            this.depth = d;
            this.color = c;
            this.entity = e;
            this.showHealth = false;
            this.healthPercent = 1.0f;

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

        // Initialize tiled renderer
        int tileThreads = Math.max(2, threadCount / 2); // Use half threads for tiling
        this.tiledRenderer = new TiledRenderer(TILE_SIZE, tileThreads);

        System.out.println("[UltraOptimizedRenderer] Initialized with " + threadCount +
                " mesh threads and " + tileThreads + " tile threads");
    }

    public void setCanvasSize(java.awt.Dimension dimension) {
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

        // Update tile layout
        if (tiledRenderer != null) {
            tiledRenderer.updateTiles(w, h);
        }
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

        // Clear buffers (can be done in parallel with tiles)
        Arrays.fill(pixels, 0x87CEEB);
        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);

        renderFaces.clear();
        entitySprites.clear();
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

        // Render chunks (collect faces)
        for (int i = 0; i < chunkTasks.size(); i++) {
            ChunkRenderTask task = chunkTasks.get(i);
            if (renderChunk(task.chunkX, task.chunkZ, task.key)) {
                chunksRendered++;
            }
        }

        // Collect entity sprites
        collectEntitySprites();

        // Sort faces back to front
        sortFaces();

        // Render all faces with tiled rendering
        if (tiledRenderer != null && renderFaces.size() > 0) {
            try {
                // Render tiles in parallel
                tiledRenderer.renderTiles(tile -> {
                    for (int i = 0; i < renderFaces.size(); i++) {
                        fillTexturedQuadInTile(renderFaces.get(i), tile);
                    }
                });
            } catch (Exception e) {
                System.err.println("[Renderer] Tiled rendering error: " + e.getMessage());
                // Fallback to non-tiled rendering
                for (int i = 0; i < renderFaces.size(); i++) {
                    fillTexturedQuad(renderFaces.get(i));
                }
            }
        } else {
            // Fallback: render all faces normally
            for (int i = 0; i < renderFaces.size(); i++) {
                fillTexturedQuad(renderFaces.get(i));
            }
        }

        // Render entity sprites (after terrain, uses z-buffer)
        renderEntitySprites();

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
                camZ[i] = NEAR_PLANE;
            }
        }

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

    /**
     * Collect all visible entity sprites
     */
    private void collectEntitySprites() {
        entitySprites.clear();

        Collection<Entity> entities = world.getEntityManager().getEntities();

        for (Entity entity : entities) {
            if (entity.isRemoved()) continue;

            // Distance culling
            double dx = entity.x - x;
            double dy = entity.y + entity.height / 2 - y;
            double dz = entity.z - z;
            double distSq = dx*dx + dy*dy + dz*dz;

            if (distSq > 100 * 100) continue; // 100 block render distance

            // Camera space depth
            double camDepth = dx * fx + dy * fy + dz * fz;
            if (camDepth < NEAR_PLANE) continue; // Behind camera

            // Project to screen
            double scale = invTanHalfFov / camDepth;
            int screenX = (int)(halfWidth + (dx * rx + dz * rz) * scale * halfHeight);
            int screenY = (int)(halfHeight - (dx * ux + dy * uy + dz * uz) * scale * halfHeight);

            // Calculate size
            int entitySize = (int)(entity.height * scale * halfHeight);
            entitySize = Math.max(8, Math.min(entitySize, 200));

            // Frustum culling (simple)
            if (screenX + entitySize < 0 || screenX - entitySize >= width) continue;
            if (screenY + entitySize < 0 || screenY - entitySize >= height) continue;

            // Get entity color
            int color = ENTITY_COLORS.getOrDefault(entity.getType(), 0xC8C8C8);

            // Apply distance fog
            color = applyDistanceFog(color, camDepth);

            entitySprites.add(new EntitySprite(screenX, screenY, entitySize, entitySize,
                    camDepth, color, entity));
        }

        // Sort sprites back to front
        entitySprites.sort((a, b) -> Double.compare(b.depth, a.depth));
    }

    /**
     * Render all entity sprites with depth testing
     */
    private void renderEntitySprites() {
        for (EntitySprite sprite : entitySprites) {
            Entity entity = sprite.entity;

            if (entity instanceof ItemEntity) {
                renderItemEntitySprite(sprite, (ItemEntity)entity);
            } else if (entity instanceof PlayerEntity) {
                renderPlayerSprite(sprite, (PlayerEntity)entity);
            } else if (entity instanceof MobEntity) {
                renderMobSprite(sprite, (MobEntity)entity);
            } else {
                renderGenericEntitySprite(sprite);
            }
        }
    }

    /**
     * Render a player sprite
     */
    private void renderPlayerSprite(EntitySprite sprite, PlayerEntity player) {
        int bodyWidth = sprite.width / 3;
        int bodyHeight = sprite.height;
        int headSize = bodyWidth;

        // Body
        fillRectWithDepth(
                sprite.screenX - bodyWidth/2,
                sprite.screenY - bodyHeight/2,
                bodyWidth, bodyHeight,
                sprite.color, sprite.depth
        );

        // Head
        int headColor = brighten(sprite.color, 1.2f);
        fillRectWithDepth(
                sprite.screenX - headSize/2,
                sprite.screenY - bodyHeight/2 - headSize,
                headSize, headSize,
                headColor, sprite.depth - 0.01
        );

        // Sprint indicator
        if (player.isSprinting()) {
            int indicatorY = sprite.screenY - bodyHeight/2 - headSize - 5;
            fillRectWithDepth(sprite.screenX - 3, indicatorY, 6, 3, 0xFFFF00, sprite.depth - 0.02);
        }

        // Health bar
        if (sprite.showHealth) {
            renderHealthBar(sprite.screenX, sprite.screenY - bodyHeight/2 - headSize - 10,
                    bodyWidth * 2, sprite.healthPercent, sprite.depth - 0.02);
        }
    }

    /**
     * Render a mob sprite
     */
    private void renderMobSprite(EntitySprite sprite, MobEntity mob) {
        int bodyWidth = sprite.width / 3;
        int bodyHeight = (int)(sprite.height * 0.8);
        int headSize = bodyWidth;

        // Body
        fillRectWithDepth(
                sprite.screenX - bodyWidth/2,
                sprite.screenY - bodyHeight/2,
                bodyWidth, bodyHeight,
                sprite.color, sprite.depth
        );

        // Head
        int headColor = brighten(sprite.color, 1.2f);
        fillRectWithDepth(
                sprite.screenX - headSize/2,
                sprite.screenY - bodyHeight/2 - headSize/2,
                headSize, headSize,
                headColor, sprite.depth - 0.01
        );

        // Health bar (only if damaged)
        if (sprite.showHealth) {
            renderHealthBar(sprite.screenX, sprite.screenY - bodyHeight/2 - headSize/2 - 5,
                    bodyWidth * 2, sprite.healthPercent, sprite.depth - 0.02);
        }

        // AI state indicator
        if (mob.getAIState() == MobEntity.AIState.CHASE ||
                mob.getAIState() == MobEntity.AIState.ATTACK) {
            fillCircleWithDepth(sprite.screenX, sprite.screenY - bodyHeight/2 - headSize - 3,
                    3, 0xFF0000, sprite.depth - 0.02);
        }
    }

    /**
     * Render an item entity sprite
     */
    private void renderItemEntitySprite(EntitySprite sprite, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItemStack();
        if (stack.isEmpty()) return;

        // Apply bobbing
        double bobHeight = itemEntity.getBobHeight();
        int bobbedY = sprite.screenY - (int)(bobHeight * 10);

        int cubeSize = Math.max(8, sprite.width / 4);

        // Shadow
        fillCircleWithDepth(sprite.screenX, sprite.screenY + cubeSize/2,
                cubeSize/2, 0x00000080, sprite.depth + 0.01);

        // Item cube
        fillRectWithDepth(
                sprite.screenX - cubeSize/2,
                bobbedY - cubeSize/2,
                cubeSize, cubeSize,
                sprite.color, sprite.depth
        );

        // Highlight
        int highlightColor = brighten(sprite.color, 1.5f);
        fillRectWithDepth(
                sprite.screenX - cubeSize/2 + 1,
                bobbedY - cubeSize/2 + 1,
                cubeSize/3, cubeSize/3,
                highlightColor, sprite.depth - 0.01
        );
    }

    /**
     * Render generic entity sprite
     */
    private void renderGenericEntitySprite(EntitySprite sprite) {
        int width = sprite.width / 3;
        int height = sprite.height;

        fillRectWithDepth(
                sprite.screenX - width/2,
                sprite.screenY - height/2,
                width, height,
                sprite.color, sprite.depth
        );
    }

    /**
     * Render health bar with depth testing
     */
    private void renderHealthBar(int x, int y, int width, float healthPercent, double depth) {
        int barHeight = 3;

        // Background
        fillRectWithDepth(x - width/2, y, width, barHeight, 0x00000096, depth);

        // Health
        int healthColor = healthPercent > 0.6f ? 0x00FF00 :
                healthPercent > 0.3f ? 0xFFFF00 : 0xFF0000;
        fillRectWithDepth(x - width/2, y, (int)(width * healthPercent), barHeight,
                healthColor, depth - 0.001);
    }

    /**
     * Fill rectangle with depth testing
     */
    private void fillRectWithDepth(int x, int y, int w, int h, int color, double depth) {
        int minX = Math.max(0, x);
        int maxX = Math.min(width - 1, x + w);
        int minY = Math.max(0, y);
        int maxY = Math.min(height - 1, y + h);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        if (a == 0) a = 255; // Default opaque

        for (int sy = minY; sy < maxY; sy++) {
            int rowStart = sy * width;
            for (int sx = minX; sx < maxX; sx++) {
                int idx = rowStart + sx;
                if (idx >= 0 && idx < pixels.length && depth < zBuffer[idx]) {
                    zBuffer[idx] = depth;

                    if (a == 255) {
                        pixels[idx] = (r << 16) | (g << 8) | b;
                    } else {
                        // Alpha blending
                        int oldColor = pixels[idx];
                        int oldR = (oldColor >> 16) & 0xFF;
                        int oldG = (oldColor >> 8) & 0xFF;
                        int oldB = oldColor & 0xFF;

                        int newR = (r * a + oldR * (255 - a)) / 255;
                        int newG = (g * a + oldG * (255 - a)) / 255;
                        int newB = (b * a + oldB * (255 - a)) / 255;

                        pixels[idx] = (newR << 16) | (newG << 8) | newB;
                    }
                }
            }
        }
    }

    /**
     * Fill circle with depth testing
     */
    private void fillCircleWithDepth(int cx, int cy, int radius, int color, double depth) {
        int minX = Math.max(0, cx - radius);
        int maxX = Math.min(width - 1, cx + radius);
        int minY = Math.max(0, cy - radius);
        int maxY = Math.min(height - 1, cy + radius);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        if (a == 0) a = 255;

        int radiusSq = radius * radius;

        for (int sy = minY; sy <= maxY; sy++) {
            int dy = sy - cy;
            int rowStart = sy * width;
            for (int sx = minX; sx <= maxX; sx++) {
                int dx = sx - cx;
                if (dx*dx + dy*dy <= radiusSq) {
                    int idx = rowStart + sx;
                    if (idx >= 0 && idx < pixels.length && depth < zBuffer[idx]) {
                        zBuffer[idx] = depth;

                        if (a == 255) {
                            pixels[idx] = (r << 16) | (g << 8) | b;
                        } else {
                            int oldColor = pixels[idx];
                            int oldR = (oldColor >> 16) & 0xFF;
                            int oldG = (oldColor >> 8) & 0xFF;
                            int oldB = oldColor & 0xFF;

                            int newR = (r * a + oldR * (255 - a)) / 255;
                            int newG = (g * a + oldG * (255 - a)) / 255;
                            int newB = (b * a + oldB * (255 - a)) / 255;

                            pixels[idx] = (newR << 16) | (newG << 8) | newB;
                        }
                    }
                }
            }
        }
    }

    /**
     * Brighten a color
     */
    private int brighten(int color, float factor) {
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int)((color & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Apply distance fog to color
     */
    private int applyDistanceFog(int color, double depth) {
        float fogStart = 40.0f;
        float fogEnd = 80.0f;

        if (depth < fogStart) return color;

        float fogFactor = (float)((depth - fogStart) / (fogEnd - fogStart));
        fogFactor = Math.max(0, Math.min(1, fogFactor));

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int newR = (int)(r * (1 - fogFactor) + 135 * fogFactor);
        int newG = (int)(g * (1 - fogFactor) + 206 * fogFactor);
        int newB = (int)(b * (1 - fogFactor) + 235 * fogFactor);

        return (newR << 16) | (newG << 8) | newB;
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
     * Standard texture interpolation with bounds checking (fallback)
     */
    private void fillTexturedQuad(FastFaceList.Face face) {
        int minY = Math.min(Math.min(face.y[0], face.y[1]), Math.min(face.y[2], face.y[3]));
        int maxY = Math.max(Math.max(face.y[0], face.y[1]), Math.max(face.y[2], face.y[3]));

        // Safety clip to screen bounds with margin
        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);

        // Skip if completely off-screen
        if (minY > height - 1 || maxY < 0) return;

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

            // Clip X to screen bounds
            int minX = Math.max(0, (int)ex0);
            int maxX = Math.min(width - 1, (int)ex1);

            // Skip if span is off-screen
            if (minX > width - 1 || maxX < 0) continue;

            double spanWidth = ex1 - ex0;
            if (spanWidth < 0.001) continue;

            double invSpanWidth = 1.0 / spanWidth;
            int rowStart = sy * width;

            for (int sx = minX; sx <= maxX; sx++) {
                // Additional safety check
                int idx = rowStart + sx;
                if (idx < 0 || idx >= pixels.length) continue;

                double t = (sx - ex0) * invSpanWidth;
                double invD = eInvD0 + t * (eInvD1 - eInvD0);
                double depth = 1.0 / invD;

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

    /**
     * Tiled texture interpolation with bounds checking (parallel rendering)
     */
    private void fillTexturedQuadInTile(FastFaceList.Face face, TiledRenderer.RenderTile tile) {
        int minY = Math.min(Math.min(face.y[0], face.y[1]), Math.min(face.y[2], face.y[3]));
        int maxY = Math.max(Math.max(face.y[0], face.y[1]), Math.max(face.y[2], face.y[3]));

        // Clip to tile bounds with safety margin
        minY = Math.max(tile.startY, Math.max(0, minY));
        maxY = Math.min(tile.endY - 1, Math.min(height - 1, maxY));

        if (minY > maxY || minY >= height || maxY < 0) return; // Quad doesn't intersect this tile

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

            // Clip to tile X bounds with safety
            int minX = Math.max(tile.startX, Math.max(0, (int)ex0));
            int maxX = Math.min(tile.endX - 1, Math.min(width - 1, (int)ex1));

            // Skip if span is off-screen
            if (minX > maxX || minX >= width || maxX < 0) continue;

            double spanWidth = ex1 - ex0;
            if (spanWidth < 0.001) continue;

            double invSpanWidth = 1.0 / spanWidth;
            int rowStart = sy * width;

            for (int sx = minX; sx <= maxX; sx++) {
                // Additional safety check for array bounds
                int idx = rowStart + sx;
                if (idx < 0 || idx >= pixels.length) continue;

                double t = (sx - ex0) * invSpanWidth;
                double invD = eInvD0 + t * (eInvD1 - eInvD0);
                double depth = 1.0 / invD;

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
        System.out.println("[UltraOptimizedRenderer] Shutting down...");
        mesher.shutdown();
        if (tiledRenderer != null) {
            tiledRenderer.shutdown();
        }
    }
}