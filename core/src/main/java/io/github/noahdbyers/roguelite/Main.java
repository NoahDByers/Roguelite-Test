package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Main extends ApplicationAdapter {

    private OrthographicCamera camera;
    private Viewport viewport;     // world viewport (camera follows player)

    // NEW: UI viewport (fixed virtual UI space; used to unproject mouse for UI hit-tests)
    private OrthographicCamera uiCamera;
    private Viewport uiViewport;

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
    private Texture dungeonTileSheet;

    // Weapon textures
    private Texture swordSheet;
    private TextureRegion broadswordRegion;
    private ArrayList<TextureRegion> swordSwing;
    private Texture swordSwingSheet;

    private final ArrayList<TextureRegion> dungeonTiles = new ArrayList<>();
    private ArrayList<Room> rooms = new ArrayList<>();

    // Weapon objects
    private Weapon broadsword;

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

    // Audio manager
    private AudioManager audio;

    // Mouse in world coords
    private final Vector2 mouseWorld = new Vector2();

    // -------------------- Screen shake --------------------
    private final Random shakeRng = new Random();
    private float shakeTime = 0f;
    private float shakeDuration = 0.12f;
    private float shakeIntensity = 6f;

    // Camera follow base (shake offsets are added on top)
    private float baseCamX = VIRTUAL_WIDTH / 2f;
    private float baseCamY = VIRTUAL_HEIGHT / 2f;

    // Camera follow tuning
    private float camZoom = 0.85f;  // < 1 = zoom IN, > 1 = zoom OUT
    private float followLerp = 12f; // higher = snappier follow

    /** Call this from GameWorld (through a callback) to trigger screen shake. */
    public void addShake(float intensity, float duration) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeDuration = Math.max(shakeDuration, duration);
        shakeTime = shakeDuration;
    }

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        font = new BitmapFont();
        font.getData().setScale(1.2f);
        font.setColor(1f, 1f, 1f, 1f);

        // -------------------- WORLD CAMERA/VIEWPORT --------------------
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply(true);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0f);
        camera.update();

        // -------------------- UI CAMERA/VIEWPORT (FIX FOR FULLSCREEN UI HITTEST) --------------------
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        uiViewport.apply(true);
        uiCamera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0f);
        uiCamera.update();

        // Load textures (once)
        titleScreenBackgroundTex = Utility.loadNearest("ui/title_screen.png");
        generalAssets = Utility.loadNearest("ui/general_assets.png");
        uiBanners = Utility.loadNearest("ui/bannerSpritesheet.png");

        swordSheet = Utility.loadNearest("weapons/File.png");
        broadswordRegion = new TextureRegion(swordSheet, 0, 192, 64, 64);

        swordSwingSheet = Utility.loadNearest("weapons/swordSwing.png");
        swordSwing = new ArrayList<>();
        Collections.addAll(swordSwing,
            new TextureRegion(swordSwingSheet, 0, 0, 32, 32),
            new TextureRegion(swordSwingSheet, 32, 0, 32, 32),
            new TextureRegion(swordSwingSheet, 64, 0, 32, 32),
            new TextureRegion(swordSwingSheet, 96, 0, 32, 32),
            new TextureRegion(swordSwingSheet, 128, 0, 32, 32)
        );

        cemeteryTiles = Utility.loadNearest("cemetery/cemeteryTiles.png");
        cemeteryFloor = Utility.loadNearest("cemetery/cemeteryFloor.png");
        dungeonTileSheet = Utility.loadNearest("tilesets/dungeonTileset.png");

        // Audio
        audio = new AudioManager();
        audio.load();
        audio.startMainMusic();

        // Extract UI regions
        uiBannerRegion = new TextureRegion(uiBanners, 16, 16, 192, 275);
        flagBannerRegion = new TextureRegion(generalAssets, 20, 292, 111, 32);

        // Build dungeon tiles (10x10, 16px)
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                dungeonTiles.add(new TextureRegion(dungeonTileSheet, x * 16, y * 16, 16, 16));
            }
        }

        // Button textures
        ArrayList<TextureRegion> marketButtonTextures = new ArrayList<>();
        ArrayList<TextureRegion> basicButtonTextures = new ArrayList<>();
        ArrayList<TextureRegion> settingsCogTextures = new ArrayList<>();
        titleScreenButtons = new ArrayList<>();

        Collections.addAll(basicButtonTextures,
            new TextureRegion(uiBanners, 816, 16, 64, 26),
            new TextureRegion(uiBanners, 736, 16, 64, 26),
            new TextureRegion(uiBanners, 896, 16, 64, 26)
        );

        Collections.addAll(settingsCogTextures,
            new TextureRegion(generalAssets, 84, 372, 32, 32),
            new TextureRegion(generalAssets, 84, 404, 32, 32),
            new TextureRegion(generalAssets, 84, 404, 32, 32)
        );

        Collections.addAll(marketButtonTextures,
            new TextureRegion(generalAssets, 116, 372, 32, 32),
            new TextureRegion(generalAssets, 116, 404, 32, 32),
            new TextureRegion(generalAssets, 116, 404, 32, 32)
        );

        // Title screen buttons
        marketButton = new Button(205, 20, 80, 80, null, marketButtonTextures);
        settingsCogButton = new Button(45, 20, 80, 80, null, settingsCogTextures);
        playButton = new Button(45, 360, 240, 80, "PLAY", basicButtonTextures);
        Collections.addAll(titleScreenButtons, marketButton, settingsCogButton, playButton);

        // Weapon object
        broadsword = new Weapon("Iron Broadsword", 0f, 64, 64, 1, broadswordRegion, swordSwing);

        // Rooms
        InitializeRooms createTool = new InitializeRooms(dungeonTiles);
        rooms = createTool.getRooms();
    }

    private void startNewRun() {
        shakeTime = 0f;

        if (player != null) { player.dispose(); player = null; }
        if (UI != null) { UI.dispose(); UI = null; }
        if (world != null) { world.dispose(); world = null; }

        room = rooms.get(0);
        room.setViewport(viewport);

        player = new Player(100, 100, 170, 32, 32);

        world = new GameWorld(room, player, spriteBatch);
        world.setAudio(audio);
        world.setWeapon(broadsword);
        world.setScreenShake(this::addShake);

        // Use authoritative instances
        player = world.getPlayer();
        room = world.getRoom();

        UI = new UserInterface(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, world, shapeRenderer, new ArrayList<>(), spriteBatch, viewport);

        // ✅ Critical: give UI its own viewport so mouse hit-tests stay correct in fullscreen/letterbox
        UI.setUiViewport(uiViewport);
    }

    @Override
    public void resize(int width, int height) {
        // ✅ Let FitViewport compute letterboxing properly (fixes fullscreen mouse mapping)
        viewport.update(width, height, true);
        uiViewport.update(width, height, true);

        // If your Room/UI cache viewport bounds anywhere, keep it in sync
        if (room != null) room.setViewport(viewport);
        if (UI != null) UI.setUiViewport(uiViewport);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        float delta = Gdx.graphics.getDeltaTime();

        // Always apply world viewport before unprojecting world mouse coords
        viewport.apply();

        // -------------------- Title Screen --------------------
        if (titleScreen) {
            baseCamX = VIRTUAL_WIDTH / 2f;
            baseCamY = VIRTUAL_HEIGHT / 2f;
            camera.zoom = 1f;

            updateCameraWithShake(delta);

            spriteBatch.setProjectionMatrix(camera.combined);
            shapeRenderer.setProjectionMatrix(camera.combined);

            // Mouse world for buttons (title screen uses world viewport)
            mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(mouseWorld);

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
            return;
        }

        // -------------------- Gameplay --------------------
        if (world != null) {
            Player p = world.getPlayer();
            Room r = world.getRoom();

            // ---- CAMERA FOLLOW + CLAMP (WITH ZOOM) ----
            camera.zoom = camZoom;

            if (p != null && r != null) {
                float px = p.getX() + p.getWidth() * 0.5f;
                float py = p.getY() + p.getHeight() * 0.5f;

                float roomW = r.getRoomWidth() * r.getTileSize();
                float roomH = r.getRoomHeight() * r.getTileSize();

                float halfW = (VIRTUAL_WIDTH * camZoom) * 0.5f;
                float halfH = (VIRTUAL_HEIGHT * camZoom) * 0.5f;

                float targetX = clampf(px, halfW, Math.max(halfW, roomW - halfW));
                float targetY = clampf(py, halfH, Math.max(halfH, roomH - halfH));

                // Smooth follow
                float a = 1f - (float) Math.exp(-followLerp * delta);
                baseCamX += (targetX - baseCamX) * a;
                baseCamY += (targetY - baseCamY) * a;
            } else {
                baseCamX = VIRTUAL_WIDTH / 2f;
                baseCamY = VIRTUAL_HEIGHT / 2f;
            }

            // Apply shake + camera.update()
            updateCameraWithShake(delta);

            spriteBatch.setProjectionMatrix(camera.combined);
            shapeRenderer.setProjectionMatrix(camera.combined);

            // Mouse world AFTER camera update
            mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(mouseWorld);
            world.setAimWorld(mouseWorld.x, mouseWorld.y);

            world.update(delta);
        }

        if (UI != null) {
            // Main has already set spriteBatch/shaperenderer projection to camera.combined
            // and applied the world viewport. UI will swap to screen/UI space internally.
            UI.drawQueue();
        }
    }

    private void updateCameraWithShake(float delta) {
        float sx = 0f;
        float sy = 0f;

        if (shakeTime > 0f) {
            shakeTime -= delta;
            if (shakeTime < 0f) shakeTime = 0f;

            float t = (shakeDuration <= 0f) ? 0f : (shakeTime / shakeDuration); // 1 -> 0
            float strength = shakeIntensity * t * t; // ease out

            sx = (shakeRng.nextFloat() * 2f - 1f) * strength;
            sy = (shakeRng.nextFloat() * 2f - 1f) * strength;
        }

        // Snap to whole pixels for cleaner pixel-art rendering
        float camX = Math.round(baseCamX + sx);
        float camY = Math.round(baseCamY + sy);

        camera.position.set(camX, camY, 0f);
        camera.update();
    }

    private static float clampf(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public void dispose() {
        if (UI != null) UI.dispose();
        if (world != null) world.dispose();
        if (player != null) player.dispose();

        if (shapeRenderer != null) shapeRenderer.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (font != null) font.dispose();

        if (uiBanners != null) uiBanners.dispose();
        if (titleScreenBackgroundTex != null) titleScreenBackgroundTex.dispose();
        if (generalAssets != null) generalAssets.dispose();

        if (swordSheet != null) swordSheet.dispose();
        if (swordSwingSheet != null) swordSwingSheet.dispose();

        if (cemeteryTiles != null) cemeteryTiles.dispose();
        if (cemeteryFloor != null) cemeteryFloor.dispose();
        if (dungeonTileSheet != null) dungeonTileSheet.dispose();

        Zombie.disposeShared();

        if (audio != null) audio.dispose();
    }

    public void drawTitleScreen() {
        titleScreen = true;
    }
}
