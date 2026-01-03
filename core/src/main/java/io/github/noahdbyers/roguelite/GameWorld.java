package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Random;

public class GameWorld {
    private Shrine shrine;
    private boolean shrineOpen = false;
    private float shrineInteractCooldown = 0f;
    public Shrine getShrine() { return shrine; }
    public boolean isShrineOpen() { return shrineOpen; }
    public void closeShrine() { shrineOpen = false; }
    private static final float SHRINE_INTERACT_RADIUS = 60f;
    private static final int SHRINE_UPGRADE_COST = 3;



    // -------------------- Door system --------------------
    public interface DoorListener {
        void onDoorUsed(Dir dir);
    }
    public interface RoomClearListener { void onRoomCleared(); }
    private RoomClearListener roomClearListener;
    public void setRoomClearListener(RoomClearListener l){ roomClearListener = l; }

    private DoorListener doorListener;
    private float doorCooldown = 0f;
    private static final float DOOR_COOLDOWN_TIME = 0.25f;
    private static final int DOOR_EDGE_BAND = 2;   // doors are usually within 0..1 or H-2..H-1
    private static final int DOOR_SCAN_RADIUS = 2; // tiles around player to scan
    private static final float DOOR_INTERACT_PAD = 10f; // expand door tile for easier interaction

    // Derived from the room's door grid (room.isDoor(x,y))
    private final ArrayList<Doorway> doorways = new ArrayList<>();
    private final Rectangle tmpRectA = new Rectangle();
    private final Rectangle tmpRectB = new Rectangle();

    // -------------------- Core state --------------------
    private Room room;
    private final SpriteBatch spriteBatch;
    private Player player;

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private boolean gameOver = false;

    private int enemiesKilled = 0;
    private int coins = 0;
    private int souls = 0;

    private int wave = 1;
    private boolean waveActive = false;

    private boolean choosingUpgrade = false;
    private final Upgrade[] offeredUpgrades = new Upgrade[3];

    // Aim point in WORLD coordinates (set from Main every frame)
    private float aimWorldX = 0f;
    private float aimWorldY = 0f;

    // Difficulty/spawn (left as-is)
    private float spawnTimer = 0f;
    private float difficultyTimer = 0f;

    private final float startSpawnInterval = 1.0f;
    private final float startMinSpawnInterval = 0.5f;
    private final int startMaxEnemies = 20;

    private float spawnInterval = startSpawnInterval;
    private float minSpawnInterval = startMinSpawnInterval;
    private int maxEnemies = startMaxEnemies;

    // Current Weapon
    private Weapon weapon;

    // Damage popups
    private final ArrayList<DamagePopup> damagePopups = new ArrayList<>();

    // Melee hitboxes
    private final ArrayList<AttackHitbox> meleeHitboxes = new ArrayList<>();
    private final IdentityHashMap<AttackHitbox, HashSet<Enemy>> hitboxHits = new IdentityHashMap<>();

    // -------------------- Freeze Frames (Hit Stop) --------------------
    private float freezeTimer = 0f;
    private final float FREEZE_DURATION = 0.08f;

    private final IdentityHashMap<AttackHitbox, Boolean> hitboxFreezeUsed = new IdentityHashMap<>();
    private final IdentityHashMap<AttackHitbox, Boolean> hitboxHitSfxUsed = new IdentityHashMap<>();
    private final IdentityHashMap<AttackHitbox, Boolean> hitboxShakeUsed = new IdentityHashMap<>();

    // Upgrade animations (kept)
    private final Texture healthUpgradeSheet = Utility.loadNearest("ui/healthUpgrade.png");
    private final Texture damageUpgradeSheet = Utility.loadNearest("ui/damageUpgrade.png");
    private final Texture fireRateUpgradeSheet = Utility.loadNearest("ui/fireRateUpgrade.png");
    private final Texture movementUpgradeSheet = Utility.loadNearest("ui/movementUpgrade.png");

