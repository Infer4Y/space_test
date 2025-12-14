package xyz.ignite4inferneo.space_test.common.block;

import xyz.ignite4inferneo.space_test.api.block.Block;

/**
 * Basic implementation of Block for simple blocks.
 * Mods can extend this instead of implementing Block directly.
 */
public class BaseBlock implements Block {
    protected final String id;
    protected final String name;
    protected final int[] textureIndices;
    protected final boolean solid;
    protected final boolean transparent;
    protected final float hardness;

    /**
     * Create a simple block with same texture on all sides
     */
    public BaseBlock(String id, String name, int textureIndex) {
        this(id, name, new int[]{textureIndex, textureIndex, textureIndex, textureIndex, textureIndex, textureIndex});
    }

    /**
     * Create a block with different textures per face
     * @param textureIndices [bottom, top, north, south, west, east]
     */
    public BaseBlock(String id, String name, int[] textureIndices) {
        this(id, name, textureIndices, true, false, 1.0f);
    }

    /**
     * Full constructor with all properties
     */
    public BaseBlock(String id, String name, int[] textureIndices, boolean solid, boolean transparent, float hardness) {
        this.id = id;
        this.name = name;
        this.textureIndices = textureIndices;
        this.solid = solid;
        this.transparent = transparent;
        this.hardness = hardness;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int[] getTextureIndices() {
        return textureIndices;
    }

    @Override
    public boolean isSolid() {
        return solid;
    }

    @Override
    public boolean isTransparent() {
        return transparent;
    }

    @Override
    public float getHardness() {
        return hardness;
    }
}