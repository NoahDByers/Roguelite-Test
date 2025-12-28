package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.GL20;

public class Main extends ApplicationAdapter {
    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float VIRTUAL_WIDTH = 640;
    private static final float VIRTUAL_HEIGHT = 480;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private Texture upgradeCardTex;
    private Texture titleScreenTex;
    private final GlyphLayout titleLayout = new GlyphLayout();
    private final GlyphLayout descLayout = new GlyphLayout();
    private GameWorld world;
    private Texture playButtonTex;
    private Texture settingsButtonTex;
    private Texture shopButtonTex;
    // Optional: show which card is “selected” (1/2/3)
    private int selectedUpgradeIndex = -1;
    //Title screen bool
    private boolean titleScreen = true;
    private Button play;
    private Button settings;
    private Button shop;

    // simple starter room
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

        Room room = new Room(32, 20, 15, starterRoom);

        // Make sure upgrade_card.png is in your assets folder
        upgradeCardTex = new Texture("upgrade_card.png");
        titleScreenTex = new Texture("title_screen.png");
        playButtonTex = new Texture("play_button.png");
        settingsButtonTex = new Texture("settings_button.png");
        shopButtonTex = new Texture("shop_button.png");
        play = new Button(10, 280, 300, 200, playButtonTex);
        settings = new Button(10, 200, 150, 110, settingsButtonTex);
        shop = new Button(150, 185, 150, 140, shopButtonTex);


        world = new GameWorld(room);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        if (titleScreen) {
            spriteBatch.begin();

            spriteBatch.draw(titleScreenTex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            play.drawButton(spriteBatch);
            settings.drawButton(spriteBatch);
            shop.drawButton(spriteBatch);
            spriteBatch.end();
        }
        else {
            float delta = Gdx.graphics.getDeltaTime();
            world.update(delta);

            ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

            drawWorld();
            drawUI();
        }
    }

    private void drawWorld() {
        Room room = world.getRoom();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // tiles
        for (int y = 0; y < room.getRoomHeight(); y++) {
            for (int x = 0; x < room.getRoomWidth(); x++) {
                if (room.getTile(x, y) == 1) {
                    shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
                    shapeRenderer.rect(
                        x * room.getTileSize(),
                        y * room.getTileSize(),
                        room.getTileSize(),
                        room.getTileSize()
                    );
                }
            }
        }

        // bullets
        shapeRenderer.setColor(1, 1, 0, 1);
        for (Bullet b : world.getBullets()) {
            shapeRenderer.rect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        }

        // enemies
        shapeRenderer.setColor(1, 0, 0, 1);
        for (Enemy e : world.getEnemies()) {
            shapeRenderer.rect(e.getX(), e.getY(), e.getWidth(), e.getHeight());
        }

        // player (flash while invulnerable)
        Player p = world.getPlayer();
        if (p.isInvulnerable()) shapeRenderer.setColor(1, 1, 0, 1);
        else shapeRenderer.setColor(0, 1, 0, 1);

        shapeRenderer.rect(p.getX(), p.getY(), p.getWidth(), p.getHeight());

        shapeRenderer.end();
    }

