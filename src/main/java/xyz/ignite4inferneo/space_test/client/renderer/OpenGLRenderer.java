package xyz.ignite4inferneo.space_test.client.renderer;

import org.joml.Matrix4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import xyz.ignite4inferneo.space_test.common.world.Chunk;
import xyz.ignite4inferneo.space_test.common.world.World;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * OpenGL-based hardware renderer using LWJGL
 * Coexists with software renderer - can switch between them
 */
public class OpenGLRenderer {

    private final World world;
    private long window;

    // Camera
    public double x = 0, y = 70, z = 0;
    public double yaw = 0, pitch = 0;

    // Screen
    private int width = 1280;
    private int height = 720;

    // OpenGL objects
    private int shaderProgram;
    private int textureAtlasID;

    // Chunk meshes (VAO -> ChunkMesh)
    private final Map<Long, ChunkGLMesh> glMeshCache = new ConcurrentHashMap<>();
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();

    // Matrices
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();

    // Stats
    private int chunksRendered = 0;

    private static class ChunkGLMesh {
        int vao;
        int vbo;
        int vertexCount;
        long lastUsed;
    }

    public OpenGLRenderer(World world) {
        this.world = world;
    }

    /**
     * Initialize OpenGL context and window
     */
    public void init() {
        // Initialize GLFW
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4); // 4x MSAA

