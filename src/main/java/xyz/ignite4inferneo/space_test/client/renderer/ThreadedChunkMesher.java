package xyz.ignite4inferneo.space_test.client.renderer;

import xyz.ignite4inferneo.space_test.common.world.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Multi-threaded chunk mesher that processes multiple chunks in parallel.
 * Significantly improves performance on multi-core systems.
 */
public class ThreadedChunkMesher {

    private final ExecutorService executor;
    private final int threadCount;

    // Queue for pending mesh tasks
    private final BlockingQueue<MeshTask> taskQueue;
    private final ConcurrentHashMap<Long, CompletableFuture<MeshResult>> pendingTasks;

    public static class MeshTask {
        public final Chunk chunk;
        public final int chunkX;
        public final int chunkZ;
        public final long key;

        public MeshTask(Chunk chunk, int chunkX, int chunkZ, long key) {
            this.chunk = chunk;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.key = key;
        }
    }

    public static class MeshResult {
        public final long key;
        public final List<GreedyMesher.Quad> quads;
        public final int chunkX;
        public final int chunkZ;

        public MeshResult(long key, List<GreedyMesher.Quad> quads, int chunkX, int chunkZ) {
            this.key = key;
            this.quads = quads;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    public ThreadedChunkMesher() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ThreadedChunkMesher(int threadCount) {
        this.threadCount = threadCount;
        this.taskQueue = new LinkedBlockingQueue<>();
        this.pendingTasks = new ConcurrentHashMap<>();

        // Create thread pool with custom thread factory for better debugging
        this.executor = Executors.newFixedThreadPool(threadCount, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ChunkMesher-" + counter++);
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1); // Slightly lower priority
                return t;
            }
        });

        System.out.println("[ThreadedMesher] Initialized with " + threadCount + " threads");
    }

    /**
     * Submit a chunk for meshing (non-blocking)
     * Returns a CompletableFuture that completes when meshing is done
     */
    public CompletableFuture<MeshResult> submitChunk(Chunk chunk, int chunkX, int chunkZ, long key) {
        // Check if already pending
        CompletableFuture<MeshResult> existing = pendingTasks.get(key);
        if (existing != null && !existing.isDone()) {
            return existing;
        }

        // Create new task
        CompletableFuture<MeshResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Perform meshing on worker thread
                List<GreedyMesher.Quad> quads = GreedyMesher.mesh(chunk);
                return new MeshResult(key, quads, chunkX, chunkZ);
            } catch (Exception e) {
                System.err.println("[ThreadedMesher] Error meshing chunk (" + chunkX + ", " + chunkZ + "): " + e.getMessage());
                e.printStackTrace();
                return new MeshResult(key, new ArrayList<>(), chunkX, chunkZ);
            }
        }, executor);

        // Store and clean up when done
        pendingTasks.put(key, future);
        future.thenRun(() -> pendingTasks.remove(key));

        return future;
    }

    /**
     * Submit multiple chunks for batch meshing
     */
    public List<CompletableFuture<MeshResult>> submitBatch(List<MeshTask> tasks) {
        List<CompletableFuture<MeshResult>> futures = new ArrayList<>();

        for (MeshTask task : tasks) {
            futures.add(submitChunk(task.chunk, task.chunkX, task.chunkZ, task.key));
        }

        return futures;
    }

    /**
     * Wait for all pending tasks to complete (blocking)
     * Useful for loading screens
     */
    public void waitForAll() {
        List<CompletableFuture<MeshResult>> allFutures = new ArrayList<>(pendingTasks.values());
        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Get number of pending tasks
     */
    public int getPendingCount() {
        return pendingTasks.size();
    }

    /**
     * Shutdown the thread pool (call on game exit)
     */
    public void shutdown() {
        System.out.println("[ThreadedMesher] Shutting down...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public int getThreadCount() {
        return threadCount;
    }
}