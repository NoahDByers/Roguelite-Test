package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class Entity {
    private float x, y;
    private float speed;

    private float width, height;

    Entity(float x, float y, float speed, float width, float height) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.width = width;
        this.height = height;
    }

    Entity() {}

    //Getter and setter methods
    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }
    public float getWidth() {
        return width;
    }
    public float getHeight() {
        return height;
    }
    public float getSpeed() {
        return speed;
    }
    public void setX(float x) {
        this.x = x;
    }
    public void setY(float y) {
        this.y = y;
    }
    public void setSpeed(float speed) {
        this.speed = speed;
    }
    public void setWidth(float width) {
        this.width = width;
    }
    public void setHeight(float height) {
        this.height = height;
    }

    //Logic Methods

    //This is a method to check the boundaries with the edge of the screen
    public void clampToScreen() {
        x = Math.max(0, Math.min(x, Gdx.graphics.getWidth() - width));
        y = Math.max(0, Math.min(y, Gdx.graphics.getHeight() - height));
    }

    //Abstract methods
    abstract public void update();

    public void draw(ShapeRenderer shapeRenderer){}
}
