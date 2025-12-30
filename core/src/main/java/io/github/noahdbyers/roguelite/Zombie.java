package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.Collections;

public class Zombie extends Enemy {

    // ----------------------------
    // Shared sprite sheet (loaded once)
    // ----------------------------
    private static Texture zombieRunSheet;              // shared GPU texture
    private static boolean framesBuilt = false;

    private static final ArrayList<TextureRegion> downRunFrames  = new ArrayList<>();
    private static final ArrayList<TextureRegion> upRunFrames    = new ArrayList<>();
    private static final ArrayList<TextureRegion> rightRunFrames = new ArrayList<>();
    private static final ArrayList<TextureRegion> leftRunFrames  = new ArrayList<>();

    // ----------------------------
    // Animation (per zombie)
    // ----------------------------
    private float animTimer = 0f;
    private static final float FRAME_TIME = 0.20f;
    private int frameIndex = 0;

    public Zombie(float x, float y, float speed, float size, int health) {
        super(x, y, speed, size, health);

        // Load/build frames once for all zombies
        ensureAssetsLoaded();
    }

    private static void ensureAssetsLoaded() {
        if (zombieRunSheet == null) {
            zombieRunSheet = new Texture("Zombie/Walk.png");
        }
        if (!framesBuilt) {
            buildFrames();
            framesBuilt = true;
        }
    }

    /**
     * Build the directional frame lists exactly once.
     * Adjust these numbers if your sheet layout differs.
     */
    private static void buildFrames() {
        downRunFrames.clear();
        upRunFrames.clear();
        rightRunFrames.clear();
        leftRunFrames.clear();

        final int fw = 12;
        final int fh = 26;

        final int xStart = 9;   // x of frame 0
        final int xStep  = 20;  // distance between frames

        final int downY  = 5;
        final int upY    = 37;
        final int rightY = 69;
        final int leftY  = 101;

        final int frameCount = 10;

        Collections.addAll(downRunFrames,
            new TextureRegion(zombieRunSheet, 9, 5, 16, fh),
            new TextureRegion(zombieRunSheet, 40, 5, 16, fh),
            new TextureRegion(zombieRunSheet, 72, 5, 16, fh),
            new TextureRegion(zombieRunSheet, 105, 5, 16, fh));
        Collections.addAll(upRunFrames,
            new TextureRegion(zombieRunSheet, 10, 37, 16, fh),
            new TextureRegion(zombieRunSheet, 42, 37, 16, fh),
            new TextureRegion(zombieRunSheet, 74, 37, 16, fh),
            new TextureRegion(zombieRunSheet, 106, 37, 16, fh));
        Collections.addAll(rightRunFrames,
            new TextureRegion(zombieRunSheet, 11, 70, 16, fh),
            new TextureRegion(zombieRunSheet, 42, 70, 16, fh),
            new TextureRegion(zombieRunSheet, 74, 70, 16, fh),
            new TextureRegion(zombieRunSheet, 107, 70, 16, fh));
        Collections.addAll(leftRunFrames,
            new TextureRegion(zombieRunSheet, 9, 101, 16, fh),
            new TextureRegion(zombieRunSheet, 41, 101, 16, fh),
            new TextureRegion(zombieRunSheet, 72, 101, 16, fh),
            new TextureRegion(zombieRunSheet, 104, 101, 16, fh));
    }

    private ArrayList<TextureRegion> getFrames() {
        Facing f = getFacing();
        if (f == Facing.UP) return upRunFrames;
        if (f == Facing.RIGHT) return rightRunFrames;
        if (f == Facing.LEFT) return leftRunFrames;
        return downRunFrames;
    }

    /**
     * Draw zombie using SpriteBatch.
     * Call only inside spriteBatch.begin() / end().
     */
    public void draw(SpriteBatch spriteBatch, float delta) {
        ArrayList<TextureRegion> frames = getFrames();
        if (frames == null || frames.isEmpty()) return;

        animTimer += delta;
        while (animTimer >= FRAME_TIME) {
            animTimer -= FRAME_TIME;
            frameIndex = (frameIndex + 1) % frames.size();
        }

        TextureRegion frame = frames.get(frameIndex);

        // If you want to enforce consistent on-screen size:
        float drawW = 32f;
        float drawH = 52f;
        spriteBatch.draw(frame, getX(), getY(), drawW, drawH);
    }

    /**
     * Dispose shared texture ONCE, typically when the game closes.
     * Call Zombie.disposeShared() from Main.dispose().
     */
    public static void disposeShared() {
        if (zombieRunSheet != null) {
            zombieRunSheet.dispose();
            zombieRunSheet = null;
        }
        framesBuilt = false;
        downRunFrames.clear();
        upRunFrames.clear();
        rightRunFrames.clear();
        leftRunFrames.clear();
    }
}
