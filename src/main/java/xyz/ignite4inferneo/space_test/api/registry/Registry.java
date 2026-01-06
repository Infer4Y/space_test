package xyz.ignite4inferneo.space_test.api.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generic registry for game objects (blocks, items, entities, etc.)
 * Allows mods to register their content with unique identifiers.
 */
public class Registry<T> {
    private final Map<String, T> entries = new HashMap<>();
    private final String name;
    private boolean frozen = false;

    public Registry(String name) {
        this.name = name;
    }

    /**
     * Register an object with a namespaced ID (e.g., "minecraft:stone" or "modid:custom_block")
     */
    public void register(String id, T object) {
        if (frozen) {
            throw new IllegalStateException("Cannot register to frozen registry: " + name);
        }

        if (entries.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate registration: " + id + " in registry " + name);
        }

        entries.put(id, object);
        System.out.println("[Registry/" + name + "] Registered: " + id);
    }

    /**
     * Get an object by its ID
     */
    public T get(String id) {
        return entries.get(id);
    }

    /**
     * Check if an ID is registered
     */
    public boolean contains(String id) {
        return entries.containsKey(id);
    }

    /**
     * Get all registered IDs
     */
    public Set<String> getIds() {
        return entries.keySet();
    }

    /**
     * Get the number of registered entries
     */
    public int size() {
        return entries.size();
    }

    /**
     * Freeze the registry (prevent further modifications)
     * Called after initialization to prevent runtime changes
     */
    public void freeze() {
        frozen = true;
        System.out.println("[Registry/" + name + "] Frozen with " + entries.size() + " entries");
    }

    /**
     * Get the registry name
     */
    public String getName() {
        return name;
    }
}