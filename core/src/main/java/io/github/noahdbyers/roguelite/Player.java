package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;

public class Player extends Entity {

    public enum Facing { UP, DOWN, LEFT, RIGHT }

    // Movement / Facing state
    private Facing facing = Facing.DOWN;
    private Facing lastFacing = Facing.DOWN;

    // Attack-facing lock
    // While > 0, the player cannot change facing direction.
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
    private static final float IDLE_FRAME_TIME = 0.25f; // slower idle
    private static final float RUN_FRAME_TIME  = 0.12f; // faster run

    private float animTimer = 0f;
    private int frameIndex = 0;

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
        runSheet = new Texture("player/Run.png");

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
            upRunFrames.add(new TextureRegion(runSheet, x0, 132,   fw, 26));
        }
    }

    /**
     * Call this when the attack animation starts.
     * For the duration, facing changes are ignored (WASD and setFacing()).
     */
    public void startAttackLock(float durationSeconds) {
        if (durationSeconds <= 0f) return;

        // Lock to whatever direction we are currently facing
        lockedFacing = facing;

        // Keep the longer lock if overlapping attacks happen
        if (durationSeconds > attackLockTimer) {
            attackLockTimer = durationSeconds;
        }
    }

    /** True if the player is currently locked from changing facing direction. */
    public boolean isAttackLocked() {
        return attackLockTimer > 0f;
    }

    /**
     * If you ever want to lock to a specific facing (e.g., aim direction),
     * call this instead of setFacing + startAttackLock.
     */
    public void startAttackLock(float durationSeconds, Facing lockTo) {
        if (durationSeconds <= 0f) return;
        if (lockTo != null) {
            lockedFacing = lockTo;
            facing = lockTo; // immediately face that way
        } else {
            lockedFacing = facing;
        }

        if (durationSeconds > attackLockTimer) {
            attackLockTimer = durationSeconds;
        }
    }

    public void update(Room room, int tileSize) {
        float delta = Gdx.graphics.getDeltaTime();

        // Dash movement (kept as-is)
        if (isDashing()) {
            float oldX = getX();
            float oldY = getY();

            setX(oldX + dashVX * delta);
            if (collidesWithRoom(room.getRoom(), tileSize, room.getRoomWidth(),
                room.getRoomHeight())) setX(oldX);

            setY(oldY + dashVY * delta);
            if (collidesWithRoom(room.getRoom(), tileSize, room.getRoomWidth(),
                room.getRoomHeight())) setY(oldY);

            clampToScreen();
            return;
        }

        if(!isAttackLocked()) {
            float moveX = 0f;
            float moveY = 0f;

            boolean left = Gdx.input.isKeyPressed(Input.Keys.A);
            boolean right = Gdx.input.isKeyPressed(Input.Keys.D);
            boolean down = Gdx.input.isKeyPressed(Input.Keys.S);
            boolean up = Gdx.input.isKeyPressed(Input.Keys.W);

            // Facing priority (horizontal > vertical)
            // IMPORTANT: only allow changing facing if not attack-locked.
            if (!isAttackLocked()) {
                if (left && !right) facing = Facing.LEFT;
                else if (right && !left) facing = Facing.RIGHT;
                else if (down && !up) facing = Facing.DOWN;
                else if (up && !down) facing = Facing.UP;
            } else {
                // Ensure facing stays at the lockedFacing even if something else modified it
                facing = lockedFacing;
            }

            // Intended movement (movement is still allowed during attack lock)
            if (left) moveX -= getSpeed() * delta;
            if (right) moveX += getSpeed() * delta;
            if (down) moveY -= getSpeed() * delta;
            if (up) moveY += getSpeed() * delta;

            float oldX = getX();
            float oldY = getY();

            setX(oldX + moveX);
            if (collidesWithRoom(room.getRoom(), tileSize, room.getRoomWidth(), room.getRoomHeight())) {
                setX(oldX);
            }

            setY(oldY + moveY);
            if (collidesWithRoom(room.getRoom(), tileSize, room.getRoomWidth(), room.getRoomHeight())) {
                setY(oldY);
            }

            boolean newMoving = (getX() != oldX) || (getY() != oldY);

            boolean stateChanged =
                (facing != lastFacing) ||
                    (newMoving && !lastMoving) ||
                    (!newMoving && lastMoving);

            isMoving = newMoving;

            // Reset the animation when we change direction OR start/stop moving
            if (stateChanged) {
                animTimer = 0f;
                frameIndex = 0;
                lastFacing = facing;
                lastMoving = isMoving;
            } else {
                lastMoving = isMoving;
            }
        }
    }

    private ArrayList<TextureRegion> getFramesForFacing() {
        if (facing == Facing.UP && isMoving) return upRunFrames;
        if ((facing == Facing.LEFT || facing == Facing.RIGHT) && isMoving) return rightRunFrames;
        if (facing == Facing.DOWN && isMoving) return downRunFrames;

        if (facing == Facing.UP && !isMoving) return upIdleFrames;
        if ((facing == Facing.LEFT || facing == Facing.RIGHT) && !isMoving) return rightIdleFrames;
        return downIdleFrames;
    }

    public void draw(SpriteBatch spriteBatch, float delta) {
        ArrayList<TextureRegion> frames = getFramesForFacing();
        if (frames.isEmpty()) return;

        float frameTime = isMoving ? RUN_FRAME_TIME : IDLE_FRAME_TIME;

        animTimer += delta;
        while (animTimer >= frameTime) {
            animTimer -= frameTime;
            frameIndex = (frameIndex + 1) % frames.size();
        }

        if (isInvulnerable()) {
            if (((int)(invulnTimer * 20f)) % 2 == 0) return;
        }

        TextureRegion region = frames.get(frameIndex);

        float drawW = 32f;
        float drawH = 48f;

        boolean flipX = (facing == Facing.LEFT);
        float x = flipX ? (getX() + drawW) : getX();
        float w = flipX ? -drawW : drawW;

        spriteBatch.draw(region, x, getY(), w, drawH);
    }

    public void updateTimers(float delta) {
        if (invulnTimer > 0f) {
            invulnTimer -= delta;
            if (invulnTimer < 0f) invulnTimer = 0f;
        }

        if (dashCooldown > 0f) dashCooldown -= delta;
        if (dashTimer > 0f) dashTimer -= delta;

        // Attack-facing lock timer
        if (attackLockTimer > 0f) {
            attackLockTimer -= delta;
            if (attackLockTimer <= 0f) {
                attackLockTimer = 0f;
                // lock ends; facing remains whatever it currently is (lockedFacing == facing)
            } else {
                // Keep facing pinned during the lock
                facing = lockedFacing;
            }
        }
    }

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

    private boolean collidesWithRoom(int[][] grid, int tileSize, int roomW, int roomH) {
        int leftTile   = (int)(getX() / tileSize);
        int rightTile  = (int)((getX() + getWidth()) / tileSize);
        int bottomTile = (int)(getY() / tileSize);
        int topTile    = (int)((getY() + getHeight()) / tileSize);

        leftTile   = clamp(leftTile, 0, roomW - 1);
        rightTile  = clamp(rightTile, 0, roomW - 1);
        bottomTile = clamp(bottomTile, 0, roomH - 1);
        topTile    = clamp(topTile, 0, roomH - 1);

        for (int ty = bottomTile; ty <= topTile; ty++) {
            for (int tx = leftTile; tx <= rightTile; tx++) {
                if (grid[ty][tx] == 1 || grid[ty][tx] == 2 || grid[ty][tx] == 3 || grid[ty][tx] == 4 || grid[ty][tx] == 5) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * External facing setter.
     * IMPORTANT: now respects attack lock (ignores changes during the lock).
     */
    public void setFacing(Facing facing) {
        if (facing == null) return;
        if (isAttackLocked()) return; // ignore during attack animation
        this.facing = facing;
    }

    public void startDash(float dirX, float dirY) {
        if (dashCooldown > 0f || isDashing()) return;

        float len = (float)Math.sqrt(dirX*dirX + dirY*dirY);
        if (len == 0f) return;
        dirX /= len; dirY /= len;

        dashVX = dirX * dashSpeed;
        dashVY = dirY * dashSpeed;

        dashTimer = dashDuration;
        dashCooldown = dashCooldownTime;
    }

    public void dispose() {
        idleSheet.dispose();
        runSheet.dispose();
    }

    // Unused overload present in your original class; kept to avoid breaking callers.
    public void update() {}
}
