package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;

public class Player extends Entity {
    public enum Facing { UP, DOWN, LEFT, RIGHT }
    private Facing facing = Facing.DOWN;
    private int maxHealth = 5;
    private int health = 5;

    private float invulnTimer = 0f;
    private float invulnDuration = 0.5f; //invincibility timer after being hit
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
            facing = Facing.LEFT;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            moveX += getSpeed() * delta;
            facing = Facing.RIGHT;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) {
            moveY -= getSpeed() * delta;
            facing = facing.DOWN;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) {
            moveY += getSpeed() * delta;
            facing = facing.UP;
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

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public Facing getFacing() {
        return facing;
    }
    public void updateTimers() {
        float delta = Gdx.graphics.getDeltaTime();
        if (invulnTimer > 0f) {
            invulnTimer -= delta;
            if (invulnTimer < 0f) invulnTimer = 0f;
        }
    }

    public boolean isInvulnerable() {
        return invulnTimer > 0f;
    }

    public void takeDamage(int amount) {
        if (isInvulnerable()) return;

        health -= amount;
        if (health < 0) health = 0;

        invulnTimer = invulnDuration;
    }

    public void heal(int amount) {
        health += amount;
        if (health > maxHealth) health = maxHealth;
    }

    public void increaseMaxHealth(int amount) {
        maxHealth += amount;
        health += amount;
    }
}
