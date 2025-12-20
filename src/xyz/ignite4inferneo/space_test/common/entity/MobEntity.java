package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Base class for mobs (hostile/passive creatures)
 */
public abstract class MobEntity extends LivingEntity {

    // AI state
    protected Entity target;
    protected double[] targetPos;
    protected AIState aiState = AIState.IDLE;

    // AI timers
    protected int idleTimer = 0;
    protected int targetSearchTimer = 0;
    protected int pathfindTimer = 0;

    // Mob properties
    protected float detectionRange = 16.0f;
    protected float attackRange = 2.0f;
    protected float attackDamage = 2.0f;
    protected int attackCooldown = 0;
    protected static final int ATTACK_DELAY = 20; // Ticks between attacks

    public enum AIState {
        IDLE,
        WANDER,
        CHASE,
        ATTACK,
        FLEE
    }

    public MobEntity(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected void onTick(double deltaTime) {
        // Decrease attack cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Run AI
        updateAI(deltaTime);
    }

    /**
     * Main AI update loop
     */
    protected void updateAI(double deltaTime) {
        targetSearchTimer++;

        // Search for targets periodically
        if (targetSearchTimer > 20) { // Every second
            targetSearchTimer = 0;
            searchForTarget();
        }

        // Execute current state
        switch (aiState) {
            case IDLE -> handleIdle(deltaTime);
            case WANDER -> handleWander(deltaTime);
            case CHASE -> handleChase(deltaTime);
            case ATTACK -> handleAttack(deltaTime);
            case FLEE -> handleFlee(deltaTime);
        }
    }

    /**
     * Search for targets (players, other entities)
     */
    protected void searchForTarget() {
        if (dead) return;

        // Find nearest player
        var entities = world.getEntityManager().getEntitiesNear(x, y, z, detectionRange);

        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (entity == this) continue;
            if (!canTarget(entity)) continue;

            double dist = distanceTo(entity);
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }

        if (closest != null) {
            target = closest;
            aiState = AIState.CHASE;
        } else {
            target = null;
            if (aiState == AIState.CHASE || aiState == AIState.ATTACK) {
                aiState = AIState.IDLE;
            }
        }
    }

    /**
     * Can this mob target the entity?
     */
    protected boolean canTarget(Entity entity) {
        // Override in subclasses
        // Hostile mobs target players, passive mobs don't
        return entity instanceof PlayerEntity;
    }

    /**
     * Idle behavior
     */
    protected void handleIdle(double deltaTime) {
        idleTimer++;

        // Apply friction
        vx *= 0.8;
        vz *= 0.8;

        // Sometimes start wandering
        if (idleTimer > 60 && Math.random() < 0.02) {
            aiState = AIState.WANDER;
            idleTimer = 0;
        }
    }

    /**
     * Wander behavior
     */
    protected void handleWander(double deltaTime) {
        idleTimer++;

        // Pick random direction if needed
        if (targetPos == null || idleTimer > 100) {
            double angle = Math.random() * Math.PI * 2;
            double dist = 5 + Math.random() * 10;
            targetPos = new double[]{
                    x + Math.sin(angle) * dist,
                    y,
                    z + Math.cos(angle) * dist
            };
            idleTimer = 0;
        }

        // Move towards target position
        moveTowards(targetPos[0], targetPos[2], moveSpeed * (float) deltaTime * 0.5f);

        // Reached target or timeout
        double dx = targetPos[0] - x;
        double dz = targetPos[2] - z;
        if (dx*dx + dz*dz < 1.0 || idleTimer > 200) {
            aiState = AIState.IDLE;
            targetPos = null;
        }
    }

    /**
     * Chase behavior
     */
    protected void handleChase(double deltaTime) {
        if (target == null || target.isRemoved()) {
            aiState = AIState.IDLE;
            return;
        }

        double dist = distanceTo(target);

        // Too far away, give up
        if (dist > detectionRange * 1.5) {
            target = null;
            aiState = AIState.IDLE;
            return;
        }

        // Close enough to attack
        if (dist < attackRange) {
            aiState = AIState.ATTACK;
            return;
        }

        // Move towards target
        moveTowards(target.x, target.z, moveSpeed * (float) deltaTime);

        // Look at target
        double dx = target.x - x;
        double dz = target.z - z;
        lookYaw = (float) Math.atan2(dx, dz);
    }

    /**
     * Attack behavior
     */
    protected void handleAttack(double deltaTime) {
        if (target == null || target.isRemoved()) {
            aiState = AIState.IDLE;
            return;
        }

        double dist = distanceTo(target);

        // Target moved away
        if (dist > attackRange * 1.5) {
            aiState = AIState.CHASE;
            return;
        }

        // Look at target
        double dx = target.x - x;
        double dz = target.z - z;
        lookYaw = (float) Math.atan2(dx, dz);

        // Attack
        if (attackCooldown == 0 && target instanceof LivingEntity living) {
            living.damage(attackDamage);
            attackCooldown = ATTACK_DELAY;
            onAttack(target);
        }
    }

    /**
     * Flee behavior
     */
    protected void handleFlee(double deltaTime) {
        if (target == null) {
            aiState = AIState.IDLE;
            return;
        }

        // Run away from target
        double dx = x - target.x;
        double dz = z - target.z;
        double dist = Math.sqrt(dx*dx + dz*dz);

        if (dist < 0.01) {
            dx = Math.random() - 0.5;
            dz = Math.random() - 0.5;
        }

        dx /= dist;
        dz /= dist;

        moveTowards(x + dx * 10, z + dz * 10, moveSpeed * (float) deltaTime * 1.5f);

        // Safe distance reached
        if (distanceTo(target) > detectionRange) {
            target = null;
            aiState = AIState.IDLE;
        }
    }

    /**
     * Move towards a position
     */
    protected void moveTowards(double targetX, double targetZ, float speed) {
        double dx = targetX - x;
        double dz = targetZ - z;
        double dist = Math.sqrt(dx*dx + dz*dz);

        if (dist < 0.1) return;

        dx /= dist;
        dz /= dist;

        vx += dx * speed;
        vz += dz * speed;

        // Limit velocity
        double vel = Math.sqrt(vx*vx + vz*vz);
        if (vel > moveSpeed) {
            vx = (vx / vel) * moveSpeed;
            vz = (vz / vel) * moveSpeed;
        }
    }

    /**
     * Called when mob attacks
     */
    protected void onAttack(Entity target) {
        // Override for attack effects
    }

    @Override
    protected void onDamage(float amount) {
        super.onDamage(amount);
        // React to damage (e.g., aggro)
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // Drop items, play animation, etc.
        remove();
    }

    // Getters/Setters
    public AIState getAIState() {
        return aiState;
    }

    public Entity getTarget() {
        return target;
    }

    public float getDetectionRange() {
        return detectionRange;
    }

    public void setDetectionRange(float range) {
        this.detectionRange = range;
    }
}