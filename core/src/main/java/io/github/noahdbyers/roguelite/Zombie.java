package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

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
    // Knockback (from player hits)
    // ----------------------------
    private float kbVX = 0f;
    private float kbVY = 0f;

    private static final float KB_DAMPING = 10f;
    private static final float KB_STOP_EPS = 6f;

    // ----------------------------
    // Collision
    // ----------------------------
    private static final int COLLISION_SOLID = 76;

    // ----------------------------
    // Bite attack state machine
    // ----------------------------
    private enum BiteState { READY, WINDUP, ACTIVE, RECOVERY, COOLDOWN }
    private BiteState biteState = BiteState.READY;

    private float biteTimer = 0f;
    private boolean biteDidHit = false;

    // Locked bite direction (set at windup start, reused for whole attack)
    private float biteDirX = 1f;
    private float biteDirY = 0f;

    // Lunge motion (separate from knockback so it doesn’t “stack forever”)
    private boolean lungeStarted = false;
    private float lungeVX = 0f;
    private float lungeVY = 0f;
    private float lungeTimeLeft = 0f;

    // ----------------------------
    // Tuning (feel knobs)
    // ----------------------------
    private static final float TRIGGER_RANGE = 80f;     // start windup when near player (before touching)
    private static final float WINDUP_TIME   = 0.30f;   // telegraph
    private static final float ACTIVE_TIME   = 0.12f;   // bite window
    private static final float RECOVERY_TIME = 0.18f;   // after attack
    private static final float COOLDOWN_TIME = 0.55f;   // time between attacks
    private static final int   BITE_DAMAGE   = 1;

    // Lunge tuning
    private static final float LUNGE_SPEED = 220f;      // px/sec
    private static final float LUNGE_TIME  = 0.50f;     // seconds

    // Bite hitbox (in front of zombie)
    private static final float BITE_W = 34f;
    private static final float BITE_H = 28f;
    private static final float BITE_REACH = 18f;

    private final Rectangle biteRect = new Rectangle();

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
    // Update
    // ----------------------------
    @Override
    public void update(Player player, Room room, int tileSize) {
        float dt = Gdx.graphics.getDeltaTime();

        // tick hitstun (assumes Enemy has this)
        tickHitstun(dt);

        // knockback + lunge always update (even while stunned)
        updateKnockback(dt, room, tileSize);
        updateLunge(dt, room, tileSize);

        // If stunned, cancel attack + do not steer
        if (isHitstunned()) {
            cancelBiteIntoCooldown();
            return;
        }

        if (player == null || room == null) return;

        updateBite(dt, player);

        // During bite states, do not chase (telegraph fairness)
        if (biteState == BiteState.WINDUP || biteState == BiteState.ACTIVE || biteState == BiteState.RECOVERY) {
            return;
        }

        chasePlayer(dt, player, room, tileSize);
    }

    private void chasePlayer(float dt, Player player, Room room, int tileSize) {
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

        if (Math.abs(dx) > Math.abs(dy)) setFacing(dx > 0 ? Facing.RIGHT : Facing.LEFT);
        else setFacing(dy > 0 ? Facing.UP : Facing.DOWN);

        float vx = dx * getSpeed();
        float vy = dy * getSpeed();

        moveWithCollisions(room, tileSize, vx * dt, vy * dt);
    }

    private void updateBite(float dt, Player player) {
        biteTimer += dt;

        float zx = getX() + getWidth() * 0.5f;
        float zy = getY() + getHeight() * 0.5f;

        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;

        float dx = px - zx;
        float dy = py - zy;

        // Trigger range padded by player size so it doesn’t require touching
        float playerPad = Math.max(player.getWidth(), player.getHeight()) * 0.35f;
        float trigger = TRIGGER_RANGE + playerPad;
        float dist2 = dx * dx + dy * dy;

        switch (biteState) {
            case READY: {
                if (dist2 <= trigger * trigger) {
                    lockFacingTo(dx, dy);
                    biteState = BiteState.WINDUP;
                    biteTimer = 0f;
                    biteDidHit = false;

                    lungeStarted = false;
                    lungeTimeLeft = 0f;
                }
                break;
            }

            case WINDUP: {
                // keep facing locked; do not re-track player
                if (biteTimer >= WINDUP_TIME) {
                    biteState = BiteState.ACTIVE;
                    biteTimer = 0f;
                }
                break;
            }

            case ACTIVE: {
                // Start lunge ONCE at ACTIVE start
                if (!lungeStarted) {
                    lungeStarted = true;
                    lungeVX = biteDirX * LUNGE_SPEED;
                    lungeVY = biteDirY * LUNGE_SPEED;
                    lungeTimeLeft = LUNGE_TIME;
                }

                // Attempt hit once during ACTIVE
                if (!biteDidHit) {
                    buildBiteRect();
                    if (overlapsPlayer(player, biteRect) && !player.isInvulnerable()) {
                        biteDidHit = true;
                        player.takeDamage(BITE_DAMAGE);
                    }
                }

                if (biteTimer >= ACTIVE_TIME) {
                    biteState = BiteState.RECOVERY;
                    biteTimer = 0f;
                }
                break;
            }

            case RECOVERY: {
                if (biteTimer >= RECOVERY_TIME) {
                    biteState = BiteState.COOLDOWN;
                    biteTimer = 0f;
                }
                break;
            }

            case COOLDOWN: {
                if (biteTimer >= COOLDOWN_TIME) {
                    biteState = BiteState.READY;
                    biteTimer = 0f;
                }
                break;
            }
        }
    }

    private void lockFacingTo(float dx, float dy) {
        float len2 = dx * dx + dy * dy;
        if (len2 < 0.0001f) {
            biteDirX = 1f;
            biteDirY = 0f;
        } else {
            float inv = 1f / (float)Math.sqrt(len2);
            biteDirX = dx * inv;
            biteDirY = dy * inv;
        }

        if (Math.abs(biteDirX) > Math.abs(biteDirY)) {
            setFacing(biteDirX > 0 ? Facing.RIGHT : Facing.LEFT);
        } else {
            setFacing(biteDirY > 0 ? Facing.UP : Facing.DOWN);
        }
    }

    private void cancelBiteIntoCooldown() {
        if (biteState == BiteState.WINDUP || biteState == BiteState.ACTIVE || biteState == BiteState.RECOVERY) {
            biteState = BiteState.COOLDOWN;
            biteTimer = 0f;
        }
        biteDidHit = false;
        lungeStarted = false;
        lungeTimeLeft = 0f;
    }

    private void buildBiteRect() {
        float cx = getX() + getWidth() * 0.5f;
        float cy = getY() + getHeight() * 0.5f;

        float hx = cx + biteDirX * BITE_REACH;
        float hy = cy + biteDirY * BITE_REACH;

        biteRect.set(hx - BITE_W * 0.5f, hy - BITE_H * 0.5f, BITE_W, BITE_H);
    }

    private boolean overlapsPlayer(Player p, Rectangle r) {
        float px = p.getX(), py = p.getY(), pw = p.getWidth(), ph = p.getHeight();
        return px < r.x + r.width && px + pw > r.x && py < r.y + r.height && py + ph > r.y;
    }

    // ----------------------------
    // Knockback & lunge motion
    // ----------------------------
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

    private void updateLunge(float dt, Room room, int tileSize) {
        if (room == null) return;
        if (lungeTimeLeft <= 0f) return;

        float step = Math.min(dt, lungeTimeLeft);
        lungeTimeLeft -= step;

        moveWithCollisions(room, tileSize, lungeVX * step, lungeVY * step);

        // If we hit a wall mid-lunge, moveWithCollisions will zero kbVX/kbVY,
        // but lunge uses its own velocity — so we can stop lunge early if blocked.
        // A simple version: if lunge is still active but we can’t move, it will feel “sticky”.
        // If you want early-stop, we’d need to detect collision; keeping simple for now.

        if (lungeTimeLeft <= 0f) {
            lungeTimeLeft = 0f;
        }
    }

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
    // Movement with collision
    // ----------------------------
    private void moveWithCollisions(Room room, int tileSize, float dx, float dy) {
        if (dx != 0f) {
            float nx = getX() + dx;
            if (!rectHitsCollision(room, nx, getY(), getWidth(), getHeight(), tileSize)) {
                this.x = nx; // assumes Enemy.x is protected (as in your previous code)
            } else {
                // stop knockback if blocked
                kbVX = 0f;
            }
        }

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
            int cy = (roomH - 1) - ty; // match your flip
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
    // Rendering (with windup telegraph)
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

        // Telegraph: windup = warm pulse
        if (biteState == BiteState.WINDUP) {
            float t = (WINDUP_TIME <= 0f) ? 0f : (biteTimer / WINDUP_TIME);
            float pulse = 0.6f + 0.4f * (float)Math.sin(t * 18f);
            spriteBatch.setColor(1f, 0.75f + 0.15f * pulse, 0.2f, 1f);
        } else if (isFlashing()) {
            spriteBatch.setColor(1f, 0.4f, 0.4f, 1f);
        }

        spriteBatch.draw(frame, getX(), getY(), drawW, drawH);
        spriteBatch.setColor(1f, 1f, 1f, 1f);
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
    }
}
