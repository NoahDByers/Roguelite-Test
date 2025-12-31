package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Enemy extends Entity {

    public enum Facing { UP, DOWN, LEFT, RIGHT }

    private int health;

    // Direction the enemy is currently facing
    private Facing facing = Facing.DOWN;

    // Flash effect
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.1f;

    // ----------------------------
    // Knockback
    // ----------------------------
    private float knockVX = 0f;
    private float knockVY = 0f;
    private float knockTimer = 0f;

    // How long knockback influence lasts (seconds)
    private static final float KNOCK_DURATION = 0.12f;

    // Exponential-ish damping so knockback eases out
    private static final float KNOCK_DAMPING = 14f; // higher = faster stop

    public Enemy(float x, float y, float speed, float width, float height, int health) {
        super(x, y, speed, width, height);
        this.health = health;
    }

    /**
     * Apply knockback in the given direction.
     *
     * @param dirX direction x (doesn't need to be normalized)
     * @param dirY direction y (doesn't need to be normalized)
     * @param strength knockback speed magnitude (world units per second)
     */
    public void takeKnockback(float dirX, float dirY, float strength) {
        float len2 = dirX * dirX + dirY * dirY;
        if (len2 < 0.0001f) return;

        float invLen = (float)(1.0 / Math.sqrt(len2));
        dirX *= invLen;
        dirY *= invLen;

        // Additive knockback (lets multiple hits "stack" a bit)
        knockVX += dirX * strength;
        knockVY += dirY * strength;

        // Refresh timer
        knockTimer = KNOCK_DURATION;
    }

    public void update(Player player, Room room, int tileSize) {
        float delta = Gdx.graphics.getDeltaTime();
        if (player == null || room == null) return;

        // 1) Apply knockback movement first (with collision), then damp it
        applyKnockback(room, tileSize, delta);

        // 2) Normal movement toward player
        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;
        float ex = getX() + getWidth() * 0.5f;
        float ey = getY() + getHeight() * 0.5f;

        float dx = px - ex;
        float dy = py - ey;

        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length != 0f) {
            dx /= length;
            dy /= length;
        }

        float moveX = dx * getSpeed() * delta;
        float moveY = dy * getSpeed() * delta;

        float oldX = getX();
        float oldY = getY();

        // Move X then resolve collision
        setX(oldX + moveX);
        if (collidesWithRoom(room, tileSize)) {
            setX(oldX);
            moveX = 0f;
        }

        // Move Y then resolve collision
        setY(oldY + moveY);
        if (collidesWithRoom(room, tileSize)) {
            setY(oldY);
            moveY = 0f;
        }

        updateFacing(moveX, moveY);

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= delta;
        }
    }

    private void applyKnockback(Room room, int tileSize, float delta) {
        if (knockTimer <= 0f) return;

        float oldX = getX();
        float oldY = getY();

        // X axis
        setX(oldX + knockVX * delta);
        if (collidesWithRoom(room, tileSize)) {
            setX(oldX);
            knockVX = 0f;
        }

        // Y axis
        setY(oldY + knockVY * delta);
        if (collidesWithRoom(room, tileSize)) {
            setY(oldY);
            knockVY = 0f;
        }

        // Damping + timer
        knockTimer -= delta;
        if (knockTimer <= 0f) {
            knockTimer = 0f;
            knockVX = 0f;
            knockVY = 0f;
        } else {
            // Smoothly decay velocity while timer is active
            float damp = (float)Math.exp(-KNOCK_DAMPING * delta);
            knockVX *= damp;
            knockVY *= damp;
        }
    }

    /**
     * Updates facing based on movement direction.
     * Uses dominant axis to avoid jitter.
     */
    private void updateFacing(float moveX, float moveY) {
        if (moveX == 0f && moveY == 0f) return;

        if (Math.abs(moveX) > Math.abs(moveY)) {
            facing = (moveX > 0f) ? Facing.RIGHT : Facing.LEFT;
        } else {
            facing = (moveY > 0f) ? Facing.UP : Facing.DOWN;
        }
    }

    public Facing getFacing() { return facing; }

    public void takeDamage(int amount) {
        health -= amount;
        hitFlashTimer = HIT_FLASH_DURATION;
    }

    public boolean isDead() { return health <= 0; }

    public boolean isFlashing() { return hitFlashTimer > 0f; }

    /**
     * Bounds-safe tile collision check.
     * (Prevents ArrayIndexOutOfBounds when near edges.)
     */
    public boolean collidesWithRoom(Room room, int tileSize) {
        int[][] grid = room.getRoom();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        int leftTile   = clamp((int)(getX() / tileSize), 0, roomW - 1);
        int rightTile  = clamp((int)((getX() + getWidth()) / tileSize), 0, roomW - 1);
        int bottomTile = clamp((int)(getY() / tileSize), 0, roomH - 1);
        int topTile    = clamp((int)((getY() + getHeight()) / tileSize), 0, roomH - 1);

        for (int y = bottomTile; y <= topTile; y++) {
            for (int x = leftTile; x <= rightTile; x++) {
                int t = grid[y][x];
                if (t == 1 || t == 2 || t == 3 || t == 4 || t == 5) return true;
            }
        }
        return false;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Debug draw (ShapeRenderer).
     * IMPORTANT: caller must have already called shapeRenderer.begin(...)
     * Do NOT call begin/end/set ShapeType here.
     */
    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        // body
        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.rect(getX(), getY(), getWidth(), getHeight());

        // facing line
        shapeRenderer.setColor(1, 1, 0, 1);
        float cx = getX() + getWidth() / 2f;
        float cy = getY() + getHeight() / 2f;
        float len = 8f;

        switch (facing) {
            case UP:    shapeRenderer.line(cx, cy, cx, cy + len); break;
            case DOWN:  shapeRenderer.line(cx, cy, cx, cy - len); break;
            case LEFT:  shapeRenderer.line(cx, cy, cx - len, cy); break;
            case RIGHT: shapeRenderer.line(cx, cy, cx + len, cy); break;
        }
    }

    // Kept to avoid breaking callers
    public void update() {}
}
