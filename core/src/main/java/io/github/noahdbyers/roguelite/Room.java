package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;

public class Room {
    //Door trigger
    private Rectangle doorUp, doorDown, doorLeft, doorRight;

    private int tileSize = 32;
    private int roomWidth = 20;   // tiles
    private int roomHeight = 15;  // tiles

    // Draw layer (what you render) — tile IDs (usually 1-based)
    private int[][] draw;

    // Collision layer (what blocks movement) — you use 76 as “solid”
    private int[][] collisions;

    // Door slot layer (where doors exist)
    // 0 = no door slot
    // nonzero = door slot exists (we will decide if it's active)
    private int[][] doors;

    // Door activation flags (which exits are connected / “used”)
    private boolean doorUpActive = false;
    private boolean doorDownActive = false;
    private boolean doorLeftActive = false;
    private boolean doorRightActive = false;

    // Tile IDs to draw for doors (tile IDs in the SAME ID space as your draw grid)
    // Example: if your map IDs are 1-based, these should be 1-based too.
    // If <= 0, we won’t override the draw tile.
    private int doorOpenTileId = -1;
    private int doorCoverTileId = -1;

    // Tile textures (index -> TextureRegion), index is 0-based
    private ArrayList<TextureRegion> tileSet;

    // Needed for correct mouse->world conversion with FitViewport
    private Viewport viewport;

    //Storing information about the room template in a room template object
    private RoomTemplate template;

    public enum DoorSide { UP, DOWN, LEFT, RIGHT, NONE }
    public Room(int tileSize,
                int roomWidth,
                int roomHeight,
                int[][] draw,
                int[][] collisions,
                ArrayList<TextureRegion> tileSet,
                RoomTemplate roomTemplate) {

        this.tileSize = tileSize;
        this.roomWidth = roomWidth;
        this.roomHeight = roomHeight;

        this.draw = draw;
        this.collisions = collisions;
        this.tileSet = tileSet;
        this.template = roomTemplate;
    }

