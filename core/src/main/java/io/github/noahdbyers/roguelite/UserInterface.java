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
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;

public class UserInterface {
    private final float width;
    private final float height;

    private final GameWorld world;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch spriteBatch;
    private final ArrayList<Entity> entities;

    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    // Fallback card art (loaded once)
    private final Texture fallbackCardTexture = new Texture("ui/upgrade_card.png");

    // Visual highlight only (GameWorld applies upgrades)
    private int selectedUpgradeIndex = -1;

    // Colors
    private final Color bulletColor = new Color(1f, 1f, 0f, 1f);

    // UI textures
    private Texture uiBarBg = new Texture("ui/BarIcon.png");
    private Texture uiBarManaBg = new Texture("ui/ManaBarIcon.png");
    private Texture uiBarHealthFill = new Texture("ui/FullHPBar.png");
    private Texture uiBarManaFill = new Texture("ui/FullManaBar.png");

    private Texture iconHeart = new Texture("ui/HeartIcon.png");
    private Texture iconMana = new Texture("ui/ManaIcon.png");
    private Texture iconCoin = new Texture("ui/CoinIcon.png");
    private Texture iconSoul = new Texture("ui/SoulIcon.png");
    private Texture iconKill = new Texture("ui/SkullIcon.png");
    private Texture iconWave = new Texture("ui/ClearIcon.png");

    // Layout
    private static final int UPGRADE_COUNT = 3;
    private static final float CARD_W = 170f;
    private static final float CARD_H = 300f;
    private static final float CARD_GAP = 18f;
    private static final float SELECT_LIFT = 10f;

    // Text box region inside card
    private static final float BOX_X_FRAC = 0.18f;
    private static final float BOX_Y_FRAC = 0.44f;
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

        float delta = Gdx.graphics.getDeltaTime();

        // ----------------------------
        // 1) WORLD (SpriteBatch): tiles + sprites (player + zombies)
        // ----------------------------
        spriteBatch.begin();
        drawWorldTilesSafe();
        drawSpritesSafe(delta);   // ✅ player + zombies drawn here
        Player p = world.getPlayer();
        if (p != null) {
            p.draw(spriteBatch, Gdx.graphics.getDeltaTime());
        }

        world.getWeapon().draw(spriteBatch, delta, world.getAttackPosition(), world.getPlayer(), world.getAimWorld());
        spriteBatch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 1f, 1f, 0.35f);
        for (AttackHitbox hb : world.getMeleeHitboxes()) {
            shapeRenderer.rect(hb.rect.x, hb.rect.y, hb.rect.width, hb.rect.height);
        }
        shapeRenderer.end();

        // ----------------------------
        // 2) WORLD (ShapeRenderer): bullets (and any debug shapes)
        // ----------------------------
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawBulletsSafe();
        shapeRenderer.end();

        // ----------------------------
        // 3) UI (SpriteBatch): HUD + upgrade menu + game over
        // ----------------------------
        spriteBatch.begin();

        if (!world.isChoosingUpgrade()) {
            drawHud();
            drawDamagePopups();
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
    // World drawing (tiles)
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

    // ----------------------------
    // World drawing (sprites): player + zombies
    // ----------------------------
    private void drawSpritesSafe(float delta) {
        // Player
        Player p = world.getPlayer();
        if (p != null) {
            p.draw(spriteBatch, delta);
        }

        // Enemies: draw zombies as sprites
        for (Enemy e : world.getEnemies()) {
            if (e == null) continue;

            if (e instanceof Zombie) {
                ((Zombie) e).draw(spriteBatch, delta);
            } else {
                // If you ever have non-zombie enemies, you can draw them later.
                // For now, do nothing here (they could still be drawn as shapes if desired).
            }
        }
    }

    // ----------------------------
    // World drawing (shapes): bullets
    // ----------------------------
    private void drawBulletsSafe() {
        shapeRenderer.setColor(bulletColor);
        for (Bullet b : world.getBullets()) {
            if (b == null) continue;
            shapeRenderer.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        }
    }

    // ----------------------------
    // HUD
    // ----------------------------
    private void drawHud() {
        Player p = world.getPlayer();
        if (p == null) return;

        float startX = 140;
        float startY = height - 448;
        float rowGap = 20;

        drawManaBar(iconMana, uiBarManaBg, uiBarManaFill, startX + 45, startY + rowGap, p.getMana() / (float) p.getMaxMana());
        drawHealthBar(iconHeart, uiBarBg, uiBarHealthFill, startX, startY, p.getHealth() / (float) p.getMaxHealth());

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

            font.getData().setScale(0.85f);
            font.setColor(Color.BLACK);
            font.draw(spriteBatch, "Press " + (i + 1), x + 14, y + 26);
            font.getData().setScale(1.0f);
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(1.0f);
    }

    private void drawUpgradeCardTextInWhiteBox(Upgrade[] offered) {
        if (offered == null || offered.length < UPGRADE_COUNT) return;

        float totalW = CARD_W * UPGRADE_COUNT + CARD_GAP * (UPGRADE_COUNT - 1);
        float startX = (width - totalW) / 2f;
        float startY = (height - CARD_H) / 2f - 10f;

        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;
        Color oldColor = new Color(font.getColor());

        for (int i = 0; i < UPGRADE_COUNT; i++) {
            Upgrade up = offered[i];
            if (up == null) continue;

            float cardX = startX + i * (CARD_W + CARD_GAP);
            float cardY = startY + (i == selectedUpgradeIndex ? SELECT_LIFT : 0f);

            float boxX = cardX + CARD_W * BOX_X_FRAC;
            float boxY = cardY + CARD_H * BOX_Y_FRAC;
            float boxW = CARD_W * BOX_W_FRAC;
            float boxH = CARD_H * BOX_H_FRAC;

            font.setColor(Color.BLACK);
            font.getData().setScale(0.95f);

            float titleY = boxY + boxH - 8f;
            layout.setText(font, safe(up.name), font.getColor(), boxW, Align.center, false);
            font.draw(spriteBatch, layout, boxX, titleY);

            font.setColor(new Color(0.15f, 0.15f, 0.15f, 1f));
            font.getData().setScale(0.72f);

            float descTopY = titleY - 18f;
            layout.setText(font, safe(up.desc), font.getColor(), boxW, Align.center, true);

            float maxDescTop = boxY + boxH - 26f;
            float clampedDescTop = Math.min(descTopY, maxDescTop);

            font.draw(spriteBatch, layout, boxX, clampedDescTop);
        }

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
            return up.getCardTexture();
        } catch (Throwable ignored) {
            try {
                return up.cardTexture;
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    // ----------------------------
    // Bars / stats (unchanged)
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
        for (DamagePopup p : world.getDamagePopups()) {
            font.draw(spriteBatch, "" + p.amount, p.x, p.y);
        }
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public void dispose() {
        fallbackCardTexture.dispose();
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
