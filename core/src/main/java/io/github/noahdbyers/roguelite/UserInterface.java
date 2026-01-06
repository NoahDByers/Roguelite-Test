package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

public class UserInterface {
    // World viewport (the one used for camera/gameplay rendering)
    private final Viewport viewport;

    // UI viewport (FitViewport / ScreenViewport used for HUD & menus)
    private Viewport uiViewport;
    private final Vector2 uiMouse = new Vector2();

    // Virtual UI size
    private final float width;
    private final float height;

    // When running 16:9, we keep the original 640-wide HUD layout and center it.
    private final float uiOffsetX;

    private final GameWorld world;
    private final ShapeRenderer shapeRenderer; // kept for compatibility (you removed bullets)
    private final SpriteBatch spriteBatch;
    private final ArrayList<Entity> entities; // kept for compatibility; not used here

    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    // Fallback card art
    private final Texture fallbackCardTexture = Utility.loadNearest("ui/upgrade_card.png");

    // 1x1 white pixel for overlays
    private final Texture whitePixel;

// ----------------------------
// Lighting (screen-space overlay over the world)
// ----------------------------
private final Texture radialLightTex;   // white radial gradient (alpha falloff)
private final Texture vignetteTex;      // black vignette (alpha towards edges)
private boolean lightingEnabled = true;

    // Dungeon map (minimap + full-screen overlay)
    private DungeonMap dungeonMap;
    private boolean mapOpen = false;

// Tune these to taste (values assume the game's 16:9 virtual size ~853x480).
private static final float AMBIENT_DARKNESS_ALPHA = 0.48f; // overall darkness level
private static final float VIGNETTE_ALPHA = 0.70f;         // extra edge darkening

private static final float PLAYER_LIGHT_RADIUS = 240f;
private static final float PLAYER_LIGHT_INTENSITY = 0.04f;

private static final float SHRINE_LIGHT_RADIUS = 260f;
private static final float SHRINE_LIGHT_INTENSITY = 0.05f;

private final Vector2 tmpUiPos2 = new Vector2();

    // ----------------------------
    // Upgrade input delay (prevents accidental selection)
    // ----------------------------
    private static final float UPGRADE_INPUT_DELAY = 0.25f; // seconds
    private float upgradeInputTimer = 0f;
    private boolean prevChoosingUpgrade = false;

    // Visual highlight only
    private int selectedUpgradeIndex = -1;

    // UI textures
    private final Texture uiBarBg = Utility.loadNearest("ui/BarIcon.png");
    private final Texture uiBarManaBg = Utility.loadNearest("ui/ManaBarIcon.png");
    private final Texture uiBarHealthFill = Utility.loadNearest("ui/FullHPBar.png");
    private final Texture uiBarManaFill = Utility.loadNearest("ui/FullManaBar.png");

    private final Texture iconHeart = Utility.loadNearest("ui/HeartIcon.png");
    private final Texture iconMana = Utility.loadNearest("ui/ManaIcon.png");
    private final Texture iconCoin = Utility.loadNearest("ui/CoinIcon.png");
    private final Texture iconSoul = Utility.loadNearest("ui/SoulIcon.png");
    private final Texture iconKill = Utility.loadNearest("ui/SkullIcon.png");
    private final Texture iconWave = Utility.loadNearest("ui/ClearIcon.png");

    // HUD colors (avoid per-frame allocations)
    private static final Color HUD_HP_COLOR = new Color(0.75f, 0.15f, 0.15f, 1f);
    private static final Color HUD_MANA_COLOR = new Color(0.20f, 0.45f, 0.90f, 1f);

    // ----------------------------
    // NEW: Drop + Shrine visuals (WORLD SPACE)
    // ----------------------------
    // If you have a dedicated drop sprite, swap this path.
    private final TextureRegion dropTextureRegion = new TextureRegion(iconSoul);
    private final Texture itemSheet = new Texture("items/itemSprites.png");
    private ArrayList<TextureRegion> itemSprites = new ArrayList<>();

    // If you already have a shrine texture/region somewhere else, swap this.
    private final Texture shrineSheet = new Texture("ui/shrineSheet.png");
    private final TextureRegion shrineTextureRegion = new TextureRegion(shrineSheet, 445, 21, 36, 71);

    // ----------------------------
    // Upgrade layout tuning
    // ----------------------------
    private static final int UPGRADE_COUNT = 3;

    private static final float CLUSTER_WIDTH_FRAC = 0.95f;
    private static final float STAR_HEIGHT_FRAC = 0.58f;
    private static final float STAR_MIN = 200f;
    private static final float STAR_MAX = 340f;

    private static final float GAP_MIN = -70f;
    private static final float GAP_MAX = 24f;

    private static final float SELECT_LIFT = 14f;
    private static final float PULSE_SPEED = 8f;
    private static final float PULSE_AMT = 0.04f;

    // ----------------------------
    // Keyboard glyphs (interaction prompts)
    // ----------------------------
    private final Texture keyboardSheet = Utility.loadNearest("ui/keyboard.png");
    private static final int KEY_TILE = 16; // keyboard sheet is 256x256, 16px tiles
    // "E" key is at (col=5,row=2) on the provided keyboard sheet
    private final TextureRegion keyE = new TextureRegion(keyboardSheet, 5 * KEY_TILE, 2 * KEY_TILE, KEY_TILE, KEY_TILE);


    // ----------------------------
    // Upgrade Animation State
    // ----------------------------
    private static final float CARD_FRAME_TIME = 0.03f;

    private final float[] cardAnimTimer = new float[UPGRADE_COUNT];
    private final int[] cardFrameIndex = new int[UPGRADE_COUNT];
    private final boolean[] cardAnimFinished = new boolean[UPGRADE_COUNT];

    private boolean wasChoosingUpgrade = false;
    private int lastOfferedSignature = 0;

    // Reused vectors
    private final Vector2 tmpMouseWorld = new Vector2();
    private final Vector3 tmpV3 = new Vector3();

    private float uiTime = 0f;

    // ----------------------------
    // Screen-space projection for HUD/menus
    // ----------------------------
    private final Matrix4 screenProjection = new Matrix4();
    private final Matrix4 identityTransform = new Matrix4();
    private final Matrix4 savedWorldProjection = new Matrix4();

    // in PlayerCombat fields
    private float lockedAimX, lockedAimY;
    private float lockedDirX = 1f, lockedDirY = 0f;

    // Matches GameWorld's door-interact scan behavior
    private static final int UI_DOOR_SCAN_RADIUS = 2;
    private static final int UI_DOOR_EDGE_BAND = 2;
    private static final float UI_DOOR_INTERACT_PAD = 10f;


