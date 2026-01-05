package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Small rendering/asset helpers. */
public class Utility {

    /** Loads a texture with nearest-neighbor filtering (pixel art friendly). */
    public static Texture loadNearest(String path) {
        Texture t = new Texture(path);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    /**
     * Creates a TextureRegion from a spritesheet but insets the UVs by a half texel.
     *
     * Why: At certain resolutions (especially fullscreen) FitViewport rounds its internal
     * pixel bounds, and combined with floating point UVs this can produce 1px seams/gaps.
     * Insetting the UVs ensures sampling stays inside the intended tile.
     */
    public static TextureRegion regionNoBleed(Texture texture, int x, int y, int width, int height) {
        TextureRegion region = new TextureRegion(texture, x, y, width, height);

        // Half-texel inset in UV space.
        float invW = 1f / (float) texture.getWidth();
        float invH = 1f / (float) texture.getHeight();

        float u  = (x + 0.5f) * invW;
        float v  = (y + 0.5f) * invH;
        float u2 = (x + width  - 0.5f) * invW;
        float v2 = (y + height - 0.5f) * invH;

        region.setRegion(u, v, u2, v2);
        return region;
    }
}
