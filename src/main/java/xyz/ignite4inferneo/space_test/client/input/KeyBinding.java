package xyz.ignite4inferneo.space_test.client.input;

public class KeyBinding {
    private final String name;
    private int keyCode;

    public KeyBinding(String name, int defaultKey) {
        this.name = name;
        this.keyCode = defaultKey;
    }

    public String getName() {
        return name;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int newKey) {
        this.keyCode = newKey;
    }

    public boolean isDown() {
        return KeyInput.isDown(keyCode);
    }

    public boolean isPressed() {
        return KeyInput.isPressed(keyCode);
    }

    public boolean isReleased() {
        return KeyInput.isReleased(keyCode);
    }
}

