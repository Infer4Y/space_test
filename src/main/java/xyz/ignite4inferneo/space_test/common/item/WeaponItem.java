package xyz.ignite4inferneo.space_test.common.item;

public class WeaponItem extends BaseItem {
    private final float attackDamage;
    private final ToolTier tier;
    private final int durability;

    public WeaponItem(String id, String name, int textureIndex, float attackDamage, ToolTier tier) {
        super(id, name, textureIndex, 1); // Weapons don't stack
        this.attackDamage = attackDamage;
        this.tier = tier;
        this.durability = tier.getDurability();
    }

    public float getAttackDamage() {
        return attackDamage;
    }

    public ToolTier getTier() {
        return tier;
    }

    public int getDurability() {
        return durability;
    }
}
