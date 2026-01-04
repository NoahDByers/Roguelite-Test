package io.github.noahdbyers.roguelite;

public enum CombatState {
    FREE, // normal movement + can attack / interact
    ATTACK_STARTUP, // Pressed attack, not active yet
    ATTACK_ACTIVE, // hitbox is active / created here
    ATTACK_RECOVERY, // after swing; may allow buffering / chaining
    HITSTUN, // got hit (technically optional, will be needed later)
    DEAD
}
