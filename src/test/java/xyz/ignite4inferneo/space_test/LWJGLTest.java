package xyz.ignite4inferneo.space_test;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

/**
 * Simple LWJGL test to verify installation
 * Run this to make sure everything is working
 */
public class LWJGLTest {

    public static void main(String[] args) {
        System.out.println("Testing LWJGL Installation...");

        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        System.out.println("✓ GLFW initialized");

        // Create window
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(800, 600, "LWJGL Test",
                MemoryUtil.NULL, MemoryUtil.NULL);

        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create window");
        }

        System.out.println("✓ Window created");

        // Make OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        System.out.println("✓ OpenGL context created");

        // Check OpenGL version
        String version = GL11.glGetString(GL11.GL_VERSION);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String vendor = GL11.glGetString(GL11.GL_VENDOR);

        System.out.println("\n=== OpenGL Info ===");
        System.out.println("Version:  " + version);
        System.out.println("Renderer: " + renderer);
        System.out.println("Vendor:   " + vendor);

        // Cleanup
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();

        System.out.println("\n✓ LWJGL is working correctly!");
        System.out.println("You're ready to start using OpenGL rendering.");
    }
}