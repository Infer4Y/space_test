
package xyz.ignite4inferneo.space_test.api.registry;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.item.Item;

/**
 * Central holder for all game registries.
 * Mods access these to register their content.
 */
public class Registries {
    public static final Registry<Block> BLOCKS = new Registry<>("blocks");
    public static final Registry<Item> ITEMS = new Registry<>("items");

    // Additional registries can be added here:
    // public static final Registry<Entity> ENTITIES = new Registry<>("entities");
    // public static final Registry<Dimension> DIMENSIONS = new Registry<>("dimensions");
    // public static final Registry<Biome> BIOMES = new Registry<>("biomes");

    /**
     * Freeze all registries after initialization
     */
    public static void freezeAll() {
        BLOCKS.freeze();
        ITEMS.freeze();
    }

    private Registries() {} // Prevent instantiation
}