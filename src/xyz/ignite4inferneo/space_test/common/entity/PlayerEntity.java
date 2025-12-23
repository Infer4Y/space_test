package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Enhanced Player Entity - Multiplayer Ready
 *
 * Features:
 * - Proper physics with ground friction and air resistance
 * - Smooth interpolation for remote players
 * - Network state serialization
 * - Improved collision detection
 * - Sprint/sneak mechanics
 */
public class PlayerEntity extends LivingEntity {

    // Identity
    private final String username;
    private final Inventory inventory;

    // Multiplayer state
    private boolean isLocalPlayer = false;
    private long lastNetworkUpdate = 0;
    private static final long NETWORK_UPDATE_INTERVAL = 50; // ms

    // Interpolation for remote players
    private double prevX, prevY, prevZ;
    private float prevYaw, prevPitch;
    private double targetX, targetY, targetZ;
    private float targetYaw, targetPitch;
    private float interpolationAlpha = 0;
    private static final float INTERPOLATION_SPEED = 10.0f;

    // Movement state
    private boolean sprinting = false;
    private boolean sneaking = false;
    private float sprintMultiplier = 1.5f; // Increased from 1.3 for better sprint feel
    private float sneakMultiplier = 0.3f;

    // Stamina system
    private float stamina = 100.0f;
    private float maxStamina = 100.0f;
    private static final float SPRINT_DRAIN = 10.0f; // per second
    private static final float STAMINA_REGEN = 20.0f; // per second

    // Physics constants - tuned for responsive feel
    private static final double GROUND_FRICTION = 0.88;
    private static final double AIR_FRICTION = 0.98;
    private static final double GROUND_ACCELERATION = 0.25; // Increased for snappier movement
    private static final double AIR_ACCELERATION = 0.04;

    // Stats tracking
    private double totalDistanceMoved = 0;
    private int jumpCount = 0;
    private long playTime = 0;

    public PlayerEntity(World world, double x, double y, double z, String username) {
        super(world, x, y, z);
        this.username = username;
        this.inventory = new Inventory();

        // Player stats
        this.maxHealth = 20.0f;
        this.health = maxHealth;
        this.moveSpeed = 8.5f; // Increased for better feel
        this.jumpStrength = 10.0f;

        // Collision box
        this.width = 0.6;
        this.height = 1.8;
    }

    /**
     * Create local player instance
     */
    public static PlayerEntity createLocal(World world, double x, double y, double z, String username) {
        PlayerEntity player = new PlayerEntity(world, x, y, z, username);
        player.isLocalPlayer = true;
        return player;
    }

    @Override
    protected void onTick(double deltaTime) {
        playTime++;

        if (isLocalPlayer) {
            updateLocalPlayer(deltaTime);
        } else {
            updateRemotePlayer(deltaTime);
        }

        // Stamina regeneration
        if (!sprinting && stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + STAMINA_REGEN * (float)deltaTime);
        }

