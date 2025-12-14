package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Player entity with position, velocity, and collision box
 */
public class Player {
    // Position (camera is at eye level)
    public double x, y, z;

    // Velocity
    public double vx, vy, vz;

    // Collision box size (AABB - Axis-Aligned Bounding Box)
    public static final double WIDTH = 0.6;   // Player width (X and Z)
    public static final double HEIGHT = 1.8;  // Player height (Y)
    public static final double EYE_HEIGHT = 1.62; // Eye level from feet

    // Physics constants
    private static final double GRAVITY = -32.0;  // Blocks per second^2
    private static final double JUMP_VELOCITY = 10.0; // Blocks per second
    private static final double TERMINAL_VELOCITY = -78.4; // Max fall speed
    private static final double GROUND_DRAG = 10.0; // Friction when on ground
    private static final double AIR_DRAG = 2.0;     // Air resistance

    // State
    private boolean onGround = false;
    private boolean wasOnGround = false;

    private final World world;

    public Player(World world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = 0;
        this.vy = 0;
        this.vz = 0;
    }

    /**
     * Update physics - call this every frame
     * @param deltaTime Time since last update in seconds
     */
    public void tick(double deltaTime) {
        wasOnGround = onGround;

        // Apply gravity
        vy += GRAVITY * deltaTime;

        // Clamp to terminal velocity
        if (vy < TERMINAL_VELOCITY) {
            vy = TERMINAL_VELOCITY;
        }

        // Apply drag
        double drag = onGround ? GROUND_DRAG : AIR_DRAG;
        vx *= Math.max(0, 1.0 - drag * deltaTime);
        vz *= Math.max(0, 1.0 - drag * deltaTime);

        // Move and collide
        moveWithCollision(vx * deltaTime, vy * deltaTime, vz * deltaTime);

        // Check if on ground
        onGround = checkGround();

        // Reset vertical velocity if hit ground or ceiling
        if (onGround && vy < 0) {
            vy = 0;
        }
    }

    /**
     * Add movement input (call this before tick)
     */
    public void addMovement(double dx, double dz, double speed) {
        vx += dx * speed;
        vz += dz * speed;
    }

    /**
     * Try to jump
     */
    public void jump() {
        if (onGround) {
            vy = JUMP_VELOCITY;
            onGround = false;
        }
    }

    /**
     * Get camera position (at eye level)
     */
    public double[] getCameraPosition() {
        return new double[]{x, y + EYE_HEIGHT, z};
    }

    /**
     * Get feet position
     */
    public double[] getFeetPosition() {
        return new double[]{x, y, z};
    }

    /**
     * Check if player is on ground
     */
    private boolean checkGround() {
        // Check slightly below feet
        double checkY = y - 0.01;

        // Check all corners of the player's bounding box
        return checkCollision(x - WIDTH/2, checkY, z - WIDTH/2) ||
                checkCollision(x + WIDTH/2, checkY, z - WIDTH/2) ||
                checkCollision(x - WIDTH/2, checkY, z + WIDTH/2) ||
                checkCollision(x + WIDTH/2, checkY, z + WIDTH/2);
    }

    /**
     * Move with collision detection
     */
    private void moveWithCollision(double dx, double dy, double dz) {
        // Try to move in each axis separately for smooth sliding

        // Move X
        x += dx;
        if (checkCollisionBox()) {
            x -= dx; // Undo movement
            vx = 0;  // Stop horizontal velocity
        }

        // Move Y
        y += dy;
        if (checkCollisionBox()) {
            y -= dy;
            if (dy > 0) {
                // Hit ceiling
                vy = 0;
            } else {
                // Hit ground (will be detected by checkGround)
                vy = 0;
            }
        }

        // Move Z
        z += dz;
        if (checkCollisionBox()) {
            z -= dz;
            vz = 0;
        }
    }

    /**
     * Check if player's bounding box collides with any blocks
     */
    private boolean checkCollisionBox() {
        double hw = WIDTH / 2; // Half width

        // Get bounds of player box
        int minX = (int) Math.floor(x - hw);
        int maxX = (int) Math.floor(x + hw);
        int minY = (int) Math.floor(y);
        int maxY = (int) Math.floor(y + HEIGHT);
        int minZ = (int) Math.floor(z - hw);
        int maxZ = (int) Math.floor(z + hw);

        // Check all blocks in range
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (checkCollision(bx, by, bz)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if there's a solid block at position
     */
    private boolean checkCollision(double x, double y, double z) {
        return world.isSolid((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean justLanded() {
        return onGround && !wasOnGround;
    }
}