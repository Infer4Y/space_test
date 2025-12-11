package xyz.ignite4inferneo.space_test.client.input;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class MouseInput implements MouseListener, MouseMotionListener {
    private static int mouseX = 0;
    private static int mouseY = 0;
    private static int mouseDX = 0;
    private static int mouseDY = 0;
    private static boolean[] mouseButtons = new boolean[8];
    private static boolean[] mousePressed = new boolean[8];
    private static boolean[] mouseReleased = new boolean[8];
    private static boolean mouseLocked = false;
    private static Component component;
    private static Robot robot;
    private static int centerX, centerY;

    public static void init(Component comp) {
        component = comp;
        MouseInput listener = new MouseInput();
        comp.addMouseListener(listener);
        comp.addMouseMotionListener(listener);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.err.println("Failed to create Robot for mouse locking: " + e.getMessage());
        }
    }

    public static void setMouseLocked(boolean locked) {
        mouseLocked = locked;
        if (component != null) {
            if (locked) {
                // Hide cursor
                Cursor invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                        new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB),
                        new Point(0, 0),
                        "invisible"
                );
                component.setCursor(invisibleCursor);

                // Calculate center of component
                centerX = component.getWidth() / 2;
                centerY = component.getHeight() / 2;

                // Move mouse to center
                if (robot != null) {
                    Point screenPos = component.getLocationOnScreen();
                    robot.mouseMove(screenPos.x + centerX, screenPos.y + centerY);
                    mouseX = centerX;
                    mouseY = centerY;
                }
            } else {
                // Show cursor
                component.setCursor(Cursor.getDefaultCursor());
            }
        }
        mouseDX = 0;
        mouseDY = 0;
    }

    public static boolean isMouseLocked() {
        return mouseLocked;
    }

    public static int getMouseX() {
        return mouseX;
    }

    public static int getMouseY() {
        return mouseY;
    }

    public static int getMouseDX() {
        return mouseDX;
    }

    public static int getMouseDY() {
        return mouseDY;
    }

    public static boolean isButtonDown(int button) {
        if (button < 0 || button >= mouseButtons.length) return false;
        return mouseButtons[button];
    }

    public static boolean isButtonPressed(int button) {
        if (button < 0 || button >= mousePressed.length) return false;
        return mousePressed[button];
    }

    public static boolean isButtonReleased(int button) {
        if (button < 0 || button >= mouseReleased.length) return false;
        return mouseReleased[button];
    }

    public static void endFrame() {
        // Clear delta
        if (!mouseLocked) {
            mouseDX = 0;
            mouseDY = 0;
        }

        // Clear one-frame states
        for (int i = 0; i < mousePressed.length; i++) {
            mousePressed[i] = false;
            mouseReleased[i] = false;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int button = e.getButton() - 1;
        if (button >= 0 && button < mouseButtons.length) {
            if (!mouseButtons[button]) {
                mousePressed[button] = true;
            }
            mouseButtons[button] = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int button = e.getButton() - 1;
        if (button >= 0 && button < mouseButtons.length) {
            mouseButtons[button] = false;
            mouseReleased[button] = true;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (mouseLocked && robot != null && component != null) {
            // Calculate delta from center
            mouseDX = e.getX() - centerX;
            mouseDY = e.getY() - centerY;

            // Re-center mouse
            Point screenPos = component.getLocationOnScreen();
            robot.mouseMove(screenPos.x + centerX, screenPos.y + centerY);

            mouseX = centerX;
            mouseY = centerY;
        } else {
            int newX = e.getX();
            int newY = e.getY();
            mouseDX = newX - mouseX;
            mouseDY = newY - mouseY;
            mouseX = newX;
            mouseY = newY;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}
