package io.github.noahdbyers.roguelite;

/**
 * Simple interactable chest.
 * Sprite comes from dungeonTileSheet tile index 93 (drawn by UserInterface in world space).
 */
public class Chest {
    public float x, y;
    public float w = 32f, h = 32f;

    public boolean opened = false;

    /** Souls granted when opened (deterministic per chest). */
    public int soulReward = 0;

    /** Optional item reward. If non-null, opening the chest grants the item. */
    public ItemId itemReward = null;

    public Chest(float x, float y, int soulReward) {
        this.x = x;
        this.y = y;
        this.soulReward = soulReward;
    }

    public Chest(float x, float y, int soulReward, ItemId itemReward) {
        this.x = x;
        this.y = y;
        this.soulReward = soulReward;
        this.itemReward = itemReward;
    }
}
