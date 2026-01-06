package xyz.ignite4inferneo.space_test;

import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.client.Window;
import xyz.ignite4inferneo.space_test.client.input.KeyBindings;
import xyz.ignite4inferneo.space_test.client.input.KeyInput;
import xyz.ignite4inferneo.space_test.common.VanillaBlocks;
import xyz.ignite4inferneo.space_test.common.VanillaItems;
import xyz.ignite4inferneo.space_test.common.util.EntitySpawnHelper;
import xyz.ignite4inferneo.space_test.common.world.ImprovedWorldGenerator;
import xyz.ignite4inferneo.space_test.common.world.World;

public class Main {

    private static World world;

    public static void main(String[] args) {
        System.out.println("=== Space Test - Voxel Engine ===");
        System.out.println("Initializing game systems...");

        System.out.println("[Init] Setting up registries...");

        System.out.println("[Init] Registering vanilla blocks...");
        VanillaBlocks.register();

        System.out.println("[Init] Registering vanilla items...");
        VanillaItems.register();

        System.out.println("[Init] Registering vanilla recipes...");
        xyz.ignite4inferneo.space_test.common.VanillaRecipes.register();

        System.out.println("[Init] Loading mods...");
        loadMods();

        System.out.println("[Init] Freezing registries...");
        Registries.freezeAll();

        System.out.println("[Init] Creating world with improved terrain...");
        world = new World(new ImprovedWorldGenerator(System.currentTimeMillis()));

        System.out.println("[Init] Initializing client...");
        KeyInput.init();
        KeyBindings.init();

        System.out.println("[Init] Starting renderer...");
        Window window = new Window(world);

        // Spawn test entities after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait 2 seconds for chunks to load
                int[] spawn = {0, 70, 0};
                EntitySpawnHelper.spawnTestEntities(world, spawn[0], spawn[1], spawn[2]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("=== Initialization Complete ===");
        System.out.println("Controls:");
        System.out.println("  WASD - Move");
        System.out.println("  Mouse - Look");
        System.out.println("  Space - Jump");
        System.out.println("  Left Ctrl - Sprint");
        System.out.println("  Shift - Sneak");
        System.out.println("  Left Click - Break Block / Attack Entity");
        System.out.println("  Right Click - Place Block / Interact with Entity");
        System.out.println("  E - Toggle Inventory");
        System.out.println("  ESC - Toggle Mouse Lock");
        System.out.println("  F3 - Toggle Debug Info");
        System.out.println("  1-9 - Select Hotbar Slot");
        System.out.println("");
        System.out.println("Features:");
        System.out.println("  - Complete item system with tools, weapons, and food");
        System.out.println("  - Sprint to move faster (drains stamina)");
        System.out.println("  - Sneak to move slowly");
        System.out.println("  - Entity interaction (attack mobs, pickup items)");
        System.out.println("  - 3D item icons in inventory");
        System.out.println("  - Crafting system with recipes");
    }

    private static void loadMods() {
        System.out.println("[ModLoader] No mods found");
    }

    public static World getWorld() {
        return world;
    }
}