package xyz.ignite4inferneo.space_test.client;

import xyz.ignite4inferneo.space_test.client.gui.InventoryInteraction;
import xyz.ignite4inferneo.space_test.client.gui.InventoryRenderer;
import xyz.ignite4inferneo.space_test.client.input.KeyBindings;
import xyz.ignite4inferneo.space_test.client.input.KeyInput;
import xyz.ignite4inferneo.space_test.client.input.MouseInput;
import xyz.ignite4inferneo.space_test.client.renderer.ThreadedOptimizedRenderer;
import xyz.ignite4inferneo.space_test.common.entity.Player;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;
import xyz.ignite4inferneo.space_test.common.util.RayCast;
import xyz.ignite4inferneo.space_test.common.world.World;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main game window with multi-threaded rendering
 */
public class Window {
    private JFrame displayWindow;
    private ThreadedOptimizedRenderer renderer;
    private World world;
    private Player player;
    private Inventory inventory;
    private InventoryInteraction inventoryInteraction;

    private java.util.Queue<Integer> fpsHistory = new LinkedList<>();
    private int historySize = 100;
    private AtomicInteger fpsEstimater = new AtomicInteger();
    private long lastUpdateTime = System.currentTimeMillis();
    private long lastFrameTime = System.nanoTime();

    // Block interaction
    private RayCast.RaycastResult currentTarget = null;

    // Camera
    private double yaw = 0;
    private double pitch = 0;

    // Performance tracking
    private long renderTimeMs = 0;

    // UI state
    private boolean inventoryOpen = false;

