package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * ENHANCED: Renders 3D isometric item icons with real block textures and procedural tool textures
 *
 * Features:
 * - Uses actual block textures from TextureAtlas
 * - 3D isometric projection
 * - Procedurally generated tool/item textures
 * - Caching for performance
 */
public class ItemIconRenderer {

    private static final Map<String, BufferedImage> iconCache = new HashMap<>();
    private static TextureAtlas textureAtlas;

    /**
     * Initialize with texture atlas
     */
    public static void init(TextureAtlas atlas) {
        textureAtlas = atlas;
        clearCache(); // Clear any old cache
    }

    /**
     * Render an item icon
     */
    public static void renderItemIcon(Graphics2D g, ItemStack stack, int x, int y, int size) {
        if (stack.isEmpty()) return;

        // Get or create cached icon
        String cacheKey = stack.getBlockId() + "_" + size;
        BufferedImage icon = iconCache.get(cacheKey);

        if (icon == null) {
            icon = createItemIcon(stack.getBlockId(), size);
            iconCache.put(cacheKey, icon);
        }

        // Draw icon
        g.drawImage(icon, x, y, null);

        // Draw count if > 1
        if (stack.getCount() > 1) {
            drawItemCount(g, stack.getCount(), x, y, size);
        }
    }

    /**
     * Create item icon based on type
     */
    private static BufferedImage createItemIcon(String blockId, int size) {
        Block block = Registries.BLOCKS.get(blockId);

        if (block != null && !blockId.equals("space_test:air")) {
            // It's a block - render as 3D cube with actual textures
            return createBlockIcon(block, size);
        } else {
            // It's an item - render procedurally
            return createProceduralItemIcon(blockId, size);
        }
    }

    /**
     * Create 3D isometric block icon using actual block textures
     */
    private static BufferedImage createBlockIcon(Block block, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[] texIndices = block.getTextureIndices();

        // Isometric cube parameters
        int cubeSize = (int)(size * 0.85);
        int offsetX = (size - cubeSize) / 2;
        int offsetY = (int)(size * 0.15);

        int cx = offsetX + cubeSize / 2;
        int cy = offsetY + cubeSize / 2;
        int w = cubeSize / 2;
        int h = cubeSize / 4;
        int depth = cubeSize / 3;

        // Get actual textures from atlas
        if (textureAtlas != null) {
            // Top face (index 1) - brightest
            drawTexturedIsometricFace(g, cx, cy - depth, w, h, texIndices[1], 1.0f);

            // Right face (index 5) - medium
            drawTexturedIsometricFaceRight(g, cx, cy - depth + h, w, depth, texIndices[5], 0.8f);

            // Left face (index 4) - darkest
            drawTexturedIsometricFaceLeft(g, cx, cy - depth + h, w, depth, texIndices[4], 0.6f);
        } else {
            // Fallback to solid colors
            Color baseColor = getBlockBaseColor(block.getId());
            drawSolidIsometricCube(g, cx, cy, w, h, depth, baseColor);
        }

        // Add highlight for shiny effect
        g.setColor(new Color(255, 255, 255, 80));
        int hlSize = w / 3;
        g.fillOval(cx - hlSize/2, cy - depth + h - hlSize/2, hlSize, hlSize);

        g.dispose();
        return img;
    }

    /**
     * Draw textured top face (diamond shape)
     */
    private static void drawTexturedIsometricFace(Graphics2D g, int cx, int cy, int w, int h,
                                                  int texIndex, float brightness) {
        // Create polygon for top face
        int[] xPoints = {cx, cx + w, cx, cx - w};
        int[] yPoints = {cy, cy + h, cy + h * 2, cy + h};

        // Sample texture and draw
        BufferedImage faceTex = sampleTextureForFace(texIndex, w * 2, h * 2, brightness);
        if (faceTex != null) {
            // Draw textured polygon
            Polygon poly = new Polygon(xPoints, yPoints, 4);
            Shape oldClip = g.getClip();
            g.setClip(poly);
            g.drawImage(faceTex, cx - w, cy, w * 2, h * 2, null);
            g.setClip(oldClip);
        }

        // Draw outline
        g.setColor(new Color(0, 0, 0, 100));
        g.drawPolygon(xPoints, yPoints, 4);
    }

