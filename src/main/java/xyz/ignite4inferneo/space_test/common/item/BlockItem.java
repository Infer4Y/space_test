package xyz.ignite4inferneo.space_test.common.item;

/**
 * Block item - an item that places a block when used
 */
public class BlockItem extends BaseItem {
    private final String blockId;

    public BlockItem(String id, String name, int textureIndex, String blockId) {
        super(id, name, textureIndex);
        this.blockId = blockId;
    }

    @Override
    public boolean isBlockItem() {
        return true;
    }

    @Override
    public String getBlockId() {
        return blockId;
    }
}