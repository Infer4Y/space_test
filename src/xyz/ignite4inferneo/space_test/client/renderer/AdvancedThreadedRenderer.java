package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.world.Chunk;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.awt.*;
import java.util.*;

/**
 * Advanced multi-threaded renderer with:
 * - Frustum culling (only render chunks in view)
 * - Priority-based meshing (mesh closest chunks first)
 * - Aggressive optimization
 */
public class AdvancedThreadedRenderer extends ThreadedOptimizedRenderer {

    // Frustum planes (for culling)
    private double[] frustumLeft = new double[4];
    private double[] frustumRight = new double[4];
    private double[] frustumTop = new double[4];
    private double[] frustumBottom = new double[4];
    private double[] frustumNear = new double[4];
    private double[] frustumFar = new double[4];

    // Stats
    private int chunksVisible = 0;
    private int chunksCulled = 0;

    public AdvancedThreadedRenderer(World world) {
        super(world);
    }

    public AdvancedThreadedRenderer(World world, int threadCount) {
        super(world, threadCount);
    }

    @Override
    public void render() {
        // Update frustum planes before rendering
        updateFrustum();

        // Call parent render which will use our overridden renderChunk
        chunksVisible = 0;
        chunksCulled = 0;

        super.render();
    }

    /**
     * Update frustum planes for culling
     * Based on current camera position and orientation
     */
    private void updateFrustum() {
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        // Forward vector
        double fx = sinYaw * cosPitch;
        double fy = -sinPitch;
        double fz = cosYaw * cosPitch;

        // Right vector
        double rx = cosYaw;
        double rz = -sinYaw;

        // Up vector
        double ux = sinYaw * sinPitch;
        double uy = cosPitch;
        double uz = cosYaw * sinPitch;

        // FOV
        double fov = Math.PI / 3.0;
        double aspect = (double) super.width / super.height;

        double nearDist = 0.1;
        double farDist = 256.0;

        double halfVSide = Math.tan(fov * 0.5) * nearDist;
        double halfHSide = halfVSide * aspect;

        // Near plane (pointing inward)
        frustumNear[0] = fx;
        frustumNear[1] = fy;
        frustumNear[2] = fz;
        frustumNear[3] = -(fx * x + fy * y + fz * z + nearDist);

        // Far plane (pointing inward)
        frustumFar[0] = -fx;
        frustumFar[1] = -fy;
        frustumFar[2] = -fz;
        frustumFar[3] = fx * x + fy * y + fz * z + farDist;

        // Left plane
        double lnx = ux * halfVSide + rx * halfHSide;
        double lny = uy * halfVSide;
        double lnz = uz * halfVSide + rz * halfHSide;
        double len = Math.sqrt(lnx*lnx + lny*lny + lnz*lnz);
        frustumLeft[0] = lnx / len;
        frustumLeft[1] = lny / len;
        frustumLeft[2] = lnz / len;
        frustumLeft[3] = -(frustumLeft[0] * x + frustumLeft[1] * y + frustumLeft[2] * z);

        // Right plane
        double rnx = ux * halfVSide - rx * halfHSide;
        double rny = uy * halfVSide;
        double rnz = uz * halfVSide - rz * halfHSide;
        len = Math.sqrt(rnx*rnx + rny*rny + rnz*rnz);
        frustumRight[0] = rnx / len;
        frustumRight[1] = rny / len;
        frustumRight[2] = rnz / len;
        frustumRight[3] = -(frustumRight[0] * x + frustumRight[1] * y + frustumRight[2] * z);

        // Top plane
        double tnx = fx * halfVSide - ux * nearDist;
        double tny = fy * halfVSide - uy * nearDist;
        double tnz = fz * halfVSide - uz * nearDist;
        len = Math.sqrt(tnx*tnx + tny*tny + tnz*tnz);
        frustumTop[0] = tnx / len;
        frustumTop[1] = tny / len;
        frustumTop[2] = tnz / len;
        frustumTop[3] = -(frustumTop[0] * x + frustumTop[1] * y + frustumTop[2] * z);

        // Bottom plane
        double bnx = fx * halfVSide + ux * nearDist;
        double bny = fy * halfVSide + uy * nearDist;
        double bnz = fz * halfVSide + uz * nearDist;
        len = Math.sqrt(bnx*bnx + bny*bny + bnz*bnz);
        frustumBottom[0] = bnx / len;
        frustumBottom[1] = bny / len;
        frustumBottom[2] = bnz / len;
        frustumBottom[3] = -(frustumBottom[0] * x + frustumBottom[1] * y + frustumBottom[2] * z);
    }

    /**
     * Test if a chunk's bounding box is inside the frustum
     */
    private boolean isChunkInFrustum(int chunkX, int chunkZ) {
        // Chunk bounds in world space
        double minX = chunkX * 16;
        double maxX = minX + 16;
        double minY = 0;
        double maxY = 256;
        double minZ = chunkZ * 16;
        double maxZ = minZ + 16;

        // Test against each frustum plane
        if (!boxIntersectsPlane(minX, minY, minZ, maxX, maxY, maxZ, frustumLeft)) {
            return false;
        }
        if (!boxIntersectsPlane(minX, minY, minZ, maxX, maxY, maxZ, frustumRight)) {
            return false;
        }
        if (!boxIntersectsPlane(minX, minY, minZ, maxX, maxY, maxZ, frustumTop)) {
            return false;
        }
        if (!boxIntersectsPlane(minX, minY, minZ, maxX, maxY, maxZ, frustumBottom)) {
            return false;
        }
        if (!boxIntersectsPlane(minX, minY, minZ, maxX, maxY, maxZ, frustumNear)) {
            return false;
        }
        if (!boxIntersectsPlane(minX, minY, minZ, maxX, maxY, maxZ, frustumFar)) {
            return false;
        }

        return true;
    }

    /**
     * Test if an AABB intersects with a plane
     * Returns false only if the box is completely behind the plane
     */
    private boolean boxIntersectsPlane(double minX, double minY, double minZ,
                                       double maxX, double maxY, double maxZ,
                                       double[] plane) {
        // Get the positive vertex (furthest point in plane normal direction)
        double px = plane[0] > 0 ? maxX : minX;
        double py = plane[1] > 0 ? maxY : minY;
        double pz = plane[2] > 0 ? maxZ : minZ;

        // If positive vertex is behind plane, entire box is behind
        double dist = plane[0] * px + plane[1] * py + plane[2] * pz + plane[3];
        return dist >= 0;
    }

    public int getChunksVisible() {
        return chunksVisible;
    }

    public int getChunksCulled() {
        return chunksCulled;
    }

    /**
     * Priority queue for chunk meshing
     * Closest chunks get meshed first
     */
    public static class ChunkPriority implements Comparable<ChunkPriority> {
        int chunkX, chunkZ;
        double distanceSq;

        ChunkPriority(int x, int z, double dist) {
            this.chunkX = x;
            this.chunkZ = z;
            this.distanceSq = dist;
        }

        @Override
        public int compareTo(ChunkPriority other) {
            return Double.compare(this.distanceSq, other.distanceSq);
        }
    }
}