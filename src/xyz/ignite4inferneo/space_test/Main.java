package xyz.ignite4inferneo.space_test;

import xyz.ignite4inferneo.space_test.client.Window;
import xyz.ignite4inferneo.space_test.client.input.KeyBindings;
import xyz.ignite4inferneo.space_test.client.input.KeyInput;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Space Test - Voxel Renderer...");

        // Initialize input system
        KeyInput.init();
        KeyBindings.init();

        new Window();
    }
}