package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;

public class Player extends Entity {
    Player(float x, float y, float speed, float width, float height) {
        super(x, y, speed, width, height);
    }

    //Input Handling Methods

    //This is a method to handle basic WASD movement
    public void update(Room room, int tileSize) {

        float delta = Gdx.graphics.getDeltaTime();

        float moveX = 0;
        float moveY = 0;

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
            moveX -= getSpeed() * delta;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            moveX += getSpeed() * delta;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) {
            moveY -= getSpeed() * delta;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) {
            moveY += getSpeed() * delta;
        }

        //Move X axis
        setX(getX() + moveX);
        if (collidesWithRoom(room.getRoom(), tileSize)) {
            setX(getX() - moveX);
        }

        //Move Y axis
        setY(getY() + moveY);
        if (collidesWithRoom(room.getRoom(), tileSize)) {
            setY(getY() - moveY);
        }
    }

    public void update() {}
}
