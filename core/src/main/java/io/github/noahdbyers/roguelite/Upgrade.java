package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;

public class Upgrade {
    public final String name;
    public final String desc;
    public Texture cardTexture;
    public Texture iconTexture;

    public Upgrade(String name, String desc, Texture cardTexture) {
        this.name = name;
        this.desc = desc;
    }

    public Texture getCardTexture() {
        return cardTexture;
    }
    public Texture getCardIcon() {
        return iconTexture;
    }
}
