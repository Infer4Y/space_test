package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Base class for living entities (players, mobs, NPCs)
 * Adds health, damage, and AI capabilities
 */
public abstract class LivingEntity extends Entity {

    // Health
    protected float health;
    protected float maxHealth;
    protected boolean dead = false;

    // Damage
    protected int invulnerableTicks = 0;
    protected static final int DAMAGE_COOLDOWN = 10; // Ticks between damage

    // Movement
    protected float moveSpeed = 4.3f; // Blocks per second
    protected float jumpStrength = 10.0f;

    // Look direction (for entities with AI)
    protected float lookYaw = 0;
    protected float lookPitch = 0;

    public LivingEntity(World world, double x, double y, double z) {
        super(world, x, y, z);
        this.maxHealth = 20.0f;
        this.health = maxHealth;
    }

    @Override
    public void tick(double deltaTime) {
        super.tick(deltaTime);

        // Decrease invulnerability
        if (invulnerableTicks > 0) {
            invulnerableTicks--;
        }

        // Death handling
        if (health <= 0 && !dead) {
            onDeath();
        }
    }

    /**
     * Damage this entity
     * @return true if damage was applied
     */
    public boolean damage(float amount) {
        if (dead || invulnerableTicks > 0) return false;

        health -= amount;
        invulnerableTicks = DAMAGE_COOLDOWN;

        onDamage(amount);

        if (health <= 0) {
            health = 0;
            onDeath();
        }

        return true;
    }

    /**
     * Heal this entity
     */
    public void heal(float amount) {
        if (dead) return;
        health = Math.min(health + amount, maxHealth);
    }

    /**
     * Move in the direction the entity is facing
     */
    public void moveForward(float distance) {
        double dx = Math.sin(lookYaw) * distance;
        double dz = Math.cos(lookYaw) * distance;
        vx += dx;
        vz += dz;
    }

    /**
     * Strafe (move sideways)
     */
    public void strafe(float distance) {
        double dx = Math.cos(lookYaw) * distance;
        double dz = -Math.sin(lookYaw) * distance;
        vx += dx;
        vz += dz;
    }

    /**
     * Jump
     */
    public void jump() {
        if (onGround) {
            vy = jumpStrength;
            onGround = false;
        }
    }

    /**
     * Set look direction
     */
    public void setLookDirection(float yaw, float pitch) {
        this.lookYaw = yaw;
        this.lookPitch = pitch;
    }

    /**
     * Get look direction as unit vector
     */
    public double[] getLookDirection() {
        double cosPitch = Math.cos(lookPitch);
        double sinPitch = Math.sin(lookPitch);
        double cosYaw = Math.cos(lookYaw);
        double sinYaw = Math.sin(lookYaw);

        return new double[]{
                sinYaw * cosPitch,
                -sinPitch,
                cosYaw * cosPitch
        };
    }

    // Getters/Setters
    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = Math.max(0, Math.min(health, maxHealth));
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
        this.health = Math.min(health, maxHealth);
    }

    public boolean isDead() {
        return dead;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float speed) {
        this.moveSpeed = speed;
    }

    public float getLookYaw() {
        return lookYaw;
    }

    public float getLookPitch() {
        return lookPitch;
    }

    // Events to override

    /**
     * Called when entity takes damage
     */
    protected void onDamage(float amount) {
        // Override in subclasses for damage effects
    }

    /**
     * Called when entity dies
     */
    protected void onDeath() {
        dead = true;
        // Override in subclasses for death behavior
    }

    /**
     * Called when entity respawns
     */
    protected void onRespawn() {
        dead = false;
        health = maxHealth;
        invulnerableTicks = 40; // Brief invulnerability on respawn
    }
}