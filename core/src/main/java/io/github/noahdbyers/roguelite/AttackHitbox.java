// AttackHitbox.java
package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class AttackHitbox {
    public final Rectangle rect = new Rectangle();
    public final Vector2 dir = new Vector2();   // for knockback, etc.

    /** Distance from the attacker center to the hitbox center (world units). */
    public float reach;
    public float timeLeft;
    public int damage;
    public float strength = 1f;   // 1 = normal, >1 = stronger
    public float hitStop = 0.08f; // seconds; can be derived from strength

    /**
     * A melee hitbox that can be re-anchored each frame to follow the attacker.
     *
     * @param w        hitbox width
     * @param h        hitbox height
     * @param dir      direction of the attack (will be normalized)
     * @param reach    distance from attacker center to the hitbox center
     * @param duration lifetime in seconds
     * @param damage   damage dealt on hit
     * @param attackerCenterX attacker center x (used to initialize rect position)
     * @param attackerCenterY attacker center y (used to initialize rect position)
     */
    public AttackHitbox(float w,
                        float h,
                        Vector2 dir,
                        float reach,
                        float duration,
                        int damage,
                        float attackerCenterX,
                        float attackerCenterY) {
        this.rect.set(0f, 0f, w, h);
        this.dir.set(dir).nor();
        this.reach = reach;
        this.timeLeft = duration;
        this.damage = damage;
        reposition(attackerCenterX, attackerCenterY);
    }

    /** Recompute rect.x/y from the current attacker center. */
    public void reposition(float attackerCenterX, float attackerCenterY) {
        float cx = attackerCenterX + dir.x * reach;
        float cy = attackerCenterY + dir.y * reach;
        rect.x = cx - rect.width * 0.5f;
        rect.y = cy - rect.height * 0.5f;
    }

    /** Update lifetime and keep the hitbox aligned to the attacker center. */
    public void update(float delta, float attackerCenterX, float attackerCenterY) {
        timeLeft -= delta;
        if (timeLeft > 0f) {
            reposition(attackerCenterX, attackerCenterY);
        }
    }

    public boolean isExpired() {
        return timeLeft <= 0f;
    }
}
