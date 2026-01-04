package io.github.noahdbyers.roguelite;

public abstract class Enemy {

    public enum Facing { UP, DOWN, LEFT, RIGHT }

    // Position + movement
    protected float x, y;
    protected float speed;
    protected float width, height;

    // Health
    protected int health;

    // Facing / visuals
    private Facing facing = Facing.DOWN;

    // Hit flash
    private float flashTimer = 0f;
    private static final float FLASH_TIME = 0.08f;

    // Hitstun
    private float hitstunTimer = 0f;

    public Enemy(float x, float y, float speed, float width, float height, int health) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.width = width;
        this.height = height;
        this.health = health;
    }

    // ----------------------------
    // Core update (implemented by subclasses)
    // ----------------------------
    public abstract void update(Player player, Room room, int tileSize);

    // ----------------------------
    // Hitstun
    // ----------------------------
    public void applyHitstun(float seconds) {
        if (seconds <= 0f) return;
        hitstunTimer = Math.max(hitstunTimer, seconds); // don't overwrite longer stun
    }

    public boolean isHitstunned() {
        return hitstunTimer > 0f;
    }

    protected void tickHitstun(float dt) {
        if (hitstunTimer > 0f) {
            hitstunTimer -= dt;
            if (hitstunTimer < 0f) hitstunTimer = 0f;
        }
    }

    // ----------------------------
    // Knockback hook
    // (Zombie overrides updateKnockback + takeKnockback)
    // ----------------------------
    protected void updateKnockback(float dt, Room room, int tileSize) {
        // default: nothing
    }

    public void takeKnockback(float dirX, float dirY, float force) {
        // default: do nothing (Zombie overrides)
    }

    // ----------------------------
    // Damage / death
    // ----------------------------
    public void takeDamage(int amount) {
        if (amount <= 0) return;
        health -= amount;
        if (health < 0) health = 0;
        flashTimer = FLASH_TIME;
    }

    public boolean isDead() {
        return health <= 0;
    }

    // ----------------------------
    // Flashing
    // ----------------------------
    public boolean isFlashing() {
        return flashTimer > 0f;
    }

    // Call this from GameWorld each frame if you want flash to tick globally,
    // OR tick it inside each Enemy.update(). Your Zombie draw checks isFlashing().
    public void tickFlash(float dt) {
        if (flashTimer > 0f) {
            flashTimer -= dt;
            if (flashTimer < 0f) flashTimer = 0f;
        }
    }

    // ----------------------------
    // Facing
    // ----------------------------
    public Facing getFacing() { return facing; }
    public void setFacing(Facing f) { if (f != null) facing = f; }

    // ----------------------------
    // Getters
    // ----------------------------
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getSpeed() { return speed; }
    public int getHealth() { return health; }

    // Optional setters if you prefer not to access x/y directly
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
}
