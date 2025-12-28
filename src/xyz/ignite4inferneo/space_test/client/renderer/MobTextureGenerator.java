package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.entity.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Procedural texture generator for mobs
 * Creates detailed, recognizable textures for each mob type
 */
public class MobTextureGenerator {

    private static final Map<String, BufferedImage> textureCache = new HashMap<>();
    private static final int TEXTURE_SIZE = 32; // 32x32 texture

    /**
     * Get or create texture for a mob type
     */
    public static BufferedImage getTexture(String mobType) {
        return textureCache.computeIfAbsent(mobType, MobTextureGenerator::generateTexture);
    }

    /**
     * Generate texture based on mob type
     */
    private static BufferedImage generateTexture(String mobType) {
        return switch (mobType.toLowerCase()) {
            case "player" -> generatePlayerTexture();
            case "zombie" -> generateZombieTexture();
            case "pig" -> generatePigTexture();
            case "cow" -> generateCowTexture();
            case "sheep" -> generateSheepTexture();
            case "chicken" -> generateChickenTexture();
            default -> generateGenericMobTexture();
        };
    }

    /**
     * Generate player texture (Steve-like)
     */
    private static BufferedImage generatePlayerTexture() {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Skin tone
        Color skinBase = new Color(240, 180, 120);
        Color skinDark = new Color(200, 140, 90);

        // Shirt (blue)
        Color shirtBase = new Color(100, 150, 255);
        Color shirtDark = new Color(60, 100, 200);

        // Pants (dark blue)
        Color pantsBase = new Color(50, 50, 150);
        Color pantsDark = new Color(30, 30, 100);

        int unit = TEXTURE_SIZE / 8;

        // HEAD (top section)
        // Face
        fillRect(g, 1*unit, 0*unit, 2*unit, 2*unit, skinBase);
        // Hair
        fillRect(g, 1*unit, 0*unit, 2*unit, 1*unit, new Color(100, 60, 20));
        // Eyes
        setPixel(g, 1*unit + 2, 1*unit + 2, new Color(100, 200, 255)); // Left eye
        setPixel(g, 2*unit - 2, 1*unit + 2, new Color(100, 200, 255)); // Right eye
        // Mouth
        drawLine(g, 1*unit + 2, 1*unit + 5, 2*unit - 2, 1*unit + 5, new Color(200, 100, 100));

        // BODY (middle section)
        // Torso (shirt)
        fillRect(g, 1*unit, 2*unit, 2*unit, 3*unit, shirtBase);
        fillRect(g, 1*unit + 1, 2*unit, 2*unit - 2, 3*unit, shirtDark);

        // Arms
        fillRect(g, 0*unit, 2*unit, 1*unit, 3*unit, shirtBase);
        fillRect(g, 3*unit, 2*unit, 1*unit, 3*unit, shirtBase);
        fillRect(g, 0*unit, 4*unit + 2, 1*unit, 1*unit, skinBase); // Left hand
        fillRect(g, 3*unit, 4*unit + 2, 1*unit, 1*unit, skinBase); // Right hand

        // LEGS (bottom section)
        // Left leg
        fillRect(g, 1*unit, 5*unit, 1*unit, 3*unit, pantsBase);
        // Right leg
        fillRect(g, 2*unit, 5*unit, 1*unit, 3*unit, pantsBase);
        // Shoes
        fillRect(g, 1*unit, 7*unit + 4, 1*unit, unit/2, Color.BLACK);
        fillRect(g, 2*unit, 7*unit + 4, 1*unit, unit/2, Color.BLACK);

        g.dispose();
        return img;
    }

