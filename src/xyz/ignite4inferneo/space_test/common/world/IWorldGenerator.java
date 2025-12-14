package xyz.ignite4inferneo.space_test.common.world;

/**
 * Interface for world generators.
 * Mods can implement custom world generation.
 */
public interface IWorldGenerator {

    /**
     * Generate terrain for a chunk
     */
    void generateChunk(World world, Chunk chunk);

    /**
     * Get the spawn position for new players
     */
    default int[] getSpawnPosition() {
        return new int[]{0, 64, 0};
    }
}