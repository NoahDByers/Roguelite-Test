package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Enemy extends Entity {
    private int health;
    Enemy(float x, float y, float speed, float size, int health) {
        super(x, y, speed, size, size);

        this.health = health;
    }

    public void update(Player player, Room room, int tileSize) {
        float delta = Gdx.graphics.getDeltaTime();

        float dx = player.getX() - getX();
        float dy = player.getY() - getY();

        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length != 0) {
            dx /= length;
            dy /= length;
        }

        float moveX = dx * getSpeed() * delta;
        float moveY = dy * getSpeed() * delta;

        //x axis
        setX(getX() + moveX);
        if (collidesWithRoom(room.getRoom(), tileSize)) {
            setX(getX() - moveX);
        }

        //y axis
        setY(getY() + moveY);
        if (collidesWithRoom(room.getRoom(), tileSize)) {
            setY(getY() - moveY);
        }
    }
    public void update() {}

    public void takeDamage(int amount) {
        health -= amount;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean collidesWithRoom(int[][] room, int tileSize) {
        int leftTile = (int)(getX() / tileSize);
        int rightTile = (int)((getX() + getWidth()) / tileSize);
        int bottomTile = (int)(getY() / tileSize);
        int topTile = (int)((getY() + getHeight()) / tileSize);

        for (int a = bottomTile; a <= topTile; a++) {
            for(int b = leftTile; b <= rightTile; b++) {
                if (room[a][b] == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    //This method is used to draw the enemies
    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.rect(getX(), getY(), getWidth(), getHeight());
    }
}
