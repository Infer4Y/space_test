package xyz.ignite4inferneo.space_test.client;

import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;

/**
 * Client-side player controller for PlayerEntity
 * Simplified since PlayerEntity now handles most of the logic
 */
public class PlayerController {

    private final PlayerEntity player;

    // Camera angles (separate from player's look direction for client prediction)
    private double yaw = 0;
    private double pitch = 0;

    public PlayerController(PlayerEntity player) {
        this.player = player;
    }

    /**
     * Apply movement input to player
     */
    public void applyMovement(double forward, double strafe, double deltaTime) {
        player.applyMovementInput(forward, strafe, deltaTime);
    }

    /**
     * Apply camera rotation
     */
    public void rotate(double deltaYaw, double deltaPitch) {
        yaw += deltaYaw;
        pitch = Math.max(-Math.PI/2, Math.min(Math.PI/2, pitch + deltaPitch));

        // Update player look direction
        player.setLookDirection((float) yaw, (float) pitch);
    }

    /**
     * Make player jump
     */
    public void jump() {
        player.jump();
    }

    /**
     * Set sprint state
     */
    public void setSprinting(boolean sprinting) {
        player.setSprinting(sprinting);
    }

    /**
     * Set sneak state
     */
    public void setSneaking(boolean sneaking) {
        player.setSneaking(sneaking);
    }

    /**
     * Get camera position (from player's eyes)
     */
    public double[] getCameraPosition() {
        return player.getCameraPosition();
    }

    /**
     * Get camera direction (from player's look direction)
     */
    public double[] getCameraDirection() {
        return player.getLookDirection();
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public PlayerEntity getPlayer() {
        return player;
    }
}