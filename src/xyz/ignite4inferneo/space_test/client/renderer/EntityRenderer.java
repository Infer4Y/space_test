package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.entity.*;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders all types of entities in the world
 *
 * Features:
 * - 3D billboarded sprites (always face camera)
 * - Player rendering with name tags
 * - Mob rendering with health bars
 * - Item entities with bobbing animation
 * - Projectile rendering
 * - Distance-based LOD (level of detail)
 */
public class EntityRenderer {

    // Entity colors (since we don't have textures yet)
    private static final Map<String, Color> ENTITY_COLORS = new HashMap<>();

    static {
        ENTITY_COLORS.put("player", new Color(100, 150, 255));
        ENTITY_COLORS.put("zombie", new Color(100, 150, 100));
        ENTITY_COLORS.put("pig", new Color(255, 180, 180));
        ENTITY_COLORS.put("item", new Color(255, 255, 100));
    }

    /**
     * Render all entities to graphics context
     */
    public static void renderEntities(Graphics2D g, java.util.Collection<Entity> entities,
                                      double camX, double camY, double camZ,
                                      double camYaw, double camPitch,
                                      int screenWidth, int screenHeight) {

        // Calculate camera vectors
        double cosPitch = Math.cos(camPitch);
        double sinPitch = Math.sin(camPitch);
        double cosYaw = Math.cos(camYaw);
        double sinYaw = Math.sin(camYaw);

        double fx = sinYaw * cosPitch;
        double fy = -sinPitch;
        double fz = cosYaw * cosPitch;
        double rx = cosYaw;
        double rz = -sinYaw;
        double ux = sinYaw * sinPitch;
        double uy = cosPitch;
        double uz = cosYaw * sinPitch;

        double fov = Math.PI / 3.0;
        double invTanHalfFov = 1.0 / Math.tan(fov * 0.5);
        double halfWidth = screenWidth * 0.5;
        double halfHeight = screenHeight * 0.5;

        // Sort entities by distance (far to near for proper transparency)
        java.util.List<Entity> sortedEntities = new java.util.ArrayList<>(entities);
        sortedEntities.sort((a, b) -> {
            double distA = distanceSquared(a.x, a.y, a.z, camX, camY, camZ);
            double distB = distanceSquared(b.x, b.y, b.z, camX, camY, camZ);
            return Double.compare(distB, distA); // Far to near
        });

        // Render each entity
        for (Entity entity : sortedEntities) {
            // Skip if too far
            double distSq = distanceSquared(entity.x, entity.y, entity.z, camX, camY, camZ);
            if (distSq > 100 * 100) continue; // 100 block render distance

            // Calculate screen position
            double dx = entity.x - camX;
            double dy = entity.y + entity.height / 2 - camY; // Center of entity
            double dz = entity.z - camZ;

            // Camera space
            double camDepth = dx * fx + dy * fy + dz * fz;
            if (camDepth < 0.1) continue; // Behind camera

            // Project to screen
            double scale = invTanHalfFov / camDepth;
            int screenX = (int)(halfWidth + (dx * rx + dz * rz) * scale * halfHeight);
            int screenY = (int)(halfHeight - (dx * ux + dy * uy + dz * uz) * scale * halfHeight);

            // Calculate entity size on screen
            int entitySize = (int)(entity.height * scale * halfHeight);
            entitySize = Math.max(8, Math.min(entitySize, 200)); // Clamp size

            // Render based on entity type
            if (entity instanceof PlayerEntity) {
                renderPlayer(g, (PlayerEntity)entity, screenX, screenY, entitySize, camDepth);
            } else if (entity instanceof ItemEntity) {
                renderItemEntity(g, (ItemEntity)entity, screenX, screenY, entitySize, camDepth);
            } else if (entity instanceof MobEntity) {
                renderMob(g, (MobEntity)entity, screenX, screenY, entitySize, camDepth);
            } else if (entity instanceof Projectile) {
                renderProjectile(g, (Projectile)entity, screenX, screenY, entitySize, camDepth);
            } else {
                renderGenericEntity(g, entity, screenX, screenY, entitySize, camDepth);
            }
        }
    }

