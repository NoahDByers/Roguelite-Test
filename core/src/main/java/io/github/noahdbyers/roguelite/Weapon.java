package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

public class Weapon {
    private String name;
    private float attackCooldown;
    private float attackCooldownTime;
    private TextureRegion weaponTexture;
    private ArrayList<TextureRegion> frames;

    //Animation Tracking
    private float width;
    private float height;
    private boolean animate;
    private float animTimer = 0f;
    private float frameTime = 0.03f;
    private int storedFrame = 0;
    private float drawX;
    private float drawY;
    private int damage;


    Weapon(String name, float attackCooldownTime, float width, float height, int damage, TextureRegion weaponTexture, ArrayList<TextureRegion> frames) {
        this.name = name;
        this.attackCooldownTime = attackCooldownTime;
        this.weaponTexture = weaponTexture;
        this.frames = frames;
        this.width = width;
        this.height = height;
        this.damage = damage;
    }

    public void draw(SpriteBatch spriteBatch, float delta, Vector2 drawLocationIgnored, Player player, Vector2 mouseWorld) {
        if (!animate) return;

        // --- Advance animation ---
        animTimer += delta;
        while (animTimer >= frameTime) {
            animTimer -= frameTime;
            storedFrame++;
            if (storedFrame >= frames.size()) {
                storedFrame = frames.size() - 1;
                animate = false;
                break;
            }
        }

        float px;
        float py = player.getY() + 35f;

        // --- Player center (anchor point) ---
        if(mouseWorld.y > player.getY()) {
            px = player.getX() + 5f;
        }
        else {
            px = player.getX() + 25f;
        }

        if(mouseWorld.x > player.getX()) {
            py = player.getY() + 35;
        }
        else {
            py = player.getY() + 5;
        }

        // --- Aim direction (player -> mouse) ---
        float dirX = mouseWorld.x - px;
        float dirY = mouseWorld.y - py;
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);

        // Avoid NaNs when mouse is exactly on player
        if (len < 0.0001f) {
            dirX = 1f;
            dirY = 0f;
            len = 1f;
        }
        dirX /= len;
        dirY /= len;

        // --- Smear handle pivot in sprite space ---
        // You said the handle is at (16,16) in the sprite.
        float originX = 16f;
        float originY = 16f;

        // --- Where the HANDLE should be in world space ---
        // This is the "spawn point" for the smear, NOT the center of the sprite.
        float offset = 16f; // increase to push farther from player
        float handleX = px + dirX * offset;
        float handleY = py + dirY * offset;

        // --- Rotation: aim direction ---
        // If your smear art points "up" by default, you may need -90f.
        // If it points "right" by default, use 0f.
        float angleDeg = (float) Math.toDegrees(Math.atan2(dirY, dirX)) - 90f;

        // --- Draw position so that origin lands exactly on the handle point ---
        drawX = handleX - originX;
        drawY = handleY - originY;

        spriteBatch.draw(
            frames.get(storedFrame),
            drawX, drawY,
            originX, originY,     // pivot at the handle (16,16)
            width, height,
            1f, 1f,
            angleDeg
        );
    }

    public void startAttack() {
        animate = true;
        animTimer = 0f;
        storedFrame = 0;
    }
    public float getAttackCooldown() {
        return attackCooldown;
    }

    public float getAttackCooldownTime() {
        return attackCooldownTime;
    }
    public TextureRegion getWeaponTexture() {
        return weaponTexture;
    }
    public float getWidth() {
        return width;
    }
    public float getHeight() {
        return height;
    }
    public void setAttackCooldown(float attackCooldown) {
        this.attackCooldown = attackCooldown;
    }

    public void setAttackCooldownTime(float attackCooldownTime) {
        this.attackCooldownTime = attackCooldownTime;
    }

    public void setAnimateStatus(boolean status) {
        this.animate = status;
    }

    public void setDamge(int damage) {
        this.damage = damage;
    }

    public Vector2 getDrawCoords(Vector2 mouseWorld, Player player) {
        float px;
        float py = player.getY() + 35f;

        // --- Player center (anchor point) ---
        if(mouseWorld.y > player.getY()) {
            px = player.getX() + 5f;
        }
        else {
            px = player.getX() + 25f;
        }

        if(mouseWorld.x > player.getX()) {
            py = player.getY() + 35;
        }
        else {
            py = player.getY() + 5;
        }

        // --- Aim direction (player -> mouse) ---
        float dirX = mouseWorld.x - px;
        float dirY = mouseWorld.y - py;
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);

        // Avoid NaNs when mouse is exactly on player
        if (len < 0.0001f) {
            dirX = 1f;
            dirY = 0f;
            len = 1f;
        }
        dirX /= len;
        dirY /= len;

        // --- Smear handle pivot in sprite space ---
        // You said the handle is at (16,16) in the sprite.
        float originX = 16f;
        float originY = 16f;

        // --- Where the HANDLE should be in world space ---
        // This is the "spawn point" for the smear, NOT the center of the sprite.
        float offset = 16f; // increase to push farther from player
        float handleX = px + dirX * offset;
        float handleY = py + dirY * offset;

        // --- Rotation: aim direction ---
        // If your smear art points "up" by default, you may need -90f.
        // If it points "right" by default, use 0f.
        float angleDeg = (float) Math.toDegrees(Math.atan2(dirY, dirX)) - 90f;

        // --- Draw position so that origin lands exactly on the handle point ---
        drawX = handleX - originX;
        drawY = handleY - originY;

        Vector2 drawCoords = new Vector2(drawX, drawY);
        return drawCoords;
    }

    public int getDamage() {
        return damage;
    }
}
