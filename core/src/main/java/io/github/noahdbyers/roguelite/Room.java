package io.github.noahdbyers.roguelite;

public class Room {
    private int tileSize = 32;
    private int roomWidth = 20; //tiles
    private int roomHeight = 15; //tiles
    int[][] room;


    Room(int tileSize, int roomWidth, int roomHeight, int[][] room) {
        this.tileSize = tileSize;
        this.roomWidth = roomWidth;
        this.roomHeight = roomHeight;
        this.room = room;
    }

    public int getTile(int x, int y) {
        return room[y][x];
    }
    public int[][] getRoom() {
        return room;
    }
    public void setTile(int x, int y, int newValue) {
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

}
