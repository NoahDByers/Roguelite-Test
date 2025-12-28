package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Button {
    private float x;
    private float y;
    private float width;
    private float height;
    private Texture texture;
    private SpriteBatch spriteBatch;

    Button(float x, float y, float width, float height, Texture texture) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.texture = texture;
    }
    public void buttonHover() {
        width += 20f;
        height += 20f;
    }

    public void drawButton(SpriteBatch spriteBatch) {
        spriteBatch.draw(texture, x, y, width, height);
    }
    public void drawButton() {
        spriteBatch.begin();

        spriteBatch.draw(texture, x, y, width, height);
        spriteBatch.end();
    }

    public void detectClick() {

    }

}
