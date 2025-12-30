package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Weapon {
    private String name;
    private float attackCooldown;
    private float attackCooldownTime;
    private TextureRegion weaponTexture;


    Weapon(String name, float attackCooldownTime, TextureRegion weaponTexture) {
        this.name = name;
        this.attackCooldownTime = attackCooldownTime;
        this.weaponTexture = weaponTexture;
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }

    public float getAttackCooldownTime() {
        return attackCooldownTime;
    }
    public TextureRegion getWeaponTexture() {
        return weaponTexture;
    }
    public void setAttackCooldown(float attackCooldown) {
        this.attackCooldown = attackCooldown;
    }

    public void setAttackCooldownTime(float attackCooldownTime) {
        this.attackCooldownTime = attackCooldownTime;
    }

}
