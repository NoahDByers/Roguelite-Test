package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

public class Room {

    private int tileSize = 32;
    private int roomWidth = 20;   // tiles
    private int roomHeight = 15;  // tiles

    // Draw layer (what you render)
    private int[][] draw;

    // Collision layer (what blocks movement)
    private int[][] collisions;

    // Tile textures (index -> TextureRegion)
    private ArrayList<TextureRegion> tileSet;

    // Needed for correct mouse->world conversion with FitViewport
    private Viewport viewport;

    public Room(int tileSize,
                int roomWidth,
                int roomHeight,
                int[][] draw,
                int[][] collisions,
                ArrayList<TextureRegion> tileSet) {

        this.tileSize = tileSize;
        this.roomWidth = roomWidth;
        this.roomHeight = roomHeight;

        this.draw = draw;
        this.collisions = collisions;
        this.tileSet = tileSet;

        // Optional safety: if provided grids don’t match given dimensions,
        // you can still run, but out-of-bounds checks will protect you.
    }

    /** Set once from Main after you create the viewport (and again if you recreate Room). */
    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }

    // -------------------- Mouse -> World --------------------

    /** World X position of the mouse (works correctly after resize with FitViewport). */
    public float mouseToWorldX() {
        return mouseToWorld().x;
    }

    /** World Y position of the mouse (works correctly after resize with FitViewport). */
    public float mouseToWorldY() {
        return mouseToWorld().y;
    }

    /** Returns mouse position in WORLD coordinates. */
    public Vector2 mouseToWorld() {
        if (viewport == null) {
            // Fallback (not ideal after resizing) but prevents crashes if you forget setViewport
            return new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
        }

        Vector2 screen = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(screen);
        return screen;
    }

    // -------------------- Tile access --------------------

    /**
     * Backwards-compatible: returns the DRAW tile index at (x,y).
     * This keeps existing rendering code working:
     *   room.getTextureRegion(room.getTile(x,y))
     */
    public int getTile(int x, int y) {
        if (!inBounds(x, y)) return 0;
        if (draw == null) return 0;
        if (y >= draw.length || x >= draw[y].length) return 0;
        return draw[y][x];
    }

    /**
     * Backwards-compatible: GameWorld currently calls room.getRoom() for collision checks.
     * So we return the COLLISION grid here.
     */
    public int[][] getRoom() {
        return collisions;
    }

    /** Explicit getter for draw grid (useful for debugging or future features). */
    public int[][] getDrawGrid() {
        return draw;
    }

    /** Explicit getter for collision grid. */
    public int[][] getCollisionGrid() {
        return collisions;
    }

    public void setDrawTile(int x, int y, int newValue) {
        if (!inBounds(x, y)) return;
        if (draw == null) return;
        if (y >= draw.length || x >= draw[y].length) return;
        draw[y][x] = newValue;
    }

    public void setCollisionTile(int x, int y, int newValue) {
        if (!inBounds(x, y)) return;
        if (collisions == null) return;
        if (y >= collisions.length || x >= collisions[y].length) return;
        collisions[y][x] = newValue;
    }

    public void setDrawGrid(int[][] newDraw) {
        this.draw = newDraw;
    }

    public void setCollisionGrid(int[][] newCollisions) {
        this.collisions = newCollisions;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getRoomWidth() {
        return roomWidth;
    }

    public int getRoomHeight() {
        return roomHeight;
    }

    public TextureRegion getTextureRegion(int regionIndex) {
        if (tileSet == null || tileSet.isEmpty()) return null;
        if (regionIndex < 0 || regionIndex >= tileSet.size()) return null;
        return tileSet.get(regionIndex);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < roomWidth && y >= 0 && y < roomHeight;
    }

    public int[][] getDraw() {
        return draw;
    }

    public int[][] getCollisions() {
        return collisions;
    }

    /** Treat out-of-bounds as solid to prevent leaving the room. */
    public boolean isSolidTile(int tx, int ty) {
        if (tx < 0 || tx >= roomWidth || ty < 0 || ty >= roomHeight) return true;
        return collisions != null && collisions[ty][tx] == 76;
    }

}
