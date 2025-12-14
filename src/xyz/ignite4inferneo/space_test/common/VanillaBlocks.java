package xyz.ignite4inferneo.space_test.common;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.block.BaseBlock;

/**
 * Registers all vanilla (base game) blocks.
 * Called during game initialization before mods.
 */
public class VanillaBlocks {

    // Texture indices (must match your TextureAtlas)
    private static final int TEX_STONE = 0;
    private static final int TEX_DIRT = 1;
    private static final int TEX_GRASS_SIDE = 2;
    private static final int TEX_GRASS_TOP = 3;
    private static final int TEX_WOOD = 4;
    private static final int TEX_LEAVES = 5;

    public static void register() {
        // Air (special case - not rendered)
        Registries.BLOCKS.register("space_test:air", new BaseBlock(
                "space_test:air",
                "Air",
                new int[]{0, 0, 0, 0, 0, 0},
                false,  // not solid
                true,   // transparent
                0.0f    // can't break
        ));

        // Stone
        Registries.BLOCKS.register("space_test:stone", new BaseBlock(
                "space_test:stone",
                "Stone",
                TEX_STONE
        ));

        // Dirt
        Registries.BLOCKS.register("space_test:dirt", new BaseBlock(
                "space_test:dirt",
                "Dirt",
                TEX_DIRT
        ));

        // Grass (different textures for top/bottom/sides)
        Registries.BLOCKS.register("space_test:grass", new BaseBlock(
                "space_test:grass",
                "Grass Block",
                new int[]{
                        TEX_DIRT,       // bottom
                        TEX_GRASS_TOP,  // top
                        TEX_GRASS_SIDE, // north
                        TEX_GRASS_SIDE, // south
                        TEX_GRASS_SIDE, // west
                        TEX_GRASS_SIDE  // east
                }
        ));

        // Wood/Logs
        Registries.BLOCKS.register("space_test:wood", new BaseBlock(
                "space_test:wood",
                "Oak Wood",
                TEX_WOOD
        ));

        // Leaves (transparent)
        Registries.BLOCKS.register("space_test:leaves", new BaseBlock(
                "space_test:leaves",
                "Oak Leaves",
                new int[]{TEX_LEAVES, TEX_LEAVES, TEX_LEAVES, TEX_LEAVES, TEX_LEAVES, TEX_LEAVES},
                true,   // solid (for now)
                true,   // transparent
                0.2f    // easy to break
        ));

        System.out.println("[VanillaBlocks] Registered " + Registries.BLOCKS.size() + " blocks");
    }
}