    public UserInterface(float width, float height,
                         GameWorld world,
                         ShapeRenderer shapeRenderer,
                         ArrayList<Entity> entities,
                         SpriteBatch spriteBatch,
                         Viewport viewport) {
        this.width = width;
        this.height = height;

        // Center HUD elements that were authored for a 640-wide layout.
        this.uiOffsetX = Math.max(0f, (width - 640f) * 0.5f);
        this.world = world;
        this.shapeRenderer = shapeRenderer;
        this.entities = entities;
        this.spriteBatch = spriteBatch;
        this.viewport = viewport;

        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        whitePixel = new Texture(pm);
        pm.dispose();

// Lighting textures (procedurally generated so no assets needed).
radialLightTex = buildRadialLightTexture(256);
radialLightTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

vignetteTex = buildVignetteTexture(512);
vignetteTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Screen-space: (0..width, 0..height) where width/height are VIRTUAL size
        screenProjection.setToOrtho2D(0f, 0f, width, height);
        identityTransform.idt();

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 5; x++) {
                itemSprites.add(new TextureRegion(itemSheet, x * 32, y * 32, 32, 32));
            }
        }
    }

    /** Provide the UI viewport from Main (the one updated in resize). */
    public void setUiViewport(Viewport vp) {
        this.uiViewport = vp;
    }

    /** Provide the dungeon map from Main (for minimap + full map). */
    public void setDungeonMap(DungeonMap map) {
        this.dungeonMap = map;
        this.mapOpen = false;
    }

    /** If Main expects UI.resize(...), keep this method. */
    public void resize(int screenW, int screenH) {
        // Virtual UI size is fixed; viewport itself is updated in Main.
        // This method exists mainly to satisfy older call sites.
        screenProjection.setToOrtho2D(0f, 0f, width, height);
    }

    /** Get mouse in UI coordinates (virtual coords), robust to fullscreen/letterboxing. */
    private void getMouseUi(Vector2 out) {
        out.set(Gdx.input.getX(), Gdx.input.getY());
        if (uiViewport != null) {
            uiViewport.unproject(out);
        } else {
            out.set(Gdx.input.getX(), height - Gdx.input.getY());
        }
    }

    /** Call once per frame AFTER Main has set spriteBatch projection to camera.combined. */
    public void drawQueue() {
        if (world == null) return;

        float delta = Gdx.graphics.getDeltaTime();
        uiTime += delta;

        // Toggle full map overlay
        if (!world.isChoosingUpgrade() && Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            mapOpen = !mapOpen;
        }

        // Detect entering upgrade state and start input delay
        boolean choosing = world.isChoosingUpgrade();
        if (choosing && !prevChoosingUpgrade) {
            upgradeInputTimer = UPGRADE_INPUT_DELAY;
        }
        prevChoosingUpgrade = choosing;

        // Count down
        if (upgradeInputTimer > 0f) {
            upgradeInputTimer -= delta;
            if (upgradeInputTimer < 0f) upgradeInputTimer = 0f;
        }

        // Save whatever Main set (should be camera.combined)
        savedWorldProjection.set(spriteBatch.getProjectionMatrix());

        // ----------------------------
        // 1) WORLD (SpriteBatch): tiles + shrine + drops + sprites + weapon
        // Must use CAMERA projection so the camera follows player.
        // ----------------------------
        spriteBatch.setProjectionMatrix(savedWorldProjection);
        spriteBatch.setTransformMatrix(identityTransform);

        spriteBatch.begin();

        drawWorldTilesSafe();

        // ✅ SHRINE (WORLD SPACE)
        drawShrineWorldSpace();

        // ✅ CHESTS (WORLD SPACE)
        drawChestsWorldSpace();

        // ✅ DROPS (WORLD SPACE)
        drawDropsWorldSpace();

        drawSpritesSafe(delta);

        Weapon w = world.getWeapon();
        Player p = world.getPlayer();
        if (w != null && p != null) {
            tmpMouseWorld.set(world.getAimWorldX(), world.getAimWorldY());
            w.draw(spriteBatch, delta, p);
        }


        // ✅ PARTICLES (WORLD SPACE)
        if (world.getParticles() != null) world.getParticles().draw(spriteBatch);

        spriteBatch.end();

        // ----------------------------
        // 2) UI (SpriteBatch): HUD + upgrade menu + game over
        // Switch to SCREEN SPACE so UI doesn’t move/zoom with the camera.
        // ----------------------------
        spriteBatch.setProjectionMatrix(screenProjection);
        spriteBatch.setTransformMatrix(identityTransform);

        spriteBatch.begin();

        if (lightingEnabled && !world.isChoosingUpgrade() && !mapOpen) {
            drawLightingOverlay();
        }

        if (!world.isChoosingUpgrade()) {
            if (mapOpen) {
                drawDimOverlay(0.65f);
                drawFullMapOverlay();
            } else {
                drawHud();

                drawChestPrompt();
                drawDamagePopupsScreenSpace();
                drawShrinePromptIfNear(); // optional UI prompt
                drawDoorPromptIfNear();
            }
        }

        if (world.isChoosingUpgrade()) {
            drawDimOverlay(0.55f);

            handleUpgradeAnimationResetIfNeeded();
            updateUpgradeCardAnimations(delta);
            updateSelectionHighlight();

            drawUpgradeScene(world.getOfferedUpgrades());
        } else {
            selectedUpgradeIndex = -1;
        }

        if (world.isGameOver()) {
            drawDimOverlay(0.55f);
            font.setColor(Color.WHITE);
            font.getData().setScale(1.2f);
            drawCenteredText("GAME OVER - Press R to Restart", width / 2f, height / 2f + 10);
            font.getData().setScale(1.0f);
        }

        spriteBatch.end();

        // IMPORTANT: restore world projection so other code doesn't get "stuck" in UI projection
        spriteBatch.setProjectionMatrix(savedWorldProjection);
        spriteBatch.setTransformMatrix(identityTransform);

        wasChoosingUpgrade = world.isChoosingUpgrade();
    }

    // ----------------------------
    // NEW: Shrine rendering (WORLD SPACE)
    // ----------------------------
    private void drawShrineWorldSpace() {
        // Assumes your GameWorld has: public Shrine getShrine()
        // And Shrine has fields x,y,w,h (or getters).
        Shrine s;
        try {
            s = world.getShrine();
        } catch (Throwable t) {
            return; // if your world doesn’t have shrine yet, don’t crash UI
        }

        if (s == null) return;

        float x = s.x;
        float y = s.y;
        float w = s.w;
        float h = s.h * 2;

        // If your shrine uses a fixed size, you can ignore w/h and set constants.
        spriteBatch.draw(shrineTextureRegion, x, y, w, h);
    }


    private void drawChestsWorldSpace() {
        Room r = world.getRoom();
        if (r == null) return;

        ArrayList<Chest> cs = r.getChests();
        if (cs == null || cs.isEmpty()) return;

        // dungeonTileSheet sprite #93 (0-based index in the 10x10 tiles list)
        TextureRegion chestRegion = r.getTextureRegion(83);
        if (chestRegion == null) return;

        for (Chest c : cs) {
            if (c == null || c.opened) continue;
            spriteBatch.draw(chestRegion, c.x, c.y, c.w, c.h);
        }
    }

        private void drawDropsWorldSpace() {
        if (world.getDrops() == null) return;

        for (Drop d : world.getDrops()) {
            if (d == null) continue;
            spriteBatch.draw(dropTextureRegion, d.x, d.y, d.w, d.h);
        }
    }



    // ----------------------------
    // NEW: Chest prompt (SCREEN SPACE)
    // ----------------------------
    private void drawChestPrompt() {
        Chest c;
        try {
            c = world.getNearestInteractableChest();
        } catch (Throwable t) {
            return; // if your GameWorld doesn't have chest helpers yet
        }
        if (c == null) return;
        if (viewport == null) return;

        // Project chest top into UI virtual coords
        float cx = c.x + c.w * 0.5f;
        float cy = c.y + c.h + 10f;

        tmpV3.set(cx, cy, 0f);
        viewport.project(tmpV3);

        float sxToUi = width / (float) viewport.getScreenWidth();
        float syToUi = height / (float) viewport.getScreenHeight();

        int vx = viewport.getScreenX();
        int vy = viewport.getScreenY();

        float uiX = (tmpV3.x - vx) * sxToUi;
        float uiY = (tmpV3.y - vy) * syToUi;

        String action = "Open Chest";

        font.setColor(Color.WHITE);
        font.getData().setScale(0.9f);
        drawKeyPrompt(keyE, action, uiX, uiY);
        font.getData().setScale(1.0f);
    }

    private void drawKeyPrompt(TextureRegion keyRegion, String actionText, float centerX, float centerY) {
        if (keyRegion == null) {
            // Fallback to text if glyph is missing
            drawCenteredText(actionText, centerX, centerY);
            return;
        }
        if (actionText == null) actionText = "";

        // Size in UI virtual units
        float iconSize = 18f;
        float gap = 6f;

        layout.setText(font, actionText, font.getColor(), 0, Align.left, false);
        float totalW = iconSize + gap + layout.width;

        float startX = centerX - totalW * 0.5f;
        float iconX = startX;
        float iconY = centerY - iconSize * 0.5f;

        spriteBatch.draw(keyRegion, iconX, iconY, iconSize, iconSize);

        float textX = iconX + iconSize + gap;
        float textY = centerY + layout.height * 0.5f;
        font.draw(spriteBatch, actionText, textX, textY);
    }

    private void drawShrinePromptIfNear() {
        // Optional: show “Press E” when player is near shrine.
        // Doesn’t require any extra methods; uses simple distance check.

        Player p = world.getPlayer();
        if (p == null) return;

        Shrine s;
        try {
            s = world.getShrine();
        } catch (Throwable t) {
            return;
        }
        if (s == null) return;

        float px = p.getX() + p.getWidth() * 0.5f;
        float py = p.getY() + p.getHeight() * 0.5f;

        float sx = s.x + s.w * 0.5f;
        float sy = s.y + s.h * 0.5f;

        float dx = sx - px;
        float dy = sy - py;
        float dist2 = dx * dx + dy * dy;

        float near = 60f; // tweak
        if (dist2 > near * near) return;

        // Draw prompt above shrine in UI space: WORLD -> SCREEN -> UI virtual
        if (viewport == null) return;

        tmpV3.set(sx, s.y + s.h + 10f, 0f);
        viewport.project(tmpV3);

        float sxToUi = width / (float) viewport.getScreenWidth();
        float syToUi = height / (float) viewport.getScreenHeight();

        int vx = viewport.getScreenX();
        int vy = viewport.getScreenY();

        float uiX = (tmpV3.x - vx) * sxToUi;
        float uiY = (tmpV3.y - vy) * syToUi + 30f;

        font.setColor(Color.WHITE);
        font.getData().setScale(0.9f);
        drawKeyPrompt(keyE, "Pray", uiX, uiY);
        font.getData().setScale(1.0f);
    }



