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

    // -------------------- World/camera --------------------
    private OrthographicCamera camera;
    private Viewport viewport;     // world viewport (camera follows player)

    // UI viewport (fixed virtual UI space; used to unproject mouse for UI hit-tests)
    private OrthographicCamera uiCamera;
    private Viewport uiViewport;

    /**
     * Baseline layout size that the game/UI was authored against.
     *
     * We keep the authored height (480) so the classic 20x15 tile room (32px tiles) still fits
     * vertically without forcing a re-authoring of room templates.
     *
     * To render the game in a true 16:9 aspect ratio, we expand the virtual width to 16:9 while
     * preserving pixel-art friendliness via FitViewport letterboxing.
     */
    private static final float DESIGN_WIDTH = 640f;
    private static final float DESIGN_HEIGHT = 480f;

    private static final float VIRTUAL_HEIGHT = DESIGN_HEIGHT;
    private static final float VIRTUAL_WIDTH = DESIGN_HEIGHT * (16f / 9f);

    /** Extra horizontal space introduced by moving from 4:3 (640x480) -> 16:9 (~853.33x480). */
    private static final float SIDE_PAD_X = (VIRTUAL_WIDTH - DESIGN_WIDTH) * 0.5f;

    // -------------------- Rendering --------------------
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    // -------------------- Title / UI textures --------------------
    private Texture titleScreenBackgroundTex;
    private Texture uiBanners;
    private Texture generalAssets;

    private TextureRegion uiBannerRegion;
    private TextureRegion flagBannerRegion;

    // -------------------- World textures (tiles) --------------------
    private Texture cemeteryTiles;
    private Texture cemeteryFloor;
    private Texture dungeonTileSheet;

    // -------------------- Weapon textures --------------------
    private Texture swordSheet;
    private TextureRegion broadswordRegion;
    private ArrayList<TextureRegion> swordSwing;
    private Texture swordSwingSheet;

    // Tiles + rooms
    private final ArrayList<TextureRegion> dungeonTiles = new ArrayList<>();
    private ArrayList<Room> rooms = new ArrayList<>();

    // NEW: WFC world (grid of rooms)
    private static final int WORLD_W = 10;
    private static final int WORLD_H = 10;

    // Chests (dungeonTileSheet sprite index 93)
    private static final int CHEST_TILE_INDEX = 93;
    private static final float CHEST_CHANCE_BASE = 0.78f;
    private static final float CHEST_CHANCE_PER_DIST = 0.03f;   // farther from start => more chests
    private static final float CHEST_SECOND_CHANCE = 0.06f;
    private static final float CHEST_ITEM_CHANCE = 1f;       // chance chest contains an item instead of souls
    private Room[][] worldRooms;          // [y][x]
    private int[][] chosenTemplates;      // [y][x]
    private int worldCellX = 5;
    private int worldCellY = 5;

    // Weapon objects
    private Weapon broadsword;

    // Game objects
    private Room room;
    private Player player;
    private GameWorld world;
    private UserInterface UI;

    // Dungeon map state (for minimap and full map)
    private DungeonMap dungeonMap;

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
    private float camZoom = 1f;  // < 1 = zoom IN, > 1 = zoom OUT
    private float followLerp = 12f; // higher = snappier follow

    //Game state information
    private boolean[][] cleared = new boolean[WORLD_H][WORLD_W];

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

        // -------------------- UI CAMERA/VIEWPORT --------------------
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        uiViewport.apply(true);
        uiCamera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0f);
        uiCamera.update();

        // Load textures
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
        // Use a half-texel inset when creating regions from a spritesheet.
        // This prevents thin “seams”/gaps that can show up at certain fullscreen resolutions
        // when the viewport scale is not a perfect integer.
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                dungeonTiles.add(Utility.regionNoBleed(dungeonTileSheet, x * 16, y * 16, 16, 16));
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
        marketButton = new Button((int)(205f + SIDE_PAD_X), 20, 80, 80, null, marketButtonTextures);
        settingsCogButton = new Button((int)(45f + SIDE_PAD_X), 20, 80, 80, null, settingsCogTextures);
        playButton = new Button((int)(45f + SIDE_PAD_X), 360, 240, 80, "PLAY", basicButtonTextures);
        Collections.addAll(titleScreenButtons, marketButton, settingsCogButton, playButton);

        // Weapon object
        broadsword = new Weapon("Iron Broadsword", 0.2f, 64, 64, 1, broadswordRegion, swordSwing);

        // Rooms
        InitializeRooms createTool = new InitializeRooms(dungeonTiles);
        rooms = createTool.getRooms();
    }

    private void startNewRun() {
        // Reset shake
        shakeTime = 0f;

        // Clean up previous run
        if (player != null) { player.dispose(); player = null; }
        if (UI != null) { UI.dispose(); UI = null; }
        if (world != null) { world.dispose(); world = null; }

        // Reset run-specific map state
        for (int y = 0; y < WORLD_H; y++) {
            for (int x = 0; x < WORLD_W; x++) {
                cleared[y][x] = false;
            }
        }
        dungeonMap = null;

        // -------------------- WFC GENERATION --------------------
        // Build templates from your room library (you said all combos exist except ffff)
        RoomLibrary lib = new RoomLibrary(rooms);
        MapCreator gen = new MapCreator(lib.getTemplates());

        chosenTemplates = gen.generate();

        worldRooms = new Room[WORLD_H][WORLD_W];
        Random rng = new Random();

        for (int y = 0; y < WORLD_H; y++) {
            for (int x = 0; x < WORLD_W; x++) {
                int tid = chosenTemplates[y][x];
                worldRooms[y][x] = lib.pickRoomForTemplate(tid, rng);
                generateChestsForRoom(worldRooms[y][x], rng, x, y);

            }
        }

        // Start in the center cell
        worldCellX = WORLD_W / 2;
        worldCellY = WORLD_H / 2;

        room = worldRooms[worldCellY][worldCellX];
        if (room == null) room = rooms.get(0); // ultra-safe fallback

        // Dungeon map (discovery state for minimap/map overlay)
        dungeonMap = new DungeonMap(worldRooms, cleared, worldCellX, worldCellY);
        dungeonMap.discover(worldCellX, worldCellY);

        // Keep viewport references in sync for any project/unproject or UI conversions
        room.setViewport(viewport);

        player = new Player(0, 0, 170, 32, 32); // temp
        Vector2 spawn = findSafeSpawn(room, player.getWidth(), player.getHeight());
        player.setX(spawn.x);
        player.setY(spawn.y);

        // Build world
        world = new GameWorld(room, player, spriteBatch);
        world.setAudio(audio);
        world.setWeapon(broadsword);
        world.setScreenShake(this::addShake);
        world.setDoorListener(this::handleDoorUsed);

        //This marks the cell as cleared when a wave ends
        world.setRoomClearListener(() -> {
            cleared[worldCellY][worldCellX] = true;
            if (dungeonMap != null) dungeonMap.setCleared(worldCellX, worldCellY, true);
        });


        // Authoritative instances
        player = world.getPlayer();
        room = world.getRoom();

        UI = new UserInterface(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, world, shapeRenderer, new ArrayList<>(), spriteBatch, viewport);

        // Critical: UI viewport for fullscreen/letterbox mouse mapping
        UI.setUiViewport(uiViewport);

        // Provide dungeon map for minimap/map overlay
        UI.setDungeonMap(dungeonMap);
    }

    @Override
    public void resize(int width, int height) {
        // Let FitViewport compute letterboxing properly (fixes fullscreen mouse mapping)
        viewport.update(width, height, true);
        uiViewport.update(width, height, true);

        if (room != null) room.setViewport(viewport);
        if (UI != null) UI.setUiViewport(uiViewport);

        // Provide dungeon map for minimap/map overlay
        if (UI != null) UI.setDungeonMap(dungeonMap);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        float delta = Gdx.graphics.getDeltaTime();

        // Apply world viewport before any world unproject calls
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

            drawTextureCover(spriteBatch, titleScreenBackgroundTex, 0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            spriteBatch.draw(uiBannerRegion, 320f + SIDE_PAD_X, 20f, 256f, 420f);
            spriteBatch.draw(uiBannerRegion, 45f + SIDE_PAD_X, 105f, 240f, 230f);
            spriteBatch.draw(flagBannerRegion, 65f + SIDE_PAD_X, 300f, 200f, 60f);
            spriteBatch.draw(flagBannerRegion, 340f + SIDE_PAD_X, 400f, 220f, 60f);

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

            font.draw(spriteBatch, "STATS", 115f + SIDE_PAD_X, 338f, 100f, Align.center, true);
            font.draw(spriteBatch, "CLASS", 395f + SIDE_PAD_X, 438f, 100f, Align.center, true);

            spriteBatch.end();
            return;
        }

        // -------------------- Gameplay --------------------
        if (world != null) {
            Player p = world.getPlayer();
            Room r = world.getRoom();

            // Camera follow + zoom
            camera.zoom = camZoom;

            if (p != null && r != null) {
                float px = p.getX() + p.getWidth() * 0.5f;
                float py = p.getY() + p.getHeight() * 0.5f;

                float roomW = r.getRoomWidth() * r.getTileSize();
                float roomH = r.getRoomHeight() * r.getTileSize();

                float halfW = (VIRTUAL_WIDTH * camZoom) * 0.5f;
                float halfH = (VIRTUAL_HEIGHT * camZoom) * 0.5f;

                float targetX = (halfW >= roomW * 0.5f)
                        ? roomW * 0.5f
                        : clampf(px, halfW, Math.max(halfW, roomW - halfW));

                float targetY = (halfH >= roomH * 0.5f)
                        ? roomH * 0.5f
                        : clampf(py, halfH, Math.max(halfH, roomH - halfH));

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
            // Main already set projection to camera.combined; UI swaps internally to screen space when needed.
            UI.drawQueue();
        }
    }

    /**
     * Draws a texture so it *covers* the target rectangle while preserving the texture's aspect
     * ratio (no stretching). Any excess is cropped.
     */
    private static void drawTextureCover(SpriteBatch batch, Texture tex, float x, float y, float targetW, float targetH) {
        if (batch == null || tex == null) return;
        float tw = tex.getWidth();
        float th = tex.getHeight();
        if (tw <= 0f || th <= 0f) return;

        float scale = Math.max(targetW / tw, targetH / th);
        float dw = tw * scale;
        float dh = th * scale;

        float dx = x + (targetW - dw) * 0.5f;
        float dy = y + (targetH - dh) * 0.5f;

        batch.draw(tex, dx, dy, dw, dh);
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

        // Snap camera to the underlying *screen pixel* grid (prevents tile seams/gaps).
        //
        // IMPORTANT: In fullscreen, FitViewport often has to round the internal viewport pixel size to ints,
        // which can make the world-to-screen mapping slightly "off" even if the camera position itself is
        // rounded. The most robust fix is to snap the *viewport's left/bottom edges* (in world units) to the
        // pixel grid, then reconstruct the camera center.
        float wuppX = (viewport.getScreenWidth() == 0) ? 1f : (viewport.getWorldWidth() / (float) viewport.getScreenWidth());
        float wuppY = (viewport.getScreenHeight() == 0) ? 1f : (viewport.getWorldHeight() / (float) viewport.getScreenHeight());

        float desiredCamX = baseCamX + sx;
        float desiredCamY = baseCamY + sy;

        float halfW = viewport.getWorldWidth() * 0.5f;
        float halfH = viewport.getWorldHeight() * 0.5f;

        float left = desiredCamX - halfW;
        float bottom = desiredCamY - halfH;

        // Snap edges to pixel grid.
        left = Math.round(left / wuppX) * wuppX;
        bottom = Math.round(bottom / wuppY) * wuppY;

        float camX = left + halfW;
        float camY = bottom + halfH;

        camera.position.set(camX, camY, 0f);
        camera.update();
    }

    private static float clampf(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }


    private void generateChestsForRoom(Room r, Random rng, int cellX, int cellY) {
        if (r == null) return;

        // Ensure unique chest state per room instance
        r.clearChests();

        // Seed local RNG for determinism within this run regardless of generation order
        long seed = rng.nextLong() ^ (cellX * 73856093L) ^ (cellY * 19349663L);
        Random local = new Random(seed);

        // Chance scales slightly with distance from the start cell
        int startX = WORLD_W / 2;
        int startY = WORLD_H / 2;
        int dist = Math.abs(cellX - startX) + Math.abs(cellY - startY);

        float p = CHEST_CHANCE_BASE + dist * CHEST_CHANCE_PER_DIST;
        if (p > 0.55f) p = 0.55f;

        int count = 0;
        if (local.nextFloat() < p) count++;
        if (local.nextFloat() < CHEST_SECOND_CHANCE) count++;

        if (count <= 0) return;

        int[][] col = r.getCollisions();
        if (col == null) return;

        int roomW = r.getRoomWidth();
        int roomH = r.getRoomHeight();
        int ts = r.getTileSize();

        // Candidate tiles (avoid edges/doors; collisions are stored TOP-DOWN)
        ArrayList<int[]> candidates = new ArrayList<>();
        for (int srcY = 0; srcY < roomH; srcY++) {
            int tyWorld = (roomH - 1) - srcY;
            if (tyWorld < 2 || tyWorld > roomH - 3) continue;
            for (int tx = 0; tx < roomW; tx++) {
                if (tx < 2 || tx > roomW - 3) continue;

                // Solid wall uses tile id 76 in your collision layer
                if (col[srcY][tx] == 76) continue;

                candidates.add(new int[]{tx, tyWorld});
            }
        }

        if (candidates.isEmpty()) return;

        // Place chests on random floor tiles
        for (int i = 0; i < count; i++) {
            int[] pick = candidates.get(local.nextInt(candidates.size()));

            int tx = pick[0];
            int ty = pick[1];

            // Reward: more souls farther out
            int reward = 2 + local.nextInt(4) + Math.max(0, dist / 2);

            // Some chests contain items.
            ItemId item = null;
            int soulReward = reward;
            if (local.nextFloat() < CHEST_ITEM_CHANCE) {
                item = ItemRegistry.rollRandom(local);
                soulReward = 0;
            }

            r.addChest(new Chest(tx * ts, ty * ts, soulReward, item));
        }
    }

private void handleDoorUsed(Dir dir) {
        if (worldRooms == null || world == null) return;

        int nx = worldCellX + dir.dx;
        int ny = worldCellY + dir.dy;

        // bounds
        if (nx < 0 || nx >= WORLD_W || ny < 0 || ny >= WORLD_H) return;

        Room next = worldRooms[ny][nx];
        if (next == null) return;

        // Optional safety: ensure doors match (they should from WFC)
        RoomTemplate curT = room != null ? room.getTemplate() : null;
        RoomTemplate nextT = next.getTemplate();
        if (curT == null || nextT == null) return;

        if (dir == Dir.UP && (curT.up != nextT.down)) return;
        if (dir == Dir.DOWN && (curT.down != nextT.up)) return;
        if (dir == Dir.LEFT && (curT.left != nextT.right)) return;
        if (dir == Dir.RIGHT && (curT.right != nextT.left)) return;

        // Switch cell
        worldCellX = nx;
        worldCellY = ny;

        // Update dungeon map discovery
        if (dungeonMap != null) {
            dungeonMap.setCurrent(worldCellX, worldCellY);
            dungeonMap.discover(worldCellX, worldCellY);
        }

        // Activate room
        room = next;
        room.setViewport(viewport);

        // Tell GameWorld to use it (this should rebuild doorways in GameWorld, clear per-room effects, etc.)
        world.setRoom(room);

        // Warp player to the actual doorway tile in the new room (opposite side)
        warpPlayerToEntranceUsingDoorGrid(dir);
        boolean start = !cleared[worldCellY][worldCellX];
        world.onEnterRoom(start);

        // Reset camera base so it doesn’t lerp across rooms
        if (player != null) {
            baseCamX = player.getX() + player.getWidth() * 0.5f;
            baseCamY = player.getY() + player.getHeight() * 0.5f;
        }
    }

    private void warpPlayerToEntranceUsingDoorGrid(Dir usedDir) {
        if (player == null || room == null) return;

        Dir enterSide = usedDir.opposite();

        // Prefer matching along the axis so transitions feel “continuous”
        float playerCenterX = player.getX() + player.getWidth() * 0.5f;
        float playerCenterY = player.getY() + player.getHeight() * 0.5f;

        Doorway target;
        if (enterSide == Dir.UP || enterSide == Dir.DOWN) {
            // choose door whose tileX best matches player's current x position
            float preferTileX = playerCenterX / room.getTileSize();
            target = findBestDoorway(room, enterSide, preferTileX);
        } else {
            // choose door whose tileY best matches player's current y position
            float preferTileY = playerCenterY / room.getTileSize();
            target = findBestDoorway(room, enterSide, preferTileY);
        }

        if (target == null) {
            // No door found on that side (shouldn't happen if WFC is correct)
            // Fallback: put player near center
            float ts = room.getTileSize();
            float mapW = room.getRoomWidth() * ts;
            float mapH = room.getRoomHeight() * ts;
            player.setX(mapW * 0.5f - player.getWidth() * 0.5f);
            player.setY(mapH * 0.5f - player.getHeight() * 0.5f);
            return;
        }

        float ts = room.getTileSize();
        float mapW = room.getRoomWidth() * ts;
        float mapH = room.getRoomHeight() * ts;

        // Base position: center player on the door tile
        float newX = target.getTileX() * ts + (ts - player.getWidth()) * 0.5f;
        float newY = target.getTileY() * ts + (ts - player.getHeight()) * 0.5f;

        // Push player *into* the room so they don't sit inside the doorway tile/wall edge
        float inset = ts * 1.1f;
        switch (enterSide) {
            case UP:    newY -= inset; break; // door at top -> move down
            case DOWN:  newY += inset; break; // door at bottom -> move up
            case LEFT:  newX += inset; break; // door at left -> move right
            case RIGHT: newX -= inset; break; // door at right -> move left
        }

        // Clamp inside room bounds
        float pad = 2f;
        newX = clampf(newX, pad, mapW - player.getWidth() - pad);
        newY = clampf(newY, pad, mapH - player.getHeight() - pad);

        player.setX(newX);
        player.setY(newY);
    }

    private Doorway findBestDoorway(Room r, Dir side, float preferAxisTile) {
        if (r == null) return null;

        int w = r.getRoomWidth();
        int h = r.getRoomHeight();

        Doorway best = null;
        float bestDist = Float.MAX_VALUE;

        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (!isDoorSafe(r, tx, ty)) continue;
                if (!tileBelongsToSide(tx, ty, w, h, side)) continue;

                float axis = (side == Dir.UP || side == Dir.DOWN) ? tx : ty;
                float dist = Math.abs(axis - preferAxisTile);

                if (dist < bestDist) {
                    bestDist = dist;
                    best = new Doorway(side, tx, ty);
                }
            }
        }

        return best;
    }

    private boolean isDoorSafe(Room r, int tx, int tyWorld) {
        try {
            int h = r.getRoomHeight();
            int tyData = (h - 1) - tyWorld;
            return r.isDoor(tx, tyData);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Doors might be on the edge OR 1 tile in from edge, depending on your room art.
     * This matches the “near edge” logic.
     */
    private boolean tileBelongsToSide(int tx, int ty, int roomW, int roomH, Dir side) {
        switch (side) {
            case UP:    return ty >= roomH - 2;
            case DOWN:  return ty <= 1;
            case LEFT:  return tx <= 1;
            case RIGHT: return tx >= roomW - 2;
        }
        return false;
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

    // Match your collision rule
    private static final int COLLISION_SOLID = 76;

    private Vector2 findSafeSpawn(Room room, float pw, float ph) {
        if (room == null) return new Vector2(100, 100);

        final int ts = room.getTileSize();
        final int tilesW = room.getRoomWidth();
        final int tilesH = room.getRoomHeight();
        final float roomW = tilesW * ts;
        final float roomH = tilesH * ts;

        // 1) Try room center first
        Vector2 center = new Vector2(roomW * 0.5f - pw * 0.5f, roomH * 0.5f - ph * 0.5f);
        if (isSpawnRectOpen(room, center.x, center.y, pw, ph)) return center;

        // 2) Random attempts (fast)
        Random rng = new Random();
        for (int i = 0; i < 600; i++) {
            float x = rng.nextFloat() * (roomW - pw);
            float y = rng.nextFloat() * (roomH - ph);
            if (isSpawnRectOpen(room, x, y, pw, ph)) return new Vector2(x, y);
        }

        // 3) Deterministic scan (reliable)
        for (int ty = 1; ty < tilesH - 1; ty++) {
            for (int tx = 1; tx < tilesW - 1; tx++) {
                float x = tx * ts + (ts - pw) * 0.5f;
                float y = ty * ts + (ts - ph) * 0.5f;
                if (isSpawnRectOpen(room, x, y, pw, ph)) return new Vector2(x, y);
            }
        }

        // Worst-case fallback
        return new Vector2(ts, ts);
    }

    private boolean isSpawnRectOpen(Room room, float x, float y, float w, float h) {
        // Pad slightly so we don’t spawn hugging a wall
        float pad = 2f;
        float rx = x + pad;
        float ry = y + pad;
        float rw = Math.max(1f, w - pad * 2f);
        float rh = Math.max(1f, h - pad * 2f);

        // 1) Must not hit collision
        if (rectHitsCollision(room, rx, ry, rw, rh)) return false;

        // 2) Optional: don’t spawn on a door tile (if your Room exposes door grid)
        // If you don’t have isDoor/getDoorsGrid accessible here, you can delete this block.
        if (rectOverlapsDoorTile(room, rx, ry, rw, rh)) return false;

        return true;
    }

    private boolean rectHitsCollision(Room room, float x, float y, float w, float h) {
        int[][] col;
        try { col = room.getCollisions(); }
        catch (Throwable t) { return false; }

        if (col == null) return false;

        final int ts = room.getTileSize();
        final int roomW = room.getRoomWidth();
        final int roomH = room.getRoomHeight();

        int left   = clampi((int)Math.floor(x / ts), 0, roomW - 1);
        int right  = clampi((int)Math.floor((x + w - 1f) / ts), 0, roomW - 1);
        int bottom = clampi((int)Math.floor(y / ts), 0, roomH - 1);
        int top    = clampi((int)Math.floor((y + h - 1f) / ts), 0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            int cy = (roomH - 1) - ty; // IMPORTANT: y-flip
            for (int tx = left; tx <= right; tx++) {
                if (col[cy][tx] == COLLISION_SOLID) return true;
            }
        }
        return false;
    }

    private boolean rectOverlapsDoorTile(Room room, float x, float y, float w, float h) {
        final int ts = room.getTileSize();
        final int roomW = room.getRoomWidth();
        final int roomH = room.getRoomHeight();

        int left   = clampi((int)Math.floor(x / ts), 0, roomW - 1);
        int right  = clampi((int)Math.floor((x + w - 1f) / ts), 0, roomW - 1);
        int bottom = clampi((int)Math.floor(y / ts), 0, roomH - 1);
        int top    = clampi((int)Math.floor((y + h - 1f) / ts), 0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            int srcY = (roomH - 1) - ty; // match the door grid convention you used in drawing
            for (int tx = left; tx <= right; tx++) {
                try {
                    if (room.isDoor(tx, srcY)) return true;
                } catch (Throwable ignored) {
                    // If room.isDoor isn't available, just skip door checks.
                    return false;
                }
            }
        }
        return false;
    }

    private static int clampi(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public void drawTitleScreen() {
        titleScreen = true;
    }
}


