package xyz.ignite4inferneo.space_test.api.event;

public class TickEvent extends Event {
    private final long tickCount;

    public TickEvent(long tickCount) {
        this.tickCount = tickCount;
    }

    public long getTickCount() { return tickCount; }
}