// ----------------------------
// Lighting helpers (SCREEN SPACE)
// ----------------------------

/** Toggle the lighting overlay (useful if you add a settings menu later). */
public void setLightingEnabled(boolean enabled) {
    this.lightingEnabled = enabled;
}

/** Project a WORLD point into UI-virtual coords (0..width, 0..height). */
private boolean worldToUi(float worldX, float worldY, Vector2 out) {
    if (viewport == null) return false;

    tmpV3.set(worldX, worldY, 0f);
    viewport.project(tmpV3);

    float sxToUi = width / (float) viewport.getScreenWidth();
    float syToUi = height / (float) viewport.getScreenHeight();

    int vx = viewport.getScreenX();
    int vy = viewport.getScreenY();

    out.set((tmpV3.x - vx) * sxToUi,
            (tmpV3.y - vy) * syToUi);
    return true;
}

private void drawLight(float centerX, float centerY, float radius, float intensity,
                       float r, float g, float b) {
    if (radialLightTex == null) return;
    float d = radius * 2f;
    spriteBatch.setColor(r, g, b, clamp(intensity, 0f, 1f));
    spriteBatch.draw(radialLightTex, centerX - radius, centerY - radius, d, d);
}

/**
 * Draws:
 * 1) ambient darkness + vignette (normal alpha blending)
 * 2) player + shrine lights (additive blending)
 *
 * Must be called while the SpriteBatch is begun and using screenProjection.
 */
private void drawLightingOverlay() {
    if (spriteBatch == null || whitePixel == null) return;

    // Save current batch state
    Color c = spriteBatch.getColor();
    float sr = c.r, sg = c.g, sb = c.b, sa = c.a;

    // 1) Ambient darkness
    spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    spriteBatch.setColor(0f, 0f, 0f, AMBIENT_DARKNESS_ALPHA);
    spriteBatch.draw(whitePixel, 0f, 0f, width, height);

    // 2) Extra edge darkening (vignette)
    if (vignetteTex != null) {
        spriteBatch.setColor(0f, 0f, 0f, VIGNETTE_ALPHA);
        spriteBatch.draw(vignetteTex, 0f, 0f, width, height);
    }

    // 3) Lights (additive)
    spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

    // Player light (keeps nearby area clearly visible)
    Player p = world.getPlayer();
    if (p != null && worldToUi(p.getX() + p.getWidth() * 0.5f, p.getY() + p.getHeight() * 0.5f, tmpUiPos2)) {
        drawLight(tmpUiPos2.x, tmpUiPos2.y, PLAYER_LIGHT_RADIUS, PLAYER_LIGHT_INTENSITY,
                1f, 1f, 1f);
    }

    // Shrine light (warm glow)
    Shrine s = null;
    try {
        s = world.getShrine();
    } catch (Throwable ignored) { }
    if (s != null && worldToUi(s.x + s.w * 0.5f, s.y + s.h * 0.65f, tmpUiPos2)) {
        drawLight(tmpUiPos2.x, tmpUiPos2.y, SHRINE_LIGHT_RADIUS, SHRINE_LIGHT_INTENSITY,
                1.0f, 0.9f, 0.65f);
    }

    // Restore blend + color
    spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    spriteBatch.setColor(sr, sg, sb, sa);
}

private static Texture buildRadialLightTexture(int size) {
    int s = Math.max(8, size);
    Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);

    float cx = (s - 1) * 0.5f;
    float cy = (s - 1) * 0.5f;
    float max = Math.max(1f, cx);

    for (int y = 0; y < s; y++) {
        for (int x = 0; x < s; x++) {
            float dx = (x - cx);
            float dy = (y - cy);
            float d = (float) Math.sqrt(dx * dx + dy * dy) / max;
            float a = clamp(1f - d, 0f, 1f);
            a = a * a;
            pm.setColor(1f, 1f, 1f, a);
            pm.drawPixel(x, y);
        }
    }

    Texture t = new Texture(pm);
    pm.dispose();
    return t;
}

