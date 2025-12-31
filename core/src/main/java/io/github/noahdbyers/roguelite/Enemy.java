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


    public Enemy(float x, float y, float speed, float width, float height, int health) {
        super(x, y, speed, width, height);
        this.health = health;
    }

    public void update(Player player, Room room, int tileSize) {
        float delta = Gdx.graphics.getDeltaTime();
        if (player == null || room == null) return;

        // Move toward player's center (reduces corner jitter)
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
            hitFlashTimer -= Gdx.graphics.getDeltaTime();
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

    public Facing getFacing() {
        return facing;
    }

    public void takeDamage(int amount) {
        health -= amount;
        hitFlashTimer = HIT_FLASH_DURATION;
    }


    public boolean isDead() {
        return health <= 0;
    }

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
                if (grid[y][x] == 1 || grid[y][x] == 2 || grid[y][x] == 3 || grid[y][x] == 4 || grid[y][x] == 5) return true;
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

    public boolean isFlashing() {
        return hitFlashTimer > 0f;
    }

    public void update() {}
}

