package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Example hostile mob - Zombie
 */
public class ZombieEntity extends MobEntity {

    public ZombieEntity(World world, double x, double y, double z) {
        super(world, x, y, z);

        // Zombie stats
        this.maxHealth = 20.0f;
        this.health = maxHealth;
        this.moveSpeed = 3.0f; // Slower than player
        this.attackDamage = 3.0f;
        this.attackRange = 2.0f;
        this.detectionRange = 16.0f;

        // Zombie is slightly bigger
        this.width = 0.6;
        this.height = 1.95;
    }

    @Override
    protected boolean canTarget(Entity entity) {
        // Zombies target players
        return entity instanceof PlayerEntity && !((PlayerEntity) entity).isDead();
    }

    @Override
    protected void onAttack(Entity target) {
        // Play attack sound/animation
        System.out.println("Zombie attacks " + target.getUUID());
    }

    @Override
    protected void onDamage(float amount) {
        super.onDamage(amount);
        // Play hurt sound
        System.out.println("Zombie takes " + amount + " damage!");
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // Drop items (rotten flesh, etc.)
        System.out.println("Zombie died at " + x + ", " + y + ", " + z);
    }

    @Override
    public String getType() {
        return "zombie";
    }
}