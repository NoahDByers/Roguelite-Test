package io.github.noahdbyers.roguelite;

public class Shrine {
    public float x, y;
    public float w = 32f, h = 32f;

    public Upgrade[] stock = new Upgrade[3];

    public Shrine(float x, float y, Upgrade[] stock) {
        this.x = x; this.y = y;
        this.stock = stock;
    }
}
