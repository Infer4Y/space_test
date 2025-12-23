package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.world.World;

import java.util.List;
import java.util.UUID;

/**
 * Enhanced Entity with better collision and interaction
 * This REPLACES the existing Entity class
 *
 * Features:
 * - Entity-entity collision
 * - Pushback physics
 * - Interaction system
 * - Better AABB collision
 */
public abstract class Entity {
    protected final UUID uuid;
    protected World world;

    // Position and velocity
    public double x, y, z;
    public double vx, vy, vz;

    // Rotation (yaw and pitch)
    public double yaw, pitch;

    // Bounding box
    protected double width = 0.6;
    protected double height = 1.8;

    // State
    protected boolean onGround = false;
    protected boolean removed = false;
    protected int ticksExisted = 0;

    // Physics constants
    protected static final double GRAVITY = -32.0;
    protected static final double TERMINAL_VELOCITY = -78.4;

    // NEW: Entity collision
    protected boolean hasEntityCollision = true;
    protected boolean canBePushed = true;
    protected float pushResistance = 1.0f; // 0 = no resistance, 1 = normal, >1 = heavy

    // NEW: Collision
    protected double collisionMargin = 0.001; // Prevents getting stuck

    // NEW: Interaction
    protected boolean canBeInteracted = true;
    protected float interactionRange = 3.0f;

