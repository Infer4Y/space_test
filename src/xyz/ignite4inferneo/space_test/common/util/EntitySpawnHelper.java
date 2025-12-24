package xyz.ignite4inferneo.space_test.common.util;

import xyz.ignite4inferneo.space_test.common.entity.*;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;
import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Helper class for spawning test entities
 * Use this to test entity rendering
 */
public class EntitySpawnHelper {

    /**
     * Spawn some test entities around the player
     */
    public static void spawnTestEntities(World world, double playerX, double playerY, double playerZ) {
        System.out.println("[EntitySpawnHelper] Spawning test entities...");

        // Spawn some pigs
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2 * i) / 3;
            double x = playerX + Math.cos(angle) * 10;
            double z = playerZ + Math.sin(angle) * 10;

            PigEntity pig = new PigEntity(world, x, playerY, z);
            world.getEntityManager().addEntity(pig);
            System.out.println("[EntitySpawnHelper] Spawned pig at " + x + ", " + playerY + ", " + z);
        }

        // Spawn some zombies further away
        for (int i = 0; i < 2; i++) {
            double angle = (Math.PI * 2 * i) / 2 + Math.PI / 4;
            double x = playerX + Math.cos(angle) * 20;
            double z = playerZ + Math.sin(angle) * 20;

            ZombieEntity zombie = new ZombieEntity(world, x, playerY, z);
            world.getEntityManager().addEntity(zombie);
            System.out.println("[EntitySpawnHelper] Spawned zombie at " + x + ", " + playerY + ", " + z);
        }

        // Spawn some item entities
        for (int i = 0; i < 5; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = 5 + Math.random() * 10;
            double x = playerX + Math.cos(angle) * dist;
            double z = playerZ + Math.sin(angle) * dist;

            String[] items = {"space_test:stone", "space_test:wood", "space_test:dirt", "space_test:grass"};
            String randomItem = items[(int)(Math.random() * items.length)];
            int count = 1 + (int)(Math.random() * 10);

            ItemStack stack = new ItemStack(randomItem, count);
            ItemEntity itemEntity = new ItemEntity(world, x, playerY + 1, z, stack);
            world.getEntityManager().addEntity(itemEntity);
            System.out.println("[EntitySpawnHelper] Spawned item " + randomItem + " x" + count + " at " + x + ", " + (playerY+1) + ", " + z);
        }

        System.out.println("[EntitySpawnHelper] Spawned " + world.getEntityManager().getEntityCount() + " entities total");
    }

    /**
     * Clear all entities except players
     */
    public static void clearNonPlayerEntities(World world) {
        int removed = 0;
        for (Entity entity : world.getEntityManager().getEntities()) {
            if (!(entity instanceof PlayerEntity)) {
                entity.remove();
                removed++;
            }
        }
        System.out.println("[EntitySpawnHelper] Removed " + removed + " entities");
    }

    /**
     * Spawn a single entity at position
     */
    public static void spawnEntity(World world, String type, double x, double y, double z) {
        Entity entity = switch(type.toLowerCase()) {
            case "pig" -> new PigEntity(world, x, y, z);
            case "zombie" -> new ZombieEntity(world, x, y, z);
            case "item" -> new ItemEntity(world, x, y, z, new ItemStack("space_test:stone", 1));
            default -> {
                System.out.println("[EntitySpawnHelper] Unknown entity type: " + type);
                yield null;
            }
        };

        if (entity != null) {
            world.getEntityManager().addEntity(entity);
            System.out.println("[EntitySpawnHelper] Spawned " + type + " at " + x + ", " + y + ", " + z);
        }
    }
}