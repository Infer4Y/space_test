package xyz.ignite4inferneo.space_test.common.item;

import xyz.ignite4inferneo.space_test.api.item.Item;

/**
 * Basic implementation of Item
 */
public class BaseItem implements Item {
    protected final String id;
    protected final String name;
    protected final int textureIndex;
    protected final int maxStackSize;

    public BaseItem(String id, String name, int textureIndex) {
        this(id, name, textureIndex, 64);
    }

    public BaseItem(String id, String name, int textureIndex, int maxStackSize) {
        this.id = id;
        this.name = name;
        this.textureIndex = textureIndex;
        this.maxStackSize = maxStackSize;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getTextureIndex() {
        return textureIndex;
    }

    @Override
    public int getMaxStackSize() {
        return maxStackSize;
    }
}