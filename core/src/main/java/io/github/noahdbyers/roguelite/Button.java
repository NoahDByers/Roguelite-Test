package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

public class Button {
    BitmapFont font = new BitmapFont();
    private float x;
    private float y;
    private float width;
    private float height;
    private String buttonText;
    private ArrayList<TextureRegion> textures;
    private int currTextureIndex;

    Button(float x, float y, float width, float height, String buttonText, ArrayList<TextureRegion> textures) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.buttonText = buttonText;
        this.textures = textures;
    }
    public boolean isHovered(Viewport viewport) {
        Vector3 mouse = new Vector3(
            Gdx.input.getX(),
            Gdx.input.getY(),
            0
        );
        viewport.unproject(mouse);

        return mouse.x >= x &&
                mouse.x <= x + width &&
                mouse.y >= y &&
                mouse.y <= y + height;
    }

    public void drawButton(SpriteBatch spriteBatch) {
        if (buttonText != null) {
            spriteBatch.draw(textures.get(currTextureIndex), x, y, width, height);

            font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            font.getData().setScale(3f);
            font.draw(spriteBatch, buttonText, x, y + height / 2 + 20f, width, Align.center, true);
        }
        else {
            spriteBatch.draw(textures.get(currTextureIndex), x, y, width, height);
        }
    }
    public boolean isClicked(Viewport viewport) {
        return isHovered(viewport) && Gdx.input.justTouched();
    }

    public void setCurrTextureIndex(int index) {
        this.currTextureIndex = index;
    }

    public int getCurrTextureIndex() {
        return currTextureIndex;
    }

}
