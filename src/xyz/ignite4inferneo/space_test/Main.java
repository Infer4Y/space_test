package xyz.ignite4inferneo.space_test;

import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.client.Window;
import xyz.ignite4inferneo.space_test.client.input.KeyBindings;
import xyz.ignite4inferneo.space_test.client.input.KeyInput;
import xyz.ignite4inferneo.space_test.common.VanillaBlocks;
import xyz.ignite4inferneo.space_test.common.world.ImprovedWorldGenerator;  // CHANGED
import xyz.ignite4inferneo.space_test.common.world.World;

public class Main {

    private static World world;

    public static void main(String[] args) {
        System.out.println("=== Space Test - Voxel Engine ===");
        System.out.println("Initializing game systems...");

        System.out.println("[Init] Setting up registries...");
        System.out.println("[Init] Registering vanilla blocks...");
        VanillaBlocks.register();

        System.out.println("[Init] Loading mods...");
        loadMods();

        System.out.println("[Init] Freezing registries...");
        Registries.freezeAll();

        System.out.println("[Init] Creating world with improved terrain...");
        // CHANGED: Use ImprovedWorldGenerator instead of DefaultWorldGenerator
        world = new World(new ImprovedWorldGenerator(System.currentTimeMillis()));

        System.out.println("[Init] Initializing client...");
        KeyInput.init();
        KeyBindings.init();

        System.out.println("[Init] Starting renderer...");
        new Window(world);

        System.out.println("=== Initialization Complete ===");
        System.out.println("Controls:");
        System.out.println("  WASD - Move");
        System.out.println("  Mouse - Look");
        System.out.println("  Space - Jump");
        System.out.println("  Left Click - Break Block");
        System.out.println("  Right Click - Place Block");
        System.out.println("  ESC - Toggle Mouse Lock");
        System.out.println("  F3 - Toggle Debug Info");
        System.out.println("");
        System.out.println("Explore mountains, valleys, and rivers!");
    }

    private static void loadMods() {
        System.out.println("[ModLoader] No mods found");
    }

    public static World getWorld() {
        return world;
    }
}