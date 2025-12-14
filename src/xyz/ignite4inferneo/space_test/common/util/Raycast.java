package xyz.ignite4inferneo.space_test.common.util;

import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * DDA-based voxel raycast for block selection.
 * Used for breaking/placing blocks with mouse.
 */
public class RayCast {

    public static class RaycastResult {
        public final boolean hit;
        public final int x, y, z;        // Hit block position
        public final int nx, ny, nz;     // Face normal (for placement)
        public final double distance;

        public RaycastResult(boolean hit, int x, int y, int z, int nx, int ny, int nz, double distance) {
            this.hit = hit;
            this.x = x;
            this.y = y;
            this.z = z;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.distance = distance;
        }

        public static RaycastResult miss() {
            return new RaycastResult(false, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Cast a ray from camera position in the direction of view.
     * Returns the first solid block hit, or null if no hit within maxDistance.
     *
     * @param world The world to raycast in
     * @param startX Ray start X
     * @param startY Ray start Y
     * @param startZ Ray start Z
     * @param dirX Ray direction X (normalized)
     * @param dirY Ray direction Y (normalized)
     * @param dirZ Ray direction Z (normalized)
     * @param maxDistance Maximum ray distance
     * @return RaycastResult with hit info, or miss() if no hit
     */
    public static RaycastResult cast(World world, double startX, double startY, double startZ,
                                     double dirX, double dirY, double dirZ, double maxDistance) {

        // Current voxel position
        int x = (int) Math.floor(startX);
        int y = (int) Math.floor(startY);
        int z = (int) Math.floor(startZ);

        // Direction signs
        int stepX = dirX > 0 ? 1 : -1;
        int stepY = dirY > 0 ? 1 : -1;
        int stepZ = dirZ > 0 ? 1 : -1;

        // Distance to next voxel boundary
        double tMaxX = intbound(startX, dirX);
        double tMaxY = intbound(startY, dirY);
        double tMaxZ = intbound(startZ, dirZ);

        // Distance to travel one voxel in each direction
        double tDeltaX = stepX / dirX;
        double tDeltaY = stepY / dirY;
        double tDeltaZ = stepZ / dirZ;

        // Track which face we entered from
        int faceX = 0, faceY = 0, faceZ = 0;

        double radius = maxDistance / Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);

        while (true) {
            // Check current voxel
            String block = world.getBlock(x, y, z);
            if (block != null && !block.equals("space_test:air") && world.isSolid(x, y, z)) {
                double dx = x + 0.5 - startX;
                double dy = y + 0.5 - startY;
                double dz = z + 0.5 - startZ;
                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                return new RaycastResult(true, x, y, z, -faceX, -faceY, -faceZ, distance);
            }

            // Advance to next voxel
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > radius) break;
                    x += stepX;
                    tMaxX += tDeltaX;
                    faceX = stepX;
                    faceY = 0;
                    faceZ = 0;
                } else {
                    if (tMaxZ > radius) break;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    faceX = 0;
                    faceY = 0;
                    faceZ = stepZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > radius) break;
                    y += stepY;
                    tMaxY += tDeltaY;
                    faceX = 0;
                    faceY = stepY;
                    faceZ = 0;
                } else {
                    if (tMaxZ > radius) break;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    faceX = 0;
                    faceY = 0;
                    faceZ = stepZ;
                }
            }
        }

        return RaycastResult.miss();
    }

    /**
     * Calculate distance to the next integer boundary
     */
    private static double intbound(double s, double ds) {
        if (ds < 0) {
            return intbound(-s, -ds);
        } else {
            s = mod(s, 1);
            return (1 - s) / ds;
        }
    }

    private static double mod(double value, double modulus) {
        return (value % modulus + modulus) % modulus;
    }
}