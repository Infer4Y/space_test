package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.world.World;

import java.util.UUID;

/**
 * Base class for all entities (mobs, dropped items, projectiles, etc.)
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

        // Drag
        vx *= 0.98;
        vz *= 0.98;

        if (onGround) {
            vx *= 0.8;
            vz *= 0.8;
        }

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
        x += dx;
        if (checkCollision()) {
            x -= dx;
            vx = 0;
        }

        y += dy;
        if (checkCollision()) {
            y -= dy;
            if (dy > 0) vy = 0;
            if (dy < 0) {
                vy = 0;
                onGround = true;
            }
        }

        z += dz;
        if (checkCollision()) {
            z -= dz;
            vz = 0;
        }
    }

    protected boolean checkCollision() {
        double hw = width / 2;

        int minX = (int) Math.floor(x - hw);
        int maxX = (int) Math.floor(x + hw);
        int minY = (int) Math.floor(y);
        int maxY = (int) Math.floor(y + height);
        int minZ = (int) Math.floor(z - hw);
        int maxZ = (int) Math.floor(z + hw);

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

    public double[] getBoundingBox() {
        double hw = width / 2;
        return new double[]{
                x - hw, y, z - hw,
                x + hw, y + height, z + hw
        };
    }

    public double distanceTo(Entity other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
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

    protected abstract void onTick(double deltaTime);

    protected boolean isNoGravity() {
        return false;
    }

    public abstract String getType();
}