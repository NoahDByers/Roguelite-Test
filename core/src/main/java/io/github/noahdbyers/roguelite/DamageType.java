package io.github.noahdbyers.roguelite;

/**
 * Damage typing is currently mostly cosmetic / future-proofing.
 * Right now enemies don't have resistances, but having a type makes
 * it easy to extend items & enemies later.
 */
public enum DamageType {
    PHYSICAL,
    HOLY,
    FIRE,
    DARK,
    RADIANT,
    LIGHTNING,
    TRUE
}
