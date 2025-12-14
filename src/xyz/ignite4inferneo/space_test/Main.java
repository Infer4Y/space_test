package xyz.ignite4inferneo.space_test;

import xyz.ignite4inferneo.space_test.api.event.EventBus;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.client.Window;
import xyz.ignite4inferneo.space_test.client.input.KeyBindings;
import xyz.ignite4inferneo.space_test.client.input.KeyInput;
import xyz.ignite4inferneo.space_test.common.VanillaBlocks;
import xyz.ignite4inferneo.space_test.common.world.DefaultWorldGenerator;
import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Main entry point for the game.
 * Handles initialization of all systems in the correct order.
 */
public class Main {

    private static World world;

    public static void main(String[] args) {
        System.out.println("=== Space Test - Voxel Engine ===");
        System.out.println("Initializing game systems...");

        // Phase 1: Initialize API systems
        System.out.println("[Init] Setting up registries...");
        // Registries are already static, just need to register content

        // Phase 2: Register vanilla content
        System.out.println("[Init] Registering vanilla blocks...");
        VanillaBlocks.register();

        // Phase 3: Load mods (if any)
        System.out.println("[Init] Loading mods...");
        loadMods();

        // Phase 4: Freeze registries (prevent further modifications)
        System.out.println("[Init] Freezing registries...");
        Registries.freezeAll();

        // Phase 5: Create world
        System.out.println("[Init] Creating world...");
        world = new World(new DefaultWorldGenerator(12345L));

        // Phase 6: Initialize client systems
        System.out.println("[Init] Initializing client...");
        KeyInput.init();
        KeyBindings.init();

        // Phase 7: Create window and start rendering
        System.out.println("[Init] Starting renderer...");
        new Window(world);

        System.out.println("=== Initialization Complete ===");
        System.out.println("Controls:");
        System.out.println("  WASD - Move");
        System.out.println("  Mouse - Look");
        System.out.println("  Space/Shift - Up/Down");
        System.out.println("  Left Click - Break Block");
        System.out.println("  Right Click - Place Block");
        System.out.println("  ESC - Toggle Mouse Lock");
        System.out.println("  F3 - Toggle FPS Display");
    }

    /**
     * Load mods from a mods directory.
     * Mods can register blocks, items, and event listeners before registries freeze.
     */
    private static void loadMods() {
        // TODO: Implement mod loading
        // For now, this is where mods would be loaded and initialized
        // Example:
        // ModLoader.loadMods("mods/");

        // Example of how a mod would register content:
        // Registries.BLOCKS.register("examplemod:custom_block", new CustomBlock());

        System.out.println("[ModLoader] No mods found");
    }

    /**
     * Get the current world instance
     */
    public static World getWorld() {
        return world;
    }
}