    /**
     * Draw textured right face
     */
    private static void drawTexturedIsometricFaceRight(Graphics2D g, int cx, int cy,
                                                       int w, int depth, int texIndex, float brightness) {
        int[] xPoints = {cx, cx + w, cx + w, cx};
        int[] yPoints = {cy + depth, cy, cy + depth, cy + depth * 2};

        BufferedImage faceTex = sampleTextureForFace(texIndex, w, depth * 2, brightness);
        if (faceTex != null) {
            Polygon poly = new Polygon(xPoints, yPoints, 4);
            Shape oldClip = g.getClip();
            g.setClip(poly);
            g.drawImage(faceTex, cx, cy, w, depth * 2, null);
            g.setClip(oldClip);
        }

        g.setColor(new Color(0, 0, 0, 100));
        g.drawPolygon(xPoints, yPoints, 4);
    }

    /**
     * Draw textured left face
     */
    private static void drawTexturedIsometricFaceLeft(Graphics2D g, int cx, int cy,
                                                      int w, int depth, int texIndex, float brightness) {
        int[] xPoints = {cx, cx - w, cx - w, cx};
        int[] yPoints = {cy + depth, cy, cy + depth, cy + depth * 2};

        BufferedImage faceTex = sampleTextureForFace(texIndex, w, depth * 2, brightness);
        if (faceTex != null) {
            Polygon poly = new Polygon(xPoints, yPoints, 4);
            Shape oldClip = g.getClip();
            g.setClip(poly);
            g.drawImage(faceTex, cx - w, cy, w, depth * 2, null);
            g.setClip(oldClip);
        }

        g.setColor(new Color(0, 0, 0, 100));
        g.drawPolygon(xPoints, yPoints, 4);
    }

    /**
     * Sample texture from atlas and apply brightness
     */
    private static BufferedImage sampleTextureForFace(int texIndex, int width, int height, float brightness) {
        if (textureAtlas == null) return null;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double u = (double)x / width;
                double v = (double)y / height;

                int color = textureAtlas.sample(texIndex, u, v);

                // Apply brightness
                int r = (int)(((color >> 16) & 0xFF) * brightness);
                int g = (int)(((color >> 8) & 0xFF) * brightness);
                int b = (int)((color & 0xFF) * brightness);

                img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }

