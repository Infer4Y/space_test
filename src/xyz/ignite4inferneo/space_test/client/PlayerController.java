package xyz.ignite4inferneo.space_test.client;

import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;

/**
 * Client-side player controller
 * Handles local player input and camera
 */
public class PlayerController {

    private final PlayerEntity player;

    // Camera
    private double yaw = 0;
    private double pitch = 0;

    public PlayerController(PlayerEntity player) {
        this.player = player;
    }

    /**
     * Apply movement input
     */
    public void applyMovement(boolean forward, boolean back, boolean left, boolean right, double deltaTime) {
        player.applyMovementInput(forward, back, left, right, deltaTime);
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
     * Get camera position
     */
    public double[] getCameraPosition() {
        return player.getCameraPosition();
    }

    /**
     * Get camera direction
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