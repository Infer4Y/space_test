package xyz.ignite4inferneo.space_test.client;

import xyz.ignite4inferneo.space_test.client.input.KeyBindings;
import xyz.ignite4inferneo.space_test.client.input.KeyInput;
import xyz.ignite4inferneo.space_test.client.input.MouseInput;
import xyz.ignite4inferneo.space_test.client.renderer.Renderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Window {
    private JFrame displayWindow;
    private Renderer renderer = new Renderer();
    private java.util.Queue<Integer> fpsHistory = new LinkedList<>();
    private int historySize = 100;
    private AtomicInteger fpsEstimater = new AtomicInteger();
    private long lastUpdateTime = System.currentTimeMillis();
    private long lastFrameTime = System.nanoTime();

    public Window() {
        displayWindow = new JFrame("Space Test - Voxel Renderer");
        displayWindow.setSize(ClientSettings.windowSize);
        displayWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        displayWindow.setVisible(true);
        displayWindow.createBufferStrategy(3);
        displayWindow.setFocusable(true);
        displayWindow.requestFocus();
        displayWindow.setLocationRelativeTo(null);

        // Initialize mouse input
        MouseInput.init(displayWindow);

        // Start with mouse locked for FPS controls
        MouseInput.setMouseLocked(true);

        renderer.setCanvasSize(displayWindow.getSize());

        Timer renderTimer = new Timer(1, e -> {
            // Calculate delta time
            long now = System.nanoTime();
            double deltaTime = (now - lastFrameTime) / 1_000_000_000.0;
            lastFrameTime = now;

            // Handle input
            handleInput(deltaTime);

            renderer.setCanvasSize(displayWindow.getSize());

            BufferStrategy bufferStrategy = displayWindow.getBufferStrategy();
            if (bufferStrategy == null) return;

            Graphics2D graphics = (Graphics2D) bufferStrategy.getDrawGraphics();
            if (graphics == null) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime >= 1000) {
                updateAverageFPS();
                lastUpdateTime = currentTime;
            }

            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, displayWindow.getWidth(), displayWindow.getHeight());

            if (renderer != null) {
                renderer.render(deltaTime);
                graphics.drawImage(renderer.getScreenBuffer(), 0, 0, null);
            }

            // Draw UI
            if (ClientSettings.showFPS) {
                graphics.setColor(Color.WHITE);
                graphics.drawString("FPS: " + String.format("%.1f", getAverageFPS()), 10, 80);

                String mouseLockStatus = MouseInput.isMouseLocked() ? "LOCKED" : "UNLOCKED";
                graphics.drawString("Mouse: " + mouseLockStatus + " (ESC to toggle)", 10, 100);
                graphics.drawString("Pixel Scale: (+/- to change)", 10, 120);
                graphics.drawString("WASD: Move | Mouse: Look | Q/Shift: Up/Down | G: Toggle Greedy Mesh", 10, 140);
            }

            graphics.dispose();
            bufferStrategy.show();

            fpsEstimater.incrementAndGet();

            // Clear one-frame input states
            KeyInput.endFrame();
            MouseInput.endFrame();
        });

        renderTimer.start();
    }

    private void handleInput(double deltaTime) {
        double moveSpeed = ClientSettings.MOVE_SPEED;
        double lookSpeed = ClientSettings.LOOK_SPEED * ClientSettings.MOUSE_SENSITIVITY;

        // Toggle mouse lock
        if (KeyBindings.TOGGLE_MOUSE_LOCK.isPressed()) {
            MouseInput.setMouseLocked(!MouseInput.isMouseLocked());
        }

        // Movement
        double forward = 0, strafe = 0, vertical = 0;

        if (KeyBindings.MOVE_FORWARD.isDown()) forward += moveSpeed;
        if (KeyBindings.MOVE_BACK.isDown()) forward -= moveSpeed;
        if (KeyBindings.MOVE_RIGHT.isDown()) strafe += moveSpeed;
        if (KeyBindings.MOVE_LEFT.isDown()) strafe -= moveSpeed;
        if (KeyBindings.MOVE_UP.isDown()) vertical += moveSpeed;
        if (KeyBindings.MOVE_DOWN.isDown()) vertical -= moveSpeed;

        if (forward != 0 || strafe != 0 || vertical != 0) {
            renderer.getChunkRenderer().move(forward, strafe, vertical);
        }

        // Camera rotation
        if (MouseInput.isMouseLocked()) {
            int dx = MouseInput.getMouseDX();
            int dy = MouseInput.getMouseDY();

            if (dx != 0 || dy != 0) {
                renderer.getChunkRenderer().rotate(dx * lookSpeed, dy * lookSpeed);
            }
        }

        if (KeyBindings.TOGGLE_FPS.isPressed()) {
            ClientSettings.showFPS = !ClientSettings.showFPS;
        }
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