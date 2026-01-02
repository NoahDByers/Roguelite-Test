package io.github.noahdbyers.roguelite;

public class DamagePopup {
    public float x, y;
    public int amount;
    public float timeLeft = 0.6f;

    // small horizontal drift so multiple numbers don't perfectly overlap
    private final float driftX = ((float)Math.random() * 2f - 1f) * 10f;

    public DamagePopup(float x, float y, int amount) {
        this.x = x;
        this.y = y;
        this.amount = amount;
    }

    public void update(float dt) {
        timeLeft -= dt;
        y += 18f * dt;     // float upward (world units)
        x += driftX * dt;  // slight drift
    }

    public boolean isDead() {
        return timeLeft <= 0f;
    }
}
