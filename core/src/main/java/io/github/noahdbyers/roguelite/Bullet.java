package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;

public class Bullet extends Entity{
    private float vx, vy;
    private float speed;
    private int damage;

    public Bullet(float startX, float startY, float dirX, float dirY, float speed, float size) {
        super(startX, startY, speed, size, size);

        float len = (float)Math.sqrt(dirX * dirX + dirY * dirY);
        if (len != 0) {
            dirX /= len;
            dirY /= len;
        }

        this.vx = dirX * getSpeed();
        this.vy = dirY * getSpeed();
    }

    public void update() {
        float delta = Gdx.graphics.getDeltaTime();
        setX(getX() + vx * delta);
        setY(getY() + vy * delta);
    }

    public boolean isOffScreen() {
        return getX() < -getWidth() || getY() < -getHeight() ||
                getX() > Gdx.graphics.getWidth() + getWidth() ||
                getY() > Gdx.graphics.getHeight() + getHeight();
    }

    @Override
    public boolean collidesWithRoom(int[][] room, int tileSize) {
        int tx = (int)((getX() + getWidth() / 2f) / tileSize);
        int ty = (int)((getY() + getHeight() / 2f) / tileSize);

        //Safety clamp (stops crashes at edges)
        if (ty < 0 || ty >= room.length || tx < 0 || tx >= room[0].length) return true;

        return room[ty][tx] == 1;
    }
}
