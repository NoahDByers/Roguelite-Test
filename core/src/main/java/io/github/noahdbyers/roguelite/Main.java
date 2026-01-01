package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
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
import java.util.Random;

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
    private float gameplayZoom = 1.0f;   // < 1 zooms IN, > 1 zooms OUT

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
    private ArrayList<TextureRegion> dungeonTiles = new ArrayList<>();
    ArrayList<Room> rooms = new ArrayList<>();

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

    // If you want camera follow, we use these as base and then add shake.
    private float baseCamX = VIRTUAL_WIDTH / 2f;
    private float baseCamY = VIRTUAL_HEIGHT / 2f;

    // Camera follow / zoom
    private float camZoom = 0.75f;      // < 1 = zoom IN, > 1 = zoom OUT
    private float zoomSpeed = 0.15f;    // optional zoom adjustment speed
    private float followLerp = 12f;     // higher = snappier follow

    // (optional) smooth follow target
    private float camTargetX = VIRTUAL_WIDTH / 2f;
    private float camTargetY = VIRTUAL_HEIGHT / 2f;


    /** Call this from GameWorld (through a callback) to trigger screen shake. */
    public void addShake(float intensity, float duration) {
        // Keep the strongest/longest if multiple hits happen close together
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

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply(true);

        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        camera.update();

        // Load textures (once)
        titleScreenBackgroundTex = new Texture("ui/title_screen.png");
        generalAssets = new Texture("ui/general_assets.png");
        uiBanners = new Texture("ui/bannerSpritesheet.png");

        swordSheet = new Texture("weapons/File.png");
        broadswordRegion = new TextureRegion(swordSheet, 0, 192, 64, 64);

        swordSwingSheet = new Texture("weapons/swordSwing.png");
        swordSwing = new ArrayList<>();
        Collections.addAll(swordSwing,
            new TextureRegion(swordSwingSheet, 0, 0, 32, 32),
            new TextureRegion(swordSwingSheet, 32, 0, 32, 32),
            new TextureRegion(swordSwingSheet, 64, 0, 32, 32),
            new TextureRegion(swordSwingSheet, 96, 0, 32, 32),
            new TextureRegion(swordSwingSheet, 128, 0, 32, 32)
        );

        cemeteryTiles = new Texture("cemetery/cemeteryTiles.png");
        cemeteryFloor = new Texture("cemetery/cemeteryFloor.png");
        dungeonTileSheet = new Texture("tilesets/dungeonTileset.png");

        // Audio
        audio = new AudioManager();
        audio.load();
        audio.startMainMusic();

        // Pixel art settings
        cemeteryTiles.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        cemeteryFloor.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        cemeteryTiles.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        cemeteryFloor.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        // Extract UI regions
        uiBannerRegion = new TextureRegion(uiBanners, 16, 16, 192, 275);
        flagBannerRegion = new TextureRegion(generalAssets, 20, 292, 111, 32);

        for(int y = 0; y < 10; y++) {
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
        broadsword = new Weapon("Iron Broadsword",
            0.5f, 64, 64, 1, broadswordRegion, swordSwing);

        // Do NOT create the world here — only when starting run
        InitializeRooms createTool = new InitializeRooms(dungeonTiles);
        rooms = createTool.getRooms();
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
    // Call this every frame BEFORE world.update(...)
    private void updateAimWorld(GameWorld world) {
        Room room = world.getRoom();
        if (room == null) return;

        float ts = room.getTileSize();
        float mapW = room.getRoomWidth() * ts;
        float mapH = room.getRoomHeight() * ts;

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        float scale = Math.min(screenW / mapW, screenH / mapH);
        float offsetX = (screenW - mapW * scale) * 0.5f;
        float offsetY = (screenH - mapH * scale) * 0.5f;

        // Mouse in screen pixels (origin top-left), convert to bottom-left
        float sx = Gdx.input.getX();
        float sy = screenH - Gdx.input.getY();

        // Convert to world coords (inverse of translate+scale used for rendering)
        float wx = (sx - offsetX) / scale;
        float wy = (sy - offsetY) / scale;

        // Optional: clamp to map bounds so it doesn't go negative/outside
        wx = Math.max(0f, Math.min(mapW, wx));
        wy = Math.max(0f, Math.min(mapH, wy));

        world.setAimWorld(wx, wy);
    }

    private void startNewRun() {
        // Reset shake
        shakeTime = 0f;

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

        room = rooms.get(0);
        room.setViewport(viewport);

        player = new Player(100, 100, 170, 32, 32);

        world = new GameWorld(room, player, spriteBatch);
        world.setAudio(audio);
        world.setWeapon(broadsword);
        world.setScreenShake(this::addShake);

        player = world.getPlayer();
        room   = world.getRoom();
        // IMPORTANT: GameWorld must implement setScreenShake(...) for this to compile.
        // Example signature:
        // public interface ScreenShake { void addShake(float intensity, float duration); }
        // public void setScreenShake(ScreenShake s) { this.shake = s; }
        world.setScreenShake(this::addShake);

        UI = new UserInterface(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, world, shapeRenderer, new ArrayList<>(), spriteBatch, viewport);
    }

    /**
     * Integer-scaled viewport on resize (prevents tile seams when resizing).
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

        float delta = Gdx.graphics.getDeltaTime();

        // Apply viewport BEFORE any unproject calls
        viewport.apply();

        // -------------------- Title Screen --------------------
        if (titleScreen) {
            baseCamX = VIRTUAL_WIDTH / 2f;
            baseCamY = VIRTUAL_HEIGHT / 2f;

            // Keep title zoom stable for UI consistency
            camera.zoom = 1f;

            // Apply shake (if any) and update camera
            updateCameraWithShake(delta);

            spriteBatch.setProjectionMatrix(camera.combined);
            shapeRenderer.setProjectionMatrix(camera.combined);

            // Mouse world for buttons (title screen uses same viewport/camera)
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

            // Always use the authoritative instances from the world
            Player p = world.getPlayer();
            Room r = world.getRoom();

            // ---- 1) CAMERA FOLLOW + CLAMP (WITH ZOOM) ----
            float camZoom = 0.75f;     // <1 zoom in, >1 zoom out
            camera.zoom = camZoom;

            if (p != null && r != null) {
                float px = p.getX() + p.getWidth() * 0.5f;
                float py = p.getY() + p.getHeight() * 0.5f;

                float roomW = r.getRoomWidth() * r.getTileSize();
                float roomH = r.getRoomHeight() * r.getTileSize();

                // visible world size depends on zoom
                float halfW = (VIRTUAL_WIDTH * camZoom) * 0.5f;
                float halfH = (VIRTUAL_HEIGHT * camZoom) * 0.5f;

                baseCamX = clampf(px, halfW, Math.max(halfW, roomW - halfW));
                baseCamY = clampf(py, halfH, Math.max(halfH, roomH - halfH));
            } else {
                baseCamX = VIRTUAL_WIDTH / 2f;
                baseCamY = VIRTUAL_HEIGHT / 2f;
            }

            // Apply shake + snap + camera.update()
            updateCameraWithShake(delta);

            // IMPORTANT: set projection after camera update
            spriteBatch.setProjectionMatrix(camera.combined);
            shapeRenderer.setProjectionMatrix(camera.combined);

            // ---- 2) Mouse world AFTER camera update ----
            mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(mouseWorld);

            // Aim uses world coords
            world.setAimWorld(mouseWorld.x, mouseWorld.y);

            world.update(delta);
        }

        if (UI != null) {
            UI.drawQueue();
        }
    }


    private void updateCameraWithShake(float delta) {
        float sx = 0f;
        float sy = 0f;

        if (shakeTime > 0f) {
            shakeTime -= delta;
            if (shakeTime < 0f) shakeTime = 0f;

            float t = (shakeDuration <= 0f) ? 0f : (shakeTime / shakeDuration);
            float strength = shakeIntensity * t * t;

            sx = (shakeRng.nextFloat() * 2f - 1f) * strength;
            sy = (shakeRng.nextFloat() * 2f - 1f) * strength;
        }

        float camX = Math.round(baseCamX + sx);
        float camY = Math.round(baseCamY + sy);

        camera.position.set(camX, camY, 0f);
        camera.update();
    }

    private void updateCameraFollow(float delta) {
        if (player == null || room == null) {
            baseCamX = VIRTUAL_WIDTH / 2f;
            baseCamY = VIRTUAL_HEIGHT / 2f;
            return;
        }

        // Zoom
        camera.zoom = gameplayZoom;

        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;

        float roomW = room.getRoomWidth()  * room.getTileSize();
        float roomH = room.getRoomHeight() * room.getTileSize();

        // IMPORTANT: clamp using the *camera view size*, which changes with zoom
        float halfW = (viewport.getWorldWidth()  * 0.5f) * camera.zoom;
        float halfH = (viewport.getWorldHeight() * 0.5f) * camera.zoom;

        float targetX = clampf(px, halfW, Math.max(halfW, roomW - halfW));
        float targetY = clampf(py, halfH, Math.max(halfH, roomH - halfH));

        // Smooth follow (optional). If you want instant follow, just set baseCamX/Y = targetX/Y
        float a = 1f - (float)Math.exp(-followLerp * delta);
        baseCamX += (targetX - baseCamX) * a;
        baseCamY += (targetY - baseCamY) * a;
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

        Zombie.disposeShared();

        if (audio != null) audio.dispose();
    }

    public void drawTitleScreen() {
        titleScreen = true;
    }
}
