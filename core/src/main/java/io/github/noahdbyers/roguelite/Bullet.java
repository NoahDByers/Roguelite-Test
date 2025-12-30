package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

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

    public boolean collidesWithRoom(int[][] room, int tileSize) {
        int tx = (int)((getX() + getWidth() / 2f) / tileSize);
        int ty = (int)((getY() + getHeight() / 2f) / tileSize);

        //Safety clamp (stops crashes at edges)
        if (ty < 0 || ty >= room.length || tx < 0 || tx >= room[0].length) return true;

        return (room[ty][tx] == 1 || room[ty][tx] == 2 || room[ty][tx] == 3 || room[ty][tx] == 4 || room[ty][tx] == 5);
    }

    //This is a method to draw the bullets on screen
    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        //Draw bullets
        shapeRenderer.setColor(1, 1, 0, 1);
        shapeRenderer.rect(getX(), getY(), getWidth(), getHeight());
    }
}
