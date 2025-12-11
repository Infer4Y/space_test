package xyz.ignite4inferneo.space_test.client.renderer;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Renderer {
    private final ChunkRenderer chunk;

    public Renderer() {
        chunk = new ChunkRenderer();
    }

    public ChunkRenderer getChunkRenderer() {
        return chunk;
    }

    public void setCanvasSize(Dimension size) {
        chunk.setScreenSize(size.width, size.height);
    }

    public BufferedImage getScreenBuffer() {
        return chunk.getScreen();
    }

    public void render(double deltaTime) {
        chunk.render();
    }
}