    /**
     * Render a player entity
     */
    private static void renderPlayer(Graphics2D g, PlayerEntity player,
                                     int screenX, int screenY, int size, double depth) {
        // Player body (simple rectangle for now)
        int bodyWidth = size / 3;
        int bodyHeight = size;

        Color playerColor = ENTITY_COLORS.getOrDefault("player", Color.BLUE);

        // Apply distance fog
        playerColor = applyDistanceFog(playerColor, depth);

        // Body
        g.setColor(playerColor);
        g.fillRect(screenX - bodyWidth/2, screenY - bodyHeight/2, bodyWidth, bodyHeight);

        // Outline
        g.setColor(playerColor.darker());
        g.drawRect(screenX - bodyWidth/2, screenY - bodyHeight/2, bodyWidth, bodyHeight);

        // Head
        int headSize = bodyWidth;
        g.setColor(playerColor.brighter());
        g.fillRect(screenX - headSize/2, screenY - bodyHeight/2 - headSize, headSize, headSize);
        g.setColor(playerColor.darker());
        g.drawRect(screenX - headSize/2, screenY - bodyHeight/2 - headSize, headSize, headSize);

        // Sprint indicator
        if (player.isSprinting()) {
            g.setColor(Color.YELLOW);
            int indicatorY = screenY - bodyHeight/2 - headSize - 5;
            g.fillRect(screenX - 3, indicatorY, 6, 3);
        }

        // Sneak indicator
        if (player.isSneaking()) {
            g.setColor(Color.CYAN);
            int indicatorY = screenY + bodyHeight/2 + 3;
            g.fillRect(screenX - 3, indicatorY, 6, 3);
        }

        // Name tag (always visible)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        String name = player.getUsername();
        int nameWidth = g.getFontMetrics().stringWidth(name);
        int nameY = screenY - bodyHeight/2 - headSize - 15;

        // Name background
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(screenX - nameWidth/2 - 2, nameY - 12, nameWidth + 4, 14);

        // Name text
        g.setColor(Color.WHITE);
        g.drawString(name, screenX - nameWidth/2, nameY);

        // Health bar
        renderHealthBar(g, player, screenX, nameY - 5, size / 2);
    }

    /**
     * Render a mob entity
     */
    private static void renderMob(Graphics2D g, MobEntity mob,
                                  int screenX, int screenY, int size, double depth) {
        Color mobColor = ENTITY_COLORS.getOrDefault(mob.getType(), Color.GREEN);
        mobColor = applyDistanceFog(mobColor, depth);

        int bodyWidth = size / 3;
        int bodyHeight = (int)(size * 0.8);

        // Body
        g.setColor(mobColor);
        g.fillRect(screenX - bodyWidth/2, screenY - bodyHeight/2, bodyWidth, bodyHeight);
        g.setColor(mobColor.darker());
        g.drawRect(screenX - bodyWidth/2, screenY - bodyHeight/2, bodyWidth, bodyHeight);

        // Head
        int headSize = bodyWidth;
        g.setColor(mobColor.brighter());
        g.fillRect(screenX - headSize/2, screenY - bodyHeight/2 - headSize/2,
                headSize, headSize);
        g.setColor(mobColor.darker());
        g.drawRect(screenX - headSize/2, screenY - bodyHeight/2 - headSize/2,
                headSize, headSize);

        // Health bar (only if damaged)
        if (mob.getHealth() < mob.getMaxHealth()) {
            renderHealthBar(g, mob, screenX, screenY - bodyHeight/2 - headSize/2 - 5, size / 2);
        }

        // AI state indicator (debug)
        if (mob.getAIState() == MobEntity.AIState.CHASE ||
                mob.getAIState() == MobEntity.AIState.ATTACK) {
            g.setColor(Color.RED);
            g.fillOval(screenX - 3, screenY - bodyHeight/2 - headSize - 3, 6, 6);
        }
    }

