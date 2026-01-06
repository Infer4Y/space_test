package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Example passive mob - Pig
 */
public class PigEntity extends MobEntity {

    public PigEntity(World world, double x, double y, double z) {
        super(world, x, y, z);

        // Pig stats
        this.maxHealth = 10.0f;
        this.health = maxHealth;
        this.moveSpeed = 2.5f;
        this.detectionRange = 8.0f; // For fleeing

        // Pig size
        this.width = 0.9;
        this.height = 0.9;
    }

    @Override
    protected boolean canTarget(Entity entity) {
        // Pigs don't attack, but they detect threats to flee from
        return false;
    }

    @Override
    protected void searchForTarget() {
        if (dead) return;

        // Look for nearby players (to flee from)
        var entities = world.getEntityManager().getEntitiesNear(x, y, z, detectionRange);

        for (Entity entity : entities) {
            if (entity instanceof PlayerEntity) {
                double dist = distanceTo(entity);
                if (dist < 4.0) { // Very close
                    target = entity;
                    aiState = AIState.FLEE;
                    return;
                }
            }
        }

        // No threats, just wander
        if (aiState == AIState.FLEE) {
            target = null;
            aiState = AIState.IDLE;
        }
    }

    @Override
    protected void handleIdle(double deltaTime) {
        idleTimer++;

        vx *= 0.8;
        vz *= 0.8;

        // Frequently wander
        if (idleTimer > 40 && Math.random() < 0.05) {
            aiState = AIState.WANDER;
            idleTimer = 0;
        }
    }

    @Override
    protected void onDamage(float amount) {
        super.onDamage(amount);
        // Flee when damaged
        aiState = AIState.FLEE;
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // Drop porkchop
        System.out.println("Pig died at " + x + ", " + y + ", " + z);
    }

    @Override
    public String getType() {
        return "pig";
    }
}