package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;

public class UserInterface {
    private final float width;
    private final float height;

    private final GameWorld world;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch spriteBatch;

    @SuppressWarnings("unused")
    private final ArrayList<Entity> entities;

    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    // Fallback card art (loaded once)
    private final Texture fallbackCardTexture = new Texture("upgrade_card.png");

    // Visual highlight only (GameWorld applies upgrades)
    private int selectedUpgradeIndex = -1;

    // World colors
    private final Color wallColor = new Color(0.50f, 0.50f, 0.50f, 1f);
    private final Color playerColor = new Color(0f, 1f, 0f, 1f);
    private final Color playerInvulnColor = new Color(1f, 1f, 0f, 1f);
    private final Color enemyColor = new Color(1f, 0f, 0f, 1f);
    private final Color bulletColor = new Color(1f, 1f, 0f, 1f);

    //Initializing textures
    private Texture uiBarBg = new Texture("BarIcon.png");
    private Texture uiBarManaBg = new Texture("ManaBarIcon.png");
    private Texture uiBarHealthFill = new Texture("FullHPBar.png");
    private Texture uiBarManaFill = new Texture("FullManaBar.png");

    private Texture iconHeart = new Texture("HeartIcon.png");
    private Texture iconMana = new Texture("ManaIcon.png");
    private Texture iconCoin = new Texture("CoinIcon.png");
    private Texture iconSoul = new Texture("SoulIcon.png");
    private Texture iconKill = new Texture("SkullIcon.png");
    private Texture iconWave = new Texture("ClearIcon.png");

    // Layout
    private static final int UPGRADE_COUNT = 3;
    private static final float CARD_W = 170f;
    private static final float CARD_H = 300f;
    private static final float CARD_GAP = 18f;
    private static final float SELECT_LIFT = 10f;

