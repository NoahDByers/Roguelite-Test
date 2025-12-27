package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    static final int TILE_SIZE = 32;
    static final int ROOM_WIDTH = 20; //tiles
    static final int ROOM_HEIGHT = 15; //tiles
    private ShapeRenderer shapeRenderer;
    ArrayList<Enemy> enemies = new ArrayList<>();
    Player user = new Player(100, 100, 200, 32, 32);
    Room start = new Room();

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        enemies.add(new Enemy(200, 200, 100, 28));
        enemies.add(new Enemy(400, 300, 120, 28));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        //Player logic
        user.update(start, TILE_SIZE);
        user.clampToScreen();

        //Enemy logic
        for (Enemy enemy : enemies) {
            enemy.update(user, start, TILE_SIZE);
        }

        //Draw room
        for (int y = 0; y < ROOM_HEIGHT; y++) {
            for (int x = 0; x < ROOM_WIDTH; x++) {
                if (start.getTile(x, y) == 1) {
                    shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
                    shapeRenderer.rect(
                        x * TILE_SIZE,
                        y * TILE_SIZE,
                        TILE_SIZE,
                        TILE_SIZE
                    );
                }
            }
        }

        //Draw enemies
        for (Enemy enemy : enemies) {
            shapeRenderer.setColor(1, 0, 0, 1); //red
            shapeRenderer.rect(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());
        }
        //Draw player
        shapeRenderer.setColor(0, 1, 0, 1); // green
        shapeRenderer.rect(user.getX(), user.getY(), user.getWidth(), user.getHeight());

        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