private static Texture buildVignetteTexture(int size) {
    int s = Math.max(8, size);
    Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);

    float cx = (s - 1) * 0.5f;
    float cy = (s - 1) * 0.5f;
    float max = (float) Math.sqrt(cx * cx + cy * cy);

    for (int y = 0; y < s; y++) {
        for (int x = 0; x < s; x++) {
            float dx = x - cx;
            float dy = y - cy;
            float d = (float) Math.sqrt(dx * dx + dy * dy) / max;
            float a = clamp(d, 0f, 1f);
            a = (float) Math.pow(a, 2.2);
            pm.setColor(0f, 0f, 0f, a);
            pm.drawPixel(x, y);
        }
    }

    Texture t = new Texture(pm);
    pm.dispose();
    return t;
}

    private void drawDimOverlay(float alpha) {
        Color c = spriteBatch.getColor();
        float r = c.r, g = c.g, b = c.b, a = c.a;

        spriteBatch.setColor(0f, 0f, 0f, alpha);
        spriteBatch.draw(whitePixel, 0f, 0f, width, height);

        spriteBatch.setColor(r, g, b, a);
    }

    // ----------------------------
    // Upgrade scene (SCREEN SPACE)
    // ----------------------------
    private void drawUpgradeScene(Upgrade[] offered) {
        font.setColor(Color.WHITE);
        font.getData().setScale(1.15f);
        drawCenteredText("Choose an Upgrade", width / 2f, height - 34f);
        font.getData().setScale(1.0f);

        float clusterW = width * CLUSTER_WIDTH_FRAC;
        float desiredStar = clamp(height * STAR_HEIGHT_FRAC, STAR_MIN, STAR_MAX);

        float rawGap = (clusterW - desiredStar * UPGRADE_COUNT) / (UPGRADE_COUNT - 1);
        float gap = clamp(rawGap, GAP_MIN, GAP_MAX);

        float maxStarThatFits = (clusterW - gap * (UPGRADE_COUNT - 1)) / UPGRADE_COUNT;
        float star = Math.min(desiredStar, maxStarThatFits);
        star = clamp(star, STAR_MIN, STAR_MAX);

        float totalW = star * UPGRADE_COUNT + gap * (UPGRADE_COUNT - 1);
        float startX = (width - totalW) / 2f;
        float baseY = height * 0.56f - star * 0.5f;

        // Mouse in UI coords via uiViewport (works in fullscreen/letterboxing)
        getMouseUi(uiMouse);
        float mx = uiMouse.x;
        float my = uiMouse.y;

        int hovered = -1;
        for (int i = 0; i < UPGRADE_COUNT; i++) {
            float x = startX + i * (star + gap);
            float y = baseY;
            if (mx >= x && mx <= x + star && my >= y && my <= y + star) {
                hovered = i;
                break;
            }
        }

        selectedUpgradeIndex = hovered;

        boolean canSelect = (upgradeInputTimer <= 0f);
        if (canSelect && hovered != -1 && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            world.chooseUpgrade(hovered);
            return;
        }

        for (int i = 0; i < UPGRADE_COUNT; i++) {
            float x = startX + i * (star + gap);
            float y = baseY;

            float pulse = 1f;
            if (i == selectedUpgradeIndex) {
                pulse = 1f + (float) Math.sin(uiTime * PULSE_SPEED) * PULSE_AMT;
                y += SELECT_LIFT;
            }

            float drawSize = star * pulse;
            float dx = x + (star - drawSize) * 0.5f;
            float dy = y + (star - drawSize) * 0.5f;

            Upgrade up = (offered != null && i < offered.length) ? offered[i] : null;

            TextureRegion frame = null;
            if (up != null) {
                try {
                    ArrayList<TextureRegion> anim = up.getAnimation();
                    if (anim != null && !anim.isEmpty()) {
                        int idx = Math.max(0, Math.min(cardFrameIndex[i], anim.size() - 1));
                        frame = anim.get(idx);
                    }
                } catch (Throwable ignored) {}
            }

            if (frame != null) {
                spriteBatch.draw(frame, dx, dy, drawSize, drawSize);
            } else {
                Texture tex = getCardTextureSafe(up);
                if (tex == null) tex = fallbackCardTexture;
                spriteBatch.draw(tex, dx, dy, drawSize, drawSize);
            }
        }

        if (hovered != -1 && offered != null && hovered < offered.length) {
            Upgrade up = offered[hovered];
            if (up != null) {
                float panelW = width * 0.62f;
                float panelH = 78f;
                float panelX = (width - panelW) * 0.5f;
                float panelY = height * 0.14f;

                Color c = spriteBatch.getColor();
                float r = c.r, g = c.g, b = c.b, a = c.a;

                spriteBatch.setColor(0f, 0f, 0f, 0.65f);
                spriteBatch.draw(whitePixel, panelX, panelY, panelW, panelH);

                spriteBatch.setColor(1f, 1f, 1f, 0.18f);
                spriteBatch.draw(whitePixel, panelX, panelY + panelH - 2f, panelW, 2f);
                spriteBatch.draw(whitePixel, panelX, panelY, panelW, 2f);
                spriteBatch.draw(whitePixel, panelX, panelY, 2f, panelH);
                spriteBatch.draw(whitePixel, panelX + panelW - 2f, panelY, 2f, panelH);

                spriteBatch.setColor(r, g, b, a);

                float textW = panelW - 24f;
                float cx = panelX + panelW * 0.5f;

                font.setColor(Color.WHITE);
                font.getData().setScale(1.05f);
                drawCenteredText(safe(up.name), cx, panelY + panelH - 22f);

                font.getData().setScale(0.85f);
                font.setColor(new Color(0.9f, 0.9f, 0.9f, 1f));
                layout.setText(font, safe(up.desc), font.getColor(), textW, Align.center, true);
                font.draw(spriteBatch, layout, panelX + (panelW - textW) * 0.5f, panelY + 30f);

                font.getData().setScale(1.0f);
                font.setColor(Color.WHITE);
            }
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ----------------------------
    // Upgrade animation helpers
    // ----------------------------
    private void handleUpgradeAnimationResetIfNeeded() {
        Upgrade[] offered = world.getOfferedUpgrades();
        int sig = computeOfferedSignature(offered);

        boolean entering = !wasChoosingUpgrade;
        boolean changed = (sig != lastOfferedSignature);

        if (entering || changed) {
            resetUpgradeCardAnimations();
            lastOfferedSignature = sig;
        }
    }

    private int computeOfferedSignature(Upgrade[] offered) {
        int h = 17;
        if (offered != null) {
            for (int i = 0; i < UPGRADE_COUNT; i++) {
                Upgrade u = (i < offered.length) ? offered[i] : null;
                h = 31 * h + (u == null ? 0 : safe(u.name).hashCode());
                h = 31 * h + (u == null ? 0 : safe(u.desc).hashCode());
            }
        }
        return h;
    }

    private void resetUpgradeCardAnimations() {
        for (int i = 0; i < UPGRADE_COUNT; i++) {
            cardAnimTimer[i] = 0f;
            cardFrameIndex[i] = 0;
            cardAnimFinished[i] = false;
        }
    }

    private void updateUpgradeCardAnimations(float delta) {
        Upgrade[] offered = world.getOfferedUpgrades();
        if (offered == null) return;

        for (int i = 0; i < UPGRADE_COUNT; i++) {
            if (cardAnimFinished[i]) continue;

            Upgrade up = (i < offered.length) ? offered[i] : null;
            if (up == null) {
                cardAnimFinished[i] = true;
                continue;
            }

            ArrayList<TextureRegion> anim = null;
            try { anim = up.getAnimation(); } catch (Throwable ignored) {}

            if (anim == null || anim.isEmpty()) {
                cardAnimFinished[i] = true;
                continue;
            }

            int lastFrame = anim.size() - 1;
            if (cardFrameIndex[i] >= lastFrame) {
                cardFrameIndex[i] = lastFrame;
                cardAnimFinished[i] = true;
                continue;
            }

            cardAnimTimer[i] += delta;
            while (cardAnimTimer[i] >= CARD_FRAME_TIME && !cardAnimFinished[i]) {
                cardAnimTimer[i] -= CARD_FRAME_TIME;
                cardFrameIndex[i]++;

                if (cardFrameIndex[i] >= lastFrame) {
                    cardFrameIndex[i] = lastFrame;
                    cardAnimFinished[i] = true;
                }
            }
        }
    }

    // ----------------------------
    // World drawing (tiles) — WORLD SPACE
    // ----------------------------
    private void drawWorldTilesSafe() {
        Room room = world.getRoom();
        if (room == null) return;

        float ts = room.getTileSize();
        int h = room.getRoomHeight();
        int w = room.getRoomWidth();

        for (int y = 0; y < h; y++) {
            int srcY = (h - 1) - y;

            for (int x = 0; x < w; x++) {
                int tileId = room.getDrawTileWithDoors(x, srcY);
                if (tileId <= 0) continue;

                TextureRegion region = room.getTextureRegion(tileId - 1);
                if (region != null) {
                    spriteBatch.draw(region, x * ts, y * ts, ts, ts);
                }

                if (room.isDoor(x, srcY)) {
                    int doorTileId = room.getDoorTextureID(x, srcY);
                    if (doorTileId <= 0) continue;

                    TextureRegion doorRegion = room.getTextureRegion(doorTileId - 1);
                    if (doorRegion != null) {
                        spriteBatch.draw(doorRegion, x * ts, y * ts, ts, ts);
                    }
                }
            }
        }
    }

    // ----------------------------
    // World drawing (sprites) — WORLD SPACE
    // ----------------------------
    private void drawSpritesSafe(float delta) {
        for (Enemy e : world.getEnemies()) {
            if (e == null) continue;
            if (e instanceof Zombie) ((Zombie) e).draw(spriteBatch, delta);
        }

        Player p = world.getPlayer();
        if (p != null) p.draw(spriteBatch, delta);
    }

    // ----------------------------
    // HUD — SCREEN SPACE
    // ----------------------------
    private void drawHud() {
        Player p = world.getPlayer();
        if (p == null) return;

        // -----------------------------------------------------------------
        // Reworked HUD layout (matches the user's sketch)
        //  - Top-left: HP and Mana bars
        //  - Top-right: Kills / Souls / Coins box
        //  - Bottom-center: Item sprite bar
        // -----------------------------------------------------------------

        float manaPct = (p.getMaxMana() <= 0) ? 0f : (p.getMana() / (float) p.getMaxMana());
        float hpPct = (p.getMaxHealth() <= 0) ? 0f : (p.getHealth() / (float) p.getMaxHealth());

        drawTopLeftBars(hpPct, manaPct);
        drawTopRightStats();
        drawBottomItemBar();

        drawItemToast();
    }

    // -----------------------------------------------------------------
    // NEW HUD LAYOUT HELPERS
    // -----------------------------------------------------------------

    private void drawTopLeftBars(float hpPct, float manaPct) {
        final float pad = 14f;
        final float barW = 240f;
        final float hpH = 16f;
        final float manaH = 12f;
        final float gap = 6f;

        float x = pad;
        float top = height - pad;

        // Health on top, mana below (as sketched).
        float hpY = top - hpH;
        float manaY = hpY - gap - manaH;

        drawTexturedBar(uiBarBg, uiBarHealthFill,
            x, hpY, barW, hpPct,
            8, 7, 5, 6,          // frame pads (L,R,B,T)
            2, 10, 7, 9          // fill trims (L,R,T,B) for FullHPBar.png
        );

        drawTexturedBar(uiBarManaBg, uiBarManaFill,
            x, manaY, barW, manaPct,
            15, 11, 4, 6,        // frame pads (L,R,B,T)
            2, 1, 1, 0           // fill trims (L,R,T,B) for FullManaBar.png
        );


        // Small icons to the left of each bar.
        float iconSize = 14f;

        // Mini-map under the bars (left side)
        float mapGap = 10f;
        float mapW = 150f;
        float mapH = 150f;
        float mapX = x;
        float mapY = manaY - mapGap - mapH;
        if (mapY < 8f) mapY = 8f;
        drawMiniMap(mapX, mapY, mapW, mapH);
    }


    // ----------------------------
    // Dungeon map rendering (screen space)
    // ----------------------------

    /**
     * Mini-map (HUD): shows the CURRENT ROOM (node) layout:
     *  - walls/blocked tiles
     *  - doors (open vs closed)
     *  - enemy positions
     *  - player position
     */
    private void drawMiniMap(float x, float y, float w, float h) {
        Room room = world.getRoom();
        if (room == null) return;

        drawPanel(x, y, w, h, 0.35f);

        float pad = 8f;
        float innerX = x + pad;
        float innerY = y + pad;
        float innerW = Math.max(1f, w - pad * 2f);
        float innerH = Math.max(1f, h - pad * 2f);

        drawRoomMiniMap(innerX, innerY, innerW, innerH, room);
    }

    private void drawRoomMiniMap(float x, float y, float w, float h, Room room) {
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();
        if (roomW <= 0 || roomH <= 0) return;

        int[][] col;
        try { col = room.getCollisions(); }
        catch (Throwable t) { col = null; }

        // Cell size in screen space.
        float cell = Math.min(w / (float) roomW, h / (float) roomH);
        if (cell <= 2f) return;

        float gridW = cell * roomW;
        float gridH = cell * roomH;
        float ox = x + (w - gridW) * 0.5f;
        float oy = y + (h - gridH) * 0.5f;

        // Background grid (very subtle) + walls/doors.
        for (int ty = 0; ty < roomH; ty++) {
            int cy = (roomH - 1) - ty; // match world-space flip (see GameWorld rectHitsCollision)
            for (int tx = 0; tx < roomW; tx++) {
                float px = ox + tx * cell;
                float py = oy + ty * cell;

                // Light floor tint so the room reads as a shape.
                drawRect(px, py, cell, cell, 1f, 1f, 1f, 0.03f);

                boolean solid = false;
                if (col != null && cy >= 0 && cy < col.length && tx >= 0 && tx < col[cy].length) {
                    solid = (col[cy][tx] == 76);
                }

                // Door slot? (room uses a door-slot grid named "doors")
                boolean doorSlot = false;
                try { doorSlot = room.isDoor(tx, cy); }
                catch (Throwable ignored) { }

                if (doorSlot) {
                    // Open door = not solid (collision mask makes inactive doors solid)
                    boolean open = !solid;
                    if (open) {
                        drawRect(px, py, cell, cell, 0.35f, 0.95f, 0.95f, 0.95f);
                    } else {
                        drawRect(px, py, cell, cell, 0.95f, 0.55f, 0.25f, 0.95f);
                    }
                } else if (solid) {
                    // Wall/blocked
                    drawRect(px, py, cell, cell, 0.10f, 0.10f, 0.12f, 0.90f);
                }
            }
        }

        // Entities (enemies + player) on top.
        float tileSize = room.getTileSize();
        float marker = Math.max(2f, cell * 0.35f);

        // Enemies
        for (Enemy e : world.getEnemies()) {
            if (e == null || e.isDead()) continue;
            float ex = e.getX() + e.getWidth() * 0.5f;
            float ey = e.getY() + e.getHeight() * 0.5f;
            int tx = (int)Math.floor(ex / tileSize);
            int ty = (int)Math.floor(ey / tileSize);
            if (tx < 0 || tx >= roomW || ty < 0 || ty >= roomH) continue;

            float px = ox + tx * cell + (cell - marker) * 0.5f;
            float py = oy + ty * cell + (cell - marker) * 0.5f;
            drawRect(px, py, marker, marker, 0.95f, 0.25f, 0.25f, 0.95f);
        }

        // Player
        Player p = world.getPlayer();
        if (p != null) {
            float pxw = p.getX() + p.getWidth() * 0.5f;
            float pyw = p.getY() + p.getHeight() * 0.5f;
            int tx = (int)Math.floor(pxw / tileSize);
            int ty = (int)Math.floor(pyw / tileSize);
            if (tx >= 0 && tx < roomW && ty >= 0 && ty < roomH) {
                float px = ox + tx * cell + (cell - marker) * 0.5f;
                float py = oy + ty * cell + (cell - marker) * 0.5f;
                drawRect(px, py, marker, marker, 0.35f, 0.70f, 1.00f, 1.00f);
                // small outline so it doesn't disappear on bright doors
                float o = Math.max(1f, marker * 0.18f);
                drawRect(px, py, marker, o, 0f, 0f, 0f, 0.65f);
                drawRect(px, py + marker - o, marker, o, 0f, 0f, 0f, 0.65f);
                drawRect(px, py, o, marker, 0f, 0f, 0f, 0.65f);
                drawRect(px + marker - o, py, o, marker, 0f, 0f, 0f, 0.65f);
            }
        }

        spriteBatch.setColor(Color.WHITE);
    }

    private void drawFullMapOverlay() {
        if (dungeonMap == null || dungeonMap.getWidth() <= 0 || dungeonMap.getHeight() <= 0) {
            font.setColor(Color.WHITE);
            float osx = font.getData().scaleX;
            float osy = font.getData().scaleY;
            font.getData().setScale(0.9f);
            drawCenteredText("MAP (M to close)", width * 0.5f, height * 0.5f + 18f);
            font.getData().setScale(0.7f);
            drawCenteredText("No map data", width * 0.5f, height * 0.5f - 6f);
            font.getData().setScale(osx, osy);
            return;
        }

        // Panel
        float panelW = Math.min(width - 70f, 650f);
        float panelH = Math.min(height - 110f, 390f);
        float px = Math.round((width - panelW) * 0.5f);
        float py = Math.round((height - panelH) * 0.5f - 6f);

        drawPanel(px, py, panelW, panelH, 0.35f);

        // Title
        float osx = font.getData().scaleX;
        float osy = font.getData().scaleY;
        font.setColor(Color.WHITE);
        font.getData().setScale(0.9f);
        drawCenteredText("MAP (M to close)", width * 0.5f, py + panelH + 24f);

        // Map itself
        float pad = 18f;
        drawDungeonMap(px + pad, py + pad, panelW - pad * 2f, panelH - pad * 2f, true);

        // Legend (bottom inside panel)
        font.getData().setScale(0.6f);
        float lx = px + pad;
        float ly = py + 8f;
        drawMapLegend(lx, ly);

        font.getData().setScale(osx, osy);
        font.setColor(Color.WHITE);
    }

    private void drawMapLegend(float x, float y) {
        // Small color chips + text.
        float chip = 8f;
        float gap = 10f;

        float cx = x;

        // Current
        drawColorChip(cx, y, chip, 0.95f, 0.86f, 0.35f, 1f);
        font.draw(spriteBatch, "current", cx + chip + 5f, y + chip);
        cx += chip + 5f + 52f + gap;

        // Cleared
        drawColorChip(cx, y, chip, 0.20f, 0.85f, 0.35f, 1f);
        font.draw(spriteBatch, "cleared", cx + chip + 5f, y + chip);
        cx += chip + 5f + 48f + gap;

        // Visited
        drawColorChip(cx, y, chip, 0.70f, 0.70f, 0.75f, 1f);
        font.draw(spriteBatch, "visited", cx + chip + 5f, y + chip);
    }
    private void drawColorChip(float x, float y, float size, float r, float g, float b, float a) {
        Color c = spriteBatch.getColor();
        float pr = c.r, pg = c.g, pb = c.b, pa = c.a;
        spriteBatch.setColor(r, g, b, a);
        spriteBatch.draw(whitePixel, x, y, size, size);
        spriteBatch.setColor(pr, pg, pb, pa);
    }

    private void drawDungeonMap(float x, float y, float w, float h, boolean large) {
        int cw = dungeonMap.getWidth();
        int ch = dungeonMap.getHeight();
        if (cw <= 0 || ch <= 0) return;

        float cell = Math.min(w / (float) cw, h / (float) ch);
        if (cell <= 2f) return;

        float gridW = cell * cw;
        float gridH = cell * ch;

        float ox = x + (w - gridW) * 0.5f;
        float oy = y + (h - gridH) * 0.5f;

        float roomSize = cell * 0.72f;
        float margin = (cell - roomSize) * 0.5f;

        float conn = Math.max(2f, cell * 0.18f);

        // Connections first (under rooms)
        for (int cy = 0; cy < ch; cy++) {
            for (int cx = 0; cx < cw; cx++) {
                if (!dungeonMap.isDiscovered(cx, cy)) continue;

                RoomTemplate t = dungeonMap.getTemplate(cx, cy);
                if (t == null) continue;

                float baseX = ox + cx * cell;
                float baseY = oy + cy * cell;

                float roomX = baseX + margin;
                float roomY = baseY + margin;

                float midX = roomX + roomSize * 0.5f;
                float midY = roomY + roomSize * 0.5f;

                // Right connection
                if (t.openRight() && dungeonMap.inBounds(cx + 1, cy) && dungeonMap.isDiscovered(cx + 1, cy)) {
                    float x1 = roomX + roomSize;
                    float x2 = (baseX + cell) + margin;
                    drawRect(x1, midY - conn * 0.5f, x2 - x1, conn, 0.75f, 0.75f, 0.80f, 0.9f);
                }

                // Up connection
                if (t.openUp() && dungeonMap.inBounds(cx, cy + 1) && dungeonMap.isDiscovered(cx, cy + 1)) {
                    float y1 = roomY + roomSize;
                    float y2 = (baseY + cell) + margin;
                    drawRect(midX - conn * 0.5f, y1, conn, y2 - y1, 0.75f, 0.75f, 0.80f, 0.9f);
                }
            }
        }

        // Rooms
        for (int cy = 0; cy < ch; cy++) {
            for (int cx = 0; cx < cw; cx++) {
                boolean discovered = dungeonMap.isDiscovered(cx, cy);
                if (!discovered && !large) continue;

                float baseX = ox + cx * cell;
                float baseY = oy + cy * cell;
                float roomX = baseX + margin;
                float roomY = baseY + margin;

                // Colors
                boolean isCur = (cx == dungeonMap.getCurrentX() && cy == dungeonMap.getCurrentY());
                boolean isCleared = dungeonMap.isCleared(cx, cy);

                float r, g, b, a;
                if (!discovered) {
                    r = g = b = 0.25f;
                    a = 0.22f;
                } else if (isCur) {
                    r = 0.95f; g = 0.86f; b = 0.35f; a = 1f;
                } else if (isCleared) {
                    r = 0.20f; g = 0.85f; b = 0.35f; a = 1f;
                } else {
                    r = 0.70f; g = 0.70f; b = 0.75f; a = 0.95f;
                }

                drawRect(roomX, roomY, roomSize, roomSize, r, g, b, a);

                // Outline
                float o = Math.max(1f, cell * 0.06f);
                drawRect(roomX, roomY, roomSize, o, 0f, 0f, 0f, 0.65f);
                drawRect(roomX, roomY + roomSize - o, roomSize, o, 0f, 0f, 0f, 0.65f);
                drawRect(roomX, roomY, o, roomSize, 0f, 0f, 0f, 0.65f);
                drawRect(roomX + roomSize - o, roomY, o, roomSize, 0f, 0f, 0f, 0.65f);
            }
        }

        // Restore batch color (in case something left it tinted)
        spriteBatch.setColor(Color.WHITE);
    }
    private void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        Color c = spriteBatch.getColor();
        float pr = c.r, pg = c.g, pb = c.b, pa = c.a;
        spriteBatch.setColor(r, g, b, a);
        spriteBatch.draw(whitePixel, x, y, w, h);
        spriteBatch.setColor(pr, pg, pb, pa);
    }

    private void drawTopRightStats() {
        final float pad = 14f;
        final float boxW = 210f;
        final float boxH = 54f;

        float x = Math.round(width - pad - boxW);
        float y = Math.round(height - pad - boxH);

        drawPanel(x, y, boxW, boxH, 0.55f);

        int kills = world.getEnemiesKilled();
        int souls = world.getSouls();
        int coins = world.getCoins();

        // 3 columns: Kills / Souls / Coins
        float colW = boxW / 3f;
        float iconSize = 16f;

        font.setColor(Color.WHITE);

        // Labels are small; numbers are larger.
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;

        for (int i = 0; i < 3; i++) {
            float cx = x + colW * i + colW * 0.5f;

            String label;
            String value;
            if (i == 0) {
                label = "kills";
                value = String.valueOf(kills);
            } else if (i == 1) {
                label = "souls";
                value = String.valueOf(souls);
            } else {
                label = "coins";
                value = String.valueOf(coins);
            }

            // Label
            font.getData().setScale(0.55f);
            drawCenteredText(label, cx, y + boxH - 12f);

            // Value
            font.getData().setScale(0.9f);
            drawCenteredText(value, cx, y + 18f);
        }

        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(Color.WHITE);
    }

    private void drawBottomItemBar() {
        ItemSystem items = world.getItems();
        if (items == null) return;

        final float padBottom = 12f;
        final float barH = 52f;

        // Wide, centered bar as in the sketch.
        float barW = Math.min(width - 60f, 520f);
        float x = Math.round((width - barW) * 0.5f);
        float y = Math.round(padBottom);

        drawPanel(x, y, barW, barH, 0.0f);

        float innerPad = 10f;
        float iconSize = 32f;
        float gap = 6f;
        float drawX = x + innerPad;
        float drawY = y + (barH - iconSize) * 0.5f;

        int maxIcons = (int) Math.floor((barW - innerPad * 2f + gap) / (iconSize + gap));
        int shown = 0;

        for (ItemId id : items.getOwnedList()) {
            if (id == null) continue;
            if (shown >= maxIcons) break;

            ItemDefinition def = ItemRegistry.get(id);
            TextureRegion icon = null;
            if (def != null && def.iconTileIndex >= 0 && def.iconTileIndex < itemSprites.size()) {
                icon = itemSprites.get(def.iconTileIndex);
            }

            if (icon != null) {
                spriteBatch.draw(icon, Math.round(drawX), Math.round(drawY), iconSize, iconSize);
            } else {
                // Fallback: draw a small placeholder box if the icon is missing.
                drawPanel(Math.round(drawX), Math.round(drawY), iconSize, iconSize, 0.35f);
            }

            drawX += iconSize + gap;
            shown++;
        }
    }

    private void drawPanel(float x, float y, float w, float h, float alpha) {
        Color c = spriteBatch.getColor();
        float r = c.r, g = c.g, b = c.b, a = c.a;

        // Background
        spriteBatch.setColor(0f, 0f, 0f, alpha);
        spriteBatch.draw(whitePixel, x, y, w, h);

        // Subtle border
        spriteBatch.setColor(1f, 1f, 1f, 0.0f);
        spriteBatch.draw(whitePixel, x, y, w, 2f);
        spriteBatch.draw(whitePixel, x, y + h - 2f, w, 2f);
        spriteBatch.draw(whitePixel, x, y, 2f, h);
        spriteBatch.draw(whitePixel, x + w - 2f, y, 2f, h);

        spriteBatch.setColor(r, g, b, a);
    }

    private void drawTexturedBar(Texture frame, Texture fill,
                                 float x, float y, float w, float pct,
                                 int padL, int padR, int padB, int padT,
                                 int fillTrimL, int fillTrimR, int fillTrimT, int fillTrimB) {

        pct = Math.max(0f, Math.min(1f, pct));

        // Keep the frame's aspect ratio.
        float scale = w / (float) frame.getWidth();
        float h = frame.getHeight() * scale;

        // Inner rect (where fill is allowed), derived from the frame sprite.
        float innerX = x + padL * scale;
        float innerY = y + padB * scale;
        float innerW = w - (padL + padR) * scale;
        float innerH = h - (padT + padB) * scale;

        // Always draw the frame so it appears even at 0%.
        spriteBatch.draw(frame, Math.round(x), Math.round(y), Math.round(w), Math.round(h));

        // Draw cropped fill (using a trimmed source rect so "100%" actually looks full).
        if (pct > 0f && innerW > 0.5f && innerH > 0.5f) {

            int srcFullW = fill.getWidth()  - fillTrimL - fillTrimR;
            int srcFullH = fill.getHeight() - fillTrimT - fillTrimB;

            // Safety clamp
            if (srcFullW <= 0 || srcFullH <= 0) return;

            float drawW = innerW * pct;
            int srcW = Math.max(1, Math.round(srcFullW * pct));

            spriteBatch.draw(
                fill,
                Math.round(innerX), Math.round(innerY),
                Math.round(drawW), Math.round(innerH),
                fillTrimL, fillTrimT,   // NOTE: srcY is from TOP in SpriteBatch.draw(...)
                srcW, srcFullH,
                false, false
            );
        }
    }

    private void drawItemToast() {
        String toast = null;
        try { toast = world.getToastText(); } catch (Throwable ignored) {}
        if (toast == null || toast.isEmpty()) return;

        font.setColor(Color.WHITE);
        font.getData().setScale(0.95f);
        drawCenteredText(toast, width / 2f, height - 26f);
        font.getData().setScale(1.0f);
    }
    private void updateSelectionHighlight() {
        // Don’t allow number-key selection during the delay
        if (upgradeInputTimer > 0f) {
            selectedUpgradeIndex = -1;
            return;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) selectedUpgradeIndex = 0;
        else if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) selectedUpgradeIndex = 1;
        else if (Gdx.input.isKeyPressed(Input.Keys.NUM_3)) selectedUpgradeIndex = 2;
        else selectedUpgradeIndex = -1;
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private void drawCenteredText(String text, float cx, float cy) {
        layout.setText(font, text, font.getColor(), 0, Align.left, false);
        font.draw(spriteBatch, layout, cx - layout.width / 2f, cy + layout.height / 2f);
    }

    private Texture getCardTextureSafe(Upgrade up) {
        if (up == null) return null;
        try {
            return up.getCardTexture();
        } catch (Throwable ignored) {
            try { return up.cardTexture; } catch (Throwable ignored2) { return null; }
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    // ----------------------------
    // Damage popups: WORLD -> UI (screen-space)
    // ----------------------------
    private void drawDamagePopupsScreenSpace() {
        if (viewport == null) return;

        // Convert from SCREEN PIXELS -> UI VIRTUAL UNITS (0..width, 0..height)
        float sxToUi = width  / (float) viewport.getScreenWidth();
        float syToUi = height / (float) viewport.getScreenHeight();

        int vx = viewport.getScreenX();
        int vy = viewport.getScreenY();

        for (DamagePopup p : world.getDamagePopups()) {
            if (p == null) continue;

            // WORLD -> SCREEN PIXELS (absolute window coords)
            tmpV3.set(p.x, p.y, 0f);
            viewport.project(tmpV3);

            // SCREEN PIXELS -> UI VIRTUAL (remove letterbox offset, then scale)
            float uiX = (tmpV3.x - vx) * sxToUi;
            float uiY = (tmpV3.y - vy) * syToUi;

            font.draw(spriteBatch, String.valueOf(p.amount), uiX, uiY);
        }
    }

    // ----------------------------
    // Bars / stats
    // ----------------------------
    private void drawHealthBar(Texture icon, Texture frame, Texture fill, float x, float y, float percent) {
        percent = Math.max(0f, Math.min(1f, percent));

        float iconSize = 70;
        float frameW = 250;
        float frameH = 35;

        float fillInsetX = 2;
        float fillInsetY = 2;

        float fillMaxW = frameW - fillInsetX * 2;
        float fillH = frameH - fillInsetY * 2;

        x = Math.round(x);
        y = Math.round(y);

        float barX = x + iconSize + 6;
        float barY = y - frameH + 2;
        barX = Math.round(barX);
        barY = Math.round(barY);

        spriteBatch.draw(frame, barX, barY, frameW, frameH);

        float filledW = Math.round(fillMaxW * percent);
        if (filledW > 0) {
            spriteBatch.draw(fill, barX + fillInsetX, barY + fillInsetY, filledW, fillH);
        }

        spriteBatch.draw(icon, x + 100, y - iconSize, iconSize - 100, iconSize);
    }

    private void drawManaBar(Texture icon, Texture frame, Texture fill, float x, float y, float percent) {
        percent = Math.max(0f, Math.min(1f, percent));

        float iconSize = 20;
        float frameW = 250;
        float frameH = 25;

        float fillInsetX = 28;
        float fillInsetY = 9;

        float fillMaxW = frameW - fillInsetX * 2;
        float fillH = frameH - fillInsetY * 2;

        x = Math.round(x);
        y = Math.round(y);

        float barX = x + iconSize + 2;
        float barY = y - frameH - 8;
        barX = Math.round(barX);
        barY = Math.round(barY);

        spriteBatch.draw(frame, barX, barY, frameW, frameH);

        float filledW = Math.round(fillMaxW * percent);
        if (filledW > 0) {
            spriteBatch.draw(fill, barX + fillInsetX + 2, barY + fillInsetY + 3, filledW, fillH);
        }

        spriteBatch.draw(icon, x + 35, y - iconSize - 5, iconSize + 100, iconSize);
    }

    private void drawSmallStat(Texture icon, float x, float y, String value) {
        float iconSize = 65;
        spriteBatch.draw(icon, x + 5, y - iconSize + 3, iconSize + 100, iconSize);
        font.draw(spriteBatch, value, x + iconSize - 10, y - iconSize / 2 + 8);
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    private void drawDoorPromptIfNear() {
        // Don't show door prompt if a higher-priority prompt is active
        // Door prompt is lowest priority: hide it if shrine/chest prompt should show
        if (hasChestPromptActive() || hasShrinePromptActive()) return;

        Player p = world.getPlayer();
        Room room = world.getRoom();
        if (p == null || room == null) return;
        if (viewport == null) return;

        int ts = room.getTileSize();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        float px = p.getX();
        float py = p.getY();
        float pw = p.getWidth();
        float ph = p.getHeight();

        float pcx = px + pw * 0.5f;
        float pcy = py + ph * 0.5f;

        int pTileX = (int)Math.floor(pcx / ts);
        int pTileY = (int)Math.floor(pcy / ts);

        // Find the best (closest) door tile we are overlapping
        Dir bestDir = null;
        float bestDoorCenterX = 0f;
        float bestDoorTopY = 0f;
        float bestDist2 = Float.MAX_VALUE;

        for (int dy = -UI_DOOR_SCAN_RADIUS; dy <= UI_DOOR_SCAN_RADIUS; dy++) {
            for (int dx = -UI_DOOR_SCAN_RADIUS; dx <= UI_DOOR_SCAN_RADIUS; dx++) {
                int tx = pTileX + dx;
                int tyWorld = pTileY + dy;

                if (tx < 0 || tx >= roomW || tyWorld < 0 || tyWorld >= roomH) continue;
                if (!isDoorWorld(room, tx, tyWorld, roomH)) continue;

                float doorX = tx * ts;
                float doorY = tyWorld * ts;

                float rx = doorX - UI_DOOR_INTERACT_PAD;
                float ry = doorY - UI_DOOR_INTERACT_PAD;
                float rw = ts + UI_DOOR_INTERACT_PAD * 2f;
                float rh = ts + UI_DOOR_INTERACT_PAD * 2f;

                if (!overlaps(px, py, pw, ph, rx, ry, rw, rh)) continue;

                Dir dir = dirFromDoorTile(tx, tyWorld, roomW, roomH);
                if (dir == null) continue;

                float cx = doorX + ts * 0.5f;
                float cy = doorY + ts * 0.5f;
                float ddx = cx - pcx;
                float ddy = cy - pcy;
                float d2 = ddx * ddx + ddy * ddy;

                if (d2 < bestDist2) {
                    bestDist2 = d2;
                    bestDir = dir;
                    bestDoorCenterX = cx;
                    bestDoorTopY = doorY + ts + 10f; // prompt above the door tile
                }
            }
        }

        if (bestDir == null) return;

        // WORLD -> SCREEN -> UI virtual coordinates (same pattern as chest/shrine prompt)
        tmpV3.set(bestDoorCenterX, bestDoorTopY, 0f);
        viewport.project(tmpV3);

        float sxToUi = width / (float) viewport.getScreenWidth();
        float syToUi = height / (float) viewport.getScreenHeight();

        int vx = viewport.getScreenX();
        int vy = viewport.getScreenY();

        float uiX = (tmpV3.x - vx) * sxToUi;
        float uiY = (tmpV3.y - vy) * syToUi;

        // You can customize the label:
        // e.g. "Enter", "Use Door", or include direction.
        String label = "Enter";
        // Optional direction text:
        // label = "Enter (" + bestDir.name() + ")";

        drawKeyPrompt(keyE, label, uiX, uiY);
    }

    private static boolean overlaps(float ax, float ay, float aw, float ah,
                                    float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private boolean isDoorWorld(Room room, int tx, int tyWorld, int roomH) {
        // Room door grid is stored in data coords (top-down), world is bottom-up
        int tyData = (roomH - 1) - tyWorld;
        try {
            return room.isDoor(tx, tyData);
        } catch (Throwable t) {
            return false;
        }
    }

    private Dir dirFromDoorTile(int tx, int tyWorld, int roomW, int roomH) {
        if (tyWorld >= roomH - UI_DOOR_EDGE_BAND) return Dir.UP;
        if (tyWorld < UI_DOOR_EDGE_BAND) return Dir.DOWN;
        if (tx < UI_DOOR_EDGE_BAND) return Dir.LEFT;
        if (tx >= roomW - UI_DOOR_EDGE_BAND) return Dir.RIGHT;
        return null;
    }

    private boolean hasChestPrompt() {
        Player p = world.getPlayer();
        if (p == null) return false;

        for (Chest c : world.getChests()) {
            if (c == null || c.opened) continue;

            if (Math.abs(c.x - p.getX()) <= 80 && Math.abs(c.y - p.getY()) <= 80) {
                return true;
            }
        }
        return false;
    }

    private boolean hasChestPromptActive() {
        try {
            return world.getNearestInteractableChest() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasShrinePromptActive() {
        Player p = world.getPlayer();
        if (p == null) return false;

        Shrine s;
        try {
            s = world.getShrine();
        } catch (Throwable t) {
            return false;
        }
        if (s == null) return false;

        float px = p.getX() + p.getWidth() * 0.5f;
        float py = p.getY() + p.getHeight() * 0.5f;

        float sx = s.x + s.w * 0.5f;
        float sy = s.y + s.h * 0.5f;

        float dx = sx - px;
        float dy = sy - py;
        float dist2 = dx * dx + dy * dy;

        float near = 60f; // must match drawShrinePromptIfNear()
        return dist2 <= near * near;
    }

    public void dispose() {
        fallbackCardTexture.dispose();
        whitePixel.dispose();
        font.dispose();

        uiBarBg.dispose();
        uiBarHealthFill.dispose();
        uiBarManaFill.dispose();
        uiBarManaBg.dispose();

        iconHeart.dispose();
        iconMana.dispose();
        iconCoin.dispose();
        iconSoul.dispose();
        iconKill.dispose();
        iconWave.dispose();

        // NEW
        shrineSheet.dispose();
        // dropTextureRegion uses iconSoul, so nothing additional to dispose

        keyboardSheet.dispose();

        System.out.println("World dispose called");
    }
}
