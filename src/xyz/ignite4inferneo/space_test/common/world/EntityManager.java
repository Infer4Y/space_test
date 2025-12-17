package xyz.ignite4inferneo.space_test.common.world;

import xyz.ignite4inferneo.space_test.common.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all entities in a world
 * Part of the World class - entities are managed per-world
 */
public class EntityManager {
    private final Map<UUID, Entity> entities = new ConcurrentHashMap<>();
    private final List<Entity> toAdd = Collections.synchronizedList(new ArrayList<>());
    private final List<UUID> toRemove = Collections.synchronizedList(new ArrayList<>());

    /**
     * Add entity to world
     */
    public void addEntity(Entity entity) {
        toAdd.add(entity);
    }

    /**
     * Remove entity from world
     */
    public void removeEntity(UUID uuid) {
        toRemove.add(uuid);
    }

    /**
     * Get entity by UUID
     */
    public Entity getEntity(UUID uuid) {
        return entities.get(uuid);
    }

    /**
     * Get all entities
     */
    public Collection<Entity> getEntities() {
        return entities.values();
    }

    /**
     * Get entities near a position
     */
    public List<Entity> getEntitiesNear(double x, double y, double z, double radius) {
        List<Entity> nearby = new ArrayList<>();
        double radiusSq = radius * radius;

        for (Entity entity : entities.values()) {
            double dx = entity.x - x;
            double dy = entity.y - y;
            double dz = entity.z - z;
            double distSq = dx*dx + dy*dy + dz*dz;

            if (distSq <= radiusSq) {
                nearby.add(entity);
            }
        }

        return nearby;
    }

    /**
     * Update all entities
     */
    public void tick(double deltaTime) {
        // Add pending entities
        synchronized (toAdd) {
            for (Entity entity : toAdd) {
                entities.put(entity.getUUID(), entity);
            }
            toAdd.clear();
        }

        // Remove pending entities
        synchronized (toRemove) {
            for (UUID uuid : toRemove) {
                entities.remove(uuid);
            }
            toRemove.clear();
        }

        // Tick all entities
        for (Entity entity : entities.values()) {
            entity.tick(deltaTime);

            // Auto-remove if marked for removal
            if (entity.isRemoved()) {
                toRemove.add(entity.getUUID());
            }
        }
    }

    /**
     * Clear all entities
     */
    public void clear() {
        entities.clear();
        toAdd.clear();
        toRemove.clear();
    }

    /**
     * Get entity count
     */
    public int getEntityCount() {
        return entities.size();
    }
}