    public Entity(World world, double x, double y, double z) {
        this.uuid = UUID.randomUUID();
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Update entity - called by EntityManager
     */
    public void tick(double deltaTime) {
        if (removed) return;

        ticksExisted++;

        // Apply gravity
        if (!isNoGravity()) {
            vy += GRAVITY * deltaTime;
            vy = Math.max(vy, TERMINAL_VELOCITY);
        }

        // Move
        move(vx * deltaTime, vy * deltaTime, vz * deltaTime);

        // No automatic drag here - let subclasses handle it
        // (PlayerEntity handles friction in applyMovementInput)

        // Ground check
        checkGround();

        // Reset Y velocity on ground
        if (onGround && vy < 0) {
            vy = 0;
        }

        // Custom behavior
        onTick(deltaTime);
    }

    protected void move(double dx, double dy, double dz) {
        // Store original deltas
        double origDx = dx;
        double origDy = dy;
        double origDz = dz;

        // Check entity collisions if enabled
        if (hasEntityCollision) {
            handleEntityCollisions(dx, dy, dz);
        }

        // Try X movement
        x += dx;
        if (checkBlockCollision()) {
            x -= dx;
            vx = 0;
            dx = 0;
        }

        // Try Y movement
        y += dy;
        if (checkBlockCollision()) {
            y -= dy;
            if (dy > 0) {
                vy = 0; // Hit ceiling
            } else {
                vy = 0; // Hit ground
                onGround = true;
            }
            dy = 0;
        }

        // Try Z movement
        z += dz;
        if (checkBlockCollision()) {
            z -= dz;
            vz = 0;
            dz = 0;
        }

        // Call movement callback
        onMoved(origDx, origDy, origDz, dx, dy, dz);
    }

    /**
     * Handle collisions with other entities
     */
    protected void handleEntityCollisions(double dx, double dy, double dz) {
        // Get nearby entities
        List<Entity> nearby = world.getEntityManager().getEntitiesNear(
                x, y, z, Math.max(width, height) + 5.0
        );

        for (Entity other : nearby) {
            if (other == this) continue;
            if (!other.hasEntityCollision) continue;
            if (other.isRemoved()) continue;

            // Check if we would collide after movement
            if (wouldCollideWith(other, dx, dy, dz)) {
                handleCollision(other);
            }
        }
    }

    /**
     * Check if entity would collide with another after movement
     */
    protected boolean wouldCollideWith(Entity other, double dx, double dy, double dz) {
        // Calculate future bounding boxes
        double x1 = x + dx - width/2;
        double y1 = y + dy;
        double z1 = z + dz - width/2;
        double x2 = x + dx + width/2;
        double y2 = y + dy + height;
        double z2 = z + dz + width/2;

        double ox1 = other.x - other.width/2;
        double oy1 = other.y;
        double oz1 = other.z - other.width/2;
        double ox2 = other.x + other.width/2;
        double oy2 = other.y + other.height;
        double oz2 = other.z + other.width/2;

        // AABB intersection test
        return x1 < ox2 && x2 > ox1 &&
                y1 < oy2 && y2 > oy1 &&
                z1 < oz2 && z2 > oz1;
    }

    /**
     * Handle collision with another entity
     */
    protected void handleCollision(Entity other) {
        if (!canBePushed) return;

        // Calculate push direction
        double dx = x - other.x;
        double dz = z - other.z;
        double dist = Math.sqrt(dx*dx + dz*dz);

        if (dist < 0.01) {
            // Entities are on top of each other, push in random direction
            dx = Math.random() - 0.5;
            dz = Math.random() - 0.5;
            dist = Math.sqrt(dx*dx + dz*dz);
        }

        // Normalize
        dx /= dist;
        dz /= dist;

        // Apply pushback based on combined masses
        double pushStrength = 0.05 / pushResistance;

        if (other.canBePushed) {
            // Both entities push each other
            double totalMass = pushResistance + other.pushResistance;
            double myPush = other.pushResistance / totalMass;
            double otherPush = pushResistance / totalMass;

            vx += dx * pushStrength * myPush;
            vz += dz * pushStrength * myPush;

            other.vx -= dx * pushStrength * otherPush;
            other.vz -= dz * pushStrength * otherPush;
        } else {
            // Only this entity gets pushed
            vx += dx * pushStrength;
            vz += dz * pushStrength;
        }

        // Callback for custom collision behavior
        onEntityCollision(other);
    }

    /**
     * Check collision with blocks using accurate AABB
     */
    protected boolean checkBlockCollision() {
        double hw = width / 2;

        // Get integer bounds with margin
        int minX = (int) Math.floor(x - hw - collisionMargin);
        int maxX = (int) Math.floor(x + hw + collisionMargin);
        int minY = (int) Math.floor(y);
        int maxY = (int) Math.floor(y + height + collisionMargin);
        int minZ = (int) Math.floor(z - hw - collisionMargin);
        int maxZ = (int) Math.floor(z + hw + collisionMargin);

        // Check all blocks in range
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (world.isSolid(bx, by, bz)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected void checkGround() {
        double checkY = y - 0.01;
        double hw = width / 2;

        onGround = world.isSolid((int)Math.floor(x - hw), (int)Math.floor(checkY), (int)Math.floor(z - hw)) ||
                world.isSolid((int)Math.floor(x + hw), (int)Math.floor(checkY), (int)Math.floor(z - hw)) ||
                world.isSolid((int)Math.floor(x - hw), (int)Math.floor(checkY), (int)Math.floor(z + hw)) ||
                world.isSolid((int)Math.floor(x + hw), (int)Math.floor(checkY), (int)Math.floor(z + hw));
    }

    /**
     * Interact with this entity
     * @return true if interaction was handled
     */
    public boolean interact(Entity interactor) {
        if (!canBeInteracted) return false;

        // Check range
        double dist = distanceTo(interactor);
        if (dist > interactionRange) return false;

        // Handle interaction
        return onInteract(interactor);
    }

    public double[] getBoundingBox() {
        double hw = width / 2;
        return new double[]{
                x - hw, y, z - hw,
                x + hw, y + height, z + hw
        };
    }

    /**
     * Check if point is inside entity's bounding box
     */
    public boolean containsPoint(double px, double py, double pz) {
        double hw = width / 2;
        return px >= x - hw && px <= x + hw &&
                py >= y && py <= y + height &&
                pz >= z - hw && pz <= z + hw;
    }

    public double distanceTo(Entity other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
     * Get distance to another entity (edge to edge)
     */
    public double getDistanceToEdge(Entity other) {
        double dist = distanceTo(other);
        double combinedRadius = (width + other.width) / 2;
        return Math.max(0, dist - combinedRadius);
    }

    /**
     * Check if entity is within interaction range
     */
    public boolean isInRangeOf(Entity other, double range) {
        return getDistanceToEdge(other) <= range;
    }

    public void remove() {
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    public UUID getUUID() {
        return uuid;
    }

    public World getWorld() {
        return world;
    }

    // Callbacks for subclasses to override

    /**
     * Called when entity collides with another entity
     */
    protected void onEntityCollision(Entity other) {
        // Override in subclasses
    }

    /**
     * Called when entity is interacted with
     * @return true if interaction was handled
     */
    protected boolean onInteract(Entity interactor) {
        // Override in subclasses
        return false;
    }

    /**
     * Called after entity moves
     */
    protected void onMoved(double requestedDx, double requestedDy, double requestedDz,
                           double actualDx, double actualDy, double actualDz) {
        // Override in subclasses
    }

    protected abstract void onTick(double deltaTime);

    protected boolean isNoGravity() {
        return false;
    }

    public abstract String getType();

    // Getters/Setters

    public boolean hasEntityCollision() { return hasEntityCollision; }
    public void setEntityCollision(boolean enabled) { this.hasEntityCollision = enabled; }

    public boolean canBePushed() { return canBePushed; }
    public void setCanBePushed(boolean can) { this.canBePushed = can; }

    public float getPushResistance() { return pushResistance; }
    public void setPushResistance(float resistance) { this.pushResistance = resistance; }

    public boolean canBeInteracted() { return canBeInteracted; }
    public void setCanBeInteracted(boolean can) { this.canBeInteracted = can; }

    public float getInteractionRange() { return interactionRange; }
    public void setInteractionRange(float range) { this.interactionRange = range; }

    // Commonly used getters
    public boolean isOnGround() { return onGround; }
    public int getTicksExisted() { return ticksExisted; }
}