package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.Collections;

public class Zombie extends Enemy {

    // ----------------------------
    // Shared sprite sheet (loaded once)
    // ----------------------------
    private static Texture zombieRunSheet;
    private static boolean framesBuilt = false;

    private static final ArrayList<TextureRegion> downRunFrames  = new ArrayList<>();
    private static final ArrayList<TextureRegion> upRunFrames    = new ArrayList<>();
    private static final ArrayList<TextureRegion> rightRunFrames = new ArrayList<>();
    private static final ArrayList<TextureRegion> leftRunFrames  = new ArrayList<>();

    // ----------------------------
    // Animation (per zombie)
    // ----------------------------
    private float animTimer = 0f;
    private static final float FRAME_TIME = 0.20f;
    private int frameIndex = 0;

    // ----------------------------
    // Combat feel: knockback + hitstun
    // ----------------------------
    private float kbVX = 0f;
    private float kbVY = 0f;

    private static final float KB_DAMPING = 10f;     // higher = knockback stops faster
    private static final float KB_STOP_EPS = 6f;     // velocity threshold to snap to 0

    // Collision
    private static final int COLLISION_SOLID = 76;

    public Zombie(float x, float y, float speed, float width, float height, int health) {
        super(x, y, speed, width, height, health);
        ensureAssetsLoaded();
    }

    private static void ensureAssetsLoaded() {
        if (zombieRunSheet == null) zombieRunSheet = Utility.loadNearest("Zombie/Walk.png");
        if (!framesBuilt) {
            buildFrames();
            framesBuilt = true;
        }
    }

    private static void buildFrames() {
        downRunFrames.clear();
        upRunFrames.clear();
        rightRunFrames.clear();
        leftRunFrames.clear();

        final int fh = 26;

        Collections.addAll(downRunFrames,
            new TextureRegion(zombieRunSheet, 9, 5, 16, fh),
            new TextureRegion(zombieRunSheet, 40, 5, 16, fh),
            new TextureRegion(zombieRunSheet, 72, 5, 16, fh),
            new TextureRegion(zombieRunSheet, 105, 5, 16, fh));

        Collections.addAll(upRunFrames,
            new TextureRegion(zombieRunSheet, 10, 37, 16, fh),
            new TextureRegion(zombieRunSheet, 42, 37, 16, fh),
            new TextureRegion(zombieRunSheet, 74, 37, 16, fh),
            new TextureRegion(zombieRunSheet, 106, 37, 16, fh));

        Collections.addAll(rightRunFrames,
            new TextureRegion(zombieRunSheet, 11, 70, 16, fh),
            new TextureRegion(zombieRunSheet, 42, 70, 16, fh),
            new TextureRegion(zombieRunSheet, 74, 70, 16, fh),
            new TextureRegion(zombieRunSheet, 107, 70, 16, fh));

        Collections.addAll(leftRunFrames,
            new TextureRegion(zombieRunSheet, 9, 101, 16, fh),
            new TextureRegion(zombieRunSheet, 41, 101, 16, fh),
            new TextureRegion(zombieRunSheet, 72, 101, 16, fh),
            new TextureRegion(zombieRunSheet, 104, 101, 16, fh));
    }

    private ArrayList<TextureRegion> getFrames() {
        Facing f = getFacing();
        if (f == Facing.UP) return upRunFrames;
        if (f == Facing.RIGHT) return rightRunFrames;
        if (f == Facing.LEFT) return leftRunFrames;
        return downRunFrames;
    }

    // ----------------------------
    // AI + hitstun + knockback
    // ----------------------------
    @Override
    public void update(Player player, Room room, int tileSize) {
        float dt = Gdx.graphics.getDeltaTime();
        tickFlash(dt);

        // tick hitstun timer (assumes you added these in Enemy)
        tickHitstun(dt);

        // ✅ knockback/physics always updates (even while stunned)
        updateKnockback(dt, room, tileSize);

        // ✅ stunned enemies do NOT steer/chase
        if (isHitstunned()) return;

        if (player == null || room == null) return;

        // chase player
        float zx = getX() + getWidth() * 0.5f;
        float zy = getY() + getHeight() * 0.5f;

        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;

        float dx = px - zx;
        float dy = py - zy;

        float len2 = dx * dx + dy * dy;
        if (len2 < 0.0001f) return;

        float invLen = 1f / (float)Math.sqrt(len2);
        dx *= invLen;
        dy *= invLen;

        // facing from movement direction
        if (Math.abs(dx) > Math.abs(dy)) {
            setFacing(dx > 0 ? Facing.RIGHT : Facing.LEFT);
        } else {
            setFacing(dy > 0 ? Facing.UP : Facing.DOWN);
        }

        float vx = dx * getSpeed();
        float vy = dy * getSpeed();

        moveWithCollisions(room, tileSize, vx * dt, vy * dt);
    }

