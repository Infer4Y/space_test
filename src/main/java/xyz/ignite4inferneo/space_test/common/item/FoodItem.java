package xyz.ignite4inferneo.space_test.common.item;

import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;

/**
 * Food item - restores health/hunger
 */
public class FoodItem extends BaseItem {
    private final int nutrition;        // How much hunger it restores
    private final float saturation;     // How long the food lasts

    public FoodItem(String id, String name, int textureIndex, int nutrition, float saturation) {
        super(id, name, textureIndex);
        this.nutrition = nutrition;
        this.saturation = saturation;
    }

    public int getNutrition() {
        return nutrition;
    }

    public float getSaturation() {
        return saturation;
    }

    /**
     * Eat the food and restore health
     */
    public void eat(PlayerEntity player) {
        // Restore health based on nutrition
        float healAmount = nutrition * 0.5f;
        player.heal(healAmount);

        System.out.println("[Food] " + player.getUsername() + " ate " + getName() +
                " (+" + healAmount + " HP)");
    }
}