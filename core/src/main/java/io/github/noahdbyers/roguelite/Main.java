package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
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
    private Texture upgradeCardTex;
    private Texture titleScreenBackgroundTex;
    private Texture cemeteryTiles;
    private Texture cemeteryFloor;
    private GameWorld world;
    private boolean titleScreen = true;
    private Texture uiBanners;
    private Texture generalAssets;
    private TextureRegion uiBannerRegion;
    private Room room;
    private Player player;
    private UserInterface UI;
    private ArrayList<Entity> entities;
    private Button playButton;
    private Button settingsCogButton;
    private Button marketButton;
    private ArrayList<Button> titleScreenButtons;
    private TextureRegion settingsCogRegion;
    private TextureRegion flagBannerRegion;

    @Override
    public void create() {
        //Creating the rendering tools
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        //Setting default font (text) settings
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        font.setColor(1f, 1f, 1f, 1f);

        //Camera + viewport setup for proper scaling
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);

        //Instantiating and creating objects for various button texture groups and button objects
        ArrayList<TextureRegion> marketButtonTextures = new ArrayList<TextureRegion>();
        ArrayList<TextureRegion> basicButtonTextures = new ArrayList<TextureRegion>();
        ArrayList<TextureRegion> settingsCogTextures = new ArrayList<TextureRegion>();
        titleScreenButtons = new ArrayList<Button>();

        //Creating the texture objects for the various textures and sprite sheets
        upgradeCardTex = new Texture("upgrade_card.png");
        titleScreenBackgroundTex = new Texture("title_screen.png");
        generalAssets = new Texture("general_assets.png");
        uiBanners = new Texture("bannerSpritesheet.png");
        cemeteryTiles = new Texture("cemeteryTiles.png");
        cemeteryFloor = new Texture("cemeteryFloor.png");

        //Creating the arraylists for different tilesets
        ArrayList<TextureRegion> cemeteryTileset = new ArrayList<TextureRegion>();

        //Extracting texture regions from the sprite sheet
        uiBannerRegion = new TextureRegion(uiBanners, 16, 16, 192, 275);
        flagBannerRegion = new TextureRegion(generalAssets, 20, 292, 111, 32);
        Collections.addAll(basicButtonTextures, new TextureRegion(uiBanners, 816, 16, 64, 26),
            new TextureRegion(uiBanners, 736, 16, 64, 26),
            new TextureRegion(uiBanners, 896, 16, 64, 26));
        Collections.addAll(settingsCogTextures, new TextureRegion(generalAssets, 84, 372, 32, 32),
            new TextureRegion(generalAssets, 84, 404, 32, 32),
            new TextureRegion(generalAssets, 84, 404, 32, 32));
        Collections.addAll(marketButtonTextures, new TextureRegion(generalAssets, 116, 372, 32, 32),
            new TextureRegion(generalAssets, 116, 404, 32, 32),
            new TextureRegion(generalAssets, 116, 404, 32, 32));
        Collections.addAll(cemeteryTileset,
            new TextureRegion(cemeteryFloor, 80, 0, 15, 15),
            new TextureRegion(cemeteryTiles, 33, 0, 32, 32));

        //Creating the button objects for the title screen
        marketButton = new Button(205, 20, 80, 80, null, marketButtonTextures);
        settingsCogButton = new Button(45, 20, 80, 80, null, settingsCogTextures);
        playButton = new Button(45, 360, 240, 80, "PLAY", basicButtonTextures);

        //Adding the buttons to the button arraylist
        Collections.addAll(titleScreenButtons, marketButton, settingsCogButton, playButton);

        //Creating the game assets
        room = new Room(32, 20, 15, starterRoom, cemeteryTileset);
        player = new Player(100, 100, 200, 32, 32);
        world = new GameWorld(room, player);
        entities = new ArrayList<Entity>();
        UI = new UserInterface(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, world, shapeRenderer, entities, spriteBatch);

        //Adding all enemies, bullets, and the player to the entity list (aka render list)
        entities.add(player);
        entities.addAll(world.getEnemies());
        entities.addAll(world.getBullets());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        if (titleScreen) {
            spriteBatch.begin();
            spriteBatch.draw(titleScreenBackgroundTex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            spriteBatch.draw(uiBannerRegion, 320, 20, 256, 420);
            spriteBatch.draw(uiBannerRegion, 45, 105, 240, 230);
            spriteBatch.draw(flagBannerRegion, 65, 300, 200, 60);
            spriteBatch.draw(flagBannerRegion, 340, 400, 220, 60);
            for(Button b : titleScreenButtons) {
                if(b.isHovered(viewport)) {
                    b.setCurrTextureIndex(1);
                }
                else {
                    b.setCurrTextureIndex(0);
                }

                b.drawButton(spriteBatch);
            }

            if(playButton.isClicked(viewport)) {
                playButton.setCurrTextureIndex(2);

                titleScreen = false;
            }
            font.draw(spriteBatch, "STATS", 115, 338, 100, Align.center, true);
            font.draw(spriteBatch, "CLASS", 395, 438, 100, Align.center, true);
            spriteBatch.end();
        }
        else {
            float delta = Gdx.graphics.getDeltaTime();
            world.update(delta);
            UI.drawQueue();
        }


    }

    @Override
    public void dispose() {
        UI.dispose();
        shapeRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();
        upgradeCardTex.dispose();
        uiBanners.dispose();
        titleScreenBackgroundTex.dispose();
        generalAssets.dispose();
    }
    public void drawTitleScreen() {
        titleScreen = true;
        player.dispose();
    }

}
