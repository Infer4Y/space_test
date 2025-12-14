package xyz.ignite4inferneo.space_test.client;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Client-side configuration and settings
 */
public class ClientSettings {
    // Window settings
    public static Dimension windowSize = new Dimension(1280, 720);
    public static boolean showFPS = true;

    // Movement keys
    public static int KEY_FORWARD = KeyEvent.VK_W;
    public static int KEY_BACK = KeyEvent.VK_S;
    public static int KEY_LEFT = KeyEvent.VK_A;
    public static int KEY_RIGHT = KeyEvent.VK_D;
    public static int KEY_JUMP = KeyEvent.VK_SPACE;
    public static int KEY_UP = KeyEvent.VK_SPACE;
    public static int KEY_DOWN = KeyEvent.VK_SHIFT;

    // UI keys
    public static int KEY_INVENTORY = KeyEvent.VK_E;
    public static int KEY_TOGGLE_MOUSE = KeyEvent.VK_ESCAPE;
    public static int KEY_TOGGLE_FPS = KeyEvent.VK_F3;

    // Camera settings
    public static double MOVE_SPEED = 0.3;
    public static double LOOK_SPEED = 0.5;
    public static double MOUSE_SENSITIVITY = 0.003;

    // Render settings
    public static int RENDER_DISTANCE = 8; // chunks
}