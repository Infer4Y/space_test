package xyz.ignite4inferneo.space_test.client;

import xyz.ignite4inferneo.space_test.client.gui.GUIManager;
import xyz.ignite4inferneo.space_test.client.gui.InventoryRenderer;
import xyz.ignite4inferneo.space_test.client.input.KeyBindings;
import xyz.ignite4inferneo.space_test.client.input.KeyInput;
import xyz.ignite4inferneo.space_test.client.input.MouseInput;
import xyz.ignite4inferneo.space_test.client.renderer.EntityRenderer;
import xyz.ignite4inferneo.space_test.client.renderer.RendererAdapter;
import xyz.ignite4inferneo.space_test.common.entity.Entity;
import xyz.ignite4inferneo.space_test.common.entity.ItemEntity;
import xyz.ignite4inferneo.space_test.common.entity.LivingEntity;
import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;
import xyz.ignite4inferneo.space_test.common.inventory.Inventory;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;
import xyz.ignite4inferneo.space_test.common.util.EntityRaycast;
import xyz.ignite4inferneo.space_test.common.util.RayCast;
import xyz.ignite4inferneo.space_test.common.world.World;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UPDATED: Main game window with entity interaction
 */
public class Window {
    private JFrame displayWindow;
    private RendererAdapter renderer;
    private World world;
    private PlayerEntity player;
    private Inventory inventory;
    private GUIManager guiManager;
    private BlockInteractionHandler blockInteractionHandler;

    private java.util.Queue<Integer> fpsHistory = new LinkedList<>();
    private int historySize = 100;
    private AtomicInteger fpsEstimater = new AtomicInteger();
    private long lastUpdateTime = System.currentTimeMillis();
    private long lastFrameTime = System.nanoTime();

    // Interaction targets
    private RayCast.RaycastResult currentBlockTarget = null;
    private EntityRaycast.EntityRaycastResult currentEntityTarget = null;

    // Camera
    private double yaw = 0;
    private double pitch = 0;

    // Performance tracking
    private long renderTimeMs = 0;
    private long entityRenderTimeMs = 0;

