package xyz.ignite4inferneo.space_test.api.block;

/**
 * Block that can store items (chests, furnaces, etc.)
 */
public interface ContainerBlock extends InteractableBlock {

    /**
     * Get container inventory size
     */
    int getContainerSize();

    /**
     * Get container title for GUI
     */
    String getContainerTitle();
}