        return img;
    }

    /**
     * Fallback solid color cube
     */
    private static void drawSolidIsometricCube(Graphics2D g, int cx, int cy, int w, int h,
                                               int depth, Color baseColor) {
        // Top face
        int[] topX = {cx, cx + w, cx, cx - w};
        int[] topY = {cy - depth, cy - depth + h, cy - depth + h * 2, cy - depth + h};
        g.setColor(baseColor.brighter());
        g.fillPolygon(topX, topY, 4);
        g.setColor(baseColor.darker());
        g.drawPolygon(topX, topY, 4);

        // Right face
        int[] rightX = {cx, cx + w, cx + w, cx};
        int[] rightY = {cy - depth + h * 2, cy - depth + h, cy + h, cy + h * 2};
        g.setColor(baseColor);
        g.fillPolygon(rightX, rightY, 4);
        g.setColor(baseColor.darker());
        g.drawPolygon(rightX, rightY, 4);

        // Left face
        int[] leftX = {cx, cx - w, cx - w, cx};
        int[] leftY = {cy - depth + h * 2, cy - depth + h, cy + h, cy + h * 2};
        g.setColor(baseColor.darker());
        g.fillPolygon(leftX, leftY, 4);
        g.setColor(baseColor.darker().darker());
        g.drawPolygon(leftX, leftY, 4);
    }

    /**
     * Create procedural item texture (for tools, food, etc.)
     */
    private static BufferedImage createProceduralItemIcon(String itemId, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Determine item type and render accordingly
        if (itemId.contains("pickaxe")) {
            drawPickaxe(g, size, getToolMaterial(itemId));
        } else if (itemId.contains("axe") && !itemId.contains("pickaxe")) {
            drawAxe(g, size, getToolMaterial(itemId));
        } else if (itemId.contains("shovel")) {
            drawShovel(g, size, getToolMaterial(itemId));
        } else if (itemId.contains("sword")) {
            drawSword(g, size, getToolMaterial(itemId));
        } else if (itemId.contains("stick")) {
            drawStick(g, size);
        } else if (itemId.contains("coal")) {
            drawCoal(g, size);
        } else if (itemId.contains("ingot")) {
            drawIngot(g, size, getIngotColor(itemId));
        } else if (itemId.contains("apple")) {
            drawApple(g, size);
        } else {
            // Generic item
            drawGenericItem(g, size, getItemColor(itemId));
        }

        g.dispose();
        return img;
    }

    /**
     * Draw pickaxe tool
     */
    private static void drawPickaxe(Graphics2D g, int size, Color material) {
        int centerX = size / 2;
        int centerY = size / 2;

        // Handle (stick)
        g.setColor(new Color(139, 69, 19));
        g.fillRect(centerX - 2, centerY, 4, size / 2);

        // Pickaxe head
        g.setColor(material);
        int[] headX = {centerX - size/3, centerX + size/3, centerX + size/4, centerX - size/4};
        int[] headY = {centerY - size/6, centerY - size/6, centerY + size/8, centerY + size/8};
        g.fillPolygon(headX, headY, 4);

        // Highlight
        g.setColor(material.brighter());
        g.fillRect(centerX - size/3, centerY - size/6, 3, size/4);

        // Outline
        g.setColor(material.darker());
        g.drawPolygon(headX, headY, 4);
    }

    /**
     * Draw axe tool
     */
    private static void drawAxe(Graphics2D g, int size, Color material) {
        int centerX = size / 2;
        int centerY = size / 2;

        // Handle
        g.setColor(new Color(139, 69, 19));
        g.fillRect(centerX - 2, centerY, 4, size / 2);

        // Axe head
        g.setColor(material);
        int[] headX = {centerX - size/4, centerX + size/3, centerX + size/4, centerX};
        int[] headY = {centerY, centerY - size/5, centerY + size/6, centerY + size/5};
        g.fillPolygon(headX, headY, 4);

        // Highlight
        g.setColor(material.brighter());
        g.fillRect(centerX, centerY - size/5, 3, size/3);

        // Outline
        g.setColor(material.darker());
        g.drawPolygon(headX, headY, 4);
    }

    /**
     * Draw shovel tool
     */
    private static void drawShovel(Graphics2D g, int size, Color material) {
        int centerX = size / 2;
        int centerY = size / 2;

        // Handle
        g.setColor(new Color(139, 69, 19));
        g.fillRect(centerX - 2, centerY - size/4, 4, size * 3/4);

        // Shovel head
        g.setColor(material);
        int[] headX = {centerX - size/4, centerX + size/4, centerX};
        int[] headY = {centerY, centerY, centerY + size/3};
        g.fillPolygon(headX, headY, 3);

        // Highlight
        g.setColor(material.brighter());
        g.fillRect(centerX - 2, centerY, 4, size/3);

        // Outline
        g.setColor(material.darker());
        g.drawPolygon(headX, headY, 3);
    }

    /**
     * Draw sword
     */
    private static void drawSword(Graphics2D g, int size, Color material) {
        int centerX = size / 2;
        int centerY = size / 2;

        // Blade
        g.setColor(material);
        int[] bladeX = {centerX - size/8, centerX + size/8, centerX + size/10, centerX - size/10};
        int[] bladeY = {centerY + size/6, centerY + size/6, centerY - size/3, centerY - size/3};
        g.fillPolygon(bladeX, bladeY, 4);

        // Guard
        g.setColor(new Color(139, 69, 19));
        g.fillRect(centerX - size/3, centerY + size/6, size * 2/3, 3);

        // Handle
        g.fillRect(centerX - 2, centerY + size/6, 4, size/4);

        // Pommel
        g.fillOval(centerX - 4, centerY + size/3, 8, 8);

        // Blade highlight
        g.setColor(material.brighter());
        g.fillRect(centerX - size/10, centerY - size/3, 2, size/2);

        // Outline
        g.setColor(material.darker());
        g.drawPolygon(bladeX, bladeY, 4);
    }

    /**
     * Draw stick
     */
    private static void drawStick(Graphics2D g, int size) {
        int centerX = size / 2;

        // Main stick body
        g.setColor(new Color(139, 69, 19));
        g.fillRect(centerX - 2, size / 6, 4, size * 2/3);

        // Wood grain lines
        g.setColor(new Color(120, 60, 15));
        for (int i = 0; i < 3; i++) {
            int y = size / 6 + (i * size / 5);
            g.drawLine(centerX - 2, y, centerX + 2, y);
        }

        // Highlight
        g.setColor(new Color(160, 90, 30));
        g.drawLine(centerX - 1, size / 6, centerX - 1, size * 5/6);
    }

    /**
     * Draw coal
     */
    private static void drawCoal(Graphics2D g, int size) {
        int centerX = size / 2;
        int centerY = size / 2;

        // Irregular coal shape
        int[] x = {centerX - size/3, centerX, centerX + size/3, centerX + size/4,
                centerX, centerX - size/4};
        int[] y = {centerY - size/6, centerY - size/3, centerY - size/6, centerY + size/4,
                centerY + size/3, centerY + size/4};

        // Base coal color
        g.setColor(new Color(40, 40, 40));
        g.fillPolygon(x, y, 6);

        // Shiny spots
        g.setColor(new Color(80, 80, 80));
        g.fillOval(centerX - 3, centerY - size/8, 6, 6);
        g.fillOval(centerX + size/8, centerY, 4, 4);

        // Outline
        g.setColor(Color.BLACK);
        g.drawPolygon(x, y, 6);
    }

    /**
     * Draw ingot (iron, gold, diamond)
     */
    private static void drawIngot(Graphics2D g, int size, Color color) {
        int centerX = size / 2;
        int centerY = size / 2;

        // Ingot shape (rectangle with perspective)
        int w = size / 2;
        int h = size / 3;

        // Main body
        g.setColor(color);
        g.fillRect(centerX - w/2, centerY - h/2, w, h);

        // Top face (lighter)
        int[] topX = {centerX - w/2, centerX + w/2, centerX + w/2 + 5, centerX - w/2 + 5};
        int[] topY = {centerY - h/2, centerY - h/2, centerY - h/2 - 5, centerY - h/2 - 5};
        g.setColor(color.brighter());
        g.fillPolygon(topX, topY, 4);

        // Right face (darker)
        int[] rightX = {centerX + w/2, centerX + w/2 + 5, centerX + w/2 + 5, centerX + w/2};
        int[] rightY = {centerY - h/2, centerY - h/2 - 5, centerY + h/2 - 5, centerY + h/2};
        g.setColor(color.darker());
        g.fillPolygon(rightX, rightY, 4);

        // Outline
        g.setColor(color.darker().darker());
        g.drawRect(centerX - w/2, centerY - h/2, w, h);
    }

    /**
     * Draw apple
     */
    private static void drawApple(Graphics2D g, int size) {
        int centerX = size / 2;
        int centerY = size / 2;

        // Apple body
        g.setColor(new Color(200, 50, 50));
        g.fillOval(centerX - size/3, centerY - size/4, size * 2/3, size * 2/3);

        // Highlight
        g.setColor(new Color(255, 100, 100));
        g.fillOval(centerX - size/4, centerY - size/6, size/4, size/4);

        // Stem
        g.setColor(new Color(139, 69, 19));
        g.fillRect(centerX - 1, centerY - size/3, 2, size/6);

        // Leaf
        g.setColor(new Color(50, 150, 50));
        int[] leafX = {centerX, centerX + size/6, centerX + size/8};
        int[] leafY = {centerY - size/4, centerY - size/3, centerY - size/6};
        g.fillPolygon(leafX, leafY, 3);
    }

    /**
     * Draw generic item
     */
    private static void drawGenericItem(Graphics2D g, int size, Color color) {
        int centerX = size / 2;
        int centerY = size / 2;
        int itemSize = size * 2/3;

        // Simple rounded rectangle
        g.setColor(color);
        g.fillRoundRect(centerX - itemSize/2, centerY - itemSize/2,
                itemSize, itemSize, 8, 8);

        // Highlight
        g.setColor(color.brighter());
        g.fillRoundRect(centerX - itemSize/2 + 2, centerY - itemSize/2 + 2,
                itemSize/3, itemSize/3, 4, 4);

        // Outline
        g.setColor(color.darker());
        g.drawRoundRect(centerX - itemSize/2, centerY - itemSize/2,
                itemSize, itemSize, 8, 8);
    }

    /**
     * Draw item count
     */
    private static void drawItemCount(Graphics2D g, int count, int x, int y, int size) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, Math.max(10, size / 3)));
        String countStr = String.valueOf(count);

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(countStr);
        int textX = x + size - textWidth - 2;
        int textY = y + size - 2;

        // Shadow
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(countStr, textX + 1, textY + 1);

        // Text
        g.setColor(Color.WHITE);
        g.drawString(countStr, textX, textY);
    }

    // Helper methods

    private static Color getToolMaterial(String itemId) {
        if (itemId.contains("wooden")) return new Color(139, 69, 19);
        if (itemId.contains("stone")) return new Color(128, 128, 128);
        if (itemId.contains("iron")) return new Color(200, 200, 200);
        if (itemId.contains("gold")) return new Color(255, 215, 0);
        if (itemId.contains("diamond")) return new Color(100, 200, 255);
        return new Color(128, 128, 128);
    }

    private static Color getIngotColor(String itemId) {
        if (itemId.contains("iron")) return new Color(200, 200, 200);
        if (itemId.contains("gold")) return new Color(255, 215, 0);
        return new Color(150, 150, 150);
    }

    private static Color getItemColor(String itemId) {
        if (itemId.contains("diamond")) return new Color(100, 200, 255);
        if (itemId.contains("bucket")) return new Color(180, 180, 180);
        if (itemId.contains("bread")) return new Color(210, 180, 140);
        return new Color(200, 200, 200);
    }

    private static Color getBlockBaseColor(String blockId) {
        return switch(blockId) {
            case "space_test:stone" -> new Color(128, 128, 128);
            case "space_test:dirt" -> new Color(139, 69, 19);
            case "space_test:grass" -> new Color(34, 139, 34);
            case "space_test:wood" -> new Color(160, 82, 45);
            case "space_test:planks" -> new Color(180, 120, 60);
            case "space_test:leaves" -> new Color(0, 128, 0);
            case "space_test:crafting_table" -> new Color(160, 82, 45);
            case "space_test:furnace" -> new Color(90, 90, 90);
            case "space_test:chest" -> new Color(150, 100, 50);
            default -> new Color(200, 200, 200);
        };
    }

    /**
     * Clear icon cache (call when textures change)
     */
    public static void clearCache() {
        iconCache.clear();
    }
}