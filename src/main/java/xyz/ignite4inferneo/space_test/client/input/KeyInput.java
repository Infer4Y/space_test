package xyz.ignite4inferneo.space_test.client.input;

import java.awt.*;
import java.awt.event.KeyEvent;

public class KeyInput {

    private static final boolean[] keyDown = new boolean[256];
    private static final boolean[] keyPressed = new boolean[256];
    private static final boolean[] keyReleased = new boolean[256];

    public static void init() {

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    int code = e.getKeyCode();
                    if (code < 0 || code > 255) return false;

                    switch (e.getID()) {
                        case KeyEvent.KEY_PRESSED:
                            if (!keyDown[code]) keyPressed[code] = true;
                            keyDown[code] = true;
                            break;

                        case KeyEvent.KEY_RELEASED:
                            keyDown[code] = false;
                            keyReleased[code] = true;
                            break;
                    }

                    return false;
                });
    }

    public static boolean isDown(int key) {
        return keyDown[key];
    }

    public static boolean isPressed(int key) {
        return keyPressed[key];
    }

    public static boolean isReleased(int key) {
        return keyReleased[key];
    }

    public static void endFrame() {
        for (int i = 0; i < 256; i++) {
            keyPressed[i] = false;
            keyReleased[i] = false;
        }
    }
}