    /**
     * Render an item entity
     */
    private static void renderItemEntity(Graphics2D g, ItemEntity itemEntity,
                                         int screenX, int screenY, int size, double depth) {
        ItemStack stack = itemEntity.getItemStack();
        if (stack.isEmpty()) return;

        // Apply bobbing animation
        double bobHeight = itemEntity.getBobHeight();
        screenY -= (int)(bobHeight * 10); // Scale bobbing

        // Get item color based on block ID
        Color itemColor = getItemColor(stack.getBlockId());
        itemColor = applyDistanceFog(itemColor, depth);

        // Item cube (simplified)
        int cubeSize = Math.max(8, size / 4);

        // Draw as a small cube with rotation effect
        int rotation = (int)(itemEntity.yaw * 10) % 360;

        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(screenX - cubeSize/2, screenY + cubeSize/2, cubeSize, cubeSize/2);

        // Item body
        g.setColor(itemColor);
        g.fillRect(screenX - cubeSize/2, screenY - cubeSize/2, cubeSize, cubeSize);

        // Outline
        g.setColor(itemColor.darker());
        g.drawRect(screenX - cubeSize/2, screenY - cubeSize/2, cubeSize, cubeSize);

        // Highlight
        g.setColor(itemColor.brighter());
        g.fillRect(screenX - cubeSize/2 + 1, screenY - cubeSize/2 + 1,
                cubeSize/3, cubeSize/3);

        // Item count (if > 1)
        if (stack.getCount() > 1) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            String countStr = String.valueOf(stack.getCount());
            g.drawString(countStr, screenX + cubeSize/2 + 2, screenY + cubeSize/2);
        }
    }

    /**
     * Render a projectile
     */
    private static void renderProjectile(Graphics2D g, Projectile projectile,
                                         int screenX, int screenY, int size, double depth) {
        Color projColor = new Color(255, 100, 100);
        projColor = applyDistanceFog(projColor, depth);

        int projSize = Math.max(4, size / 8);

        // Projectile as a small oval
        g.setColor(projColor);
        g.fillOval(screenX - projSize/2, screenY - projSize/2, projSize, projSize);

        // Trail effect
        g.setColor(new Color(255, 150, 150, 100));
        g.fillOval(screenX - projSize, screenY - projSize/2, projSize * 2, projSize);
    }

    /**
     * Render generic entity
     */
    private static void renderGenericEntity(Graphics2D g, Entity entity,
                                            int screenX, int screenY, int size, double depth) {
        Color entityColor = ENTITY_COLORS.getOrDefault(entity.getType(), Color.GRAY);
        entityColor = applyDistanceFog(entityColor, depth);

        int width = size / 3;
        int height = size;

        g.setColor(entityColor);
        g.fillRect(screenX - width/2, screenY - height/2, width, height);
        g.setColor(entityColor.darker());
        g.drawRect(screenX - width/2, screenY - height/2, width, height);
    }

    /**
     * Render health bar for living entities
     */
    private static void renderHealthBar(Graphics2D g, LivingEntity entity,
                                        int x, int y, int width) {
        float healthPercent = entity.getHealth() / entity.getMaxHealth();
        int barWidth = width;
        int barHeight = 3;

        // Background
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x - barWidth/2, y, barWidth, barHeight);

        // Health
        Color healthColor;
        if (healthPercent > 0.6f) {
            healthColor = Color.GREEN;
        } else if (healthPercent > 0.3f) {
            healthColor = Color.YELLOW;
        } else {
            healthColor = Color.RED;
        }

        g.setColor(healthColor);
        g.fillRect(x - barWidth/2, y, (int)(barWidth * healthPercent), barHeight);

        // Border
        g.setColor(Color.BLACK);
        g.drawRect(x - barWidth/2, y, barWidth, barHeight);
    }

    /**
     * Get color for item based on block ID
     */
    private static Color getItemColor(String blockId) {
        return switch(blockId) {
            case "space_test:stone" -> new Color(128, 128, 128);
            case "space_test:dirt" -> new Color(139, 69, 19);
            case "space_test:grass" -> new Color(34, 139, 34);
            case "space_test:wood" -> new Color(160, 82, 45);
            case "space_test:planks" -> new Color(160, 82, 45);
            case "space_test:leaves" -> new Color(0, 128, 0);
            default -> new Color(200, 200, 200);
        };
    }

    /**
     * Apply distance-based fog to color
     */
    private static Color applyDistanceFog(Color color, double depth) {
        float fogStart = 40.0f;
        float fogEnd = 80.0f;

        if (depth < fogStart) return color;
        if (depth > fogEnd) {
            // Full fog
            return new Color(135, 206, 235, 50); // Sky blue, semi-transparent
        }

        // Interpolate
        float fogFactor = (float)((depth - fogStart) / (fogEnd - fogStart));
        fogFactor = Math.max(0, Math.min(1, fogFactor));

        int r = (int)(color.getRed() * (1 - fogFactor) + 135 * fogFactor);
        int g = (int)(color.getGreen() * (1 - fogFactor) + 206 * fogFactor);
        int b = (int)(color.getBlue() * (1 - fogFactor) + 235 * fogFactor);
        int a = (int)(255 * (1 - fogFactor * 0.8f)); // Fade out

        return new Color(r, g, b, a);
    }

    /**
     * Distance squared (faster than sqrt)
     */
    private static double distanceSquared(double x1, double y1, double z1,
                                          double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx*dx + dy*dy + dz*dz;
    }

    private static BufferedImage getMobTexture(String mobType) {
        return MobTextureGenerator.getTexture(mobType);
    }
}