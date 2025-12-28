package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;

public class Main extends ApplicationAdapter {
    private final int[][] starterRoom = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };
    private OrthographicCamera camera;
    private Viewport viewport;
    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private Texture upgradeCardTex;
    private Texture titleScreenBackgroundTex;
    private GameWorld world;
    private boolean titleScreen = false;
    private Texture uiBanners;
    private TextureRegion uiBannerRegion;
    private Player player;
    private UserInterface UI;
    private ArrayList<Entity> entities;
    private Button playButton;

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        camera.update();
        font.setColor(1f, 1f, 1f, 1f);

        shapeRenderer.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);
        upgradeCardTex = new Texture("upgrade_card.png");
        titleScreenBackgroundTex = new Texture("background.png");

        uiBanners = new Texture("bannerSpritesheet.png");
        uiBannerRegion = new TextureRegion(uiBanners, 16, 16, 192, 275);
        Room room = new Room(32, 20, 15, starterRoom);
        ArrayList<TextureRegion> basicButtonTextures = new ArrayList<TextureRegion>();
        basicButtonTextures.add(new TextureRegion(uiBanners, 736, 16, 64, 26));
        basicButtonTextures.add(new TextureRegion(uiBanners, 816, 16, 64, 26));
        basicButtonTextures.add(new TextureRegion(uiBanners, 896, 16, 64, 26));

        playButton = new Button(45, 360, 240, 80, "PLAY", basicButtonTextures);
        player = new Player(100, 100, 200, 32, 32);
        world = new GameWorld(room, player);
        entities = new ArrayList<Entity>();
        entities.add(player);
        entities.addAll(world.getEnemies());
        entities.addAll(world.getBullets());

        UI = new UserInterface(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, world, shapeRenderer, entities, spriteBatch);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        if (titleScreen) {
            spriteBatch.begin();
            spriteBatch.draw(titleScreenBackgroundTex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            spriteBatch.draw(uiBannerRegion, 320, 120, 256, 320);
            playButton.drawButton(spriteBatch);
            spriteBatch.end();
        }
        else {
            float delta = Gdx.graphics.getDeltaTime();
            world.update(delta);
        }

        UI.drawQueue();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();
        upgradeCardTex.dispose();
        uiBanners.dispose();
        titleScreenBackgroundTex.dispose();
    }
    public void drawTitleScreen() {
        titleScreen = true;
    }

}
