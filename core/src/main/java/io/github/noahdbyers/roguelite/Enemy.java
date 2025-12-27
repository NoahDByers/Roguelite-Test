package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;

public class Enemy extends Entity {
    Enemy(float x, float y, float speed, float size) {
        super(x, y, speed, size, size);
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
            setX(getY() - moveY);
        }
    }
    public void update() {}
}
