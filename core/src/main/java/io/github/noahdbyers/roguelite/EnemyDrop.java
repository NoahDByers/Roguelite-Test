package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
public class EnemyDrop {

    private float x, y;
        private final float size = 70f;

        private final DropType type;
        private final int value;

        private final Texture texture;
        private final Rectangle bounds;

        public EnemyDrop(float x, float y, DropType type, int value) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.value = value;

            this.texture = getTextureForType(type);
            this.bounds = new Rectangle(x, y, getDrawWidth() - 50, size - 40);
        }

    public void update(float delta) {
        bounds.setPosition(x, y);
    }

        public Rectangle getBounds() {
            return bounds;
        }

        public DropType getType() {
            return type;
        }

        public int getValue() {
            return value;
        }

    public void apply(Player player, GameWorld world) {
        switch (type) {
            case HEALTH:
                player.heal(value);
                break;
            case SOUL:
                world.addSouls(value);
                break;
            case COIN:
                world.addCoins(value);
                break;
        }
    }
    private float getDrawWidth() {
        switch (type) {
            case SOUL: return size * 2.2f;
            case COIN: return size * 2.2f;
            default:     return size;
        }
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texture, x, y, getDrawWidth() - 50, size - 40);
    }



    private Texture getTextureForType(DropType type) {
        switch (type) {
            case HEALTH:
                return DropTextures.HEART;
            case SOUL:
                return DropTextures.SOUL;
            case COIN:
                return DropTextures.COIN;
            default:
                return DropTextures.COIN;
        }
    }

}