    private void drawUI() {
        // Track “selected” card (optional)
        if (world.isChoosingUpgrade()) {
            if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) selectedUpgradeIndex = 0;
            else if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) selectedUpgradeIndex = 1;
            else if (Gdx.input.isKeyPressed(Input.Keys.NUM_3)) selectedUpgradeIndex = 2;
        } else {
            selectedUpgradeIndex = -1;
        }

        spriteBatch.begin();

        if (!world.isChoosingUpgrade()) {
            font.draw(spriteBatch, "Kills: " + world.getEnemiesKilled(), 10, VIRTUAL_HEIGHT - 10);
            font.draw(spriteBatch, "Wave: " + world.getWave(), 10, VIRTUAL_HEIGHT - 35);

            Player p = world.getPlayer();
            font.draw(spriteBatch, "HP: " + p.getHealth() + "/" + p.getMaxHealth(), 10, VIRTUAL_HEIGHT - 60);
        }

        if (world.isChoosingUpgrade()) {
            Upgrade[] ups = world.getOfferedUpgrades();
            font.getData().setScale(1.2f);
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(spriteBatch, "Choose an Upgrade", VIRTUAL_WIDTH / 2f - 90, VIRTUAL_HEIGHT - 40);
            font.getData().setScale(1.0f);

            drawUpgradeCards(ups); // <-- use the card frame instead of plain text
        }

        if (world.isGameOver()) {
            font.draw(spriteBatch, "GAME OVER - Press R to Restart", 40, VIRTUAL_HEIGHT / 2f);
        }

        spriteBatch.end();
    }

    /**
     * IMPORTANT:
     * This method assumes spriteBatch.begin() has ALREADY been called.
     * Do NOT call begin/end inside here if you call it from drawUI().
     */
    private final GlyphLayout layout = new GlyphLayout();

    private void drawUpgradeCards(Upgrade[] options) {
        // --- Polished layout (fits your 640x480 virtual size) ---
        float cardW = 170f;
        float cardH = 300f;
        float gap = 18f;

        float totalW = cardW * 3 + gap * 2;
        float startX = (VIRTUAL_WIDTH - totalW) / 2f;
        float startY = (VIRTUAL_HEIGHT - cardH) / 2f - 10f;

        // --- White box area (fractions tuned for your card art) ---
        float boxXFrac = 0.18f;
        float boxYFrac = 0.38f;
        float boxWFrac = 0.64f;
        float boxHFrac = 0.24f;

        // --- Optional: draw a panel behind cards (NOT full screen) ---
        // This assumes drawUpgradeCards is called while spriteBatch is begun (from drawUI()).
        spriteBatch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float panelPadding = 18f;
        float panelX = startX - panelPadding;
        float panelY = startY - 26f;
        float panelW = totalW + panelPadding * 2;
        float panelH = cardH + 90f;

        shapeRenderer.setColor(0.05f, 0.06f, 0.09f, 0.85f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);

        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.begin();

        // Save old font state
        Color oldFontColor = font.getColor();
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;

        for (int i = 0; i < 3; i++) {
            float x = startX + i * (cardW + gap);
            float y = startY;

            boolean selected = (i == selectedUpgradeIndex);

            // Lift and tint selected card slightly
            float lift = selected ? 8f : 0f;
            float drawX = x;
            float drawY = y + lift;

            if (selected) spriteBatch.setColor(1f, 1f, 0.9f, 1f);
            else spriteBatch.setColor(1f, 1f, 1f, 1f);

            spriteBatch.draw(upgradeCardTex, drawX, drawY, cardW, cardH);
            spriteBatch.setColor(1f, 1f, 1f, 1f);

            // Draw selection number (top-left)
            font.getData().setScale(0.9f);
            font.setColor(0.1f, 0.1f, 0.1f, 1f);
            font.draw(spriteBatch, (i + 1) + ")", drawX + 10, drawY + cardH - 10);

            if (options[i] == null) continue;

            // --- Compute white box region in card coordinates ---
            float boxX = drawX + cardW * boxXFrac;
            float boxY = drawY + cardH * boxYFrac;
            float boxW = cardW * boxWFrac;
            float boxH = cardH * boxHFrac;

            // Inner padding inside the box
            float innerPadX = 10f;
            float innerPadTop = 12f;
            float innerPadBottom = 10f;

            float contentX = boxX + innerPadX;
            float contentW = boxW - innerPadX * 2;

            // --- Title ---
            font.getData().setScale(1.0f);
            font.setColor(0f, 0f, 0f, 1f);

            titleLayout.setText(font, options[i].name, font.getColor(), contentW, Align.center, false);
            float titleY = boxY + boxH - innerPadTop;
            font.draw(spriteBatch, titleLayout, contentX, titleY);

            // --- Description (wrapped + clamped so it never spills) ---
            font.getData().setScale(0.78f);
            font.setColor(0.15f, 0.15f, 0.15f, 1f);

            // Determine how many lines fit under the title inside the box
            float lineHeight = font.getLineHeight();
            float descTopY = titleY - 26f; // spacing under title
            float availableH = descTopY - (boxY + innerPadBottom);
            int maxLines = Math.max(1, (int)(availableH / lineHeight));

            String clamped = clampWrappedTextToLines(options[i].desc, contentW, maxLines);

            descLayout.setText(font, clamped, font.getColor(), contentW, Align.center, true);
            font.draw(spriteBatch, descLayout, contentX, descTopY);

            // --- Hint text at bottom-left of the card ---
            font.getData().setScale(0.75f);
            font.setColor(0.1f, 0.1f, 0.1f, 1f);
            font.draw(spriteBatch, "Press " + (i + 1), drawX + 18, drawY + 22);
        }

        // Restore font state
        font.getData().setScale(oldScaleX, oldScaleY);
        font.setColor(oldFontColor);
    }




    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();
        upgradeCardTex.dispose();
        titleScreenTex.dispose();
    }

    private String clampWrappedTextToLines(String text, float width, int maxLines) {
        if (text == null) return "";

        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        StringBuilder line = new StringBuilder();

        int linesUsed = 1;

        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            String test = (line.length() == 0) ? word : line + " " + word;
            descLayout.setText(font, test, font.getColor(), width, Align.center, false);

            // If this "line" would wrap, commit current line and start new one
            if (descLayout.width > width && line.length() > 0) {
                // commit line
                if (result.length() > 0) result.append("\n");
                result.append(line);

                linesUsed++;
                if (linesUsed > maxLines) {
                    // add ellipsis to previous line
                    String truncated = result.toString();
                    if (!truncated.endsWith("...")) truncated += "...";
                    return truncated;
                }

                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(test);
            }
        }

        // commit final line
        if (line.length() > 0) {
            if (result.length() > 0) result.append("\n");
            result.append(line);
        }

        return result.toString();
    }

    public void drawTitleScreen() {
        titleScreen = true;


    }

}