    /**
     * Generate zombie texture (like player but green and tattered)
     */
    private static BufferedImage generateZombieTexture() {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Zombie green skin
        Color skinBase = new Color(100, 150, 100);
        Color skinDark = new Color(60, 100, 60);

        // Tattered shirt (darker blue-green)
        Color shirtBase = new Color(60, 90, 100);
        Color shirtDark = new Color(40, 60, 70);

        // Dirty pants
        Color pantsBase = new Color(50, 50, 80);

        int unit = TEXTURE_SIZE / 8;

        // HEAD
        fillRect(g, 1*unit, 0*unit, 2*unit, 2*unit, skinBase);
        // Messy hair
        fillRect(g, 1*unit, 0*unit, 2*unit, 1*unit, new Color(40, 30, 20));
        // Add some patches
        setPixel(g, 1*unit + 1, 0*unit + 3, skinDark);
        setPixel(g, 2*unit - 1, 0*unit + 4, skinDark);

        // Eyes (red/glowing)
        setPixel(g, 1*unit + 2, 1*unit + 2, new Color(255, 50, 50));
        setPixel(g, 2*unit - 2, 1*unit + 2, new Color(255, 50, 50));
        // Mouth (darker, grimacing)
        drawLine(g, 1*unit + 2, 1*unit + 5, 2*unit - 2, 1*unit + 5, new Color(50, 20, 20));

        // BODY (torn shirt)
        fillRect(g, 1*unit, 2*unit, 2*unit, 3*unit, shirtBase);
        // Tears and holes
        setPixel(g, 1*unit + 2, 2*unit + 3, skinBase);
        setPixel(g, 2*unit - 2, 3*unit, skinBase);

        // Arms (torn sleeves, exposed skin)
        fillRect(g, 0*unit, 2*unit, 1*unit, 2*unit, shirtBase);
        fillRect(g, 0*unit, 4*unit, 1*unit, 2*unit, skinBase);
        fillRect(g, 3*unit, 2*unit, 1*unit, 2*unit, shirtBase);
        fillRect(g, 3*unit, 4*unit, 1*unit, 2*unit, skinBase);

        // LEGS
        fillRect(g, 1*unit, 5*unit, 1*unit, 3*unit, pantsBase);
        fillRect(g, 2*unit, 5*unit, 1*unit, 3*unit, pantsBase);

        g.dispose();
        return img;
    }

    /**
     * Generate pig texture (pink with snout)
     */
    private static BufferedImage generatePigTexture() {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Pink colors
        Color pinkBase = new Color(255, 180, 180);
        Color pinkDark = new Color(220, 140, 140);
        Color pinkLight = new Color(255, 210, 210);

        int unit = TEXTURE_SIZE / 8;

        // HEAD (pig head)
        fillRect(g, 1*unit, 0*unit, 2*unit, 2*unit, pinkBase);
        // Snout
        fillRect(g, 1*unit + 1, 1*unit, 2*unit - 2, 1*unit, pinkLight);
        // Nostrils
        setPixel(g, 1*unit + 2, 1*unit + 2, pinkDark);
        setPixel(g, 2*unit - 2, 1*unit + 2, pinkDark);
        // Eyes (small black dots)
        setPixel(g, 1*unit + 1, 0*unit + 5, Color.BLACK);
        setPixel(g, 2*unit - 1, 0*unit + 5, Color.BLACK);
        // Ears
        fillRect(g, 0*unit + 6, 0*unit + 2, 2, 3, pinkBase);
        fillRect(g, 3*unit - 2, 0*unit + 2, 2, 3, pinkBase);

        // BODY (rotund)
        fillRect(g, 0*unit + 6, 2*unit, 2*unit + 4, 3*unit, pinkBase);
        // Belly (lighter)
        fillRect(g, 1*unit, 3*unit, 2*unit, 2*unit, pinkLight);

        // LEGS (short and stubby)
        // Front legs
        fillRect(g, 1*unit, 5*unit, unit/2 + 1, 3*unit, pinkBase);
        fillRect(g, 2*unit - 2, 5*unit, unit/2 + 1, 3*unit, pinkBase);
        // Back legs
        fillRect(g, 1*unit, 7*unit, unit/2 + 1, 1*unit, pinkDark);
        fillRect(g, 2*unit - 2, 7*unit, unit/2 + 1, 1*unit, pinkDark);

        // Tail (curly - just a dot on back)
        setPixel(g, 3*unit - 2, 3*unit, pinkDark);
        setPixel(g, 3*unit - 1, 3*unit - 1, pinkDark);

        g.dispose();
        return img;
    }

