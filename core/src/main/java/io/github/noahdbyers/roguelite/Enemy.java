package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Enemy extends Entity {

    public enum Facing { UP, DOWN, LEFT, RIGHT }

    private static final int COLLISION_SOLID = 76;

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

    private static final float KNOCK_DURATION = 0.12f;
    private static final float KNOCK_DAMPING = 14f;

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

        knockVX += dirX * strength;
        knockVY += dirY * strength;

        knockTimer = KNOCK_DURATION;
    }

    public void update(Player player, Room room, int tileSize) {
        float delta = Gdx.graphics.getDeltaTime();
        if (player == null || room == null) return;

        // 1) Apply knockback first
        applyKnockback(room, tileSize, delta);

        // 2) Move toward player
        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;
        float ex = getX() + getWidth() * 0.5f;
        float ey = getY() + getHeight() * 0.5f;

        float dx = px - ex;
        float dy = py - ey;

        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len > 0.0001f) {
            dx /= len;
            dy /= len;
        } else {
            dx = 0f;
            dy = 0f;
        }

        float moveX = dx * getSpeed() * delta;
        float moveY = dy * getSpeed() * delta;

        float beforeX = getX();
        float beforeY = getY();

        // Move X axis then resolve
        setX(beforeX + moveX);
        if (collidesWithRoom(room, tileSize)) {
            setX(beforeX);
            moveX = 0f;
        }

        // Move Y axis then resolve
        setY(beforeY + moveY);
        if (collidesWithRoom(room, tileSize)) {
            setY(beforeY);
            moveY = 0f;
        }

        updateFacing(moveX, moveY);

        if (hitFlashTimer > 0f) hitFlashTimer -= delta;
    }

    private void applyKnockback(Room room, int tileSize, float delta) {
        if (knockTimer <= 0f) return;

        float beforeX = getX();
        float beforeY = getY();

        // X axis
        setX(beforeX + knockVX * delta);
        if (collidesWithRoom(room, tileSize)) {
            setX(beforeX);
            knockVX = 0f;
        }

        // Y axis
        setY(beforeY + knockVY * delta);
        if (collidesWithRoom(room, tileSize)) {
            setY(beforeY);
            knockVY = 0f;
        }

        knockTimer -= delta;
        if (knockTimer <= 0f) {
            knockTimer = 0f;
            knockVX = 0f;
            knockVY = 0f;
        } else {
            float damp = (float)Math.exp(-KNOCK_DAMPING * delta);
            knockVX *= damp;
            knockVY *= damp;
        }
    }

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
     * Collision test using Room.collisions layer where value 76 == solid.
     * Out-of-bounds is treated as solid.
     */
    private boolean collidesWithRoom(Room room, int tileSize) {
        if (room == null) return false;

        int[][] col = room.getCollisions(); // must exist in Room
        if (col == null) return false;

        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();

        int left   = (int)Math.floor(x / tileSize);
        int right  = (int)Math.floor((x + w - 1f) / tileSize);
        int bottom = (int)Math.floor(y / tileSize);
        int top    = (int)Math.floor((y + h - 1f) / tileSize);

        left   = clamp(left,   0, roomW - 1);
        right  = clamp(right,  0, roomW - 1);
        bottom = clamp(bottom, 0, roomH - 1);
        top    = clamp(top,    0, roomH - 1);

        // ✅ If your rendering flips Y, collision should too:
        for (int ty = bottom; ty <= top; ty++) {
            int srcY = (roomH - 1) - ty;
            for (int tx = left; tx <= right; tx++) {
                if (col[srcY][tx] == 76) return true; // 76 = solid
            }
        }
        return false;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
    private boolean isSolid(int[][] col, int roomW, int roomH, int tx, int ty) {
        // out of bounds = solid
        if (tx < 0 || tx >= roomW || ty < 0 || ty >= roomH) return true;
        return col[ty][tx] == COLLISION_SOLID;
    }

    /**
     * Debug draw (ShapeRenderer).
     * Caller must have already called shapeRenderer.begin(...)
     */
    @Override
    public void draw(ShapeRenderer shapeRenderer) {
        // body
        if (isFlashing()) shapeRenderer.setColor(1f, 1f, 1f, 1f);
        else shapeRenderer.setColor(1f, 0f, 0f, 1f);

        shapeRenderer.rect(getX(), getY(), getWidth(), getHeight());

        // facing line
        shapeRenderer.setColor(1f, 1f, 0f, 1f);
        float cx = getX() + getWidth() * 0.5f;
        float cy = getY() + getHeight() * 0.5f;
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