    // --- Text box region INSIDE your card art (fractions of card size) ---
    // These are tuned for the asset you showed (white box under the diamond).
    // If you want to nudge it: tweak Y and H first.
    private static final float BOX_X_FRAC = 0.18f;
    private static final float BOX_Y_FRAC = 0.44f; // higher value = box moves UP; lower = DOWN
    private static final float BOX_W_FRAC = 0.64f;
    private static final float BOX_H_FRAC = 0.22f;

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
    }

    /** Call once per frame. */
    public void drawQueue() {
        if (world == null) return;
        spriteBatch.begin();
        drawWorldTilesSafe();
        spriteBatch.end();

        // World (shapes)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawEntitiesSafe();
        shapeRenderer.end();

        // UI (sprites/text) — NO overlay
        spriteBatch.begin();

        if (!world.isChoosingUpgrade()) {
            drawHud();
        }

        if (world.isChoosingUpgrade()) {
            updateSelectionHighlight();
            drawUpgradeCards(world.getOfferedUpgrades());
            drawUpgradeCardTextInWhiteBox(world.getOfferedUpgrades());

            font.setColor(Color.WHITE);
            font.getData().setScale(1.05f);
            drawCenteredText("Choose an Upgrade (1 / 2 / 3)", width / 2f, height - 30);
            font.getData().setScale(1.0f);
        } else {
            selectedUpgradeIndex = -1;
        }

        if (world.isGameOver()) {
            font.setColor(Color.WHITE);
            font.getData().setScale(1.2f);
            drawCenteredText("GAME OVER - Press R to Restart", width / 2f, height / 2f + 10);
            font.getData().setScale(1.0f);
        }

        spriteBatch.end();
    }

    // ----------------------------
    // World drawing (Shapes)
    // ----------------------------

    private void drawWorldTilesSafe() {
        Room room = world.getRoom();
        if (room == null) return;

        float ts = room.getTileSize();

        for (int y = 0; y < room.getRoomHeight(); y++) {
            for (int x = 0; x < room.getRoomWidth(); x++) {
                    spriteBatch.draw(room.getTextureRegion(room.getTile(x, y)), x * ts, y * ts, ts, ts);
            }
        }
    }

    private void drawEntitiesSafe() {
        Player p = world.getPlayer();
        if (p != null) {
            shapeRenderer.setColor(p.isInvulnerable() ? playerInvulnColor : playerColor);
            shapeRenderer.rect(p.getX(), p.getY(), p.getWidth(), p.getHeight());
        }

        shapeRenderer.setColor(enemyColor);
        for (Enemy e : world.getEnemies()) {
            if (e == null) continue;
            shapeRenderer.rect(e.getX(), e.getY(), e.getWidth(), e.getHeight());
        }

        shapeRenderer.setColor(bulletColor);
        for (Bullet b : world.getBullets()) {
            if (b == null) continue;
            shapeRenderer.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        }
    }

    private void drawHud() {
        Player p = world.getPlayer();
        if (p == null) return;

        float startX = 140;
        float startY = height - 448;
        float rowGap = 20;

        // Mana
        drawManaBar(iconMana, uiBarManaBg, uiBarManaFill, startX + 45, startY + rowGap, p.getMana() / (float) p.getMaxMana());

        // HP
        drawHealthBar(iconHeart, uiBarBg, uiBarHealthFill, startX, startY, p.getHealth() / (float) p.getMaxHealth());

        // Coins (pseudo bar)
        drawSmallStat(iconCoin, startX + 405, 485, "     " + world.getCoins());
        // Souls (pseudo bar)
        drawSmallStat(iconSoul, startX + 425, 440, "" + world.getSouls());

        // Kills / Wave (icon + number)
        drawSmallStat(iconKill, startX - 148, 490, "  " + world.getEnemiesKilled());
        drawSmallStat(iconWave, startX - 140, 450, "" + world.getWave());
    }

    private void updateSelectionHighlight() {
        if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) selectedUpgradeIndex = 0;
        else if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) selectedUpgradeIndex = 1;
        else if (Gdx.input.isKeyPressed(Input.Keys.NUM_3)) selectedUpgradeIndex = 2;
        else selectedUpgradeIndex = -1;
    }

    private void drawUpgradeCards(Upgrade[] offered) {
        float totalW = CARD_W * UPGRADE_COUNT + CARD_GAP * (UPGRADE_COUNT - 1);
        float startX = (width - totalW) / 2f;
        float startY = (height - CARD_H) / 2f - 10f;

        for (int i = 0; i < UPGRADE_COUNT; i++) {
            float x = startX + i * (CARD_W + CARD_GAP);
            float y = startY + (i == selectedUpgradeIndex ? SELECT_LIFT : 0f);

            Upgrade up = (offered != null && i < offered.length) ? offered[i] : null;
            Texture tex = getCardTextureSafe(up);
            if (tex == null) tex = fallbackCardTexture;

            spriteBatch.draw(tex, x, y, CARD_W, CARD_H);

            // Press label (bottom-left of card)
            font.getData().setScale(0.85f);
            font.setColor(Color.BLACK);
            font.draw(spriteBatch, "Press " + (i + 1), x + 14, y + 26);
            font.getData().setScale(1.0f);
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(1.0f);
    }

    /**
     * Draws the upgrade name/desc INSIDE the white text box region below the diamond.
     */
    private void drawUpgradeCardTextInWhiteBox(Upgrade[] offered) {
        if (offered == null || offered.length < UPGRADE_COUNT) return;

        float totalW = CARD_W * UPGRADE_COUNT + CARD_GAP * (UPGRADE_COUNT - 1);
        float startX = (width - totalW) / 2f;
        float startY = (height - CARD_H) / 2f - 10f;

        // Save font state
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;
        Color oldColor = new Color(font.getColor());

        for (int i = 0; i < UPGRADE_COUNT; i++) {
            Upgrade up = offered[i];
            if (up == null) continue;

            float cardX = startX + i * (CARD_W + CARD_GAP);
            float cardY = startY + (i == selectedUpgradeIndex ? SELECT_LIFT : 0f);

            // White box region inside the card
            float boxX = cardX + CARD_W * BOX_X_FRAC;
            float boxY = cardY + CARD_H * BOX_Y_FRAC;
            float boxW = CARD_W * BOX_W_FRAC;
            float boxH = CARD_H * BOX_H_FRAC;

            // Title near top of the box
            font.setColor(Color.BLACK);
            font.getData().setScale(0.95f);

            float titleY = boxY + boxH - 8f; // small padding from top edge
            layout.setText(font, safe(up.name), font.getColor(), boxW, Align.center, false);
            font.draw(spriteBatch, layout, boxX, titleY);

            // Description below the title, wrapped within the box
            font.setColor(new Color(0.15f, 0.15f, 0.15f, 1f));
            font.getData().setScale(0.72f);

            float descTopY = titleY - 18f; // spacing under title (tweak if needed)
            layout.setText(font, safe(up.desc), font.getColor(), boxW, Align.center, true);

            // If the wrapped description is taller than the box, this keeps it from spilling upward too much.
            // (We draw from "top", so it will flow downward.)
            float maxDescTop = boxY + boxH - 26f;
            float clampedDescTop = Math.min(descTopY, maxDescTop);

            font.draw(spriteBatch, layout, boxX, clampedDescTop);
        }

        // Restore font state
        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(oldColor);
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
            return up.getCardTexture(); // preferred
        } catch (Throwable ignored) {
            try {
                return up.cardTexture; // fallback if public
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private void drawHealthBar(Texture icon, Texture frame, Texture fill, float x, float y, float percent) {
        percent = Math.max(0f, Math.min(1f, percent));

        // --- Sizes tuned to YOUR sprites ---
        float iconSize = 70;

        float frameW = 250;   // matches your BarIcon width
        float frameH = 35;

        float fillInsetX = 2;   // padding inside frame (left/right)
        float fillInsetY = 2;

        float fillMaxW = frameW - fillInsetX * 2;
        float fillH = frameH - fillInsetY * 2;

        // Pixel snap
        x = Math.round(x);
        y = Math.round(y);


        float barX = x + iconSize + 6;
        float barY = y - frameH + 2;
        barX = Math.round(barX);
        barY = Math.round(barY);

        // Frame
        spriteBatch.draw(frame, barX, barY, frameW, frameH);

        // Fill (clipped, NOT stretched)
        float filledW = Math.round(fillMaxW * percent);

        if (filledW > 0) {
            spriteBatch.draw(fill, barX + fillInsetX, barY + fillInsetY, filledW, fillH);
        }
        // Icon
        spriteBatch.draw(icon, x + 100, y - iconSize, iconSize - 100, iconSize);

    }
    private void drawManaBar(Texture icon, Texture frame, Texture fill, float x, float y, float percent) {
        percent = Math.max(0f, Math.min(1f, percent));

        // --- Sizes tuned to YOUR sprites ---
        float iconSize = 20;

        float frameW = 250;   // matches your BarIcon width
        float frameH = 25;

        float fillInsetX = 28;   // padding inside frame (left/right)
        float fillInsetY = 9;

        float fillMaxW = frameW - fillInsetX * 2;
        float fillH = frameH - fillInsetY * 2;

        // Pixel snap
        x = Math.round(x);
        y = Math.round(y);


        float barX = x + iconSize + 2;
        float barY = y - frameH - 8;
        barX = Math.round(barX);
        barY = Math.round(barY);

        // Frame
        spriteBatch.draw(frame, barX, barY, frameW, frameH);

        // Fill (clipped, NOT stretched)
        float filledW = Math.round(fillMaxW * percent);

        if (filledW > 0) {
            spriteBatch.draw(fill, barX + fillInsetX + 2, barY + fillInsetY + 3, filledW, fillH);
        }
        // Icon
        spriteBatch.draw(icon, x + 35, y - iconSize - 5, iconSize + 100, iconSize);

    }
    private void drawSmallStat(Texture icon, float x, float y, String value) {
        float iconSize = 65;

        spriteBatch.draw(icon, x + 5, y - iconSize + 3, iconSize + 100, iconSize);
        font.draw(spriteBatch, value, x + iconSize - 10, y - iconSize / 2 + 8);
    }

    public void dispose() {
        fallbackCardTexture.dispose();
        font.dispose();
        uiBarBg.dispose();
        uiBarHealthFill.dispose();
        uiBarManaFill.dispose();
        iconHeart.dispose();
        iconMana.dispose();
        iconCoin.dispose();
        iconSoul.dispose();
        iconKill.dispose();
        iconWave.dispose();
        uiBarManaBg.dispose();
    }
}