    /**
     * Generate cow texture (black and white patches)
     */
    private static BufferedImage generateCowTexture() {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color white = new Color(255, 255, 255);
        Color black = Color.BLACK;
        Color gray = new Color(180, 180, 180);
        Color pink = new Color(255, 180, 180);

        int unit = TEXTURE_SIZE / 8;

        // HEAD (white base)
        fillRect(g, 1*unit, 0*unit, 2*unit, 2*unit, white);
        // Black patches
        fillRect(g, 1*unit, 0*unit, 1*unit, 1*unit, black);
        fillRect(g, 2*unit, 1*unit, 1*unit, 1*unit, black);
        // Eyes
        setPixel(g, 1*unit + 2, 1*unit + 2, black);
        setPixel(g, 2*unit - 2, 1*unit + 2, black);
        // Snout (pink)
        fillRect(g, 1*unit + 2, 1*unit + 4, unit + 2, 2, pink);
        // Nostrils
        setPixel(g, 1*unit + 2, 1*unit + 5, black);
        setPixel(g, 2*unit - 1, 1*unit + 5, black);
        // Horns
        setPixel(g, 1*unit, 0*unit, gray);
        setPixel(g, 2*unit + unit - 1, 0*unit, gray);

        // BODY (white with black patches)
        fillRect(g, 0*unit + 4, 2*unit, 3*unit, 3*unit, white);
        // Large black patch
        fillRect(g, 1*unit, 2*unit + 2, 2*unit, 2*unit, black);
        fillRect(g, 2*unit + 2, 3*unit, unit, unit + 2, black);

        // LEGS
        fillRect(g, 1*unit, 5*unit, unit/2 + 1, 3*unit, white);
        fillRect(g, 2*unit, 5*unit, unit/2 + 1, 3*unit, white);
        fillRect(g, 1*unit, 6*unit, unit/2 + 1, 3*unit, gray);
        fillRect(g, 2*unit, 6*unit, unit/2 + 1, 3*unit, gray);
        // Hooves (black)
        fillRect(g, 1*unit, 7*unit + 4, unit/2 + 1, 2, black);
        fillRect(g, 2*unit, 7*unit + 4, unit/2 + 1, 2, black);

        // Udder (pink, on bottom)
        fillRect(g, 1*unit + 2, 5*unit - 2, unit, 2, pink);

        g.dispose();
        return img;
    }

    /**
     * Generate sheep texture (fluffy white)
     */
    private static BufferedImage generateSheepTexture() {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color wool = new Color(245, 245, 245);
        Color face = new Color(60, 60, 60);
        Color black = Color.BLACK;

        int unit = TEXTURE_SIZE / 8;

        // HEAD (dark face)
        fillRect(g, 1*unit, 0*unit, 2*unit, 2*unit, face);
        // Wool on head
        fillRect(g, 1*unit, 0*unit, 2*unit, unit/2, wool);
        fillRect(g, 1*unit, 0*unit + 2, unit/2, unit, wool);
        fillRect(g, 2*unit + unit/2, 0*unit + 2, unit/2, unit, wool);
        // Eyes
        setPixel(g, 1*unit + 2, 1*unit + 2, black);
        setPixel(g, 2*unit - 2, 1*unit + 2, black);
        // Ears
        fillRect(g, 1*unit - 2, 0*unit + 4, 2, 3, face);
        fillRect(g, 2*unit + unit, 0*unit + 4, 2, 3, face);

        // BODY (fluffy wool - larger)
        fillRect(g, 0*unit + 2, 2*unit, 3*unit + 4, 3*unit, wool);
        // Add texture to wool (dots for fluff)
        Random rand = new Random(42); // Deterministic
        for (int i = 0; i < 30; i++) {
            int x = 2 + rand.nextInt(3*unit + 2);
            int y = 2*unit + rand.nextInt(3*unit);
            setPixel(g, x, y, new Color(230, 230, 230));
        }

        // LEGS (thin, dark)
        fillRect(g, 1*unit, 5*unit, unit/2, 3*unit, face);
        fillRect(g, 2*unit + 2, 5*unit, unit/2, 3*unit, face);

        g.dispose();
        return img;
    }

