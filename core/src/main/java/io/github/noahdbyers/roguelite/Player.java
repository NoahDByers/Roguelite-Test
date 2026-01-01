package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;

public class Player extends Entity {

    public enum Facing { UP, DOWN, LEFT, RIGHT }

    private static final int COLLISION_SOLID = 76;

    // Movement / Facing state
    private Facing facing = Facing.DOWN;
    private Facing lastFacing = Facing.DOWN;

    // Attack-facing lock
    private float attackLockTimer = 0f;
    private Facing lockedFacing = Facing.DOWN;

    // Movement
    private boolean isMoving = false;
    private boolean lastMoving = false;

    /* Dash Config */
    private float dashSpeed = 650f;
    private float dashDuration = 0.08f;
    private float dashCooldownTime = 0.25f;

    /* Dash Runtime */
    private float dashTimer = 0f;
    private float dashCooldown = 0f;
    private float dashVX = 0f, dashVY = 0f;

    public boolean isDashing() { return dashTimer > 0f; }

    // Animation
    private static final float IDLE_FRAME_TIME = 0.25f;
    private static final float RUN_FRAME_TIME  = 0.12f;

    private float animTimer = 0f;
    private int frameIndex = 0;

    private boolean animationPaused = false;

    // Sprites
    private final Texture idleSheet;
    private final Texture runSheet;

    private final ArrayList<TextureRegion> downIdleFrames = new ArrayList<>();
    private final ArrayList<TextureRegion> rightIdleFrames = new ArrayList<>();
    private final ArrayList<TextureRegion> upIdleFrames = new ArrayList<>();

    private final ArrayList<TextureRegion> downRunFrames = new ArrayList<>();
    private final ArrayList<TextureRegion> rightRunFrames = new ArrayList<>();
    private final ArrayList<TextureRegion> upRunFrames = new ArrayList<>();

    // Stats / timers
    private int maxHealth = 500000;
    private int health = 500000;

    private int mana = health;
    private int maxMana = maxHealth;

    private float invulnTimer = 0f;
    private final float invulnDuration = 0.5f;

    public Player(float x, float y, float speed, float width, float height) {
        super(x, y, speed, width, height);

        idleSheet = new Texture("player/Idle.png");
        runSheet  = new Texture("player/Run.png");

        final int fw = 16;
        final int fh = 24;

        int DOWN_Y  = 7;
        int RIGHT_Y = 39;
        int UP_Y    = 135;

        for (int i = 0; i < 4; i++) {
            int x0 = i * fw;
            downIdleFrames.add(new TextureRegion(idleSheet, x0, DOWN_Y,  fw, fh));
            rightIdleFrames.add(new TextureRegion(idleSheet, x0, RIGHT_Y, fw, fh));
            upIdleFrames.add(new TextureRegion(idleSheet, x0, UP_Y,     fw, fh));
        }

        for (int i = 0; i < 6; i++) {
            int x0 = i * fw;
            downRunFrames.add(new TextureRegion(runSheet, x0, 5,   fw, 26));
            rightRunFrames.add(new TextureRegion(runSheet, x0, 69, fw, 26));
            upRunFrames.add(new TextureRegion(runSheet, x0, 132,  fw, 26));
        }
    }

    // -------------------- Animation Pause --------------------
    public void setAnimationPaused(boolean paused) { this.animationPaused = paused; }
    public boolean isAnimationPaused() { return animationPaused; }

    // -------------------- Attack lock --------------------
    public void startAttackLock(float durationSeconds) {
        if (durationSeconds <= 0f) return;
        lockedFacing = facing;
        if (durationSeconds > attackLockTimer) attackLockTimer = durationSeconds;
    }

    public boolean isAttackLocked() { return attackLockTimer > 0f; }

    public void startAttackLock(float durationSeconds, Facing lockTo) {
        if (durationSeconds <= 0f) return;

        if (lockTo != null) {
            lockedFacing = lockTo;
            facing = lockTo;
        } else {
            lockedFacing = facing;
        }

        if (durationSeconds > attackLockTimer) attackLockTimer = durationSeconds;
    }

    // -------------------- Update --------------------
    public void update(Room room, int tileSize) {
        float delta = Gdx.graphics.getDeltaTime();
        if (room == null) return;

        // Dash movement
        if (isDashing()) {
            float oldX = getX();
            float oldY = getY();

            setX(oldX + dashVX * delta);
            if (collidesWithRoom(room, tileSize)) setX(oldX);

            setY(oldY + dashVY * delta);
            if (collidesWithRoom(room, tileSize)) setY(oldY);

            clampToRoom(room); // <- important now that room size isn't always screen size
            return;
        }

        float moveX = 0f;
        float moveY = 0f;

        boolean left  = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D);
        boolean down  = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean up    = Gdx.input.isKeyPressed(Input.Keys.W);

        // Facing: only update if NOT attack-locked
        if (!isAttackLocked()) {
            if (left && !right) facing = Facing.LEFT;
            else if (right && !left) facing = Facing.RIGHT;
            else if (down && !up) facing = Facing.DOWN;
            else if (up && !down) facing = Facing.UP;
        } else {
            facing = lockedFacing;
        }

        if (left)  moveX -= getSpeed() * delta;
        if (right) moveX += getSpeed() * delta;
        if (down)  moveY -= getSpeed() * delta;
        if (up)    moveY += getSpeed() * delta;

        float oldX = getX();
        float oldY = getY();

        // X axis
        setX(oldX + moveX);
        if (collidesWithRoom(room, tileSize)) setX(oldX);

