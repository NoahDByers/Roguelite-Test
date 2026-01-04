package io.github.noahdbyers.roguelite;

public class AttackData {
    public final float startup;
    public final float active;
    public final float recovery;

    // when in the timeline we should actually spawn the hitbox
    public final float hitboxTime; // usually inside ACTIVE, often at startup end

    public AttackData(float startup, float active, float recovery, float hitboxTime) {
        this.startup = startup;
        this.active = active;
        this.recovery = recovery;
        this.hitboxTime = hitboxTime;
    }

    public float total() {
        return startup + active + recovery;
    }
}
