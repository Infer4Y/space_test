package xyz.ignite4inferneo.space_test.common.util;

import xyz.ignite4inferneo.space_test.common.entity.Entity;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.util.List;

/**
 * Raycasting for entity interaction
 * Allows clicking on entities with the mouse
 */
public class EntityRaycast {

    public static class EntityRaycastResult {
        public final Entity entity;
        public final double distance;
        public final double hitX, hitY, hitZ;

        public EntityRaycastResult(Entity entity, double distance, double hitX, double hitY, double hitZ) {
            this.entity = entity;
            this.distance = distance;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
        }
    }

    /**
     * Cast a ray and find the closest entity it hits
     *
     * @param world World to search in
     * @param startX Ray start X
     * @param startY Ray start Y
     * @param startZ Ray start Z
     * @param dirX Ray direction X (normalized)
     * @param dirY Ray direction Y (normalized)
     * @param dirZ Ray direction Z (normalized)
     * @param maxDistance Maximum ray distance
     * @param ignoreEntity Entity to ignore (usually the player)
     * @return EntityRaycastResult or null if no hit
     */
    public static EntityRaycastResult castRay(World world,
                                              double startX, double startY, double startZ,
                                              double dirX, double dirY, double dirZ,
                                              double maxDistance,
                                              Entity ignoreEntity) {

        // Get all entities in range
        List<Entity> nearbyEntities = world.getEntityManager().getEntitiesNear(
                startX, startY, startZ, maxDistance + 5
        );

        EntityRaycastResult closestHit = null;
        double closestDistance = maxDistance;

        for (Entity entity : nearbyEntities) {
            if (entity == ignoreEntity) continue;
            if (entity.isRemoved()) continue;

            // Get entity bounding box
            double[] box = entity.getBoundingBox();
            double minX = box[0];
            double minY = box[1];
            double minZ = box[2];
            double maxX = box[3];
            double maxY = box[4];
            double maxZ = box[5];

            // Ray-box intersection test
            double hitDist = rayBoxIntersection(
                    startX, startY, startZ,
                    dirX, dirY, dirZ,
                    minX, minY, minZ,
                    maxX, maxY, maxZ
            );

            if (hitDist >= 0 && hitDist < closestDistance) {
                // Calculate hit point
                double hitX = startX + dirX * hitDist;
                double hitY = startY + dirY * hitDist;
                double hitZ = startZ + dirZ * hitDist;

                closestHit = new EntityRaycastResult(entity, hitDist, hitX, hitY, hitZ);
                closestDistance = hitDist;
            }
        }

        return closestHit;
    }

    /**
     * Ray-AABB intersection test
     * Returns distance to intersection or -1 if no hit
     */
    private static double rayBoxIntersection(double ox, double oy, double oz,
                                             double dx, double dy, double dz,
                                             double minX, double minY, double minZ,
                                             double maxX, double maxY, double maxZ) {

        // Handle rays parallel to axes
        double invDirX = dx == 0 ? Double.POSITIVE_INFINITY : 1.0 / dx;
        double invDirY = dy == 0 ? Double.POSITIVE_INFINITY : 1.0 / dy;
        double invDirZ = dz == 0 ? Double.POSITIVE_INFINITY : 1.0 / dz;

        // Calculate t values for X planes
        double t1 = (minX - ox) * invDirX;
        double t2 = (maxX - ox) * invDirX;

        double tmin = Math.min(t1, t2);
        double tmax = Math.max(t1, t2);

        // Calculate t values for Y planes
        t1 = (minY - oy) * invDirY;
        t2 = (maxY - oy) * invDirY;

        tmin = Math.max(tmin, Math.min(t1, t2));
        tmax = Math.min(tmax, Math.max(t1, t2));

        // Calculate t values for Z planes
        t1 = (minZ - oz) * invDirZ;
        t2 = (maxZ - oz) * invDirZ;

        tmin = Math.max(tmin, Math.min(t1, t2));
        tmax = Math.min(tmax, Math.max(t1, t2));

        // Check if ray intersects box
        if (tmax < 0 || tmin > tmax) {
            return -1; // No intersection
        }

        // Return distance to entry point
        return tmin >= 0 ? tmin : tmax;
    }

    /**
     * Expand bounding box slightly for easier clicking
     */
    public static double[] expandBoundingBox(double[] box, double expansion) {
        return new double[] {
                box[0] - expansion,
                box[1] - expansion,
                box[2] - expansion,
                box[3] + expansion,
                box[4] + expansion,
                box[5] + expansion
        };
    }
}