    private final ArrayList<TextureRegion> healthUpgrade = new ArrayList<>();
    private final ArrayList<TextureRegion> damageUpgrade = new ArrayList<>();
    private final ArrayList<TextureRegion> fireRateUpgrade = new ArrayList<>();
    private final ArrayList<TextureRegion> movementUpgrade = new ArrayList<>();

    // Screen shake callback
    public interface ScreenShake {
        void addShake(float intensity, float duration);
    }
    private ScreenShake shake;

    private static final float HIT_SHAKE_INTENSITY = 6f;
    private static final float HIT_SHAKE_DURATION  = 0.12f;

    private AudioManager audio;

    private final Texture cardTexture = Utility.loadNearest("ui/upgrade_card.png");
    private final Random rng = new Random();
    //Enemy Drops
    private final ArrayList<Drop> drops = new ArrayList<>();
    public ArrayList<Drop> getDrops() { return drops; }


    // -------------------- Collision rules --------------------
    private static final int COLLISION_SOLID = 76;

    public GameWorld(Room room, Player player, SpriteBatch spriteBatch) {
        this.room = room;
        this.player = player;
        this.spriteBatch = spriteBatch;

        // Build upgrade animations (your original)
        Collections.addAll(healthUpgrade,
            new TextureRegion(healthUpgradeSheet, 0, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 64, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 128, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 196, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 256, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 320, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 384, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 448, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 512, 0, 64, 64),
            new TextureRegion(healthUpgradeSheet, 576, 0, 64, 64)
        );

        Collections.addAll(damageUpgrade,
            new TextureRegion(damageUpgradeSheet, 0, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 64, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 128, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 196, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 256, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 320, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 384, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 448, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 512, 0, 64, 64),
            new TextureRegion(damageUpgradeSheet, 576, 0, 64, 64)
        );

        Collections.addAll(fireRateUpgrade,
            new TextureRegion(fireRateUpgradeSheet, 0, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 64, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 128, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 196, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 256, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 320, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 384, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 448, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 512, 0, 64, 64),
            new TextureRegion(fireRateUpgradeSheet, 576, 0, 64, 64)
        );

        Collections.addAll(movementUpgrade,
            new TextureRegion(movementUpgradeSheet, 0, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 64, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 128, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 196, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 256, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 320, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 384, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 448, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 512, 0, 64, 64),
            new TextureRegion(movementUpgradeSheet, 576, 0, 64, 64)
        );

        restart();
        rebuildDoorways(); // ✅ build door triggers from room's door grid
    }

    // -------------------- Getters / setters --------------------
    public Room getRoom() { return room; }
    public Player getPlayer() { return player; }
    public ArrayList<Enemy> getEnemies() { return enemies; }

    public boolean isGameOver() { return gameOver; }
    public boolean isChoosingUpgrade() { return choosingUpgrade; }

    public int getEnemiesKilled() { return enemiesKilled; }
    public int getWave() { return wave; }
    public int getCoins() { return coins; }
    public int getSouls() { return souls; }

    public Upgrade[] getOfferedUpgrades() { return offeredUpgrades; }
    public ArrayList<DamagePopup> getDamagePopups() { return damagePopups; }

    public float getAimWorldX() { return aimWorldX; }
    public float getAimWorldY() { return aimWorldY; }

    public ArrayList<AttackHitbox> getMeleeHitboxes() { return meleeHitboxes; }

    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
        if (this.weapon != null) this.weapon.setAttackCooldown(0f);
    }
    public Weapon getWeapon() { return weapon; }

    public void setAudio(AudioManager audio) { this.audio = audio; }

    public void setScreenShake(ScreenShake shake) { this.shake = shake; }

    public void setDoorListener(DoorListener l) { this.doorListener = l; }

    public void setAimWorld(float x, float y) {
        aimWorldX = x;
        aimWorldY = y;
    }

    public Vector2 getAimWorld() {
        return new Vector2(aimWorldX, aimWorldY);
    }

    // -------------------- Main update loop --------------------
    public void update(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart();
            return;
        }
        if (gameOver) return;

        // Door cooldown decays even during freeze/upgrade (prevents stuck)
        doorCooldown = Math.max(0f, doorCooldown - delta);

