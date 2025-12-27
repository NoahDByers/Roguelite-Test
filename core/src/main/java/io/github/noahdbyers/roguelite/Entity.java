package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;

public abstract class Entity {
    private float x, y;
    private float speed;

    private float width, height;

    Entity(float x, float y, float speed, float width, float height) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.width = width;
        this.height = height;
    }

    Entity() {}

    //Getter and setter methods
    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }
    public float getWidth() {
        return width;
    }
    public float getHeight() {
        return height;
    }
    public float getSpeed() {
        return speed;
    }
    public void setX(float x) {
        this.x = x;
    }
    public void setY(float y) {
        this.y = y;
    }
    public void setSpeed(float speed) {
        this.speed = speed;
    }
    public void setWidth(float width) {
        this.width = width;
    }
    public void setHeight(float height) {
        this.height = height;
    }

    //Logic Methods

    //This is a method to check the boundaries with the edge of the screen
    public void clampToScreen() {
        x = Math.max(0, Math.min(x, Gdx.graphics.getWidth() - width));
        y = Math.max(0, Math.min(y, Gdx.graphics.getHeight() - height));
    }

    //Tile based collision detection (based off of tile size)
    public boolean collidesWithRoom(int[][] room, int tileSize) {
        int leftTile = (int)(x / tileSize);
        int rightTile = (int)((x + width) / tileSize);
        int bottomTile = (int)(y / tileSize);
        int topTile = (int)((y + height) / tileSize);

        for (int a = bottomTile; a <= topTile; a++) {
            for(int b = leftTile; b <= rightTile; b++) {
                if (room[a][b] == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    //Abstract methods
    abstract public void update();
}
