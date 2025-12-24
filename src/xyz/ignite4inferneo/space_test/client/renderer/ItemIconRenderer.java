package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.inventory.ItemStack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders 3D isometric item icons for inventory display
 * Creates nice-looking cube previews of blocks
 */
public class ItemIconRenderer {

    private static final Map<String, BufferedImage> iconCache = new HashMap<>();

    /**
     * Render an item icon as a 3D cube
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
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            String countStr = String.valueOf(stack.getCount());

            // Shadow
            g.setColor(new Color(0, 0, 0, 180));
            g.drawString(countStr, x + size - g.getFontMetrics().stringWidth(countStr) - 1,
                    y + size - 1);

            // Text
            g.setColor(Color.WHITE);
            g.drawString(countStr, x + size - g.getFontMetrics().stringWidth(countStr) - 2,
                    y + size - 2);
        }
    }

    /**
     * Create a 3D isometric cube icon for a block
     */
    private static BufferedImage createItemIcon(String blockId, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Enable antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Block block = Registries.BLOCKS.get(blockId);
        if (block == null) {
            // Unknown block - render as question mark
            g.setColor(Color.MAGENTA);
            g.fillRect(0, 0, size, size);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, size / 2));
            g.drawString("?", size / 3, size * 2 / 3);
            g.dispose();
            return img;
        }

        // Get block colors
        Color baseColor = getBlockColor(blockId);
        Color topColor = baseColor.brighter();
        Color sideColor = baseColor;
        Color darkSideColor = baseColor.darker();

        // Draw isometric cube
        int cubeSize = (int)(size * 0.85);
        int offsetX = (size - cubeSize) / 2;
        int offsetY = (int)(size * 0.15);

        // Calculate isometric points
        int cx = offsetX + cubeSize / 2;
        int cy = offsetY + cubeSize / 2;
        int w = cubeSize / 2;
        int h = cubeSize / 4;
        int depth = cubeSize / 3;

        // Top face (brightest)
        Polygon topFace = new Polygon();
        topFace.addPoint(cx, cy - depth);           // Top
        topFace.addPoint(cx + w, cy - depth + h);   // Right
        topFace.addPoint(cx, cy - depth + h * 2);   // Bottom
        topFace.addPoint(cx - w, cy - depth + h);   // Left

        g.setColor(topColor);
        g.fillPolygon(topFace);
        g.setColor(topColor.darker());
        g.drawPolygon(topFace);

        // Right face (medium)
        Polygon rightFace = new Polygon();
        rightFace.addPoint(cx, cy - depth + h * 2);      // Top left
        rightFace.addPoint(cx + w, cy - depth + h);      // Top right
        rightFace.addPoint(cx + w, cy + depth + h);      // Bottom right
        rightFace.addPoint(cx, cy + depth + h * 2);      // Bottom left

        g.setColor(sideColor);
        g.fillPolygon(rightFace);
        g.setColor(sideColor.darker());
        g.drawPolygon(rightFace);

        // Left face (darkest)
        Polygon leftFace = new Polygon();
        leftFace.addPoint(cx, cy - depth + h * 2);       // Top right
        leftFace.addPoint(cx - w, cy - depth + h);       // Top left
        leftFace.addPoint(cx - w, cy + depth + h);       // Bottom left
        leftFace.addPoint(cx, cy + depth + h * 2);       // Bottom right

        g.setColor(darkSideColor);
        g.fillPolygon(leftFace);
        g.setColor(darkSideColor.darker());
        g.drawPolygon(leftFace);

        // Add highlight for shiny effect
        g.setColor(new Color(255, 255, 255, 80));
        int hlSize = w / 3;
        g.fillOval(cx - hlSize/2, cy - depth + h - hlSize/2, hlSize, hlSize);

        g.dispose();
        return img;
    }

    /**
     * Get base color for a block
     */
    private static Color getBlockColor(String blockId) {
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