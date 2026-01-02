package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.graphics.Texture;
public class Utility {

    public static Texture loadNearest(String path) {
        Texture t = new Texture(path);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }


}
