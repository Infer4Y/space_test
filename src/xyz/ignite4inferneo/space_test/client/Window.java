package xyz.ignite4inferneo.space_test.client;

import xyz.ignite4inferneo.space_test.client.input.KeyBindings;
import xyz.ignite4inferneo.space_test.client.input.KeyInput;
import xyz.ignite4inferneo.space_test.client.input.MouseInput;
import xyz.ignite4inferneo.space_test.client.renderer.OptimizedRenderer;
import xyz.ignite4inferneo.space_test.common.entity.Player;
import xyz.ignite4inferneo.space_test.common.util.RayCast;
import xyz.ignite4inferneo.space_test.common.world.World;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main game window with physics-based player
 */
public class Window {
    private JFrame displayWindow;
    private OptimizedRenderer renderer;
    private World world;
    private Player player;

    private java.util.Queue<Integer> fpsHistory = new LinkedList<>();
    private int historySize = 100;
    private AtomicInteger fpsEstimater = new AtomicInteger();
    private long lastUpdateTime = System.currentTimeMillis();
    private long lastFrameTime = System.nanoTime();

    // Block interaction
    private RayCast.RaycastResult currentTarget = null;
    private String selectedBlock = "space_test:stone";

    // Camera
    private double yaw = 0;
    private double pitch = 0;

