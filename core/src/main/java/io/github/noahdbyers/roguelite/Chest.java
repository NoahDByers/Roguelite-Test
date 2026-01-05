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

    public Chest(float x, float y, int soulReward) {
        this.x = x;
        this.y = y;
        this.soulReward = soulReward;
    }
}
