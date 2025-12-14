package xyz.ignite4inferneo.space_test.common.world;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.event.EventBus;
import xyz.ignite4inferneo.space_test.api.registry.Registries;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the game world, managing chunks and blocks.
 * This is common code that runs on both client and server.
 */
public class World {
    private static final int CHUNK_SIZE = 16;
    private static final int CHUNK_SHIFT = 4;

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final IWorldGenerator generator;
    private long tickCount = 0;

    public World(IWorldGenerator generator) {
        this.generator = generator;
    }

    /**
     * Get or generate a chunk at the given chunk coordinates
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        Chunk chunk = chunks.get(key);

        if (chunk == null) {
            chunk = new Chunk(chunkX, chunkZ);
            generator.generateChunk(this, chunk);
            chunks.put(key, chunk);
        }

        return chunk;
    }

    /**
     * Get block ID at world position
     */
    public String getBlock(int x, int y, int z) {
        if (y < 0 || y >= 256) return "space_test:air";

        int chunkX = x >> CHUNK_SHIFT;
        int chunkZ = z >> CHUNK_SHIFT;
        int localX = x & 15;
        int localZ = z & 15;

        Chunk chunk = getChunk(chunkX, chunkZ);
        return chunk.getBlock(localX, y, localZ);
    }

    /**
     * Set block at world position
     */
    public void setBlock(int x, int y, int z, String blockId) {
        if (y < 0 || y >= 256) return;

        int chunkX = x >> CHUNK_SHIFT;
        int chunkZ = z >> CHUNK_SHIFT;
        int localX = x & 15;
        int localZ = z & 15;

        Chunk chunk = getChunk(chunkX, chunkZ);
        String oldBlock = chunk.getBlock(localX, y, localZ);

        chunk.setBlock(localX, y, localZ, blockId);
        chunk.markDirty();

        // Notify block callbacks
        Block block = Registries.BLOCKS.get(blockId);
        if (block != null && !blockId.equals(oldBlock)) {
            block.onPlace(this, x, y, z);
        }

        Block old = Registries.BLOCKS.get(oldBlock);
        if (old != null && !blockId.equals(oldBlock)) {
            old.onBreak(this, x, y, z);
        }
    }

    /**
     * Check if a block is solid at the given position
     */
    public boolean isSolid(int x, int y, int z) {
        String blockId = getBlock(x, y, z);
        if (blockId.equals("space_test:air")) return false;

        Block block = Registries.BLOCKS.get(blockId);
        return block != null && block.isSolid();
    }

    /**
     * Update the world (called every tick)
     */
    public void tick() {
        tickCount++;
        EventBus.fire(new xyz.ignite4inferneo.space_test.api.event.TickEvent(tickCount));
    }

    /**
     * Get all loaded chunks
     */
    public Map<Long, Chunk> getChunks() {
        return chunks;
    }

    /**
     * Unload chunks far from a position
     */
    public void unloadDistantChunks(int centerChunkX, int centerChunkZ, int maxDistance) {
        chunks.entrySet().removeIf(entry -> {
            Chunk chunk = entry.getValue();
            int dx = chunk.getChunkX() - centerChunkX;
            int dz = chunk.getChunkZ() - centerChunkZ;
            return Math.abs(dx) > maxDistance || Math.abs(dz) > maxDistance;
        });
    }

    private static long chunkKey(int x, int z) {
        return ((long)x << 32) | (z & 0xFFFFFFFFL);
    }

    public long getTickCount() {
        return tickCount;
    }
}