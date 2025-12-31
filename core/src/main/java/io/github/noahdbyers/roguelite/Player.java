package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class Player extends Entity {
    private Rectangle bounds;
    private Object Input;

    public enum Facing { UP, DOWN, LEFT, RIGHT }
    private Facing facing = Facing.DOWN;
    private int maxHealth = 5;
    private int health = 5;

    //Currently using place holders
    private int mana = health;
    private int maxMana = maxHealth;
    private float invulnTimer = 0f;
    private float invulnDuration = 0.5f; //invincibility timer after being hit
    Player(float x, float y, float speed, float width, float height) {
        super(x, y, speed, width, height);
        bounds = new Rectangle(x, y, width, height);
    }
    public Rectangle getBounds() {
        return bounds;
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
            facing = Facing.DOWN;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) {
            moveY += getSpeed() * delta;
            facing = Facing.UP;
        }

        // X axis
        setX(getX() + moveX);
        if (collidesWithRoom(room.getRoom(), tileSize)) {
            setX(getX() - moveX);
        }

        // Y axis
        setY(getY() + moveY);
        if (collidesWithRoom(room.getRoom(), tileSize)) {
            setY(getY() - moveY);
        }

        bounds.set(getX(), getY(), getWidth(), getHeight());
    }


    public void update() {}

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }

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

    public boolean collidesWithRoom(int[][] room, int tileSize) {
        int leftTile = (int)(getX() / tileSize);
        int rightTile = (int)((getX() + getWidth() - 1) / tileSize);
        int bottomTile = (int)(getY() / tileSize);
        int topTile = (int)((getY() + getHeight() - 1) / tileSize);

        for (int a = bottomTile; a <= topTile; a++) {
            for(int b = leftTile; b <= rightTile; b++) {
                if (room[a][b] == 1) {
                    return true;
                }
            }
        }
        return false;
    }
    public void increaseMaxHealth(int amount) {
        maxHealth += amount;
        health += amount;
    }

    //This method is used to draw the player
    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        //Checking if the player is
        if (isInvulnerable()) shapeRenderer.setColor(1, 1, 0, 1);
        else shapeRenderer.setColor(0, 1, 0, 1);

        shapeRenderer.rect(getX(), getY(), getWidth(), getHeight());
    }
}
