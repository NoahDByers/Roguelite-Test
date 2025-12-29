package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;

public class Player extends Entity {
    public enum Facing { UP, DOWN, LEFT, RIGHT }
    private Facing facing = Facing.DOWN;
    private ArrayList<TextureRegion> spritesList = new ArrayList<TextureRegion>();
    private int spriteIndex = 0;
    private int maxHealth = 5;
    private int health = 5;

    //Currently using place holders
    private int mana = health;
    private int maxMana = maxHealth;
    private float invulnTimer = 0f;
    private float invulnDuration = 0.5f; //invincibility timer after being hit

    //Instantiating and creating the textures that you need for player animations
    private Texture idleDown = new Texture("player/idleDown.png");

    //Storing the texture region index currently being rendered for this player
    private float animTimer = 0f;
    private boolean isMoving = false;
    int currSpriteIndex = 0;
    int direction = 2;
    Player(float x, float y, float speed, float width, float height) {
        super(x, y, speed, width, height);

        //Cutting the idle sprite sheet into individual regions
        spritesList.add(new TextureRegion(idleDown, 24, 18, 17,30));
        spritesList.add(new TextureRegion(idleDown, 88, 19, 17, 30));
        spritesList.add(new TextureRegion(idleDown, 152, 19, 17, 30));
        spritesList.add(new TextureRegion(idleDown, 216, 19, 17, 30));
    }

    //Input Handling Methods

    //This is a method to handle basic WASD movement
    public void update(Room room, int tileSize) {

        float delta = Gdx.graphics.getDeltaTime();

        float moveX = 0;
        float moveY = 0;

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A) ||
            Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D) ||
            Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S) ||
            Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) {
            isMoving = true;
        }
        else {
            isMoving = false;
        }


        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
            direction = 0; //Left
            moveX -= getSpeed() * delta;
            facing = Facing.LEFT;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            direction = 1; //Right
            moveX += getSpeed() * delta;
            facing = Facing.RIGHT;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) {
            direction = 2; //Down
            moveY -= getSpeed() * delta;
            facing = Facing.DOWN;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) {
            direction = 3; //Up
            moveY += getSpeed() * delta;
            facing = Facing.UP;
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
    public void increaseMaxHealth(int amount) {
        maxHealth += amount;
        health += amount;
    }

    //This method is used to draw the player
    public void draw(SpriteBatch spriteBatch, float delta) {
        animTimer += delta;

        if(animTimer >= 0.20f) {
            animTimer -= 0.20f; // Keeps it stable even if delta is a bit big
            currSpriteIndex = (currSpriteIndex + 1) % spritesList.size();
        }

        spriteBatch.draw(spritesList.get(currSpriteIndex), getX(), getY(), 32f, 51f);
    }

    public void dispose() {
        idleDown.dispose();
    }
}