        // Track distance
        double dist = Math.sqrt(vx*vx + vz*vz);
        totalDistanceMoved += dist * deltaTime;
    }

    /**
     * Update for local player with improved physics
     */
    private void updateLocalPlayer(double deltaTime) {
        // Only apply friction when not actively moving
        // This allows input to override friction for responsive controls

        // Apply sneak speed reduction
        if (sneaking && onGround) {
            vx *= sneakMultiplier;
            vz *= sneakMultiplier;
        }

        // Drain stamina while sprinting
        if (sprinting && (Math.abs(vx) > 0.1 || Math.abs(vz) > 0.1)) {
            stamina -= SPRINT_DRAIN * (float)deltaTime;
            if (stamina <= 0) {
                stamina = 0;
                sprinting = false;
            }
        }
    }

    /**
     * Update for remote players with smooth interpolation
     */
    private void updateRemotePlayer(double deltaTime) {
        // Interpolate position smoothly
        if (interpolationAlpha < 1.0f) {
            interpolationAlpha += INTERPOLATION_SPEED * (float)deltaTime;
            interpolationAlpha = Math.min(1.0f, interpolationAlpha);

            // Smooth movement
            x = prevX + (targetX - prevX) * interpolationAlpha;
            y = prevY + (targetY - prevY) * interpolationAlpha;
            z = prevZ + (targetZ - prevZ) * interpolationAlpha;

            // Smooth rotation
            lookYaw = prevYaw + (targetYaw - prevYaw) * interpolationAlpha;
            lookPitch = prevPitch + (targetPitch - prevPitch) * interpolationAlpha;
        }
    }

    /**
     * Apply movement input (local player only)
     */
    public void applyMovementInput(double forward, double strafe, double deltaTime) {
        if (!isLocalPlayer || dead) return;

        // Direct velocity approach for more responsive movement
        double moveYaw = lookYaw;
        double dx = Math.sin(moveYaw) * forward + Math.cos(moveYaw) * strafe;
        double dz = Math.cos(moveYaw) * forward - Math.sin(moveYaw) * strafe;

        // Normalize diagonal movement
        double len = Math.sqrt(dx*dx + dz*dz);
        if (len > 0) {
            dx /= len;
            dz /= len;

            // Calculate target speed
            double targetSpeed = moveSpeed;
            if (sprinting && stamina > 0 && onGround) {
                targetSpeed *= sprintMultiplier;
            }
            if (sneaking) {
                targetSpeed *= sneakMultiplier;
            }

            // Set velocity directly (much more responsive)
            vx = dx * targetSpeed;
            vz = dz * targetSpeed;
        } else {
            // No input - apply friction to slow down
            if (onGround) {
                vx *= 0.5; // Stop quickly on ground
                vz *= 0.5;
            } else {
                vx *= 0.95; // Drift in air
                vz *= 0.95;
            }
        }
    }

    /**
     * Enhanced jump with stamina cost
     */
    @Override
    public void jump() {
        if (!onGround || dead) return;

        vy = jumpStrength;
        onGround = false;
        jumpCount++;

        // Jumping costs stamina
        stamina = Math.max(0, stamina - 5.0f);
    }

    /**
     * Set sprint state
     */
    public void setSprinting(boolean sprinting) {
        if (!isLocalPlayer) return;

        // Can only sprint if have stamina and not sneaking
        if (sprinting && stamina > 10.0f && !sneaking) {
            this.sprinting = true;
        } else {
            this.sprinting = false;
        }
    }

    /**
     * Set sneak state
     */
    public void setSneaking(boolean sneaking) {
        if (!isLocalPlayer) return;

        this.sneaking = sneaking;

        // Can't sprint while sneaking
        if (sneaking) {
            sprinting = false;
        }

        // Sneaking changes hitbox height
        if (sneaking) {
            height = 1.5;
        } else {
            height = 1.8;
        }
    }

    /**
     * Set target position for network interpolation
     */
    public void setNetworkPosition(double x, double y, double z, float yaw, float pitch) {
        if (isLocalPlayer) return; // Don't update local player from network

        // Store previous position
        prevX = this.x;
        prevY = this.y;
        prevZ = this.z;
        prevYaw = this.lookYaw;
        prevPitch = this.lookPitch;

        // Set new target
        targetX = x;
        targetY = y;
        targetZ = z;
        targetYaw = yaw;
        targetPitch = pitch;

        // Reset interpolation
        interpolationAlpha = 0;
    }

    /**
     * Get camera position (eye level)
     */
    public double[] getCameraPosition() {
        double eyeHeight = sneaking ? 1.54 : 1.62;
        return new double[]{x, y + eyeHeight, z};
    }

    /**
     * Get feet position
     */
    public double[] getFeetPosition() {
        return new double[]{x, y, z};
    }

    /**
     * Check if player should send network update
     */
    public boolean shouldSendNetworkUpdate() {
        if (!isLocalPlayer) return false;

        long now = System.currentTimeMillis();
        if (now - lastNetworkUpdate >= NETWORK_UPDATE_INTERVAL) {
            lastNetworkUpdate = now;
            return true;
        }
        return false;
    }

    /**
     * Serialize player state for network
     */
    public PlayerState getNetworkState() {
        return new PlayerState(
                uuid,
                x, y, z,
                vx, vy, vz,
                lookYaw, lookPitch,
                health, stamina,
                onGround, sprinting, sneaking
        );
    }

    /**
     * Apply state from network
     */
    public void applyNetworkState(PlayerState state) {
        if (isLocalPlayer) return;

        setNetworkPosition(state.x, state.y, state.z, state.yaw, state.pitch);
        this.health = state.health;
        this.stamina = state.stamina;
        this.sprinting = state.sprinting;
        this.sneaking = state.sneaking;
    }

    @Override
    protected void onDeath() {
        super.onDeath();

        // Drop inventory items
        // TODO: Spawn item entities

        // Reset stats
        sprinting = false;
        sneaking = false;
    }

    /**
     * Respawn at position
     */
    public void respawn(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = 0;
        this.vy = 0;
        this.vz = 0;
        this.stamina = maxStamina;
        onRespawn();
    }

    // Getters
    public String getUsername() { return username; }
    public Inventory getInventory() { return inventory; }
    public boolean isLocalPlayer() { return isLocalPlayer; }
    public boolean isSprinting() { return sprinting; }
    public boolean isSneaking() { return sneaking; }
    public float getStamina() { return stamina; }
    public float getMaxStamina() { return maxStamina; }
    public double getTotalDistanceMoved() { return totalDistanceMoved; }
    public int getJumpCount() { return jumpCount; }
    public long getPlayTime() { return playTime; }

    @Override
    public String getType() {
        return "player";
    }

    /**
     * Network-serializable player state
     */
    public static class PlayerState {
        public final java.util.UUID playerId;
        public final double x, y, z;
        public final double vx, vy, vz;
        public final float yaw, pitch;
        public final float health, stamina;
        public final boolean onGround, sprinting, sneaking;

        public PlayerState(java.util.UUID playerId, double x, double y, double z,
                           double vx, double vy, double vz,
                           float yaw, float pitch,
                           float health, float stamina,
                           boolean onGround, boolean sprinting, boolean sneaking) {
            this.playerId = playerId;
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.yaw = yaw; this.pitch = pitch;
            this.health = health;
            this.stamina = stamina;
            this.onGround = onGround;
            this.sprinting = sprinting;
            this.sneaking = sneaking;
        }
    }
}