package io.github.noahdbyers.roguelite;

public class DamagePopup {
    public float x, y;
    public int amount;
    public float timeLeft = 0.6f;

    public DamagePopup(float x, float y, int amount) {
        this.x = x;
        this.y = y;
        this.amount = amount;
    }

    public void update(float dt) {
        timeLeft -= dt;
        y += 18f * dt; // float upward
    }

    public boolean isDead() {
        return timeLeft <= 0f;
    }
}
