package xyz.ignite4inferneo.space_test.common.entity;

import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.util.UUID;

/**
 * Player entity - multiplayer ready
 * Represents both local and remote players
 */
public class PlayerEntity extends LivingEntity {

    private final String username;
    private final Inventory inventory;

    // Multiplayer state
    private boolean isLocalPlayer = false;
    private long lastUpdateTime = System.currentTimeMillis();

    // Interpolation for smooth remote player movement
    private double prevX, prevY, prevZ;
    private float prevYaw, prevPitch;
    private double targetX, targetY, targetZ;
    private float targetYaw, targetPitch;
    private float interpolationAlpha = 0;

    // Player stats
    private int selectedHotbarSlot = 0;

    public PlayerEntity(World world, double x, double y, double z, String username) {
        super(world, x, y, z);
        this.username = username;
        this.inventory = new Inventory();

        // Player-specific settings
        this.maxHealth = 20.0f;
        this.health = maxHealth;
        this.moveSpeed = 4.3f;
        this.jumpStrength = 10.0f;
        this.width = 0.6;
        this.height = 1.8;
    }

    /**
     * Create local player
     */
    public static PlayerEntity createLocalPlayer(World world, double x, double y, double z, String username) {
        PlayerEntity player = new PlayerEntity(world, x, y, z, username);
        player.isLocalPlayer = true;
        return player;
    }

    @Override
    protected void onTick(double deltaTime) {
        // Remote players use interpolation
        if (!isLocalPlayer) {
            interpolateMovement(deltaTime);
        }

        // Custom player behavior here
    }

    /**
     * Apply movement input (for local player)
     */
    public void applyMovementInput(boolean forward, boolean back, boolean left, boolean right, double deltaTime) {
        if (!isLocalPlayer || dead) return;

        float moveAmount = moveSpeed * (float) deltaTime;

        if (forward) moveForward(moveAmount);
        if (back) moveForward(-moveAmount);
        if (right) strafe(moveAmount);
        if (left) strafe(-moveAmount);
    }

    /**
     * Set target position for interpolation (remote players)
     */
    public void setTargetPosition(double x, double y, double z, float yaw, float pitch) {
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.prevYaw = this.lookYaw;
        this.prevPitch = this.lookPitch;

        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.targetYaw = yaw;
        this.targetPitch = pitch;

        this.interpolationAlpha = 0;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Smooth interpolation for remote players
     */
    private void interpolateMovement(double deltaTime) {
        interpolationAlpha += (float) deltaTime * 10.0f; // Interpolate over ~100ms
        interpolationAlpha = Math.min(interpolationAlpha, 1.0f);

        // Smoothly move to target position
        x = prevX + (targetX - prevX) * interpolationAlpha;
        y = prevY + (targetY - prevY) * interpolationAlpha;
        z = prevZ + (targetZ - prevZ) * interpolationAlpha;

        // Smoothly rotate to target angles
        lookYaw = prevYaw + (targetYaw - prevYaw) * interpolationAlpha;
        lookPitch = prevPitch + (targetPitch - prevPitch) * interpolationAlpha;
    }

    /**
     * Get camera position (eye level)
     */
    public double[] getCameraPosition() {
        return new double[]{x, y + 1.62, z};
    }

    /**
     * Get feet position
     */
    public double[] getFeetPosition() {
        return new double[]{x, y, z};
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        // Drop inventory items
        // Play death animation
        // Show death screen for local player
    }

    /**
     * Respawn player
     */
    public void respawn(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = 0;
        this.vy = 0;
        this.vz = 0;
        onRespawn();
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean isLocalPlayer() {
        return isLocalPlayer;
    }

    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public void setSelectedHotbarSlot(int slot) {
        this.selectedHotbarSlot = slot;
    }

    @Override
    public String getType() {
        return "player";
    }

    /**
     * Serialize player state for network
     */
    public PlayerState getState() {
        return new PlayerState(
                uuid,
                x, y, z,
                vx, vy, vz,
                lookYaw, lookPitch,
                health,
                selectedHotbarSlot,
                onGround
        );
    }

    /**
     * Apply state from network
     */
    public void setState(PlayerState state) {
        if (isLocalPlayer) return; // Don't overwrite local player

        setTargetPosition(state.x, state.y, state.z, state.yaw, state.pitch);
        this.health = state.health;
        this.selectedHotbarSlot = state.selectedSlot;
        this.onGround = state.onGround;
    }

    /**
     * Network-serializable player state
     */
    public static class PlayerState {
        public final UUID playerId;
        public final double x, y, z;
        public final double vx, vy, vz;
        public final float yaw, pitch;
        public final float health;
        public final int selectedSlot;
        public final boolean onGround;

        public PlayerState(UUID playerId, double x, double y, double z,
                           double vx, double vy, double vz,
                           float yaw, float pitch, float health,
                           int selectedSlot, boolean onGround) {
            this.playerId = playerId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.yaw = yaw;
            this.pitch = pitch;
            this.health = health;
            this.selectedSlot = selectedSlot;
            this.onGround = onGround;
        }
    }
}