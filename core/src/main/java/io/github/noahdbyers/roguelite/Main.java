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
        {2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3},
        {2,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,2}
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

    //Weapon Textures
    private Texture iceWeaponSheet;
    private TextureRegion damageBook;

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

    //Creating the weapon objects
    private Weapon magicBookWeapon;

    //Creating the audio manager
    private AudioManager audio;


    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        font = new BitmapFont();
        font.getData().setScale(1.2f);
        font.setColor(1f, 1f, 1f, 1f);

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply(true);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        camera.update();

        // Load textures (once)
        titleScreenBackgroundTex = new Texture("ui/title_screen.png");
        generalAssets = new Texture("ui/general_assets.png");
        uiBanners = new Texture("ui/bannerSpritesheet.png");
        iceWeaponSheet = new Texture("weapons/iceWeapons.png");

        cemeteryTiles = new Texture("cemetery/cemeteryTiles.png");
        cemeteryFloor = new Texture("cemetery/cemeteryFloor.png");

        //Load audio
        audio = new AudioManager();
        audio.load();
        audio.startMainMusic(); // start title music (or keep it off until play)

        // Pixel art settings (helps, but seams are mostly from fractional scaling)
        cemeteryTiles.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        cemeteryFloor.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        cemeteryTiles.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        cemeteryFloor.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

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

        //Weapon Textures
        damageBook = new TextureRegion(iceWeaponSheet, 146, 80, 12, 15);

        //Create weapons
        magicBookWeapon = new Weapon("Magic Book", 0.25f, damageBook);

        // Create title screen buttons
        marketButton = new Button(205, 20, 80, 80, null, marketButtonTextures);
        settingsCogButton = new Button(45, 20, 80, 80, null, settingsCogTextures);
        playButton = new Button(45, 360, 240, 80, "PLAY", basicButtonTextures);

        Collections.addAll(titleScreenButtons, marketButton, settingsCogButton, playButton);

        // Do NOT create the world here — only when starting run
    }

    /** Build a fresh tileset list using already-loaded textures. */
    private ArrayList<TextureRegion> makeCemeteryTileset() {
        ArrayList<TextureRegion> cemeteryTileset = new ArrayList<>();
        Collections.addAll(cemeteryTileset,
            new TextureRegion(cemeteryFloor, 80, 0, 15, 15),
            new TextureRegion(cemeteryTiles, 33, 0, 32, 32),
            new TextureRegion(cemeteryTiles, 32, 32, 32, 32),
            new TextureRegion(cemeteryTiles, 0, 32, 32, 32),
            new TextureRegion(cemeteryTiles, 64, 32, 32, 32),
            new TextureRegion(cemeteryTiles, 32, 64, 32, 32)
        );
        return cemeteryTileset;
    }

    /** Creates a fresh run (room/player/world/UI) safely. */
    private void startNewRun() {
        if (player != null) {
            player.dispose();
            player = null;
        }
        if (UI != null) {
            UI.dispose();
            UI = null;
        }
        if (world != null) {
            world.dispose();
            world = null;
        }

        room = new Room(32, 20, 15, starterRoom, makeCemeteryTileset());
        room.setViewport(viewport);
        player = new Player(100, 100, 170, 32, 32);
        world = new GameWorld(room, player, spriteBatch);
        world.setAudio(audio);
        world.setWeapon(magicBookWeapon);


        UI = new UserInterface(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, world, shapeRenderer, new ArrayList<>(), spriteBatch);
    }

    /**
     * ✅ Integer-scaled viewport on resize (prevents tile seams when resizing).
     * Keeps the game scaled by 1x/2x/3x... of the virtual resolution.
     */
    @Override
    public void resize(int width, int height) {
        int scale = Math.max(1, Math.min(
            width / (int) VIRTUAL_WIDTH,
            height / (int) VIRTUAL_HEIGHT
        ));

        int vpW = (int) VIRTUAL_WIDTH * scale;
        int vpH = (int) VIRTUAL_HEIGHT * scale;

        int vpX = (width - vpW) / 2;
        int vpY = (height - vpH) / 2;

        viewport.setScreenBounds(vpX, vpY, vpW, vpH);
        viewport.apply(true);

        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0f);
        camera.update();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // ✅ Always re-apply viewport and matrices (especially after resize)
        viewport.apply();

        // ✅ Snap camera to whole pixels (avoids subpixel seams/jitter)
        camera.position.set(Math.round(camera.position.x), Math.round(camera.position.y), 0f);
        camera.update();

        spriteBatch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

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
                audio.playUIClick();
                audio.stopMainMusic();
                startNewRun();
                titleScreen = false;
                audio.startGameMusic();
            }

            font.draw(spriteBatch, "STATS", 115, 338, 100, Align.center, true);
            font.draw(spriteBatch, "CLASS", 395, 438, 100, Align.center, true);

            spriteBatch.end();
        } else {
            float delta = Gdx.graphics.getDeltaTime();
            if (world != null) {
                // Convert mouse screen -> world using the SAME viewport you render with
                com.badlogic.gdx.math.Vector2 mouse = new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY());
                viewport.unproject(mouse);
                world.setAimWorld(mouse.x, mouse.y);
                world.update(delta);
            }
            if (UI != null) UI.drawQueue();
        }
    }

    @Override
    public void dispose() {
        if (UI != null) UI.dispose();
        if (player != null) player.dispose();
        if (world != null) world.dispose();

        shapeRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();

        uiBanners.dispose();
        titleScreenBackgroundTex.dispose();
        generalAssets.dispose();

        cemeteryTiles.dispose();
        cemeteryFloor.dispose();

        if (audio != null) audio.dispose();
    }

    public void drawTitleScreen() {
        titleScreen = true;
        // Do NOT dispose player/world/UI here; do it when starting a new run or in dispose().
    }
}
