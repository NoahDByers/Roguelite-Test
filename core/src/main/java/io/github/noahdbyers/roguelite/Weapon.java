package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;

public class Weapon {
    private final String name;
    private float attackCooldown;
    private float attackCooldownTime;

    private final TextureRegion weaponTexture;
    private final ArrayList<TextureRegion> frames;

    private final float width;
    private final float height;
    private int damage;

    // Animation
    private boolean animate;
    private float animTimer = 0f;
    private final float frameTime = 0.03f;
    private int storedFrame = 0;

    // ✅ Locked swing direction (set once per attack)
    private float swingDirX = 1f;
    private float swingDirY = 0f;
    private float swingAngleDeg = 0f;

    // Tuning
    private final float originX = 16f;
    private final float originY = 16f;
    private final float handleOffset = 16f;

    Weapon(String name, float attackCooldownTime, float width, float height, int damage,
           TextureRegion weaponTexture, ArrayList<TextureRegion> frames) {
        this.name = name;
        this.attackCooldownTime = attackCooldownTime;
        this.weaponTexture = weaponTexture;
        this.frames = frames;
        this.width = width;
        this.height = height;
        this.damage = damage;
    }

    /** ✅ Call once at attack start to lock direction for the whole swing. */
    public void startAttack(float playerX, float playerY, float playerW, float playerH, float aimX, float aimY) {
        // Player "anchor" point (use your same logic if you want; here’s a stable center-ish anchor)
        float px = playerX + playerW * 0.5f;
        float py = playerY + playerH * 0.5f;

        float dx = aimX - px;
        float dy = aimY - py;
        float len2 = dx * dx + dy * dy;
        if (len2 < 0.0001f) {
            dx = 1f; dy = 0f;
            len2 = 1f;
        }
        float invLen = (float)(1.0 / Math.sqrt(len2));
        swingDirX = dx * invLen;
        swingDirY = dy * invLen;

        // If your art points "up" by default, keep -90f. If it points right, use 0f.
        swingAngleDeg = (float) Math.toDegrees(Math.atan2(swingDirY, swingDirX)) - 90f;

        // Animation reset + cooldown
        animate = true;
        animTimer = 0f;
        storedFrame = 0;
        attackCooldown = attackCooldownTime;
    }

    public void draw(SpriteBatch spriteBatch, float delta, Player player) {
        if (!animate) return;

        // Advance animation
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

        // ✅ Recompute handle position each frame so sword stays attached to player,
        // but direction stays locked.
        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;

        float handleX = px + swingDirX * handleOffset;
        float handleY = py + swingDirY * handleOffset;

        float drawX = handleX - originX;
        float drawY = handleY - originY;

        spriteBatch.draw(
            frames.get(storedFrame),
            drawX, drawY,
            originX, originY,
            width, height,
            1f, 1f,
            swingAngleDeg
        );
    }

    public void updateTimers(float delta) {
        if (attackCooldown > 0f) {
            attackCooldown -= delta;
            if (attackCooldown < 0f) attackCooldown = 0f;
        }
    }

    public boolean isReady() { return attackCooldown <= 0f; }

    public float getSwingDirX() { return swingDirX; }
    public float getSwingDirY() { return swingDirY; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public float getAttackCooldownTime() { return attackCooldownTime; }
    public void setAttackCooldownTime(float t) { attackCooldownTime = t; }
    public void setAttackCooldown(float c) { attackCooldown = c; }
}