    /**
     * Generate chicken texture (white with red comb)
     */
    private static BufferedImage generateChickenTexture() {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color feathers = new Color(255, 255, 255);
        Color beak = new Color(255, 200, 0);
        Color comb = new Color(255, 50, 50);
        Color black = Color.BLACK;

        int unit = TEXTURE_SIZE / 8;

        // HEAD (small)
        fillRect(g, 1*unit + 2, 0*unit + 2, unit + 2, unit + 2, feathers);
        // Comb (red on top)
        fillRect(g, 1*unit + 3, 0*unit, unit, 2, comb);
        setPixel(g, 1*unit + 4, 0*unit + 1, comb);
        // Eyes
        setPixel(g, 1*unit + 3, 0*unit + 4, black);
        setPixel(g, 2*unit - 1, 0*unit + 4, black);
        // Beak
        fillRect(g, 2*unit, 1*unit, 2, 2, beak);
        // Wattle (under beak)
        setPixel(g, 1*unit + 4, 1*unit + 2, comb);

        // BODY (round, fluffy)
        fillRect(g, 1*unit, 2*unit, 2*unit, 3*unit, feathers);
        // Wing outline
        drawLine(g, 1*unit, 2*unit + 2, 1*unit + 2, 3*unit, new Color(230, 230, 230));
        drawLine(g, 2*unit + unit - 1, 2*unit + 2, 2*unit + 2, 3*unit, new Color(230, 230, 230));

        // TAIL (feathers pointing up/back)
        fillRect(g, 1*unit + 2, 1*unit + 4, unit + 2, 2, feathers);
        setPixel(g, 1*unit + 2, 1*unit + 3, feathers);
        setPixel(g, 2*unit + 2, 1*unit + 3, feathers);

        // LEGS (thin yellow)
        fillRect(g, 1*unit + 2, 5*unit, 2, 3*unit, beak);
        fillRect(g, 2*unit + 2, 5*unit, 2, 3*unit, beak);
        // Feet (simple lines)
        drawLine(g, 1*unit + 1, 7*unit + 4, 1*unit + 4, 7*unit + 4, beak);
        drawLine(g, 2*unit + 1, 7*unit + 4, 2*unit + 4, 7*unit + 4, beak);

        g.dispose();
        return img;
    }

    /**
     * Generate generic mob texture
     */
    private static BufferedImage generateGenericMobTexture() {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color base = new Color(150, 150, 150);
        Color dark = new Color(100, 100, 100);

        int unit = TEXTURE_SIZE / 8;

        // Simple humanoid shape
        fillRect(g, 1*unit, 0*unit, 2*unit, 2*unit, base); // Head
        fillRect(g, 1*unit, 2*unit, 2*unit, 3*unit, base); // Body
        fillRect(g, 0*unit, 2*unit, 1*unit, 3*unit, base); // Left arm
        fillRect(g, 3*unit, 2*unit, 1*unit, 3*unit, base); // Right arm
        fillRect(g, 1*unit, 5*unit, 1*unit, 3*unit, dark); // Left leg
        fillRect(g, 2*unit, 5*unit, 1*unit, 3*unit, dark); // Right leg

        // Simple face
        setPixel(g, 1*unit + 2, 1*unit, Color.BLACK);
        setPixel(g, 2*unit - 2, 1*unit, Color.BLACK);

        g.dispose();
        return img;
    }

    // Helper methods

    private static void fillRect(Graphics2D g, int x, int y, int w, int h, Color c) {
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    private static void setPixel(Graphics2D g, int x, int y, Color c) {
        if (x >= 0 && x < TEXTURE_SIZE && y >= 0 && y < TEXTURE_SIZE) {
            g.setColor(c);
            g.fillRect(x, y, 1, 1);
        }
    }

    private static void drawLine(Graphics2D g, int x1, int y1, int x2, int y2, Color c) {
        g.setColor(c);
        g.drawLine(x1, y1, x2, y2);
    }

    /**
     * Clear texture cache
     */
    public static void clearCache() {
        textureCache.clear();
    }

    /**
     * Get texture size
     */
    public static int getTextureSize() {
        return TEXTURE_SIZE;
    }
}