    /** Set once from Main after you create the viewport. */
    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }

    // -------------------- Mouse -> World --------------------

    public float mouseToWorldX() { return mouseToWorld().x; }
    public float mouseToWorldY() { return mouseToWorld().y; }

    public Vector2 mouseToWorld() {
        if (viewport == null) {
            return new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
        }
        Vector2 screen = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(screen);
        return screen;
    }

    // -------------------- Doors --------------------

    /** Provide/replace the door slot grid. */
    public void setDoorsGrid(int[][] doors) {
        this.doors = doors;
        applyDoorCollisionMask();
    }

    public int[][] getDoorsGrid() {
        return doors;
    }

    /**
     * Set which exits are “used” (connected).
     * After calling, collisions are updated so inactive doors become solid walls.
     */
    public void setDoorActive(DoorSide side, boolean active) {
        if (side == null) return;
        switch (side) {
            case UP:    doorUpActive = active; break;
            case DOWN:  doorDownActive = active; break;
            case LEFT:  doorLeftActive = active; break;
            case RIGHT: doorRightActive = active; break;
            default: break;
        }
        applyDoorCollisionMask();
    }

    /** Returns true if door grid marks this tile as a “door slot”. */
    public boolean isDoorSlot(int x, int y) {
        if (!inBounds(x, y)) return false;
        if (doors == null) return false;
        if (y >= doors.length || x >= doors[y].length) return false;
        return doors[y][x] != 0;
    }

    /** Determine which side a door slot belongs to (by being on an edge). */
    public DoorSide getDoorSideAt(int x, int y) {
        if (!inBounds(x, y)) return DoorSide.NONE;
        if (!isDoorSlot(x, y)) return DoorSide.NONE;

        if (y == roomHeight - 1) return DoorSide.UP;
        if (y == 0)              return DoorSide.DOWN;
        if (x == 0)              return DoorSide.LEFT;
        if (x == roomWidth - 1)  return DoorSide.RIGHT;
        return DoorSide.NONE; // if you ever place interior doors, handle here
    }

    /** Is the door at this tile currently “used/open” (connected)? */
    public boolean isDoorActiveAt(int x, int y) {
        DoorSide side = getDoorSideAt(x, y);
        switch (side) {
            case UP:    return doorUpActive;
            case DOWN:  return doorDownActive;
            case LEFT:  return doorLeftActive;
            case RIGHT: return doorRightActive;
            default:    return false;
        }
    }

    /**
     * Returns the tile ID you should render at (x,y), including door overrides:
     * - If no door slot: returns the base draw tile
     * - If door slot:
     *    - active => doorOpenTileId (if > 0, else falls back to draw tile)
     *    - inactive => doorCoverTileId (if > 0, else falls back to draw tile)
     */
    public int getDrawTileWithDoors(int x, int y) {
        int base = getTile(x, y);
        if (!isDoorSlot(x, y)) return base;

        boolean active = isDoorActiveAt(x, y);

        if (active) {
            return (doorOpenTileId > 0) ? doorOpenTileId : base;
        } else {
            return (doorCoverTileId > 0) ? doorCoverTileId : base;
        }
    }

    /**
     * Apply door collision masking:
     * - door slot + active => collision becomes 0 (passable)
     * - door slot + inactive => collision becomes 76 (solid)
     *
     * This makes door visuals and collisions line up automatically.
     */
    public void applyDoorCollisionMask() {
        if (doors == null || collisions == null) return;

        for (int y = 0; y < roomHeight; y++) {
            if (y >= doors.length || y >= collisions.length) continue;
            for (int x = 0; x < roomWidth; x++) {
                if (x >= doors[y].length || x >= collisions[y].length) continue;

                if (doors[y][x] == 0) continue; // not a door slot

                boolean active = isDoorActiveAt(x, y);
                collisions[y][x] = active ? 0 : 76;
            }
        }
    }

    // -------------------- Tile access --------------------

    /** Returns DRAW tile id at (x,y). */
    public int getTile(int x, int y) {
        if (!inBounds(x, y)) return 0;
        if (draw == null) return 0;
        if (y >= draw.length || x >= draw[y].length) return 0;
        return draw[y][x];
    }

    public boolean isDoor(int x, int y) {
        if (doors == null) return false;
        if (!inBounds(x, y)) return false;

        // extra safety if arrays are not exactly roomWidth/roomHeight
        if (y < 0 || y >= doors.length) return false;
        if (x < 0 || x >= doors[y].length) return false;

        return doors[y][x] != 0;
    }

    public int getDoorTextureID(int x, int y) {
        if (!isDoor(x, y)) return 0;
        return doors[y][x];
    }
    public void setCollisionTile(int x, int y, int newValue) {
        if (!inBounds(x, y)) return;
        if (collisions == null) return;
        if (y >= collisions.length || x >= collisions[y].length) return;
        collisions[y][x] = newValue;
    }

    public void setDrawGrid(int[][] newDraw) { this.draw = newDraw; }
    public void setCollisionGrid(int[][] newCollisions) { this.collisions = newCollisions; }

    public int getTileSize() { return tileSize; }
    public int getRoomWidth() { return roomWidth; }
    public int getRoomHeight() { return roomHeight; }

    public TextureRegion getTextureRegion(int regionIndex) {
        if (tileSet == null || tileSet.isEmpty()) return null;
        if (regionIndex < 0 || regionIndex >= tileSet.size()) return null;
        return tileSet.get(regionIndex);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < roomWidth && y >= 0 && y < roomHeight;
    }

    public int[][] getDraw() { return draw; }
    public int[][] getCollisions() { return collisions; }

    /** Treat out-of-bounds as solid to prevent leaving the room. */
    public boolean isSolidTile(int tx, int ty) {
        if (tx < 0 || tx >= roomWidth || ty < 0 || ty >= roomHeight) return true;
        return collisions != null && collisions[ty][tx] == 76;
    }

    public RoomTemplate getTemplate() { return template; }

    public Rectangle getDoorTrigger(Dir dir) {
        switch(dir) {
            case UP: return doorUp;
            case DOWN: return doorDown;
            case LEFT: return doorLeft;
            case RIGHT: return doorRight;
        }

        return null;
    }

    public void rebuildDoorTriggers() {
        if (template == null) return;

        float ts = getTileSize();
        float w = getRoomWidth();
        float h = getRoomHeight();

        //Size of the trigger region (tweak these)
        float triggerThickness = ts * 0.6f;
        float triggerSpan = ts * 2.0f; // how wide the "door" is along hte edge

        float midX = w * 0.5f;
        float midY = h * 0.5f;

        //Centered along each edge
        doorUp = new Rectangle(midX - triggerSpan * 0.5f, h - triggerThickness, triggerSpan, triggerThickness);
        doorDown = new Rectangle(midX - triggerSpan * 0.5f, 0f, triggerSpan, triggerThickness);
        doorLeft = new Rectangle(0f, midY - triggerSpan * 0.5f, triggerThickness, triggerSpan);
        doorRight = new Rectangle(w - triggerThickness, midY - triggerSpan * 0.5f, triggerThickness, triggerSpan);

        // If a side is closed, “disable” that trigger by shrinking it to 0
        if (!template.up) doorUp.set(0,0,0,0);
        if (!template.down) doorDown.set(0,0,0,0);
        if (!template.left) doorLeft.set(0,0,0,0);
        if (!template.right) doorRight.set(0,0,0,0);
    }
}