// If player presses E at shrine, consume interaction and DON'T also use a door.
        if (!choosingUpgrade) {
            if (tryUseShrine()) {
                return; // we opened menu; stop this frame so no other interactions happen
            }
        }

        if (doorCooldown <= 0f) checkDoorUse();

        if (freezeTimer > 0f) {
            freezeTimer -= delta;
            if (freezeTimer < 0f) freezeTimer = 0f;
            return;
        }
        if (choosingUpgrade) {
            if (player != null) player.setAnimationPaused(true);
            handleUpgradeInput();
            return;
        } else {
            if (player != null) player.setAnimationPaused(false);
        }

        if (player == null) {
            ensurePlayer();
            if (player == null) return;
        }

        // Player + enemies
        if (room != null) {
            player.update(room, room.getTileSize());
        }
        player.updateTimers(delta);
        if (weapon != null) weapon.updateTimers(delta);

        for (Enemy e : enemies) {
            if (e == null) continue;
            e.update(player, room, room.getTileSize());
        }

        updateDropPickups();
        handlePlayerEnemyContact();
        updateMeleeHitboxes(delta);

        // Attack input (kept as you had it)
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && weapon != null) {
            Vector2 aimDir = getAimDirection();
            facePlayerToward(aimDir);

            // NOTE: keep your semantics as-is
            if (weapon.isOnCooldown()) {
                player.startDash(aimDir.x, aimDir.y);
                weapon.startAttack(delta);
                if (audio != null) audio.playSwordHit();

                player.startAttackLock(0.25f);
                performMeleeAttack(getAimWorld());
            }
        }

        // Damage popups
        for (int i = damagePopups.size() - 1; i >= 0; i--) {
            DamagePopup p = damagePopups.get(i);
            p.update(delta);
            if (p.isDead()) damagePopups.remove(i);
        }
        shrineInteractCooldown = Math.max(0f, shrineInteractCooldown - delta);
        if (shrine != null && shrineInteractCooldown <= 0f) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.E) && playerNearShrine()) {
                shrineOpen = !shrineOpen;
                shrineInteractCooldown = 0.2f;
            }
        }

        // Waves
        if (waveActive && enemies.isEmpty()) {
            waveActive = false;
            wave++;
            if (roomClearListener != null) roomClearListener.onRoomCleared();
            maybeSpawnShrine(); // new
        }

        if (player.getHealth() <= 0) gameOver = true;
    }

    // -------------------- Doors: derived from room door grid --------------------

    /**
     * Rebuild cached Doorway list by scanning room.isDoor(x,y).
     * This supports rooms where doors are in different tile positions.
     */
    private void rebuildDoorways() {
        doorways.clear();
        if (room == null) return;

        int w = room.getRoomWidth();
        int h = room.getRoomHeight();

        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                if (!isDoorSafe(tx, ty)) continue;

                Dir dir = inferDoorDirFromTile(tx, ty, w, h);
                if (dir == null) continue;

                doorways.add(new Doorway(dir, tx, ty));
            }
        }
    }

    private boolean isDoorSafe(int tx, int ty) {
        try {
            return room.isDoor(tx, ty);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Infers which side the door belongs to.
     * Works even if a door tile is 1 tile in from the edge (common in some layouts).
     */
    private static Dir inferDoorDirFromTile(int tx, int ty, int roomW, int roomH) {
        // Strong edge checks first
        if (ty == roomH - 1) return Dir.UP;
        if (ty == 0) return Dir.DOWN;
        if (tx == 0) return Dir.LEFT;
        if (tx == roomW - 1) return Dir.RIGHT;

        // If doors are placed 1 tile in (e.g., y==roomH-2), still treat as that side
        if (ty >= roomH - 2) return Dir.UP;
        if (ty <= 1) return Dir.DOWN;
        if (tx <= 1) return Dir.LEFT;
        if (tx >= roomW - 2) return Dir.RIGHT;

        // Otherwise unknown (door placed weirdly)
        return null;
    }

    private void checkDoorUse() {
        if (player == null || room == null) return;

        // Only when pressing E (not movement keys)
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        Dir dir = findInteractableDoorDir();
        if (dir == null) return;

        triggerDoor(dir);
    }

    private boolean playerOverlapsAnyDoor(Dir want) {
        int ts = room.getTileSize();

        // Player rect
        tmpRectA.set(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        for (int i = 0; i < doorways.size(); i++) {
            Doorway d = doorways.get(i);
            if (d.getDir() != want) continue;

            // Door tile rect in world coords
            float dx = d.getTileX() * ts;
            float dy = d.getTileY() * ts;

            // Slightly "fatter" trigger makes it feel better (optional)
            tmpRectB.set(dx, dy, ts, ts);

            if (tmpRectA.overlaps(tmpRectB)) return true;
        }
        return false;
    }

    private void triggerDoor(Dir dir) {
        doorCooldown = DOOR_COOLDOWN_TIME;
        if (doorListener != null) doorListener.onDoorUsed(dir);
    }

    /**
     * Optional helper for Main when placing player in the next room:
     * returns the "best" doorway tile for a side (first found).
     */
    public Doorway findDoorway(Dir side) {
        if (room == null) return null;
        if (doorways.isEmpty()) rebuildDoorways();

        for (Doorway d : doorways) {
            if (d.getDir() == side) return d;
        }
        return null;
    }

    // Call this when Main changes rooms
    public void setRoom(Room newRoom) {
        this.room = newRoom;

        // Door system must rebuild because door layout differs per-room
        rebuildDoorways();

        // Clear per-room visuals/combat artifacts
        meleeHitboxes.clear();
        damagePopups.clear();

        // If you want enemies to persist across rooms later, remove this:
        enemies.clear();
        waveActive = false;
    }

    // -------------------- Restart --------------------
    public void restart() {
        gameOver = false;

        ensurePlayerFresh();

        enemies.clear();
        meleeHitboxes.clear();

        hitboxHits.clear();
        hitboxFreezeUsed.clear();
        hitboxHitSfxUsed.clear();
        hitboxShakeUsed.clear();

        freezeTimer = 0f;

        spawnTimer = 0f;
        difficultyTimer = 0f;
        spawnInterval = startSpawnInterval;
        minSpawnInterval = startMinSpawnInterval;
        maxEnemies = startMaxEnemies;

        enemiesKilled = 0;
        wave = 1;
        waveActive = false;

        choosingUpgrade = false;
        clearOfferedUpgrades();

        doorCooldown = 0f;
        rebuildDoorways();
    }

    private void ensurePlayer() {
        if (player != null) return;
        player = new Player(100, 250, 140f, 24f, 24f);
    }

    private void ensurePlayerFresh() {
        if (player == null) {
            ensurePlayer();
            return;
        }
        float w = player.getWidth();
        float h = player.getHeight();
        float spd = player.getSpeed();
        player = new Player(100, 250, spd, w, h);
    }

    // -------------------- Waves / spawning --------------------
    private void startWave() {
        enemies.clear();

        int toSpawn = 25 + wave;
        float baseSpeed = 60f + wave * 8f;

        for (int i = 0; i < toSpawn; i++) {
            spawnZombieWithSpeed(baseSpeed);
        }

        waveActive = true;
    }

    private void spawnZombieWithSpeed(float speed) {
        if (enemies.size() >= maxEnemies) return;
        if (player == null) ensurePlayer();
        if (room == null) return;

        final int tileSize = room.getTileSize();
        final float roomPixelW = room.getRoomWidth() * tileSize;
        final float roomPixelH = room.getRoomHeight() * tileSize;

        final float hbW = 28f;
        final float hbH = 58f;

        final float minDist = 120f;
        final float minDist2 = minDist * minDist;

        final float padding = 2f;

        for (int tries = 0; tries < 400; tries++) {
            float x = rng.nextFloat() * (roomPixelW - hbW);
            float y = rng.nextFloat() * (roomPixelH - hbH);

            float px = player.getX() + player.getWidth() * 0.5f;
            float py = player.getY() + player.getHeight() * 0.5f;
            float ex = x + hbW * 0.5f;
            float ey = y + hbH * 0.5f;

            float dx = ex - px;
            float dy = ey - py;
            if (dx * dx + dy * dy < minDist2) continue;

            if (rectHitsCollision(room, x + padding, y + padding, hbW - padding * 2f, hbH - padding * 2f)) {
                continue;
            }

            enemies.add(new Zombie(x, y, speed, hbW, hbH, 3));
            return;
        }

        float[] open = findFirstOpenSpotRect(hbW, hbH, minDist);
        if (open != null) {
            if (!rectHitsCollision(room, open[0], open[1], hbW, hbH)) {
                enemies.add(new Zombie(open[0], open[1], speed, hbW, hbH, 3));
            }
        }
    }

    private boolean rectHitsCollision(Room room, float x, float y, float w, float h) {
        int[][] col;
        try { col = room.getCollisions(); }
        catch (Throwable t) { col = null; }

        if (col == null) return false;

        final int tileSize = room.getTileSize();
        final int roomW = room.getRoomWidth();
        final int roomH = room.getRoomHeight();

        int left   = (int)Math.floor(x / tileSize);
        int right  = (int)Math.floor((x + w - 1f) / tileSize);
        int bottom = (int)Math.floor(y / tileSize);
        int top    = (int)Math.floor((y + h - 1f) / tileSize);

        left   = clamp(left,   0, roomW - 1);
        right  = clamp(right,  0, roomW - 1);
        bottom = clamp(bottom, 0, roomH - 1);
        top    = clamp(top,    0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            int cy = (roomH - 1) - ty; // match your flip
            for (int tx = left; tx <= right; tx++) {
                if (col[cy][tx] == COLLISION_SOLID) return true;
            }
        }
        return false;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private float[] findFirstOpenSpotRect(float w, float h, float minDistFromPlayer) {
        int tileSize = room.getTileSize();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        for (int ty = 1; ty < roomH - 1; ty++) {
            for (int tx = 1; tx < roomW - 1; tx++) {
                float x = tx * tileSize + (tileSize - w) / 2f;
                float y = ty * tileSize + (tileSize - h) / 2f;

                if (rectHitsCollision(room, x, y, w, h)) continue;

                float ex = x + w / 2f;
                float ey = y + h / 2f;
                float dx = ex - px;
                float dy = ey - py;

                if (dx * dx + dy * dy < minDistFromPlayer * minDistFromPlayer) continue;

                return new float[]{x, y};
            }
        }
        return null;
    }

    // -------------------- Combat / upgrades --------------------
    private boolean overlaps(float ax, float ay, float aw, float ah,
                             float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void handlePlayerEnemyContact() {
        if (player == null) return;

        for (Enemy enemy : enemies) {
            if (enemy == null) continue;

            boolean hit = overlaps(
                player.getX(), player.getY(), player.getWidth(), player.getHeight(),
                enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight()
            );

            if (hit && !player.isInvulnerable()) {
                player.takeDamage(1);
            }
        }
    }

    private void beginUpgradeChoice() {
        choosingUpgrade = true;
        generateOfferedUpgrades();
    }

    private void handleUpgradeInput() {
        // UI calls chooseUpgrade(index) on click; keep input here empty if desired
    }

    private void generateOfferedUpgrades() {
        offeredUpgrades[0] = randomUpgrade();
        offeredUpgrades[1] = randomUpgrade();
        offeredUpgrades[2] = randomUpgrade();
    }

    private void clearOfferedUpgrades() {
        offeredUpgrades[0] = null;
        offeredUpgrades[1] = null;
        offeredUpgrades[2] = null;
    }

    private Upgrade randomUpgrade() {
        int r = rng.nextInt(4);
        if (r == 0) return new Upgrade("Rapid Fire", "Fire rate +20%", fireRateUpgrade);
        if (r == 1) return new Upgrade("Runner", "Move speed +15%", movementUpgrade);
        if (r == 2) return new Upgrade("Vitality", "Max HP +1 and heal 1", healthUpgrade);
        return new Upgrade("Extra Damage", "Damage +1", damageUpgrade);
    }

    public void chooseUpgrade(int index) {
        if (!choosingUpgrade) return;
        if (index < 0 || index >= offeredUpgrades.length) return;
        applyUpgrade(offeredUpgrades[index]);
    }

    private void applyUpgrade(Upgrade u) {
        if (u == null || player == null) return;

        if (u.name.equals("Rapid Fire")) {
            if (weapon != null) weapon.setAttackCooldownTime(Math.max(0.05f, weapon.getAttackCooldownTime() * 0.8f));
        } else if (u.name.equals("Runner")) {
            player.setSpeed(player.getSpeed() * 1.15f);
        } else if (u.name.equals("Vitality")) {
            player.increaseMaxHealth(1);
            player.heal(1);
        } else if (u.name.equals("Extra Damage")) {
            if (weapon != null) weapon.setDamage(weapon.getDamage() + 1);
        }

        choosingUpgrade = false;
        clearOfferedUpgrades();
    }

    private Vector2 getAimDirection() {
        Vector2 mouse = new Vector2(aimWorldX, aimWorldY);

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        return mouse.sub(px, py);
    }

    private void facePlayerToward(Vector2 dir) {
        if (Math.abs(dir.x) > Math.abs(dir.y)) {
            player.setFacing(dir.x > 0 ? Player.Facing.RIGHT : Player.Facing.LEFT);
        } else {
            player.setFacing(dir.y > 0 ? Player.Facing.UP : Player.Facing.DOWN);
        }
    }

    private void performMeleeAttack(Vector2 mouseWorld) {
        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;

        Vector2 dir = new Vector2(mouseWorld.x - px, mouseWorld.y - py);
        if (dir.len2() < 0.0001f) dir.set(1, 0);
        dir.nor();

        float reach = 24f;
        float hitW = 64f;
        float hitH = 64f;
        float duration = 0.08f;

        int damage = (weapon != null) ? weapon.getDamage() : 1;

        AttackHitbox hb = new AttackHitbox(hitW, hitH, dir, reach, duration, damage, px, py);
        meleeHitboxes.add(hb);
        hitboxHits.put(hb, new HashSet<>());

        hitboxFreezeUsed.put(hb, false);
        hitboxHitSfxUsed.put(hb, false);
        hitboxShakeUsed.put(hb, false);
    }

    private void updateMeleeHitboxes(float delta) {
        if (player == null) return;

        float pcx = player.getX() + player.getWidth() * 0.5f;
        float pcy = player.getY() + player.getHeight() * 0.5f;

        for (int i = meleeHitboxes.size() - 1; i >= 0; i--) {
            AttackHitbox hb = meleeHitboxes.get(i);
            hb.update(delta, pcx, pcy);

            if (hb.isExpired()) {
                meleeHitboxes.remove(i);
                hitboxHits.remove(hb);
                hitboxFreezeUsed.remove(hb);
                hitboxHitSfxUsed.remove(hb);
                hitboxShakeUsed.remove(hb);
                continue;
            }

            HashSet<Enemy> alreadyHit = hitboxHits.get(hb);
            if (alreadyHit == null) {
                alreadyHit = new HashSet<>();
                hitboxHits.put(hb, alreadyHit);
            }

            boolean freezeUsed = Boolean.TRUE.equals(hitboxFreezeUsed.get(hb));
            boolean sfxUsed = Boolean.TRUE.equals(hitboxHitSfxUsed.get(hb));
            boolean shakeUsed = Boolean.TRUE.equals(hitboxShakeUsed.get(hb));

            for (int e = enemies.size() - 1; e >= 0; e--) {
                Enemy enemy = enemies.get(e);
                if (enemy == null) continue;
                if (alreadyHit.contains(enemy)) continue;

                if (overlaps(
                    hb.rect.x, hb.rect.y, hb.rect.width, hb.rect.height,
                    enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight()
                )) {
                    enemy.takeDamage(hb.damage);
                    alreadyHit.add(enemy);

                    float popX = enemy.getX() + enemy.getWidth() * 0.5f;
                    float popY = enemy.getY() + enemy.getHeight() + 10f;
                    damagePopups.add(new DamagePopup(popX, popY, hb.damage));

                    enemy.takeKnockback(hb.dir.x, hb.dir.y, 400f);

                    if (enemy.isDead()) {
                        // spawn a drop at enemy center
                        float dx = enemy.getX() + enemy.getWidth() * 0.5f;
                        float dy = enemy.getY() + enemy.getHeight() * 0.5f;
                        drops.add(new Drop(dx - 6f, dy - 6f, 1)); // value=1 for now

                        enemies.remove(e);
                        enemiesKilled++;
                    }

                    if (!sfxUsed) {
                        if (audio != null) audio.playHit();
                        hitboxHitSfxUsed.put(hb, true);
                    }

                    if (!freezeUsed) {
                        freezeTimer = FREEZE_DURATION;
                        hitboxFreezeUsed.put(hb, true);
                    }

                    if (!shakeUsed) {
                        if (shake != null) shake.addShake(HIT_SHAKE_INTENSITY, HIT_SHAKE_DURATION);
                        hitboxShakeUsed.put(hb, true);
                    }
                }
            }
        }
    }

    private Dir findInteractableDoorDir() {
        int ts = room.getTileSize();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        // Player rect (world coords)
        float px = player.getX();
        float py = player.getY();
        float pw = player.getWidth();
        float ph = player.getHeight();

        // Player center -> tile (world tile coords)
        float pcx = px + pw * 0.5f;
        float pcy = py + ph * 0.5f;

        int pTileX = (int)Math.floor(pcx / ts);
        int pTileY = (int)Math.floor(pcy / ts);

        Dir bestDir = null;
        float bestDist2 = Float.MAX_VALUE;

        // Scan a small neighborhood around the player for door tiles
        for (int dy = -DOOR_SCAN_RADIUS; dy <= DOOR_SCAN_RADIUS; dy++) {
            for (int dx = -DOOR_SCAN_RADIUS; dx <= DOOR_SCAN_RADIUS; dx++) {
                int tx = pTileX + dx;
                int tyWorld = pTileY + dy;

                if (tx < 0 || tx >= roomW || tyWorld < 0 || tyWorld >= roomH) continue;

                if (!isDoorWorld(tx, tyWorld, roomH)) continue;

                // Door tile rect expanded a bit (world coords)
                float doorX = tx * ts;
                float doorY = tyWorld * ts;

                float rx = doorX - DOOR_INTERACT_PAD;
                float ry = doorY - DOOR_INTERACT_PAD;
                float rw = ts + DOOR_INTERACT_PAD * 2f;
                float rh = ts + DOOR_INTERACT_PAD * 2f;

                if (!overlaps(px, py, pw, ph, rx, ry, rw, rh)) continue;

                Dir dir = dirFromDoorTile(tx, tyWorld, roomW, roomH);
                if (dir == null) continue;

                // pick closest door tile to player center
                float cx = doorX + ts * 0.5f;
                float cy = doorY + ts * 0.5f;
                float ddx = cx - pcx;
                float ddy = cy - pcy;
                float d2 = ddx * ddx + ddy * ddy;

                if (d2 < bestDist2) {
                    bestDist2 = d2;
                    bestDir = dir;
                }
            }
        }

        return bestDir;
    }

    private boolean isDoorWorld(int tx, int tyWorld, int roomH) {
        int tyData = (roomH - 1) - tyWorld;
        try {
            return room.isDoor(tx, tyData);
        } catch (Throwable t) {
            return false;
        }
    }

    private Dir dirFromDoorTile(int tx, int tyWorld, int roomW, int roomH) {
        if (tyWorld >= roomH - DOOR_EDGE_BAND) return Dir.UP;
        if (tyWorld < DOOR_EDGE_BAND) return Dir.DOWN;
        if (tx < DOOR_EDGE_BAND) return Dir.LEFT;
        if (tx >= roomW - DOOR_EDGE_BAND) return Dir.RIGHT;
        return null; // door tile not near an edge (unexpected)
    }

    public void onEnterRoom(boolean shouldStartWave) {
        // clear transient stuff
        enemies.clear();
        meleeHitboxes.clear();
        damagePopups.clear();
        drops.clear();          // new
        shrine = null;          // new
        shrineOpen = false;     // new

        waveActive = false;

        if (shouldStartWave) {
            startWave();
        }
    }

    private void updateDropPickups() {
        if (player == null) return;

        float px = player.getX(), py = player.getY();
        float pw = player.getWidth(), ph = player.getHeight();

        for (int i = drops.size() - 1; i >= 0; i--) {
            Drop d = drops.get(i);
            if (d == null) continue;

            if (overlaps(px, py, pw, ph, d.x, d.y, d.w, d.h)) {
                souls += d.value;          // or coins
                drops.remove(i);
                //if (audio != null) audio.playPickup(); // optional
            }
        }
    }

    private void maybeSpawnShrine() {
        if (rng.nextFloat() > 0.9f) return; // 25% chance

        // find open spot (you already have open-spot helpers)
        float[] p = findFirstOpenSpotRect(32f, 32f, 0f);
        if (p == null) return;

        Upgrade[] stock = new Upgrade[] { randomUpgrade(), randomUpgrade(), randomUpgrade() };
        shrine = new Shrine(p[0], p[1], stock);
    }

    private boolean playerNearShrine() {
        if (player == null || shrine == null) return false;
        float px = player.getX(), py = player.getY();
        float pw = player.getWidth(), ph = player.getHeight();
        // small interaction padding
        return overlaps(px, py, pw, ph, shrine.x - 8f, shrine.y - 8f, shrine.w + 16f, shrine.h + 16f);
    }

    public void buyShrineUpgrade(int index) {
        if (!shrineOpen || shrine == null) return;
        if (index < 0 || index >= shrine.stock.length) return;

        Upgrade u = shrine.stock[index];
        if (u == null) return;

        int cost = costFor(u);
        if (souls < cost) return;

        souls -= cost;
        applyUpgrade(u);
        shrine.stock[index] = null; // remove purchased
    }

    private int costFor(Upgrade u) { return 3; } // e.g., 3 souls each

    public void setShrine(Shrine shrine) { this.shrine = shrine; }

    public boolean isPlayerNearShrine() {
        if (player == null || shrine == null) return false;

        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;

        float sx = shrine.x + shrine.w * 0.5f;
        float sy = shrine.y + shrine.h * 0.5f;

        float dx = sx - px;
        float dy = sy - py;
        return (dx * dx + dy * dy) <= (SHRINE_INTERACT_RADIUS * SHRINE_INTERACT_RADIUS);
    }

    private boolean tryUseShrine() {
        if (player == null || shrine == null) return false;
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return false;
        if (!isPlayerNearShrine()) return false;

        // Open your existing upgrade menu (now it's "shrine shop")
        beginUpgradeChoice();
        return true; // IMPORTANT: so E doesn’t also trigger door usage this frame
    }

    public void dispose() {
        cardTexture.dispose();

        healthUpgradeSheet.dispose();
        damageUpgradeSheet.dispose();
        fireRateUpgradeSheet.dispose();
        movementUpgradeSheet.dispose();
    }
}