    public Window(World world) {
        this.world = world;
        this.inventory = new Inventory();
        this.inventoryInteraction = new InventoryInteraction();

        // Give player some starting items
        inventory.addItem("space_test:stone", 64);
        inventory.addItem("space_test:dirt", 64);
        inventory.addItem("space_test:grass", 32);
        inventory.addItem("space_test:wood", 16);

        // Create player at spawn position
        int[] spawnPos = world.getChunk(0, 0) != null ? new int[]{0, 70, 0} : new int[]{0, 70, 0};
        this.player = new Player(world, spawnPos[0], spawnPos[1], spawnPos[2]);

        displayWindow = new JFrame("Space Test - Voxel Engine [MULTITHREADED]");
        displayWindow.setSize(ClientSettings.windowSize);
        displayWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        displayWindow.setVisible(true);
        displayWindow.createBufferStrategy(3);
        displayWindow.setFocusable(true);
        displayWindow.requestFocus();
        displayWindow.setLocationRelativeTo(null);

        // Use threaded renderer
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("[Window] Creating threaded renderer with " + threads + " threads");
        renderer = new ThreadedOptimizedRenderer(world, threads);
        renderer.setCanvasSize(displayWindow.getSize());

        // Preload chunks around spawn
        System.out.println("[Window] Preloading chunks around player...");
        renderer.preloadChunksAround(spawnPos[0], spawnPos[2], 4);

        MouseInput.init(displayWindow);
        MouseInput.setMouseLocked(true);

        // Shutdown hook for clean thread pool shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Window] Shutting down renderer...");
            renderer.shutdown();
        }));

        // Start render loop (60+ FPS)
        Timer renderTimer = new Timer(1, e -> {
            long now = System.nanoTime();
            double deltaTime = (now - lastFrameTime) / 1_000_000_000.0;
            lastFrameTime = now;

            handleInput(deltaTime);

            long renderStart = System.currentTimeMillis();
            renderFrame();
            renderTimeMs = System.currentTimeMillis() - renderStart;

            KeyInput.endFrame();
            MouseInput.endFrame();
        });
        renderTimer.start();

        // Start physics tick thread (60 TPS)
        Thread physicsThread = new Thread(() -> {
            long lastPhysicsTime = System.nanoTime();
            while (true) {
                try {
                    long now = System.nanoTime();
                    double deltaTime = (now - lastPhysicsTime) / 1_000_000_000.0;
                    lastPhysicsTime = now;

                    player.tick(deltaTime);

                    Thread.sleep(16);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        physicsThread.setDaemon(true);
        physicsThread.start();

        // Start game tick thread (20 TPS)
        Thread tickThread = new Thread(() -> {
            while (true) {
                try {
                    world.tick();
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        tickThread.setDaemon(true);
        tickThread.start();
    }

    private void handleInput(double deltaTime) {
        // Toggle inventory
        if (KeyBindings.INVENTORY.isPressed()) {
            inventoryOpen = !inventoryOpen;
            MouseInput.setMouseLocked(!inventoryOpen);

            if (!inventoryOpen) {
                // Return held item when closing inventory
                inventoryInteraction.returnHeldItem(inventory);
            }
        }

        // Handle inventory interactions
        if (inventoryOpen) {
            // Update mouse position for cursor rendering
            inventoryInteraction.updateMousePosition(MouseInput.getMouseX(), MouseInput.getMouseY());

            // Handle clicks in inventory
            if (MouseInput.isButtonPressed(0)) { // Left click
                inventoryInteraction.handleClick(inventory,
                        displayWindow.getWidth(), displayWindow.getHeight(),
                        MouseInput.getMouseX(), MouseInput.getMouseY(), false);
            }
            if (MouseInput.isButtonPressed(2)) { // Right click
                inventoryInteraction.handleClick(inventory,
                        displayWindow.getWidth(), displayWindow.getHeight(),
                        MouseInput.getMouseX(), MouseInput.getMouseY(), true);
            }

            return; // Skip game controls when inventory is open
        }

        // Toggle mouse lock
        if (KeyBindings.TOGGLE_MOUSE_LOCK.isPressed()) {
            MouseInput.setMouseLocked(!MouseInput.isMouseLocked());
        }

        // Movement input
        double forward = 0, strafe = 0;

        if (KeyBindings.MOVE_FORWARD.isDown()) forward += 1;
        if (KeyBindings.MOVE_BACK.isDown()) forward -= 1;
        if (KeyBindings.MOVE_RIGHT.isDown()) strafe += 1;
        if (KeyBindings.MOVE_LEFT.isDown()) strafe -= 1;

        if (forward != 0 || strafe != 0) {
            double length = Math.sqrt(forward * forward + strafe * strafe);
            forward /= length;
            strafe /= length;

            double moveSpeed = player.isOnGround() ? 30.0 : 10.0;
            double dx = Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
            double dz = Math.cos(yaw) * forward - Math.sin(yaw) * strafe;

            player.addMovement(dx, dz, moveSpeed * deltaTime);
        }

        // Jumping
        if (KeyBindings.JUMP.isPressed()) {
            player.jump();
        }

        // Camera rotation
        if (MouseInput.isMouseLocked()) {
            int dx = MouseInput.getMouseDX();
            int dy = MouseInput.getMouseDY();
            if (dx != 0 || dy != 0) {
                double lookSpeed = ClientSettings.MOUSE_SENSITIVITY;
                yaw += dx * lookSpeed;
                pitch = Math.max(-Math.PI/2, Math.min(Math.PI/2, pitch + dy * lookSpeed));
            }
        }

        // Update renderer camera
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

        // Cycle selected block
        for (int i = 0; i < 9; i++) {
            if (KeyInput.isPressed(49 + i)) {
                inventory.setSelectedSlot(i);
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
            String blockId = world.getBlock(currentTarget.x, currentTarget.y, currentTarget.z);

            // Don't break air
            if (blockId.equals("space_test:air")) return;

            // Add to inventory
            inventory.addItem(blockId, 1);

            // Remove block
            world.setBlock(currentTarget.x, currentTarget.y, currentTarget.z, "space_test:air");
            renderer.markChunkDirty(currentTarget.x >> 4, currentTarget.z >> 4);
        }
    }

    private void placeBlock() {
        if (currentTarget != null && currentTarget.hit) {
            ItemStack selectedStack = inventory.getSelectedStack();
            if (selectedStack.isEmpty()) return;

            int placeX = currentTarget.x + currentTarget.nx;
            int placeY = currentTarget.y + currentTarget.ny;
            int placeZ = currentTarget.z + currentTarget.nz;

            double[] pos = player.getCameraPosition();
            double dx = placeX + 0.5 - pos[0];
            double dy = placeY + 0.5 - pos[1];
            double dz = placeZ + 0.5 - pos[2];
            if (dx*dx + dy*dy + dz*dz < 2.0) return;

            // Place block and remove from inventory
            world.setBlock(placeX, placeY, placeZ, selectedStack.getBlockId());
            inventory.removeItem(selectedStack.getBlockId(), 1);
            renderer.markChunkDirty(placeX >> 4, placeZ >> 4);
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

        // Draw UI with threading stats
        if (ClientSettings.showFPS) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Monospaced", Font.PLAIN, 12));

            int y = 41;
            graphics.drawString("FPS: " + String.format("%.1f", getAverageFPS()), 10, y);
            y += 20;
            graphics.drawString("Render Time: " + renderTimeMs + "ms", 10, y);
            y += 20;

            double[] pos = player.getFeetPosition();
            graphics.drawString(String.format("Pos: %.1f, %.1f, %.1f", pos[0], pos[1], pos[2]), 10, y);
            y += 20;
            graphics.drawString("On Ground: " + player.isOnGround(), 10, y);
            y += 20;

            // Threading stats
            int rendered = renderer.getChunksRendered();
            int meshing = renderer.getChunksMeshing();
            int cached = renderer.getCachedMeshCount();
            int subdivided = renderer.getQuadsSubdivided();
            int quadsRendered = renderer.getQuadsRendered();
            int quadsCulled = renderer.getQuadsCulled();

            graphics.drawString("Chunks Rendered: " + rendered, 10, y);
            y += 20;
            graphics.drawString("Chunks Meshing: " + meshing, 10, y);
            y += 20;
            graphics.drawString("Cached Meshes: " + cached, 10, y);
            y += 20;

            graphics.drawString("Quads: " + quadsRendered + " rendered, " + quadsCulled + " culled", 10, y);
            y += 20;

            if (subdivided > 0) {
                graphics.setColor(new Color(255, 200, 0));
                graphics.drawString("Quads Subdivided: " + subdivided, 10, y);
                graphics.setColor(Color.WHITE);
                y += 20;
            }

            // Show loading indicator if chunks are being meshed
            if (meshing > 0) {
                graphics.setColor(new Color(255, 255, 0, 200));
                graphics.drawString("⚡ Loading chunks...", 10, y);
            } else if (cached < 100) {
                graphics.setColor(new Color(0, 255, 0, 200));
                graphics.drawString("✓ Ready", 10, y);
            }
            graphics.setColor(Color.WHITE);
            y += 20;

            graphics.drawString("CPU Cores: " + Runtime.getRuntime().availableProcessors(), 10, y);
            y += 20;

            if (currentTarget != null && currentTarget.hit) {
                graphics.drawString(String.format("Looking at: %d, %d, %d",
                        currentTarget.x, currentTarget.y, currentTarget.z), 10, y);
                y += 20;
            }

            ItemStack selected = inventory.getSelectedStack();
            if (!selected.isEmpty()) {
                graphics.drawString("Selected: " + selected, 10, y);
                y += 20;
            }

            // Inventory stats
            int emptySlots = inventory.getEmptySlots();
            graphics.drawString("Inventory: " + (Inventory.TOTAL_SIZE - emptySlots) + "/" + Inventory.TOTAL_SIZE, 10, y);
        }

        // Render inventory or hotbar
        if (inventoryOpen) {
            InventoryRenderer.renderInventory(graphics, inventory, displayWindow.getWidth(), displayWindow.getHeight());
            // Render held item on top
            inventoryInteraction.renderHeldItem(graphics);
        } else {
            InventoryRenderer.renderHotbar(graphics, inventory, displayWindow.getWidth(), displayWindow.getHeight());
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