package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.world.World;
import xyz.ignite4inferneo.space_test.common.util.RayCast;

import java.util.List;

/**
 * Base class for projectile entities (arrows, fireballs, etc.)
 *
 * Features:
 * - Accurate trajectory simulation
 * - Entity hit detection
 * - Block collision
 * - Damage dealing
 */
public abstract class Projectile extends Entity {

    protected Entity shooter; // Who shot this projectile
    protected float damage = 2.0f;
    protected boolean hasGravity = true;
    protected int maxLifetime = 200; // 10 seconds at 20 TPS
    protected boolean stuckInBlock = false;
    protected int stuckTimer = 0;
    protected static final int MAX_STUCK_TIME = 1200; // 1 minute

    // Hit detection
    protected boolean canHitShooter = false;
    protected int canHitShooterAfter = 5; // ticks
    protected boolean piercing = false; // Can go through entities

    public Projectile(World world, double x, double y, double z, Entity shooter) {
        super(world, x, y, z);
        this.shooter = shooter;

        // Projectile properties
        this.width = 0.25;
        this.height = 0.25;
        this.hasEntityCollision = false; // Handled manually
    }

    @Override
    protected void onTick(double deltaTime) {
        if (stuckInBlock) {
            handleStuck(deltaTime);
            return;
        }

        // Apply gravity
        if (hasGravity) {
            vy += GRAVITY * deltaTime;
            vy = Math.max(vy, TERMINAL_VELOCITY);
        }

        // Store previous position for raycast
        double prevX = x;
        double prevY = y;
        double prevZ = z;

        // Move
        double dx = vx * deltaTime;
        double dy = vy * deltaTime;
        double dz = vz * deltaTime;

        x += dx;
        y += dy;
        z += dz;

        // Check for block collision
        if (checkBlockHit()) {
            onHitBlock();
            return;
        }

        // Check for entity hits along the path
        if (checkEntityHits(prevX, prevY, prevZ, x, y, z)) {
            return; // Hit something, projectile removed
        }

        // Update rotation based on velocity
        updateRotation();

        // Air resistance
        double speed = Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (speed > 0.01) {
            double drag = 0.99; // 1% slowdown per tick
            vx *= drag;
            vy *= drag;
            vz *= drag;
        }

        // Lifetime check
        ticksExisted++;
        if (ticksExisted > maxLifetime) {
            remove();
        }
    }

    /**
     * Handle projectile stuck in block
     */
    protected void handleStuck(double deltaTime) {
        stuckTimer++;

        // Remove after max stuck time
        if (stuckTimer > MAX_STUCK_TIME) {
            remove();
            return;
        }

        // Check if block is still there
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);