        // Create window
        window = glfwCreateWindow(width, height, "Space Test - OpenGL Renderer", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Center window
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        // Make context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable v-sync

        // Initialize OpenGL bindings
        GL.createCapabilities();

        System.out.println("[OpenGLRenderer] Initialized");
        System.out.println("  OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println("  GPU: " + glGetString(GL_RENDERER));

        // Setup OpenGL state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        glEnable(GL_MULTISAMPLE); // Enable MSAA

        glClearColor(0.53f, 0.81f, 0.92f, 1.0f); // Sky blue

        // Create shader program
        createShaders();

        // Create texture atlas
        createTextureAtlas();

        // Setup callbacks
        setupCallbacks();

        // Update projection matrix
        updateProjectionMatrix();
    }

    /**
     * Create and compile shaders
     */
    private void createShaders() {
        // Vertex shader
        String vertexSource = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aTexCoord;
            layout (location = 2) in float aBrightness;
            
            uniform mat4 projection;
            uniform mat4 view;
            
            out vec2 TexCoord;
            out float Brightness;
            
            void main() {
                gl_Position = projection * view * vec4(aPos, 1.0);
                TexCoord = aTexCoord;
                Brightness = aBrightness;
            }
            """;

        // Fragment shader
        String fragmentSource = """
            #version 330 core
            in vec2 TexCoord;
            in float Brightness;
            
            uniform sampler2D texAtlas;
            
            out vec4 FragColor;
            
            void main() {
                vec4 texColor = texture(texAtlas, TexCoord);
                if (texColor.a < 0.1)
                    discard;
                FragColor = vec4(texColor.rgb * Brightness, texColor.a);
            }
            """;

        // Compile vertex shader
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        checkShaderCompilation(vertexShader, "VERTEX");

        // Compile fragment shader
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        checkShaderCompilation(fragmentShader, "FRAGMENT");

        // Link program
        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        checkProgramLinking(shaderProgram);

        // Cleanup
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        System.out.println("[OpenGLRenderer] Shaders compiled successfully");
    }

    private void checkShaderCompilation(int shader, String type) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            throw new RuntimeException(type + " shader compilation failed:\n" + log);
        }
    }

    private void checkProgramLinking(int program) {
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            throw new RuntimeException("Program linking failed:\n" + log);
        }
    }

    /**
     * Create texture atlas from existing TextureAtlas
     */
    private void createTextureAtlas() {
        // For now, create a simple test texture
        // TODO: Convert your TextureAtlas to OpenGL texture

        int atlasSize = 256; // 16x16 grid of 16x16 textures
        int[] pixels = new int[atlasSize * atlasSize];

        // Generate simple test pattern
        for (int y = 0; y < atlasSize; y++) {
            for (int x = 0; x < atlasSize; x++) {
                int texX = x / 16;
                int texY = y / 16;
                int localX = x % 16;
                int localY = y % 16;

                // Checkerboard per texture
                boolean checker = ((localX / 4) + (localY / 4)) % 2 == 0;
                int gray = checker ? 200 : 150;

                // Color code by texture index
                int texIndex = texY * 16 + texX;
                int r = gray;
                int g = gray;
                int b = gray;

                if (texIndex == 0) { r = gray; g = gray; b = gray; } // Stone
                else if (texIndex == 1) { r = 139; g = 69; b = 19; } // Dirt
                else if (texIndex == 2) { r = gray; g = 139; b = 69; } // Grass side
                else if (texIndex == 3) { r = 50; g = 200; b = 50; } // Grass top
                else if (texIndex == 4) { r = 160; g = 82; b = 45; } // Wood
                else if (texIndex == 5) { r = 50; g = 150; b = 50; } // Leaves

                pixels[y * atlasSize + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        // Create OpenGL texture
        textureAtlasID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureAtlasID);

        // Upload pixels
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, atlasSize, atlasSize, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        System.out.println("[OpenGLRenderer] Texture atlas created");
    }

    /**
     * Setup GLFW callbacks
     */
    private void setupCallbacks() {
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            width = w;
            height = h;
            glViewport(0, 0, w, h);
            updateProjectionMatrix();
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true);
            }
        });
    }

    /**
     * Update projection matrix
     */
    private void updateProjectionMatrix() {
        float aspect = (float) width / height;
        float fov = (float) Math.toRadians(70.0f);
        float near = 0.1f;
        float far = 1000.0f;

        projectionMatrix.setPerspective(fov, aspect, near, far);
    }

    /**
     * Update view matrix from camera
     */
    private void updateViewMatrix() {
        viewMatrix.identity()
                .rotateX((float) pitch)
                .rotateY((float) yaw)
                .translate((float) -x, (float) -y, (float) -z);
    }

    /**
     * Main render loop
     */
    public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        updateViewMatrix();

        // Use shader
        glUseProgram(shaderProgram);

        // Set uniforms
        int projLoc = glGetUniformLocation(shaderProgram, "projection");
        int viewLoc = glGetUniformLocation(shaderProgram, "view");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer projBuffer = stack.mallocFloat(16);
            FloatBuffer viewBuffer = stack.mallocFloat(16);

            projectionMatrix.get(projBuffer);
            viewMatrix.get(viewBuffer);

            glUniformMatrix4fv(projLoc, false, projBuffer);
            glUniformMatrix4fv(viewLoc, false, viewBuffer);
        }

        // Bind texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureAtlasID);
        glUniform1i(glGetUniformLocation(shaderProgram, "texAtlas"), 0);

        // Render chunks
        chunksRendered = 0;
        renderChunks();

        // Swap buffers
        glfwSwapBuffers(window);
        glfwPollEvents();
    }

    /**
     * Render all visible chunks
     */
    private void renderChunks() {
        int camChunkX = (int) Math.floor(x / 16.0);
        int camChunkZ = (int) Math.floor(z / 16.0);
        int renderDistance = 8;

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                if (dx * dx + dz * dz > renderDistance * renderDistance) continue;

                int chunkX = camChunkX + dx;
                int chunkZ = camChunkZ + dz;
                long key = chunkKey(chunkX, chunkZ);

                // Get or create mesh
                ChunkGLMesh mesh = glMeshCache.get(key);
                if (mesh == null) {
                    Chunk chunk = world.getChunk(chunkX, chunkZ);
                    if (chunk != null) {
                        mesh = createChunkMesh(chunk, chunkX, chunkZ);
                        glMeshCache.put(key, mesh);
                    }
                }

                // Render mesh
                if (mesh != null && mesh.vertexCount > 0) {
                    glBindVertexArray(mesh.vao);
                    glDrawArrays(GL_TRIANGLES, 0, mesh.vertexCount);
                    chunksRendered++;
                }
            }
        }
    }

    /**
     * Create OpenGL mesh from chunk
     */
    private ChunkGLMesh createChunkMesh(Chunk chunk, int chunkX, int chunkZ) {
        // Use GreedyMesher to get quads
        List<GreedyMesher.Quad> quads = GreedyMesher.mesh(chunk);

        if (quads.isEmpty()) {
            ChunkGLMesh mesh = new ChunkGLMesh();
            mesh.vertexCount = 0;
            return mesh;
        }

        // Convert quads to vertex data
        // Format: x, y, z, u, v, brightness (6 floats per vertex)
        int vertexCount = quads.size() * 6; // 2 triangles per quad
        float[] vertices = new float[vertexCount * 6];

        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        int idx = 0;

        for (GreedyMesher.Quad quad : quads) {
            float wx = baseX + quad.x;
            float wy = quad.y;
            float wz = baseZ + quad.z;

            // Calculate UV coordinates (16 textures per row in 256x256 atlas)
            float texSize = 1.0f / 16.0f;
            float u0 = (quad.texIndex % 16) * texSize;
            float v0 = (quad.texIndex / 16) * texSize;
            float u1 = u0 + quad.w * (texSize / 16.0f);
            float v1 = v0 + quad.h * (texSize / 16.0f);

            float brightness = quad.brightness;

            // Build quad vertices based on axis
            float[][] corners = new float[4][3];

            if (quad.axis == 1) { // Y face
                float oy = wy + (quad.dir > 0 ? 1 : 0);
                corners[0] = new float[]{wx, oy, wz};
                corners[1] = new float[]{wx + quad.w, oy, wz};
                corners[2] = new float[]{wx + quad.w, oy, wz + quad.h};
                corners[3] = new float[]{wx, oy, wz + quad.h};
            } else if (quad.axis == 2) { // Z face
                float oz = wz + (quad.dir > 0 ? 1 : 0);
                corners[0] = new float[]{wx, wy, oz};
                corners[1] = new float[]{wx + quad.w, wy, oz};
                corners[2] = new float[]{wx + quad.w, wy + quad.h, oz};
                corners[3] = new float[]{wx, wy + quad.h, oz};
            } else { // X face
                float ox = wx + (quad.dir > 0 ? 1 : 0);
                corners[0] = new float[]{ox, wy, wz};
                corners[1] = new float[]{ox, wy, wz + quad.w};
                corners[2] = new float[]{ox, wy + quad.h, wz + quad.w};
                corners[3] = new float[]{ox, wy + quad.h, wz};
            }

            // Triangle 1: 0, 1, 2
            addVertex(vertices, idx++, corners[0], u0, v0, brightness);
            addVertex(vertices, idx++, corners[1], u1, v0, brightness);
            addVertex(vertices, idx++, corners[2], u1, v1, brightness);

            // Triangle 2: 0, 2, 3
            addVertex(vertices, idx++, corners[0], u0, v0, brightness);
            addVertex(vertices, idx++, corners[2], u1, v1, brightness);
            addVertex(vertices, idx++, corners[3], u0, v1, brightness);
        }

        // Create VAO/VBO
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // TexCoord attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Brightness attribute
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 6 * Float.BYTES, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);

        ChunkGLMesh mesh = new ChunkGLMesh();
        mesh.vao = vao;
        mesh.vbo = vbo;
        mesh.vertexCount = vertexCount;
        mesh.lastUsed = System.currentTimeMillis();

        return mesh;
    }

    private void addVertex(float[] vertices, int index, float[] pos, float u, float v, float brightness) {
        int i = index * 6;
        vertices[i + 0] = pos[0];
        vertices[i + 1] = pos[1];
        vertices[i + 2] = pos[2];
        vertices[i + 3] = u;
        vertices[i + 4] = v;
        vertices[i + 5] = brightness;
    }

    private static long chunkKey(int x, int z) {
        return ((long)x << 32) | (z & 0xFFFFFFFFL);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    public void cleanup() {
        // Delete all meshes
        for (ChunkGLMesh mesh : glMeshCache.values()) {
            glDeleteVertexArrays(mesh.vao);
            glDeleteBuffers(mesh.vbo);
        }

        // Delete shader
        glDeleteProgram(shaderProgram);

        // Delete texture
        glDeleteTextures(textureAtlasID);

        // Destroy window
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public int getChunksRendered() {
        return chunksRendered;
    }

    public long getWindow() {
        return window;
    }
}