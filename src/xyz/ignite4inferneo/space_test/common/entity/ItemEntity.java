package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;
import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Item entity - dropped items in the world
 */
public class ItemEntity extends Entity {

    private ItemStack itemStack;
    private int pickupDelay = 10; // Ticks before can be picked up
    private int lifetime = 6000; // 5 minutes (at 20 TPS)
    private int age = 0;

    // Bobbing animation
    private double bobHeight = 0;
    private double bobPhase = 0;

    public ItemEntity(World world, double x, double y, double z, ItemStack itemStack) {
        super(world, x, y, z);
        this.itemStack = itemStack;
        this.width = 0.25;
        this.height = 0.25;

        // Random initial velocity
        this.vx = (Math.random() - 0.5) * 2.0;
        this.vy = Math.random() * 5.0 + 2.0;
        this.vz = (Math.random() - 0.5) * 2.0;

        // Random spin
        this.yaw = Math.random() * Math.PI * 2;
    }

    @Override
    protected void onTick(double deltaTime) {
        age++;

        // Decrease pickup delay
        if (pickupDelay > 0) {
            pickupDelay--;
        }

        // Check lifetime
        if (age >= lifetime) {
            remove();
            return;
        }

        // Bobbing animation when on ground
        if (onGround) {
            bobPhase += deltaTime * 3.0;
            bobHeight = Math.sin(bobPhase) * 0.1;

            // Slow rotation
            yaw += deltaTime * 2.0;
        }

        // Check for nearby players to pick up
        if (pickupDelay <= 0) {
            checkPickup();
        }

        // Merge with nearby items of same type
        if (age % 20 == 0) { // Check every second
            tryMergeWithNearby();
        }
    }

    /**
     * Check if any player can pick up this item
     */
    private void checkPickup() {
        var entities = world.getEntityManager().getEntitiesNear(x, y, z, 2.0);

        for (Entity entity : entities) {
            if (entity instanceof PlayerEntity player) {
                if (player.getInventory().hasRoom(itemStack.getBlockId(), itemStack.getCount())) {
                    // Add to inventory
                    if (player.getInventory().addItem(itemStack.getBlockId(), itemStack.getCount())) {
                        // Play pickup sound
                        System.out.println("[ItemEntity] " + player.getUsername() +
                                " picked up " + itemStack);
                        remove();
                        return;
                    }
                }
            }
        }
    }

    /**
     * Try to merge with nearby items of same type
     */
    private void tryMergeWithNearby() {
        var entities = world.getEntityManager().getEntitiesNear(x, y, z, 1.0);

        for (Entity entity : entities) {
            if (entity == this) continue;
            if (!(entity instanceof ItemEntity other)) continue;

            // Same item type?
            if (other.itemStack.getBlockId().equals(this.itemStack.getBlockId())) {
                int total = this.itemStack.getCount() + other.itemStack.getCount();
                int maxStack = this.itemStack.getMaxStackSize();

                if (total <= maxStack) {
                    // Merge into this entity
                    this.itemStack = this.itemStack.withCount(total);
                    other.remove();
                    return;
                }
            }
        }
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public boolean canPickup() {
        return pickupDelay <= 0;
    }

    public double getBobHeight() {
        return bobHeight;
    }

    @Override
    public String getType() {
        return "item";
    }
}