    public Window(World world) {
        this.world = world;
        this.inventory = new Inventory();
        this.guiManager = new GUIManager();

        guiManager.getInventoryInteraction().setWorld(world);

        // Give player some starting items
        inventory.addItem("space_test:stone", 64);
        inventory.addItem("space_test:dirt", 64);
        inventory.addItem("space_test:grass", 32);
        inventory.addItem("space_test:wood", 16);

        // Create player at spawn position
        int[] spawnPos = new int[]{0, 70, 0};
        this.player = PlayerEntity.createLocal(world, spawnPos[0], spawnPos[1], spawnPos[2], "Player");

        this.blockInteractionHandler = new BlockInteractionHandler(world, player);

        displayWindow = new JFrame("Space Test - Voxel Engine [ULTRA-OPTIMIZED + ENTITIES]");
        displayWindow.setSize(ClientSettings.windowSize);
        displayWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        displayWindow.setVisible(true);
        displayWindow.createBufferStrategy(3);
        displayWindow.setFocusable(true);
        displayWindow.requestFocus();
        displayWindow.setLocationRelativeTo(null);

        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("[Window] Creating ultra-optimized renderer with " + threads + " threads");
        renderer = new RendererAdapter(world, threads);
        renderer.setCanvasSize(displayWindow.getSize());

        System.out.println("[Window] Preloading chunks around player...");
        renderer.preloadChunksAround(spawnPos[0], spawnPos[2], 4);

        MouseInput.init(displayWindow);
        MouseInput.setMouseLocked(true);

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

        // Physics thread
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

        // Game tick thread
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
        if (KeyBindings.INVENTORY.isPressed()) {
            if (guiManager.hasOpenGUI()) {
                guiManager.returnHeldItems(inventory);
                guiManager.closeGUI();
                MouseInput.setMouseLocked(true);
            } else {
                guiManager.openGUI(GUIManager.GUIType.INVENTORY);
                MouseInput.setMouseLocked(false);
            }
        }

        if (guiManager.hasOpenGUI()) {
            guiManager.updateMousePosition(MouseInput.getMouseX(), MouseInput.getMouseY());

            double[] pos = player.getCameraPosition();
            guiManager.getInventoryInteraction().setPlayerPosition(pos[0], pos[1], pos[2]);

            if (MouseInput.isButtonPressed(0)) {
                guiManager.handleClick(inventory, displayWindow.getWidth(), displayWindow.getHeight(),
                        MouseInput.getMouseX(), MouseInput.getMouseY(), false);
            }
            if (MouseInput.isButtonPressed(2)) {
                guiManager.handleClick(inventory, displayWindow.getWidth(), displayWindow.getHeight(),
                        MouseInput.getMouseX(), MouseInput.getMouseY(), true);
            }

            double[] playerPos = player.getFeetPosition();
            if (!guiManager.isInRangeOfGUIBlock(playerPos[0], playerPos[1], playerPos[2])) {
                guiManager.returnHeldItems(inventory);
                guiManager.closeGUI();
                MouseInput.setMouseLocked(true);
            }

            return;
        }

        if (KeyBindings.TOGGLE_MOUSE_LOCK.isPressed()) {
            MouseInput.setMouseLocked(!MouseInput.isMouseLocked());
        }

        // Movement
        double forward = 0, strafe = 0;

        if (KeyBindings.MOVE_FORWARD.isDown()) forward += 1;
        if (KeyBindings.MOVE_BACK.isDown()) forward -= 1;
        if (KeyBindings.MOVE_RIGHT.isDown()) strafe += 1;
        if (KeyBindings.MOVE_LEFT.isDown()) strafe -= 1;

        player.applyMovementInput(forward, strafe, deltaTime);

        if (KeyInput.isDown(KeyEvent.VK_CONTROL)) {
            player.setSprinting(true);
        } else {
            player.setSprinting(false);
        }

        if (KeyInput.isDown(KeyEvent.VK_SHIFT)) {
            player.setSneaking(true);
        } else {
            player.setSneaking(false);
        }

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

                player.setLookDirection((float)yaw, (float)pitch);
            }
        }

        // Update renderer camera
        double[] camPos = player.getCameraPosition();
        renderer.x = camPos[0];
        renderer.y = camPos[1];
        renderer.z = camPos[2];
        renderer.yaw = yaw;
        renderer.pitch = pitch;

        // NEW: Update both block and entity targets
        updateTargets();

        // Left click - attack entity or break block
        if (MouseInput.isButtonPressed(0)) {
            if (currentEntityTarget != null) {
                attackEntity(currentEntityTarget.entity);
            } else {
                breakBlock();
            }
        }

        // Right click - interact with entity or block
        if (MouseInput.isButtonPressed(2)) {
            if (currentEntityTarget != null) {
                interactWithEntity(currentEntityTarget.entity);
            } else if (blockInteractionHandler.handleBlockInteraction(currentBlockTarget)) {
                String guiType = blockInteractionHandler.getOpenGUI();
                if (guiType != null) {
                    int[] pos = blockInteractionHandler.getGUIBlockPosition();
                    switch (guiType) {
                        case "crafting_table":
                            guiManager.openGUI(GUIManager.GUIType.CRAFTING_TABLE, pos[0], pos[1], pos[2]);
                            MouseInput.setMouseLocked(false);
                            break;
                        case "furnace":
                            guiManager.openGUI(GUIManager.GUIType.FURNACE, pos[0], pos[1], pos[2]);
                            MouseInput.setMouseLocked(false);
                            break;
                        case "chest":
                            guiManager.openGUI(GUIManager.GUIType.CHEST, pos[0], pos[1], pos[2]);
                            MouseInput.setMouseLocked(false);
                            break;
                    }
                }
                return;
            } else {
                placeBlock();
            }
        }

        // Cycle selected block
        for (int i = 0; i < 9; i++) {
            if (KeyInput.isPressed(49 + i)) {
                inventory.setSelectedSlot(i);
            }
        }

        if (KeyBindings.TOGGLE_FPS.isPressed()) {
            ClientSettings.showFPS = !ClientSettings.showFPS;
        }
    }

    /**
     * NEW: Update both block and entity raycast targets
     */
    private void updateTargets() {
        double[] pos = player.getCameraPosition();
        double[] dir = player.getLookDirection();

        // Check for entity hits first (they take priority at close range)
        currentEntityTarget = EntityRaycast.castRay(
                world, pos[0], pos[1], pos[2],
                dir[0], dir[1], dir[2],
                5.0, // Max distance
                player  // Ignore self
        );

        // Always update block target too
        currentBlockTarget = RayCast.cast(world, pos[0], pos[1], pos[2],
                dir[0], dir[1], dir[2], 5.0);
    }

    /**
     * NEW: Attack an entity
     */
    private void attackEntity(Entity entity) {
        if (entity instanceof LivingEntity living) {
            // Deal damage
            float damage = 2.0f; // Base damage

            // Sprint bonus
            if (player.isSprinting()) {
                damage *= 1.5f;
            }

            boolean hit = living.damage(damage);

            if (hit) {
                System.out.println("[Combat] Hit " + entity.getType() + " for " + damage + " damage!");

                // Knockback
                double dx = entity.x - player.x;
                double dz = entity.z - player.z;
                double dist = Math.sqrt(dx*dx + dz*dz);
                if (dist > 0) {
                    dx /= dist;
                    dz /= dist;
                    entity.vx += dx * 5.0;
                    entity.vz += dz * 5.0;
                    entity.vy += 2.0;
                }
            }
        }
    }

    /**
     * NEW: Interact with an entity
     */
    private void interactWithEntity(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            // Pick up item
            ItemStack stack = itemEntity.getItemStack();
            if (inventory.hasRoom(stack.getBlockId(), stack.getCount())) {
                if (inventory.addItem(stack.getBlockId(), stack.getCount())) {
                    System.out.println("[Pickup] Picked up " + stack);
                    itemEntity.remove();
                }
            }
        } else {
            // Generic interaction
            boolean handled = entity.interact(player);
            if (handled) {
                System.out.println("[Interact] Interacted with " + entity.getType());
            }
        }
    }

    private void breakBlock() {
        if (currentBlockTarget != null && currentBlockTarget.hit) {
            String blockId = world.getBlock(currentBlockTarget.x, currentBlockTarget.y, currentBlockTarget.z);

            if (blockId.equals("space_test:air")) return;

            inventory.addItem(blockId, 1);

            world.setBlock(currentBlockTarget.x, currentBlockTarget.y, currentBlockTarget.z, "space_test:air");
            renderer.markChunkDirty(currentBlockTarget.x >> 4, currentBlockTarget.z >> 4);
        }
    }

    private void placeBlock() {
        if (currentBlockTarget != null && currentBlockTarget.hit) {
            ItemStack selectedStack = inventory.getSelectedStack();
            if (selectedStack.isEmpty()) return;

            int placeX = currentBlockTarget.x + currentBlockTarget.nx;
            int placeY = currentBlockTarget.y + currentBlockTarget.ny;
            int placeZ = currentBlockTarget.z + currentBlockTarget.nz;

            double[] pos = player.getCameraPosition();
            double dx = placeX + 0.5 - pos[0];
            double dy = placeY + 0.5 - pos[1];
            double dz = placeZ + 0.5 - pos[2];
            if (dx*dx + dy*dy + dz*dz < 2.0) return;

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
            renderer.syncCamera();
            renderer.render();
            graphics.drawImage(renderer.getScreenBuffer(), 0, 0, null);

            long entityStart = System.currentTimeMillis();
            double[] camPos = player.getCameraPosition();
            EntityRenderer.renderEntities(
                    graphics,
                    world.getEntityManager().getEntities(),
                    camPos[0], camPos[1], camPos[2],
                    yaw, pitch,
                    displayWindow.getWidth(), displayWindow.getHeight()
            );
            entityRenderTimeMs = System.currentTimeMillis() - entityStart;
        }

        // Draw UI
        if (ClientSettings.showFPS) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Monospaced", Font.PLAIN, 12));

            int y = 41;
            graphics.drawString("FPS: " + String.format("%.1f", getAverageFPS()), 10, y);
            y += 20;
            graphics.drawString("Render: " + renderTimeMs + "ms (Entities: " + entityRenderTimeMs + "ms)", 10, y);
            y += 20;

            double[] pos = player.getFeetPosition();
            graphics.drawString(String.format("Pos: %.1f, %.1f, %.1f", pos[0], pos[1], pos[2]), 10, y);
            y += 20;
            graphics.drawString("On Ground: " + player.isOnGround(), 10, y);
            y += 20;

            graphics.drawString(String.format("Health: %.1f/%.1f", player.getHealth(), player.getMaxHealth()), 10, y);
            y += 20;
            graphics.drawString(String.format("Stamina: %.0f/%.0f", player.getStamina(), player.getMaxStamina()), 10, y);
            y += 20;

            if (player.isSprinting()) {
                graphics.setColor(Color.YELLOW);
                graphics.drawString("SPRINTING", 10, y);
                graphics.setColor(Color.WHITE);
                y += 20;
            }
            if (player.isSneaking()) {
                graphics.setColor(Color.CYAN);
                graphics.drawString("SNEAKING", 10, y);
                graphics.setColor(Color.WHITE);
                y += 20;
            }

            int rendered = renderer.getChunksRendered();
            int meshing = renderer.getChunksMeshing();
            int cached = renderer.getCachedMeshCount();
            int quadsRendered = renderer.getQuadsRendered();
            int quadsCulled = renderer.getQuadsCulled();

            graphics.drawString("Chunks: " + rendered + " (meshing: " + meshing + ", cached: " + cached + ")", 10, y);
            y += 20;
            graphics.drawString("Quads: " + quadsRendered + " (culled: " + quadsCulled + ")", 10, y);
            y += 20;

            int entityCount = world.getEntityManager().getEntityCount();
            graphics.drawString("Entities: " + entityCount, 10, y);
            y += 20;

            if (meshing > 0) {
                graphics.setColor(new Color(255, 255, 0, 200));
                graphics.drawString("⚡ Loading chunks...", 10, y);
            }
            graphics.setColor(Color.WHITE);
            y += 20;

            // NEW: Show what we're looking at
            if (currentEntityTarget != null) {
                Entity entity = currentEntityTarget.entity;
                graphics.setColor(Color.YELLOW);
                String targetInfo = "Looking at: " + entity.getType();
                if (entity instanceof LivingEntity living) {
                    targetInfo += String.format(" (HP: %.1f/%.1f)", living.getHealth(), living.getMaxHealth());
                }
                graphics.drawString(targetInfo, 10, y);
                graphics.setColor(Color.WHITE);
                y += 20;
            } else if (currentBlockTarget != null && currentBlockTarget.hit) {
                graphics.drawString(String.format("Looking at: %d, %d, %d",
                        currentBlockTarget.x, currentBlockTarget.y, currentBlockTarget.z), 10, y);
                y += 20;
            }

            ItemStack selected = inventory.getSelectedStack();
            if (!selected.isEmpty()) {
                graphics.drawString("Selected: " + selected, 10, y);
            }
        }

        // Render inventory or hotbar
        if (guiManager.hasOpenGUI()) {
            guiManager.render(graphics, inventory, displayWindow.getWidth(), displayWindow.getHeight());
        } else {
            InventoryRenderer.renderHotbar(graphics, inventory, displayWindow.getWidth(), displayWindow.getHeight());
        }

        // Crosshair - changes color when targeting entity
        int cx = displayWindow.getWidth() / 2;
        int cy = displayWindow.getHeight() / 2;

        if (currentEntityTarget != null) {
            // Red crosshair when targeting entity
            graphics.setColor(Color.RED);
        } else {
            graphics.setColor(Color.WHITE);
        }

        graphics.drawLine(cx - 10, cy, cx + 10, cy);
        graphics.drawLine(cx, cy - 10, cx, cy + 10);

        if (currentBlockTarget != null && currentBlockTarget.hit && currentEntityTarget == null) {
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