package xyz.ignite4inferneo.space_test.client;

import java.awt.*;
import java.awt.event.KeyEvent;

public class ClientSettings {
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
    public static int KEY_TOGGLE_GREEDY = KeyEvent.VK_G;
    public static int KEY_TOGGLE_MOUSE = KeyEvent.VK_ESCAPE;

    // Arrow key look (backup)
    public static int KEY_LOOK_LEFT = KeyEvent.VK_LEFT;
    public static int KEY_LOOK_RIGHT = KeyEvent.VK_RIGHT;
    public static int KEY_LOOK_UP = KeyEvent.VK_UP;
    public static int KEY_LOOK_DOWN = KeyEvent.VK_DOWN;

    // Camera settings
    public static double MOVE_SPEED = 0.5; // Units per frame
    public static double LOOK_SPEED = 0.5;
    public static double MOUSE_SENSITIVITY = 0.0025;

    // Pixel scale controls
    public static int KEY_INCREASE_SCALE = KeyEvent.VK_EQUALS; // + key
    public static int KEY_DECREASE_SCALE = KeyEvent.VK_MINUS; // - key
    public static int KEY_TOGGLE_FPS = KeyEvent.VK_F3;
    public static int MIN_PIXEL_SCALE = 1;
    public static int MAX_PIXEL_SCALE = 16;
}
