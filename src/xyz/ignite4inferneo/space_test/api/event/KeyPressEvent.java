package xyz.ignite4inferneo.space_test.api.event;

public class KeyPressEvent extends Event {
    private final int keyCode;

    public KeyPressEvent(int keyCode) {
        this.keyCode = keyCode;
    }

    public int getKeyCode() { return keyCode; }
}