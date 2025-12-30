package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Collections;

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

    // Title / UI textures
    private Texture titleScreenBackgroundTex;
    private Texture uiBanners;
    private Texture generalAssets;

    private TextureRegion uiBannerRegion;
    private TextureRegion flagBannerRegion;

    // World textures (tiles)
    private Texture cemeteryTiles;
    private Texture cemeteryFloor;

    // Game objects
    private Room room;
    private Player player;
    private GameWorld world;
    private UserInterface UI;

    // Title screen buttons
    private Button playButton;
    private Button settingsCogButton;
    private Button marketButton;
    private ArrayList<Button> titleScreenButtons;

    private boolean titleScreen = true;

    @Override
    public void create() {
        // Rendering tools
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        // Font
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        font.setColor(1f, 1f, 1f, 1f);

        // Camera + viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        camera.update();

        shapeRenderer.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);

        // Load textures (once)
        titleScreenBackgroundTex = new Texture("title_screen.png");
        generalAssets = new Texture("general_assets.png");
        uiBanners = new Texture("bannerSpritesheet.png");

        cemeteryTiles = new Texture("cemeteryTiles.png");
        cemeteryFloor = new Texture("cemeteryFloor.png");

        // Extract UI regions
        uiBannerRegion = new TextureRegion(uiBanners, 16, 16, 192, 275);
        flagBannerRegion = new TextureRegion(generalAssets, 20, 292, 111, 32);

        // Button textures
        ArrayList<TextureRegion> marketButtonTextures = new ArrayList<>();
        ArrayList<TextureRegion> basicButtonTextures = new ArrayList<>();
        ArrayList<TextureRegion> settingsCogTextures = new ArrayList<>();
        titleScreenButtons = new ArrayList<>();

        Collections.addAll(basicButtonTextures,
            new TextureRegion(uiBanners, 816, 16, 64, 26),
            new TextureRegion(uiBanners, 736, 16, 64, 26),
            new TextureRegion(uiBanners, 896, 16, 64, 26));

        Collections.addAll(settingsCogTextures,
            new TextureRegion(generalAssets, 84, 372, 32, 32),
            new TextureRegion(generalAssets, 84, 404, 32, 32),
            new TextureRegion(generalAssets, 84, 404, 32, 32));

        Collections.addAll(marketButtonTextures,
            new TextureRegion(generalAssets, 116, 372, 32, 32),
            new TextureRegion(generalAssets, 116, 404, 32, 32),
            new TextureRegion(generalAssets, 116, 404, 32, 32));

        // Create title screen buttons
        marketButton = new Button(205, 20, 80, 80, null, marketButtonTextures);
        settingsCogButton = new Button(45, 20, 80, 80, null, settingsCogTextures);
        playButton = new Button(45, 360, 240, 80, "PLAY", basicButtonTextures);

        Collections.addAll(titleScreenButtons, marketButton, settingsCogButton, playButton);

        // NOTE: We intentionally do NOT create the world here.
        // We create it when the player clicks PLAY via startNewRun().
    }

    /** Build a fresh tileset list using the already-loaded textures. */
    private ArrayList<TextureRegion> makeCemeteryTileset() {
        ArrayList<TextureRegion> cemeteryTileset = new ArrayList<>();
        Collections.addAll(cemeteryTileset,
            new TextureRegion(cemeteryFloor, 80, 0, 15, 15),
            new TextureRegion(cemeteryTiles, 33, 0, 32, 32)
        );
        return cemeteryTileset;
    }

    /** Creates a fresh run (room/player/world/UI) safely. */
    private void startNewRun() {
        // If you ever call this more than once, dispose the old player/UI to avoid leaks.
        if (player != null) {
            player.dispose();
            player = null;
        }
        if (UI != null) {
            UI.dispose();
            UI = null;
        }

        // Create game assets
        room = new Room(32, 20, 15, starterRoom, makeCemeteryTileset());
        player = new Player(100, 100, 200, 32, 32);
        world = new GameWorld(room, player);

        // UI renders directly from world; entities list is not needed (pass empty list)
        UI = new UserInterface(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, world, shapeRenderer, new ArrayList<>(), spriteBatch);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // Keep matrices synced (especially useful if you add camera movement later)
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);

        if (titleScreen) {
            spriteBatch.begin();

            spriteBatch.draw(titleScreenBackgroundTex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            spriteBatch.draw(uiBannerRegion, 320, 20, 256, 420);
            spriteBatch.draw(uiBannerRegion, 45, 105, 240, 230);
            spriteBatch.draw(flagBannerRegion, 65, 300, 200, 60);
            spriteBatch.draw(flagBannerRegion, 340, 400, 220, 60);

            for (Button b : titleScreenButtons) {
                if (b.isHovered(viewport)) b.setCurrTextureIndex(1);
                else b.setCurrTextureIndex(0);

                b.drawButton(spriteBatch);
            }

            if (playButton.isClicked(viewport)) {
                playButton.setCurrTextureIndex(2);

                // ✅ Create the world only when starting the run
                startNewRun();
                titleScreen = false;
            }

            font.draw(spriteBatch, "STATS", 115, 338, 100, Align.center, true);
            font.draw(spriteBatch, "CLASS", 395, 438, 100, Align.center, true);

            spriteBatch.end();
        } else {
            float delta = Gdx.graphics.getDeltaTime();

            // Safety: if something went wrong and world/UI not created, don't crash
            if (world != null) world.update(delta);
            if (UI != null) UI.drawQueue();
        }
    }

    @Override
    public void dispose() {
        // Dispose game objects
        if (UI != null) UI.dispose();
        if (player != null) player.dispose();

        // Dispose renderers
        shapeRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();

        // Dispose textures loaded here
        uiBanners.dispose();
        titleScreenBackgroundTex.dispose();
        generalAssets.dispose();

        cemeteryTiles.dispose();
        cemeteryFloor.dispose();
    }

    /** If you ever add a "back to title screen" button, do NOT dispose player here. */
    public void drawTitleScreen() {
        titleScreen = true;
        // ✅ Do NOT dispose player here — disposing assets mid-run causes random crashes.
    }
}
