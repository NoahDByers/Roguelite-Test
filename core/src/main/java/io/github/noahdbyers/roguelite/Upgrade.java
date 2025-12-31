package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;

public class Upgrade {
    public final String name;
    public final String desc;
    public Texture cardTexture;
    public Texture iconTexture;
    private ArrayList<TextureRegion> animation;

    public Upgrade(String name, String desc, ArrayList<TextureRegion> animation) {
        this.name = name;
        this.desc = desc;
        this.animation = animation;
    }

    public Texture getCardTexture() {
        return cardTexture;
    }
    public Texture getCardIcon() {
        return iconTexture;
    }
    public ArrayList<TextureRegion> getAnimation() {
        return animation;
    }
}
