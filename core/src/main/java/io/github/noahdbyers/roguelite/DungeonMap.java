package io.github.noahdbyers.roguelite;

public class DungeonMap {
    private final int w;
    private final int h;

    private final RoomTemplate[][] templates; // [y][x]
    private final boolean[][] discovered;     // [y][x]
    private final boolean[][] cleared;        // [y][x] (may be null)

    private int currentX;
    private int currentY;

    public DungeonMap(Room[][] worldRooms, boolean[][] clearedRef, int startX, int startY) {
        this.h = (worldRooms == null) ? 0 : worldRooms.length;
        this.w = (this.h == 0 || worldRooms[0] == null) ? 0 : worldRooms[0].length;

        this.cleared = clearedRef;
        this.templates = new RoomTemplate[h][w];
        this.discovered = new boolean[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Room r = (worldRooms != null && y < worldRooms.length) ? worldRooms[y][x] : null;
                templates[y][x] = (r != null) ? r.getTemplate() : null;
                discovered[y][x] = false;
            }
        }

        setCurrent(startX, startY);
    }

    public int getWidth() { return w; }
    public int getHeight() { return h; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < w && y >= 0 && y < h;
    }

    public RoomTemplate getTemplate(int x, int y) {
        if (!inBounds(x, y)) return null;
        return templates[y][x];
    }

    public boolean isDiscovered(int x, int y) {
        if (!inBounds(x, y)) return false;
        return discovered[y][x];
    }

    public void discover(int x, int y) {
        if (!inBounds(x, y)) return;
        discovered[y][x] = true;
    }

    public boolean isCleared(int x, int y) {
        if (cleared == null || !inBounds(x, y)) return false;
        return cleared[y][x];
    }

    public void setCleared(int x, int y, boolean value) {
        if (cleared == null || !inBounds(x, y)) return;
        cleared[y][x] = value;
    }

    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }

    public void setCurrent(int x, int y) {
        if (!inBounds(x, y)) return;
        currentX = x;
        currentY = y;
    }
}
