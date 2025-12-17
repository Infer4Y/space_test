package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;
import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Represents a dropped item in the world
 */
public class ItemEntity extends Entity {
    private final ItemStack itemStack;
    private int pickupDelay = 10; // Ticks before can be picked up
    private int lifetime = 6000; // 5 minutes (at 20 TPS)

    public ItemEntity(World world, double x, double y, double z, ItemStack itemStack) {
        super(world, x, y, z);
        this.itemStack = itemStack;
        this.width = 0.25;
        this.height = 0.25;

        // Random initial velocity
        this.vx = (Math.random() - 0.5) * 2.0;
        this.vy = Math.random() * 5.0 + 2.0;
        this.vz = (Math.random() - 0.5) * 2.0;
    }

    @Override
    protected void onTick(double deltaTime) {
        // Decrease pickup delay
        if (pickupDelay > 0) {
            pickupDelay--;
        }

        // Decrease lifetime
        lifetime--;
        if (lifetime <= 0) {
            remove();
            return;
        }

        // Rotate for visual effect
        yaw += deltaTime * 2.0;

        // Bob up and down slightly when on ground
        if (onGround) {
            y += Math.sin(ticksExisted * 0.1) * 0.01;
        }
    }

    /**
     * Check if item can be picked up
     */
    public boolean canPickup() {
        return pickupDelay <= 0;
    }

    /**
     * Get the item stack
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public String getType() {
        return "item";
    }
}