    public Window(World world) {
        this.world = world;

        // Create player at spawn position
        int[] spawnPos = world.getChunk(0, 0) != null ? new int[]{0, 70, 0} : new int[]{0, 70, 0};
        this.player = new Player(world, spawnPos[0], spawnPos[1], spawnPos[2]);

        displayWindow = new JFrame("Space Test - Voxel Engine [ALPHA]");
        displayWindow.setSize(ClientSettings.windowSize);
        displayWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        displayWindow.setVisible(true);
        displayWindow.createBufferStrategy(3);
        displayWindow.setFocusable(true);
        displayWindow.requestFocus();
        displayWindow.setLocationRelativeTo(null);

        renderer = new OptimizedRenderer(world);
        renderer.setCanvasSize(displayWindow.getSize());

        MouseInput.init(displayWindow);
        MouseInput.setMouseLocked(true);

        // Start render loop (60+ FPS)
        Timer renderTimer = new Timer(1, e -> {
            long now = System.nanoTime();
            double deltaTime = (now - lastFrameTime) / 1_000_000_000.0;
            lastFrameTime = now;

            handleInput(deltaTime);
            renderFrame();

            KeyInput.endFrame();
            MouseInput.endFrame();
        });
        renderTimer.start();

        // Start physics tick thread (60 TPS for smooth physics)
        Thread physicsThread = new Thread(() -> {
            long lastPhysicsTime = System.nanoTime();
            while (true) {
                try {
                    long now = System.nanoTime();
                    double deltaTime = (now - lastPhysicsTime) / 1_000_000_000.0;
                    lastPhysicsTime = now;

                    player.tick(deltaTime);

                    Thread.sleep(16); // ~60 physics updates per second
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        physicsThread.setDaemon(true);
        physicsThread.start();

        // Start game tick thread (20 TPS for world updates)
        Thread tickThread = new Thread(() -> {
            while (true) {
                try {
                    world.tick();
                    Thread.sleep(50); // 20 ticks per second
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        tickThread.setDaemon(true);
        tickThread.start();
    }

    private void handleInput(double deltaTime) {
        // Toggle mouse lock
        if (KeyBindings.TOGGLE_MOUSE_LOCK.isPressed()) {
            MouseInput.setMouseLocked(!MouseInput.isMouseLocked());
        }

        // Movement input (applies forces to player)
        double forward = 0, strafe = 0;

        if (KeyBindings.MOVE_FORWARD.isDown()) forward += 1;
        if (KeyBindings.MOVE_BACK.isDown()) forward -= 1;
        if (KeyBindings.MOVE_RIGHT.isDown()) strafe += 1;
        if (KeyBindings.MOVE_LEFT.isDown()) strafe -= 1;

        // Calculate movement direction based on camera yaw
        if (forward != 0 || strafe != 0) {
            // Normalize diagonal movement
            double length = Math.sqrt(forward * forward + strafe * strafe);
            forward /= length;
            strafe /= length;

            // Convert to world-space movement
            double moveSpeed = player.isOnGround() ? 30.0 : 10.0; // Faster on ground, slower in air
            double dx = Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
            double dz = Math.cos(yaw) * forward - Math.sin(yaw) * strafe;

            player.addMovement(dx, dz, moveSpeed * deltaTime);
        }

        // Jumping (only when on ground)
        if (KeyBindings.JUMP.isPressed()) {
            player.jump();
        }

        // Creative mode flight (SHIFT to go down, SPACE to go up)
        // Uncomment these for creative flying:
        /*
        if (KeyBindings.MOVE_UP.isDown()) {
            player.vy = 10;
        }
        if (KeyBindings.MOVE_DOWN.isDown()) {
            player.vy = -10;
        }
        */

        // Camera rotation (mouse)
        if (MouseInput.isMouseLocked()) {
            int dx = MouseInput.getMouseDX();
            int dy = MouseInput.getMouseDY();
            if (dx != 0 || dy != 0) {
                double lookSpeed = ClientSettings.MOUSE_SENSITIVITY;
                yaw += dx * lookSpeed;
                pitch = Math.max(-Math.PI/2, Math.min(Math.PI/2, pitch + dy * lookSpeed));
            }
        }

        // Update renderer camera to follow player
        double[] camPos = player.getCameraPosition();
        renderer.x = camPos[0];
        renderer.y = camPos[1];
        renderer.z = camPos[2];
        renderer.yaw = yaw;
        renderer.pitch = pitch;

        // Block interaction
        updateBlockTarget();

        if (MouseInput.isButtonPressed(0)) {
            breakBlock();
        }

        if (MouseInput.isButtonPressed(2)) {
            placeBlock();
        }

        // Cycle selected block (1-9 keys)
        for (int i = 0; i < 9; i++) {
            if (KeyInput.isPressed(49 + i)) {
                cycleBlock(i);
            }
        }

        // Toggle FPS
        if (KeyBindings.TOGGLE_FPS.isPressed()) {
            ClientSettings.showFPS = !ClientSettings.showFPS;
        }
    }

    private void updateBlockTarget() {
        double[] pos = player.getCameraPosition();
        double[] dir = getCameraDirection();

        currentTarget = RayCast.cast(world, pos[0], pos[1], pos[2],
                dir[0], dir[1], dir[2], 5.0);
    }

    private double[] getCameraDirection() {
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        return new double[]{
                sinYaw * cosPitch,
                -sinPitch,
                cosYaw * cosPitch
        };
    }

    private void breakBlock() {
        if (currentTarget != null && currentTarget.hit) {
            world.setBlock(currentTarget.x, currentTarget.y, currentTarget.z, "space_test:air");
            renderer.markChunkDirty(currentTarget.x >> 4, currentTarget.z >> 4);
        }
    }

    private void placeBlock() {
        if (currentTarget != null && currentTarget.hit) {
            int placeX = currentTarget.x + currentTarget.nx;
            int placeY = currentTarget.y + currentTarget.ny;
            int placeZ = currentTarget.z + currentTarget.nz;

            // Don't place if it would intersect player
            double[] pos = player.getCameraPosition();
            double dx = placeX + 0.5 - pos[0];
            double dy = placeY + 0.5 - pos[1];
            double dz = placeZ + 0.5 - pos[2];
            if (dx*dx + dy*dy + dz*dz < 2.0) return;

            world.setBlock(placeX, placeY, placeZ, selectedBlock);
            renderer.markChunkDirty(placeX >> 4, placeZ >> 4);
        }
    }

    private void cycleBlock(int slot) {
        String[] blocks = {"space_test:stone", "space_test:dirt", "space_test:grass",
                "space_test:wood", "space_test:leaves"};
        if (slot < blocks.length) {
            selectedBlock = blocks[slot];
        }
    }

    private void renderFrame() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= 1000) {
            updateAverageFPS();
            lastUpdateTime = currentTime;
        }

        renderer.setCanvasSize(displayWindow.getSize());

        BufferStrategy bufferStrategy = displayWindow.getBufferStrategy();
        if (bufferStrategy == null) return;

        Graphics2D graphics = (Graphics2D) bufferStrategy.getDrawGraphics();
        if (graphics == null) return;

        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, displayWindow.getWidth(), displayWindow.getHeight());

        if (renderer != null) {
            renderer.render();
            graphics.drawImage(renderer.getScreenBuffer(), 0, 0, null);
        }

        // Draw UI
        if (ClientSettings.showFPS) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Monospaced", Font.PLAIN, 12));

            graphics.drawString("FPS: " + String.format("%.1f", getAverageFPS()), 10, 20);

            double[] pos = player.getFeetPosition();
            graphics.drawString(String.format("Pos: %.1f, %.1f, %.1f", pos[0], pos[1], pos[2]), 10, 40);
            graphics.drawString("On Ground: " + player.isOnGround(), 10, 60);
            graphics.drawString("Velocity: " + String.format("%.1f", player.vy), 10, 80);

            if (currentTarget != null && currentTarget.hit) {
                graphics.drawString(String.format("Looking at: %d, %d, %d",
                        currentTarget.x, currentTarget.y, currentTarget.z), 10, 100);
            }

            graphics.drawString("Selected: " + selectedBlock, 10, 120);
        }

        // Crosshair
        int cx = displayWindow.getWidth() / 2;
        int cy = displayWindow.getHeight() / 2;
        graphics.setColor(Color.WHITE);
        graphics.drawLine(cx - 10, cy, cx + 10, cy);
        graphics.drawLine(cx, cy - 10, cx, cy + 10);

        if (currentTarget != null && currentTarget.hit) {
            graphics.setColor(new Color(255, 255, 255, 100));
            graphics.fillRect(cx - 2, cy - 2, 4, 4);
        }

        graphics.dispose();
        bufferStrategy.show();

        fpsEstimater.incrementAndGet();
    }

    private void updateAverageFPS() {
        int currentFPS = fpsEstimater.getAndSet(0);
        fpsHistory.add(currentFPS);
        if (fpsHistory.size() > historySize) {
            fpsHistory.poll();
        }
    }

    private double getAverageFPS() {
        return fpsHistory.stream().mapToInt(i -> i).average().orElse(0);
    }

    public JFrame getDisplayWindow() {
        return displayWindow;
    }
}