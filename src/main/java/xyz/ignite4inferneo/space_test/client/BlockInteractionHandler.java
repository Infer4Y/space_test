package xyz.ignite4inferneo.space_test.client;

import xyz.ignite4inferneo.space_test.api.block.Block;
import xyz.ignite4inferneo.space_test.api.block.InteractableBlock;
import xyz.ignite4inferneo.space_test.api.registry.Registries;
import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;
import xyz.ignite4inferneo.space_test.common.util.RayCast;
import xyz.ignite4inferneo.space_test.common.world.World;

/**
 * Handles player interaction with blocks
 */
public class BlockInteractionHandler {

    private final World world;
    private final PlayerEntity player;

    // Current GUI state
    private String openGUI = null; // null, "crafting_table", "furnace", "chest"
    private int guiBlockX, guiBlockY, guiBlockZ; // Position of block with open GUI

    public BlockInteractionHandler(World world, PlayerEntity player) {
        this.world = world;
        this.player = player;
    }

    /**
     * Handle right-click on block
     * @return true if block was interacted with (don't place block)
     */
    public boolean handleBlockInteraction(RayCast.RaycastResult target) {
        if (target == null || !target.hit) return false;

        String blockId = world.getBlock(target.x, target.y, target.z);
        Block block = Registries.BLOCKS.get(blockId);

        if (block instanceof InteractableBlock interactable) {
            if (interactable.canInteract()) {
                boolean handled = interactable.onInteract(world, target.x, target.y, target.z, player);

                if (handled) {
                    // Open GUI if applicable
                    if (block instanceof xyz.ignite4inferneo.space_test.api.block.GUIBlock guiBlock) {
                        openGUI = guiBlock.getGUIType();
                        guiBlockX = target.x;
                        guiBlockY = target.y;
                        guiBlockZ = target.z;
                    }
                }

                return handled;
            }
        }

        return false;
    }

    /**
     * Close current GUI
     */
    public void closeGUI() {
        openGUI = null;
    }

    /**
     * Check if a GUI is open
     */
    public boolean hasOpenGUI() {
        return openGUI != null;
    }

    /**
     * Get current open GUI type
     */
    public String getOpenGUI() {
        return openGUI;
    }

    /**
     * Get GUI block position
     */
    public int[] getGUIBlockPosition() {
        return new int[]{guiBlockX, guiBlockY, guiBlockZ};
    }

    /**
     * Check if player is still in range of GUI block
     */
    public boolean isInRangeOfGUIBlock() {
        if (openGUI == null) return false;

        double dx = player.x - (guiBlockX + 0.5);
        double dy = player.y - (guiBlockY + 0.5);
        double dz = player.z - (guiBlockZ + 0.5);
        double distSq = dx*dx + dy*dy + dz*dz;

        // Max range of 5 blocks
        return distSq <= 25.0;
    }

    /**
     * Update - close GUI if player moves too far
     */
    public void update() {
        if (hasOpenGUI() && !isInRangeOfGUIBlock()) {
            System.out.println("[BlockInteraction] Player moved too far from GUI block, closing");
            closeGUI();
        }
    }
}