        if (!world.isSolid(bx, by, bz)) {
            // Block removed, start falling
            stuckInBlock = false;
            vy = 0;
        }
    }

    /**
     * Check for block collision
     */
    protected boolean checkBlockHit() {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);

        return world.isSolid(bx, by, bz);
    }

    /**
     * Check for entity hits along movement path
     */
    protected boolean checkEntityHits(double x1, double y1, double z1,
                                      double x2, double y2, double z2) {
        // Get entities near the path
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;
        double midZ = (z1 + z2) / 2;
        double dist = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) + (z2-z1)*(z2-z1));

        List<Entity> nearby = world.getEntityManager().getEntitiesNear(
                midX, midY, midZ, dist + 2.0
        );

        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : nearby) {
            if (entity == this) continue;
            if (entity == shooter && !canHitShooter) continue;
            if (entity.isRemoved()) continue;

            // Check if ray intersects entity's bounding box
            double[] box = entity.getBoundingBox();
            double hitDist = rayBoxIntersection(
                    x1, y1, z1,
                    x2 - x1, y2 - y1, z2 - z1,
                    box[0], box[1], box[2],
                    box[3], box[4], box[5]
            );

            if (hitDist >= 0 && hitDist < closestDist) {
                closest = entity;
                closestDist = hitDist;
            }
        }

        // Hit closest entity
        if (closest != null) {
            onHitEntity(closest);
            return true;
        }

        return false;
    }

    /**
     * Ray-box intersection test
     * Returns distance to intersection or -1 if no hit
     */
    protected double rayBoxIntersection(double ox, double oy, double oz,
                                        double dx, double dy, double dz,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        double tmin = (minX - ox) / dx;
        double tmax = (maxX - ox) / dx;

        if (tmin > tmax) {
            double tmp = tmin;
            tmin = tmax;
            tmax = tmp;
        }

        double tymin = (minY - oy) / dy;
        double tymax = (maxY - oy) / dy;

        if (tymin > tymax) {
            double tmp = tymin;
            tymin = tymax;
            tymax = tmp;
        }

        if (tmin > tymax || tymin > tmax) return -1;

        if (tymin > tmin) tmin = tymin;
        if (tymax < tmax) tmax = tymax;

        double tzmin = (minZ - oz) / dz;
        double tzmax = (maxZ - oz) / dz;

        if (tzmin > tzmax) {
            double tmp = tzmin;
            tzmin = tzmax;
            tzmax = tmp;
        }

        if (tmin > tzmax || tzmin > tmax) return -1;

        if (tzmin > tmin) tmin = tzmin;

        return tmin >= 0 ? tmin : -1;
    }

    /**
     * Update rotation based on velocity direction
     */
    protected void updateRotation() {
        double horizontalDist = Math.sqrt(vx * vx + vz * vz);

        if (horizontalDist > 0.01) {
            yaw = (float) Math.atan2(vx, vz);
            pitch = (float) Math.atan2(-vy, horizontalDist);
        }
    }

    /**
     * Called when projectile hits a block
     */
    protected void onHitBlock() {
        if (canStickInBlocks()) {
            stuckInBlock = true;
            vx = vy = vz = 0;
        } else {
            remove();
        }
    }

    /**
     * Called when projectile hits an entity
     */
    protected void onHitEntity(Entity entity) {
        // Deal damage if entity is living
        if (entity instanceof LivingEntity living) {
            living.damage(damage);
        }

        // Remove projectile unless piercing
        if (!piercing) {
            remove();
        }
    }

    /**
     * Launch projectile in direction
     */
    public void shoot(double dirX, double dirY, double dirZ, double speed) {
        // Normalize direction
        double len = Math.sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ);
        if (len > 0) {
            dirX /= len;
            dirY /= len;
            dirZ /= len;
        }

        // Set velocity
        vx = dirX * speed;
        vy = dirY * speed;
        vz = dirZ * speed;

        // Set rotation
        updateRotation();
    }

    /**
     * Launch from shooter's look direction
     */
    public void shootFromEntity(Entity shooter, double speed, double inaccuracy) {
        if (shooter instanceof LivingEntity living) {
            double[] dir = living.getLookDirection();

            // Add inaccuracy
            if (inaccuracy > 0) {
                dir[0] += (Math.random() - 0.5) * inaccuracy;
                dir[1] += (Math.random() - 0.5) * inaccuracy;
                dir[2] += (Math.random() - 0.5) * inaccuracy;
            }

            shoot(dir[0], dir[1], dir[2], speed);
        }
    }

    // Getters/Setters

    public Entity getShooter() { return shooter; }
    public float getDamage() { return damage; }
    public void setDamage(float damage) { this.damage = damage; }

    public boolean hasGravity() { return hasGravity; }
    public void setGravity(boolean gravity) { this.hasGravity = gravity; }

    public boolean isPiercing() { return piercing; }
    public void setPiercing(boolean piercing) { this.piercing = piercing; }

    public boolean isStuck() { return stuckInBlock; }

    /**
     * Override to control if projectile sticks in blocks
     */
    protected boolean canStickInBlocks() {
        return true;
    }

    @Override
    protected boolean isNoGravity() {
        return !hasGravity;
    }
}