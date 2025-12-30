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

    private int[][] room;
    private ArrayList<TextureRegion> tileSet;

    // Needed for correct mouse->world conversion with FitViewport
    private Viewport viewport;

    public Room(int tileSize, int roomWidth, int roomHeight, int[][] room, ArrayList<TextureRegion> tileSet) {
        this.tileSize = tileSize;
        this.roomWidth = roomWidth;
        this.roomHeight = roomHeight;
        this.room = room;
        this.tileSet = tileSet;
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
        viewport.unproject(screen); // modifies screen into world coords
        return screen;
    }

    // -------------------- Tile access --------------------

    public int getTile(int x, int y) {
        if (!inBounds(x, y)) return 1; // treat out-of-bounds as wall
        return room[y][x];
    }

    public int[][] getRoom() {
        return room;
    }

    public void setTile(int x, int y, int newValue) {
        if (!inBounds(x, y)) return;
        room[y][x] = newValue;
    }

    public void setRoom(int[][] newRoom) {
        this.room = newRoom;
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
        if (tileSet == null || regionIndex < 0 || regionIndex >= tileSet.size()) return null;
        return tileSet.get(regionIndex);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < roomWidth && y >= 0 && y < roomHeight;
    }
}
