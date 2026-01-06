package xyz.ignite4inferneo.space_test.common.item;

public enum ToolTier {
    WOOD(2.0f, 59),
    STONE(4.0f, 131),
    IRON(6.0f, 250),
    GOLD(12.0f, 32),
    DIAMOND(8.0f, 1561);

    private final float miningSpeed;
    private final int durability;

    ToolTier(float miningSpeed, int durability) {
        this.miningSpeed = miningSpeed;
        this.durability = durability;
    }

    public float getMiningSpeed() {
        return miningSpeed;
    }

    public int getDurability() {
        return durability;
    }
}
