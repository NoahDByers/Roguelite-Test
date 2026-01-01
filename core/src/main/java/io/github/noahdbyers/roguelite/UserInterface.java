package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;

public class UserInterface {
    private final float width;
    private final float height;

    private final GameWorld world;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch spriteBatch;
    private final ArrayList<Entity> entities; // kept for compatibility; not used here

    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    // Fallback card art
    private final Texture fallbackCardTexture = new Texture("ui/upgrade_card.png");

    // 1x1 white pixel for overlays
    private final Texture whitePixel;

    // Visual highlight only
    private int selectedUpgradeIndex = -1;

    // Colors
    private final Color bulletColor = new Color(1f, 1f, 0f, 1f);

    // UI textures
    private final Texture uiBarBg = new Texture("ui/BarIcon.png");
    private final Texture uiBarManaBg = new Texture("ui/ManaBarIcon.png");
    private final Texture uiBarHealthFill = new Texture("ui/FullHPBar.png");
    private final Texture uiBarManaFill = new Texture("ui/FullManaBar.png");

    private final Texture iconHeart = new Texture("ui/HeartIcon.png");
    private final Texture iconMana = new Texture("ui/ManaIcon.png");
    private final Texture iconCoin = new Texture("ui/CoinIcon.png");
    private final Texture iconSoul = new Texture("ui/SoulIcon.png");
    private final Texture iconKill = new Texture("ui/SkullIcon.png");
    private final Texture iconWave = new Texture("ui/ClearIcon.png");

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
    // Upgrade Animation State
    // ----------------------------
    private static final float CARD_FRAME_TIME = 0.03f;

    private final float[] cardAnimTimer = new float[UPGRADE_COUNT];
    private final int[] cardFrameIndex = new int[UPGRADE_COUNT];
    private final boolean[] cardAnimFinished = new boolean[UPGRADE_COUNT];

    private boolean wasChoosingUpgrade = false;
    private int lastOfferedSignature = 0;

    // Reused vectors to avoid per-frame allocations
    private final Vector2 tmpMouseWorld = new Vector2();

    private float uiTime = 0f;

    // ----------------------------
    // NEW: screen-space projection for HUD/menus
    // ----------------------------
    private final Matrix4 screenProjection = new Matrix4();
    private final Matrix4 identityTransform = new Matrix4();

    public UserInterface(float width, float height,
                         GameWorld world,
                         ShapeRenderer shapeRenderer,
                         ArrayList<Entity> entities,
                         SpriteBatch spriteBatch) {
        this.width = width;
        this.height = height;
        this.world = world;
        this.shapeRenderer = shapeRenderer;
        this.entities = entities;
        this.spriteBatch = spriteBatch;

        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        whitePixel = new Texture(pm);
        pm.dispose();

        screenProjection.setToOrtho2D(0f, 0f, width, height);
        identityTransform.idt();
    }

    /** Call once per frame. */
    public void drawQueue() {
        if (world == null) return;

        float delta = Gdx.graphics.getDeltaTime();
        uiTime += delta;

        // ----------------------------
        // 1) WORLD (SpriteBatch): tiles + sprites + weapon
        // Uses camera.combined (already set by Main before calling UI.drawQueue()).
        // ----------------------------
        spriteBatch.begin();
        drawWorldTilesSafe();
        drawSpritesSafe(delta);

        Weapon w = world.getWeapon();
        Player p = world.getPlayer();
        if (w != null && p != null) {
            tmpMouseWorld.set(world.getAimWorldX(), world.getAimWorldY());
            w.draw(spriteBatch, delta, p, tmpMouseWorld);
        }
        spriteBatch.end();

        // ----------------------------
        // 2) WORLD (ShapeRenderer): bullets
        // Uses camera.combined (already set by Main)
        // ----------------------------
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawBulletsSafe();
        shapeRenderer.end();

        // ----------------------------
        // 3) UI (SpriteBatch): HUD + upgrade menu + game over
        // Switch to SCREEN SPACE so UI doesn’t move/zoom with the camera.
        // ----------------------------
        spriteBatch.setProjectionMatrix(screenProjection);
        spriteBatch.setTransformMatrix(identityTransform);

        spriteBatch.begin();

        if (!world.isChoosingUpgrade()) {
            drawHud();
            drawDamagePopups(); // drawn in world space? no — we’ll convert below using camera+viewport elsewhere if needed
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

        wasChoosingUpgrade = world.isChoosingUpgrade();
    }

    /**
     * ✅ FIX: never save spriteBatch.getColor() as a reference.
     * Save RGBA floats, then restore them.
     */
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

        // IMPORTANT: for UI hover, use SCREEN mouse coords (not world aim coords)
        float mx = Gdx.input.getX();
        float my = (height - Gdx.input.getY()); // screen-space origin at bottom-left

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

        if (hovered != -1 && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
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
            int srcY = (h - 1) - y; // flip

            for (int x = 0; x < w; x++) {
                int tileId = room.getTile(x, srcY);
                int regionIndex = tileId - 1;

                TextureRegion region = room.getTextureRegion(regionIndex);
                if (region == null) continue;

                spriteBatch.draw(region, x * ts, y * ts, ts, ts);
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
    // World drawing (bullets) — WORLD SPACE
    // ----------------------------
    private void drawBulletsSafe() {
        shapeRenderer.setColor(bulletColor);
        for (Bullet b : world.getBullets()) {
            if (b == null) continue;
            shapeRenderer.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        }
    }

    // ----------------------------
    // HUD — SCREEN SPACE
    // ----------------------------
    private void drawHud() {
        Player p = world.getPlayer();
        if (p == null) return;

        float startX = 140;
        float startY = height - 448;
        float rowGap = 20;

        float manaPct = (p.getMaxMana() <= 0) ? 0f : (p.getMana() / (float) p.getMaxMana());
        float hpPct = (p.getMaxHealth() <= 0) ? 0f : (p.getHealth() / (float) p.getMaxHealth());

        drawManaBar(iconMana, uiBarManaBg, uiBarManaFill, startX + 45, startY + rowGap, manaPct);
        drawHealthBar(iconHeart, uiBarBg, uiBarHealthFill, startX, startY, hpPct);

        drawSmallStat(iconCoin, startX + 405, 485, "     " + world.getCoins());
        drawSmallStat(iconSoul, startX + 425, 440, "" + world.getSouls());

        drawSmallStat(iconKill, startX - 148, 490, "  " + world.getEnemiesKilled());
        drawSmallStat(iconWave, startX - 140, 450, "" + world.getWave());
    }

    private void updateSelectionHighlight() {
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

    private void drawDamagePopups() {
        // If you want these in screen space, you must project world->screen using viewport+camera.
        // For now, keep as-is (they will appear in screen coords).
        for (DamagePopup p : world.getDamagePopups()) {
            if (p == null) continue;
            font.draw(spriteBatch, "" + p.amount, p.x, p.y);
        }
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
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

        System.out.println("World dispose called");
    }
}
