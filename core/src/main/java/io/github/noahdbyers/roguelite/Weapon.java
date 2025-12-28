package io.github.noahdbyers.roguelite;

public class Weapon {
    private String name;
    private float attackCooldown;
    private float attackCooldownTime;


    Weapon(String name, float attackCooldown, float attackCooldownTime) {
        this.name = name;
        this.attackCooldown = attackCooldown;
        this.attackCooldownTime = attackCooldownTime;
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }

    public float getAttackCooldownTime() {
        return attackCooldownTime;
    }

    public void setAttackCooldown(float attackCooldown) {
        this.attackCooldown = attackCooldown;
    }

    public void setAttackCooldownTime(float attackCooldownTime) {
        this.attackCooldownTime = attackCooldownTime;
    }

}
