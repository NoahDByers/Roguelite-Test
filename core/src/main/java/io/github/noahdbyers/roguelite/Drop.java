package io.github.noahdbyers.roguelite;

public class Drop {
    public float x, y;
    public int value;
    public float w = 12f, h = 12f;

    public Drop(float x, float y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }
}
