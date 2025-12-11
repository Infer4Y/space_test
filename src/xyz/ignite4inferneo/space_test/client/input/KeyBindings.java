package xyz.ignite4inferneo.space_test.client.input;

import xyz.ignite4inferneo.space_test.client.ClientSettings;

public class KeyBindings {
    // Movement
    public static KeyBinding MOVE_FORWARD;
    public static KeyBinding MOVE_BACK;
    public static KeyBinding MOVE_LEFT;
    public static KeyBinding MOVE_RIGHT;
    public static KeyBinding JUMP;
    public static KeyBinding MOVE_UP;
    public static KeyBinding MOVE_DOWN;

    // UI
    public static KeyBinding INVENTORY;
    public static KeyBinding TOGGLE_GREEDY_MESHING;
    public static KeyBinding TOGGLE_MOUSE_LOCK;
    public static KeyBinding INCREASE_PIXEL_SCALE;
    public static KeyBinding DECREASE_PIXEL_SCALE;
    public static  KeyBinding TOGGLE_FPS ;

    // Camera (arrow keys as backup)
    public static KeyBinding LOOK_LEFT;
    public static KeyBinding LOOK_RIGHT;
    public static KeyBinding LOOK_UP;
    public static KeyBinding LOOK_DOWN;

    public static void init() {
        MOVE_FORWARD = new KeyBinding("forward", ClientSettings.KEY_FORWARD);
        MOVE_BACK = new KeyBinding("back", ClientSettings.KEY_BACK);
        MOVE_LEFT = new KeyBinding("left", ClientSettings.KEY_LEFT);
        MOVE_RIGHT = new KeyBinding("right", ClientSettings.KEY_RIGHT);
        JUMP = new KeyBinding("jump", ClientSettings.KEY_JUMP);
        MOVE_UP = new KeyBinding("up", ClientSettings.KEY_UP);
        MOVE_DOWN = new KeyBinding("down", ClientSettings.KEY_DOWN);

        INVENTORY = new KeyBinding("inventory", ClientSettings.KEY_INVENTORY);
        TOGGLE_GREEDY_MESHING = new KeyBinding("toggleGreedy", ClientSettings.KEY_TOGGLE_GREEDY);
        TOGGLE_MOUSE_LOCK = new KeyBinding("toggleMouse", ClientSettings.KEY_TOGGLE_MOUSE);
        INCREASE_PIXEL_SCALE = new KeyBinding("increaseScale", ClientSettings.KEY_INCREASE_SCALE);
        DECREASE_PIXEL_SCALE = new KeyBinding("decreaseScale", ClientSettings.KEY_DECREASE_SCALE);
        TOGGLE_FPS = new KeyBinding("toggleFPS", ClientSettings.KEY_TOGGLE_FPS);

        LOOK_LEFT = new KeyBinding("lookLeft", ClientSettings.KEY_LOOK_LEFT);
        LOOK_RIGHT = new KeyBinding("lookRight", ClientSettings.KEY_LOOK_RIGHT);
        LOOK_UP = new KeyBinding("lookUp", ClientSettings.KEY_LOOK_UP);
        LOOK_DOWN = new KeyBinding("lookDown", ClientSettings.KEY_LOOK_DOWN);
    }
}