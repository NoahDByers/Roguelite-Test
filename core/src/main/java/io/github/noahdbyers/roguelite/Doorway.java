package io.github.noahdbyers.roguelite;

public class Doorway {
    private final Dir dir;
    private final int tileX;
    private final int tileY;

    public Doorway(Dir dir, int tileX, int tileY) {
        this.dir = dir;
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public Dir getDir() {
        return dir;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    /** Convenience: world-space center of the doorway tile (useful for teleport/spawn). */
    public float getWorldCenterX(int tileSize) {
        return (tileX + 0.5f) * tileSize;
    }

    /** Convenience: world-space center of the doorway tile (useful for teleport/spawn). */
    public float getWorldCenterY(int tileSize) {
        return (tileY + 0.5f) * tileSize;
    }

    /** Convenience: world-space bottom-left of the doorway tile. */
    public float getWorldX(int tileSize) {
        return tileX * tileSize;
    }

    /** Convenience: world-space bottom-left of the doorway tile. */
    public float getWorldY(int tileSize) {
        return tileY * tileSize;
    }
}
