package xyz.ignite4inferneo.space_test.api.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple event bus for mods to hook into game events.
 * Events are fired synchronously in registration order.
 */
public class EventBus {

    private static final Map<Class<? extends Event>, List<Consumer<? extends Event>>> listeners = new HashMap<>();

    /**
     * Register a listener for a specific event type
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> void register(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Fire an event to all registered listeners
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> void fire(T event) {
        List<Consumer<? extends Event>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<? extends Event> listener : eventListeners) {
                ((Consumer<T>) listener).accept(event);
            }
        }
    }

    /**
     * Clear all listeners (useful for testing)
     */
    public static void clear() {
        listeners.clear();
    }
}