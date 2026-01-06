package xyz.ignite4inferneo.space_test.common.item;

import xyz.ignite4inferneo.space_test.common.world.World;

public class ToolItem extends BaseItem {
    private final ToolType toolType;
    private final ToolTier tier;
    private final float miningSpeed;
    private final int durability;

    public ToolItem(String id, String name, int textureIndex, ToolType toolType, ToolTier tier) {
        super(id, name, textureIndex, 1); // Tools don't stack
        this.toolType = toolType;
        this.tier = tier;
        this.miningSpeed = tier.getMiningSpeed();
        this.durability = tier.getDurability();
    }

    public ToolType getToolType() {
        return toolType;
    }

    public ToolTier getTier() {
        return tier;
    }

    public float getMiningSpeed() {
        return miningSpeed;
    }

    public int getDurability() {
        return durability;
    }

    @Override
    public boolean onUse(World world, int x, int y, int z) {
        // Tools are used for mining, not placing
        return false;
    }
}