        // Y axis
        setY(oldY + moveY);
        if (collidesWithRoom(room, tileSize)) setY(oldY);

        clampToRoom(room);

        boolean newMoving = (getX() != oldX) || (getY() != oldY);
        isMoving = newMoving;

        boolean visualMoving = isMoving && !isAttackLocked();

        boolean stateChanged =
            (facing != lastFacing) ||
                (visualMoving && !lastMoving) ||
                (!visualMoving && lastMoving);

        if (stateChanged) {
            animTimer = 0f;
            frameIndex = 0;
            lastFacing = facing;
            lastMoving = visualMoving;
        } else {
            lastMoving = visualMoving;
        }
    }

    // -------------------- Frame selection --------------------
    private ArrayList<TextureRegion> getFramesForFacing(boolean visualMoving) {
        if (!visualMoving) {
            if (facing == Facing.UP) return upIdleFrames;
            if (facing == Facing.LEFT || facing == Facing.RIGHT) return rightIdleFrames;
            return downIdleFrames;
        }

        if (facing == Facing.UP) return upRunFrames;
        if (facing == Facing.LEFT || facing == Facing.RIGHT) return rightRunFrames;
        return downRunFrames;
    }

    // -------------------- Draw --------------------
    public void draw(SpriteBatch spriteBatch, float delta) {
        boolean visualMoving = isMoving && !isAttackLocked();
        ArrayList<TextureRegion> frames = getFramesForFacing(visualMoving);
        if (frames.isEmpty()) return;

        float frameTime = visualMoving ? RUN_FRAME_TIME : IDLE_FRAME_TIME;

        if (!animationPaused) {
            animTimer += delta;
            while (animTimer >= frameTime) {
                animTimer -= frameTime;
                frameIndex = (frameIndex + 1) % frames.size();
            }
        }

        // Invulnerability blink
        if (isInvulnerable()) {
            if (((int)(invulnTimer * 20f)) % 2 == 0) return;
        }

        TextureRegion region = frames.get(Math.max(0, Math.min(frameIndex, frames.size() - 1)));

        float drawW = 32f;
        float drawH = 48f;

        boolean flipX = (facing == Facing.LEFT);
        float x = flipX ? (getX() + drawW) : getX();
        float w = flipX ? -drawW : drawW;

        spriteBatch.draw(region, x, getY(), w, drawH);
    }

    // -------------------- Timers --------------------
    public void updateTimers(float delta) {
        if (invulnTimer > 0f) {
            invulnTimer -= delta;
            if (invulnTimer < 0f) invulnTimer = 0f;
        }

        if (dashCooldown > 0f) dashCooldown -= delta;
        if (dashTimer > 0f) dashTimer -= delta;

        if (attackLockTimer > 0f) {
            attackLockTimer -= delta;
            if (attackLockTimer <= 0f) {
                attackLockTimer = 0f;
            } else {
                facing = lockedFacing;
            }
        }
    }

    // -------------------- Stats --------------------
    public boolean isInvulnerable() { return invulnTimer > 0f; }

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

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    public Facing getFacing() { return facing; }

    public void increaseMaxHealth(int amount) {
        maxHealth += amount;
        health += amount;
    }

    // -------------------- Collision (NEW SYSTEM) --------------------
    private boolean collidesWithRoom(Room room, int tileSize) {
        int[][] col = null;
        try { col = room.getCollisions(); } catch (Throwable ignored) {}

        // If no collision layer exists, treat as no collision
        if (col == null) return false;

        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();

        // -1 avoids sampling the next tile when exactly on an edge
        int left   = (int)Math.floor(x / tileSize);
        int right  = (int)Math.floor((x + w - 1f) / tileSize);
        int bottom = (int)Math.floor(y / tileSize);
        int top    = (int)Math.floor((y + h - 1f) / tileSize);

        for (int ty = bottom; ty <= top; ty++) {
            for (int tx = left; tx <= right; tx++) {
                if (isSolid(col, roomW, roomH, tx, ty)) return true;
            }
        }
        return false;
    }

    private boolean isSolid(int[][] col, int roomW, int roomH, int tx, int ty) {
        // out of bounds = solid
        if (tx < 0 || tx >= roomW || ty < 0 || ty >= roomH) return true;
        return col[ty][tx] == COLLISION_SOLID;
    }

    /**
     * Clamp player to the room bounds (not screen bounds).
     * This matters now that rooms can be different sizes / camera can move.
     */
    public void clampToRoom(Room room) {
        float maxX = room.getRoomWidth() * room.getTileSize() - getWidth();
        float maxY = room.getRoomHeight() * room.getTileSize() - getHeight();

        if (getX() < 0) setX(0);
        if (getY() < 0) setY(0);
        if (getX() > maxX) setX(maxX);
        if (getY() > maxY) setY(maxY);
    }

    // -------------------- Facing setter --------------------
    public void setFacing(Facing facing) {
        if (facing == null) return;
        if (isAttackLocked()) return;
        this.facing = facing;
    }

    // -------------------- Dash --------------------
    public void startDash(float dirX, float dirY) {
        if (dashCooldown > 0f || isDashing()) return;

        float len = (float)Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0f) return;
        dirX /= len;
        dirY /= len;

        dashVX = dirX * dashSpeed;
        dashVY = dirY * dashSpeed;

        dashTimer = dashDuration;
        dashCooldown = dashCooldownTime;
    }

    public void dispose() {
        idleSheet.dispose();
        runSheet.dispose();
    }

    // Kept to avoid breaking callers.
    public void update() {}
}