    /**
     * Knockback integration (called every frame, including during hitstun).
     * Uses exponential damping so it feels snappy.
     */
    @Override
    protected void updateKnockback(float dt, Room room, int tileSize) {
        if (room == null) return;

        if (kbVX != 0f || kbVY != 0f) {
            moveWithCollisions(room, tileSize, kbVX * dt, kbVY * dt);

            float k = (float)Math.exp(-KB_DAMPING * dt);
            kbVX *= k;
            kbVY *= k;

            if (Math.abs(kbVX) < KB_STOP_EPS) kbVX = 0f;
            if (Math.abs(kbVY) < KB_STOP_EPS) kbVY = 0f;
        }
    }

    /**
     * Called by GameWorld when hitbox connects.
     * If Enemy already defines takeKnockback, this overrides it.
     */
    @Override
    public void takeKnockback(float dirX, float dirY, float force) {
        float len2 = dirX * dirX + dirY * dirY;
        if (len2 < 0.0001f) return;

        float invLen = 1f / (float)Math.sqrt(len2);
        dirX *= invLen;
        dirY *= invLen;

        kbVX += dirX * force;
        kbVY += dirY * force;
    }

    // ----------------------------
    // Movement w/ collision layer (76)
    // ----------------------------
    private void moveWithCollisions(Room room, int tileSize, float dx, float dy) {
        // X axis
        if (dx != 0f) {
            float nx = getX() + dx;
            if (!rectHitsCollision(room, nx, getY(), getWidth(), getHeight(), tileSize)) {
                // assumes Enemy stores x/y as protected OR has setters; see note below
                this.x = nx;
            } else {
                // slide stop
                kbVX = 0f;
            }
        }

        // Y axis
        if (dy != 0f) {
            float ny = getY() + dy;
            if (!rectHitsCollision(room, getX(), ny, getWidth(), getHeight(), tileSize)) {
                this.y = ny;
            } else {
                kbVY = 0f;
            }
        }
    }

    private boolean rectHitsCollision(Room room, float x, float y, float w, float h, int tileSize) {
        int[][] col;
        try { col = room.getCollisions(); }
        catch (Throwable t) { col = null; }
        if (col == null) return false;

        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        int left   = clamp((int)Math.floor(x / tileSize), 0, roomW - 1);
        int right  = clamp((int)Math.floor((x + w - 1f) / tileSize), 0, roomW - 1);
        int bottom = clamp((int)Math.floor(y / tileSize), 0, roomH - 1);
        int top    = clamp((int)Math.floor((y + h - 1f) / tileSize), 0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            int cy = (roomH - 1) - ty; // match your world->collision flip
            for (int tx = left; tx <= right; tx++) {
                if (col[cy][tx] == COLLISION_SOLID) return true;
            }
        }
        return false;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ----------------------------
    // Rendering
    // ----------------------------
    public void draw(SpriteBatch spriteBatch, float delta) {
        ArrayList<TextureRegion> frames = getFrames();
        if (frames == null || frames.isEmpty()) return;

        animTimer += delta;
        while (animTimer >= FRAME_TIME) {
            animTimer -= FRAME_TIME;
            frameIndex = (frameIndex + 1) % frames.size();
        }

        TextureRegion frame = frames.get(frameIndex);

        float drawW = 32f;
        float drawH = 52f;

        if (isFlashing()) spriteBatch.setColor(1f, 0.4f, 0.4f, 1f);
        spriteBatch.draw(frame, getX(), getY(), drawW, drawH);
        spriteBatch.setColor(1, 1, 1, 1);
    }

    public static void disposeShared() {
        if (zombieRunSheet != null) {
            zombieRunSheet.dispose();
            zombieRunSheet = null;
        }
        framesBuilt = false;
        downRunFrames.clear();
        upRunFrames.clear();
        rightRunFrames.clear();
        leftRunFrames.clear();

        System.out.println("Zombie shared dispose called");
    }
}
