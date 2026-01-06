package xyz.ignite4inferneo.space_test.common.item;

/**
 * Fuel item - can be burned in furnaces
 */
public class FuelItem extends BaseItem {
    private final int burnTime; // In ticks (20 ticks = 1 second)

    public FuelItem(String id, String name, int textureIndex, int burnTime) {
        super(id, name, textureIndex);
        this.burnTime = burnTime;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public boolean isFuel() {
        return true;
    }
}