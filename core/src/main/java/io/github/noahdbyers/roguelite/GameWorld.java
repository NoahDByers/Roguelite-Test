package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Random;

public class GameWorld {

    // -------------------- Items --------------------
    private final ItemSystem items = new ItemSystem();

    // Simple pickup toast for UI
    private String toastText = null;
    private float toastTimer = 0f;

    // -------------------- Shrine / shop --------------------
    private Shrine shrine;
    private boolean shrineOpen = false;
    private float shrineInteractCooldown = 0f;

    private static final float SHRINE_INTERACT_RADIUS = 60f;
    private static final int SHRINE_BASE_UPGRADE_COST = 3;


    // -------------------- Chests --------------------
    private static final float CHEST_INTERACT_RADIUS = 60f;

        /** Convenience: current room's chest list (may be empty). */
        public ArrayList<Chest> getChests() {
            if (room == null) return null;
            return room.getChests();
        }

        /** Nearest unopened chest within interact radius (for UI prompts). */
        public Chest getNearestInteractableChest() {
            if (player == null || room == null) return null;
            ArrayList<Chest> cs = room.getChests();
            if (cs == null || cs.isEmpty()) return null;

            float px = player.getX() + player.getWidth() * 0.5f;
            float py = player.getY() + player.getHeight() * 0.5f;

            Chest best = null;
            float bestD2 = CHEST_INTERACT_RADIUS * CHEST_INTERACT_RADIUS;

            for (Chest c : cs) {
                if (c == null || c.opened) continue;
                float cx = c.x + c.w * 0.5f;
                float cy = c.y + c.h * 0.5f;
                float dx = cx - px;
                float dy = cy - py;
                float d2 = dx * dx + dy * dy;
                if (d2 <= bestD2) {
                    bestD2 = d2;
                    best = c;
                }
            }
            return best;
        }

        private boolean tryOpenChest() {
            if (player == null || room == null) return false;
            if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return false;

            Chest c = getNearestInteractableChest();
            if (c == null) return false;

            c.opened = true;

            particles.spawnChestOpen(c.x + c.w * 0.5f, c.y + c.h * 0.5f);


            // Reward: item or souls
            if (c.itemReward != null) {
                boolean got = items.addItem(c.itemReward, this);
                ItemDefinition def = ItemRegistry.get(c.itemReward);
                if (got) {
                    showToast("Picked up: " + (def != null ? def.name : c.itemReward.name()), 2.2f);
                } else {
                    // duplicate unique item -> compensation souls
                    int comp = items.modifySouls(Math.max(2, c.soulReward));
                    souls += comp;
                    showToast("Duplicate item → +" + comp + " souls", 1.8f);
                }
            } else if (c.soulReward > 0) {
                int gained = items.modifySouls(c.soulReward);
                souls += gained;
                items.onSoulsPicked(this, gained);
                showToast("+" + gained + " souls", 1.2f);
            }

            if (audio != null) audio.playUIClick();

            // small cooldown so you can't double-trigger instantly
            shrineInteractCooldown = 0.15f;
            return true;
        }


    public Shrine getShrine() { return shrine; }
    public boolean isShrineOpen() { return shrineOpen; }
    public void closeShrine() {
        shrineOpen = false;
        choosingUpgrade = false;
    }

    // -------------------- Door system --------------------
    public interface DoorListener { void onDoorUsed(Dir dir); }
    public interface RoomClearListener { void onRoomCleared(); }

    private DoorListener doorListener;
    private RoomClearListener roomClearListener;

    public void setDoorListener(DoorListener l) { this.doorListener = l; }
    public void setRoomClearListener(RoomClearListener l){ roomClearListener = l; }

    private float doorCooldown = 0f;
    private static final float DOOR_COOLDOWN_TIME = 0.25f;
    private static final int DOOR_EDGE_BAND = 2;
    private static final int DOOR_SCAN_RADIUS = 2;
    private static final float DOOR_INTERACT_PAD = 10f;

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

    // Kept for UI compatibility (we use this as "shop open")
    private boolean choosingUpgrade = false;
    private final Upgrade[] offeredUpgrades = new Upgrade[3];

    // Aim point in WORLD coordinates (set from Main every frame)
    private float aimWorldX = 0f;
    private float aimWorldY = 0f;

    // Weapon
    private Weapon weapon;

    // Damage popups
    private final ArrayList<DamagePopup> damagePopups = new ArrayList<>();

    // Melee hitboxes
    private final ArrayList<AttackHitbox> meleeHitboxes = new ArrayList<>();
    private final IdentityHashMap<AttackHitbox, HashSet<Enemy>> hitboxHits = new IdentityHashMap<>();

    // ✅ NEW: combat state machine (owns attack execution)
    private final PlayerCombat combat = new PlayerCombat();
    public PlayerCombat getCombat() { return combat; }

    // -------------------- Freeze Frames (Hit Stop) --------------------
    private float freezeTimer = 0f;
    private static final float FREEZE_DURATION = 0.08f;

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
    public interface ScreenShake { void addShake(float intensity, float duration); }
    private ScreenShake shake;

    private static final float HIT_SHAKE_INTENSITY = 6f;
    private static final float HIT_SHAKE_DURATION  = 0.12f;

    private AudioManager audio;

    private final Texture cardTexture = Utility.loadNearest("ui/upgrade_card.png");
    private final Random rng = new Random();

    // -------------------- Particles --------------------
    private final ParticleSystem particles = new ParticleSystem(rng);

    // Enemy Drops
    private final ArrayList<Drop> drops = new ArrayList<>();
    public ArrayList<Drop> getDrops() { return drops; }

    // Collision
    private static final int COLLISION_SOLID = 76;

    public GameWorld(Room room, Player player, SpriteBatch spriteBatch) {
        this.room = room;
        this.player = player;
        this.spriteBatch = spriteBatch;

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
    }

    // -------------------- Getters / setters --------------------
    public Room getRoom() { return room; }
    public Player getPlayer() { return player; }
    public ArrayList<Enemy> getEnemies() { return enemies; }


    public ParticleSystem getParticles() { return particles; }

    public boolean isGameOver() { return gameOver; }
    public boolean isChoosingUpgrade() { return choosingUpgrade; } // UI uses this

    public int getEnemiesKilled() { return enemiesKilled; }
    public int getWave() { return wave; }
    public int getCoins() { return coins; }
    public int getSouls() { return souls; }

    public ItemSystem getItems() { return items; }
    public Random getRng() { return rng; }

    public String getToastText() { return toastText; }
    public float getToastTimer() { return toastTimer; }

    public void showToast(String text, float seconds) {
        if (text == null || text.isEmpty()) return;
        toastText = text;
        toastTimer = Math.max(toastTimer, seconds);
    }

    /** Cost UI helper for shrine purchases (0 if not applicable). */
    public int getUpgradeCost(int index) {
        if (!choosingUpgrade) return 0;
        if (!shrineOpen || shrine == null) return 0;
        if (index < 0 || index >= shrine.stock.length) return 0;
        if (shrine.stock[index] == null) return 0;
        return shrine.getCost(index);
    }

    /** UI helper: whether the player can afford a given shrine upgrade slot. */
    public boolean canAffordUpgrade(int index) {
        int cost = getUpgradeCost(index);
        return cost > 0 && souls >= cost;
    }

    public Upgrade[] getOfferedUpgrades() { return offeredUpgrades; }
    public ArrayList<DamagePopup> getDamagePopups() { return damagePopups; }

    public float getAimWorldX() { return aimWorldX; }
    public float getAimWorldY() { return aimWorldY; }

    public ArrayList<AttackHitbox> getMeleeHitboxes() { return meleeHitboxes; }

    public void setWeapon(Weapon weapon) { this.weapon = weapon; }
    public Weapon getWeapon() { return weapon; }

    public void setAudio(AudioManager audio) { this.audio = audio; }
    public void setScreenShake(ScreenShake shake) { this.shake = shake; }

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

        // Particles tick even during hitstop/menus/game over.
        particles.update(delta);

        if (gameOver) return;

        // Cooldowns tick regardless
        doorCooldown = Math.max(0f, doorCooldown - delta);
        shrineInteractCooldown = Math.max(0f, shrineInteractCooldown - delta);

        // Freeze stop (hitstop). We still allow buffering dash/attack during hitstop.
        if (freezeTimer > 0f) {
            // Allow snappy inputs even during hitstop; actual movement/attack advances once freeze ends.
            if (player != null && !choosingUpgrade) {
                handleDashInput();
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    combat.bufferAttack();
                    facePlayerToward(getAimDirection());
                }
            }

            freezeTimer -= delta;
            if (freezeTimer < 0f) freezeTimer = 0f;
            return;
        }

        // ----------------------------
        // SHOP STATE (blocks gameplay)
        // ----------------------------
        if (choosingUpgrade) {
            if (player != null) player.setAnimationPaused(true);

            // Close shop
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                closeShopMenu();
            }
            return; // IMPORTANT: while menu open, do nothing else
        }

        // Not in shop
        if (player != null) player.setAnimationPaused(false);

        // ----------------------------
        // Interactions (E)
        // shrine has priority over doors
        // ----------------------------
        if (tryUseShrine()) {
            // opened shop this frame
            if (player != null) player.setAnimationPaused(true);
            return;
        }

        // Chest interaction (E)
        if (tryOpenChest()) {
            return;
        }

        if (doorCooldown <= 0f) {
            checkDoorUse();
        }

        // Ensure player exists
        if (player == null) {
            player = new Player(300, 300, 170, 32, 32);
            return;
        }

        // Apply item-derived defense each frame (covers pickups mid-room)
        player.setDamageTakenMultiplier(items.getDamageTakenMultiplier());

        // Toast timer
        if (toastTimer > 0f) {
            toastTimer -= delta;
            if (toastTimer <= 0f) {
                toastTimer = 0f;
                toastText = null;
            }
        }

        // Dead Cells-like: roll/dash input (processed before movement so it happens this frame)
        handleDashInput();

        // Movement + timers
        if (room != null) player.update(room, room.getTileSize());
        player.updateTimers(delta);

        if (weapon != null) weapon.updateTimers(delta);

        for (Enemy e : enemies) {
            if (e == null) continue;
            e.update(player, room, room.getTileSize());
            // Global ticks (Zombie doesn't tick these itself)
            e.tickFlash(delta);
            e.tickStatus(delta);
        }
        // Remove enemies that died to DoTs / non-hitbox effects.
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e != null && e.isDead()) {
                handleEnemyDeath(i, e, null);
            }
        }



        updateDropPickups();

        handlePlayerEnemyContact();
        updateMeleeHitboxes(delta);

        // Combat system owns attacks
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            combat.bufferAttack();
            facePlayerToward(getAimDirection());
        }
        combat.update(delta, this);

        // Damage popups
        for (int i = damagePopups.size() - 1; i >= 0; i--) {
            DamagePopup p = damagePopups.get(i);
            p.update(delta);
            if (p.isDead()) damagePopups.remove(i);
        }

        // Room cleared
        if (waveActive && enemies.isEmpty()) {
            waveActive = false;
            wave++;
            items.onWaveCleared(this);
            if (roomClearListener != null) roomClearListener.onRoomCleared();
            maybeSpawnShrine();
        }

        if (player.getHealth() <= 0) gameOver = true;
    }


    // -------------------- Doors --------------------
    private void checkDoorUse() {
        if (player == null || room == null) return;
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        Dir dir = findInteractableDoorDir();
        if (dir == null) return;

        doorCooldown = DOOR_COOLDOWN_TIME;
        if (doorListener != null) doorListener.onDoorUsed(dir);
    }

    private Dir findInteractableDoorDir() {
        int ts = room.getTileSize();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        float px = player.getX();
        float py = player.getY();
        float pw = player.getWidth();
        float ph = player.getHeight();

        float pcx = px + pw * 0.5f;
        float pcy = py + ph * 0.5f;

        int pTileX = (int)Math.floor(pcx / ts);
        int pTileY = (int)Math.floor(pcy / ts);

        Dir bestDir = null;
        float bestDist2 = Float.MAX_VALUE;

        for (int dy = -DOOR_SCAN_RADIUS; dy <= DOOR_SCAN_RADIUS; dy++) {
            for (int dx = -DOOR_SCAN_RADIUS; dx <= DOOR_SCAN_RADIUS; dx++) {
                int tx = pTileX + dx;
                int tyWorld = pTileY + dy;

                if (tx < 0 || tx >= roomW || tyWorld < 0 || tyWorld >= roomH) continue;
                if (!isDoorWorld(tx, tyWorld, roomH)) continue;

                float doorX = tx * ts;
                float doorY = tyWorld * ts;

                float rx = doorX - DOOR_INTERACT_PAD;
                float ry = doorY - DOOR_INTERACT_PAD;
                float rw = ts + DOOR_INTERACT_PAD * 2f;
                float rh = ts + DOOR_INTERACT_PAD * 2f;

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
        return null;
    }

    // -------------------- Room transitions --------------------
    public void setRoom(Room newRoom) {
        this.room = newRoom;

        // clear transient stuff
        enemies.clear();
        meleeHitboxes.clear();
        damagePopups.clear();
        drops.clear();

        shrine = null;
        shrineOpen = false;
        choosingUpgrade = false;

        waveActive = false;
    }

    /** Call from Main when you enter a room and want the wave to start. */
    public void onEnterRoom(boolean shouldStartWave) {
        enemies.clear();
        meleeHitboxes.clear();
        damagePopups.clear();
        drops.clear();

        shrine = null;
        shrineOpen = false;
        choosingUpgrade = false;

        waveActive = false;

        if (shouldStartWave) startWave();
    }

    // -------------------- Restart --------------------
    public void restart() {
        gameOver = false;

        enemies.clear();
        meleeHitboxes.clear();
        damagePopups.clear();
        drops.clear();

        hitboxHits.clear();
        hitboxFreezeUsed.clear();
        hitboxHitSfxUsed.clear();
        hitboxShakeUsed.clear();

        freezeTimer = 0f;

        enemiesKilled = 0;
        wave = 1;
        waveActive = false;

        choosingUpgrade = false;
        shrineOpen = false;
        shrine = null;
        clearOfferedUpgrades();
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

            float s = speed;
            int hp = 3;
            if (items.has(ItemId.BLESSED_ASH) && rng.nextFloat() < 0.25f) {
                hp = 2;
                s = speed * 0.9f;
            }
            enemies.add(new Zombie(x, y, s, hbW, hbH, hp));
            return;
        }

        float[] open = findFirstOpenSpotRect(hbW, hbH, minDist);
        if (open != null) {
            if (!rectHitsCollision(room, open[0], open[1], hbW, hbH)) {
                float s = speed;
                int hp = 3;
                if (items.has(ItemId.BLESSED_ASH) && rng.nextFloat() < 0.25f) {
                    hp = 2;
                    s = speed * 0.9f;
                }
                enemies.add(new Zombie(open[0], open[1], s, hbW, hbH, hp));
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
            int cy = (roomH - 1) - ty; // match flip
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

    // -------------------- Drops --------------------
    private void updateDropPickups() {
        if (player == null) return;

        float px = player.getX(), py = player.getY();
        float pw = player.getWidth(), ph = player.getHeight();

        for (int i = drops.size() - 1; i >= 0; i--) {
            Drop d = drops.get(i);
            if (d == null) continue;

            if (overlaps(px, py, pw, ph, d.x, d.y, d.w, d.h)) {
                int gained = items.modifySouls(d.value);
                souls += gained;
                items.onSoulsPicked(this, gained);
                drops.remove(i);
            }
        }
    }

    // -------------------- Item helpers --------------------
    public void spawnRandomSoulDrop(int value) {
        if (room == null) return;
        float[] p = findFirstOpenSpotRect(12f, 12f, 0f);
        if (p == null) return;
        drops.add(new Drop(p[0], p[1], Math.max(1, value)));
    }

    public void spawnDamagePopup(Enemy enemy, int amount) {
        if (enemy == null) return;
        float popX = enemy.getX() + enemy.getWidth() * 0.5f;
        float popY = enemy.getY() + enemy.getHeight() + 10f;
        damagePopups.add(new DamagePopup(popX, popY, amount));
    }

    /** Find nearest enemy within radius of (x,y). Optionally ignore one enemy. */
    public Enemy findNearestEnemy(float x, float y, float radius, Enemy ignore) {
        if (enemies == null || enemies.isEmpty()) return null;
        float r2 = radius * radius;
        Enemy best = null;
        float bestD2 = r2;
        for (Enemy e : enemies) {
            if (e == null || e == ignore) continue;
            float ex = e.getX() + e.getWidth() * 0.5f;
            float ey = e.getY() + e.getHeight() * 0.5f;
            float dx = ex - x;
            float dy = ey - y;
            float d2 = dx * dx + dy * dy;
            if (d2 <= bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    // -------------------- Shrine (shop) --------------------
    private void maybeSpawnShrine() {
        if (rng.nextFloat() > 0.9f) return; // 10% chance

        float[] p = findFirstOpenSpotRect(32f, 32f, 0f);
        if (p == null) return;

        Upgrade[] stock = new Upgrade[] { randomUpgrade(), randomUpgrade(), randomUpgrade() };
        int[] costs = new int[] {
            costFor(stock[0]),
            costFor(stock[1]),
            costFor(stock[2])
        };
        shrine = new Shrine(p[0], p[1], stock, costs);
    }

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
        if (shrineInteractCooldown > 0f) return false;
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return false;
        if (!isPlayerNearShrine()) return false;

        openShrineMenu();
        return true; // prevents E from also using a door this frame
    }

    private void openShrineMenu() {
        shrineOpen = true;
        choosingUpgrade = true;

        // Copy shrine stock into the UI array the UI already draws
        for (int i = 0; i < offeredUpgrades.length; i++) {
            offeredUpgrades[i] = (shrine != null && i < shrine.stock.length) ? shrine.stock[i] : null;
        }
    }

    // UI calls this on click
    public void chooseUpgrade(int index) {
        if (!choosingUpgrade) return;
        if (index < 0 || index >= offeredUpgrades.length) return;

        // If we're in shrine mode, purchases use shrine rules
        if (shrineOpen && shrine != null) {
            buyShrineUpgrade(index);
            return;
        }

        // Fallback (if you ever re-enable wave-choice upgrades)
        Upgrade u = offeredUpgrades[index];
        if (u == null) return;
        applyUpgrade(u);
        choosingUpgrade = false;
        clearOfferedUpgrades();
    }

    /** Attempt to buy an upgrade from the shrine. Returns true on success. */
    public boolean buyShrineUpgrade(int index) {
        if (!shrineOpen || shrine == null) return false;
        if (index < 0 || index >= shrine.stock.length) return false;

        Upgrade u = shrine.stock[index];
        if (u == null) return false;

        int cost = shrine.getCost(index);
        if (cost <= 0) return false;
        if (souls < cost) return false;

        souls -= cost;
        applyUpgrade(u);

        // remove purchased item from shrine inventory
        shrine.stock[index] = null;

        // keep shop open so you can buy multiple items (Dead Cells-like)
        offeredUpgrades[index] = null;

        // auto-close if everything is bought out
        boolean anyLeft = false;
        for (int i = 0; i < shrine.stock.length; i++) {
            if (shrine.stock[i] != null) { anyLeft = true; break; }
        }
        if (!anyLeft) {
            closeShopMenu();
        }

        shrineInteractCooldown = 0.2f;
        return true;
    }

    private int costFor(Upgrade u) {
        // You can tune this however you want. Simple + readable defaults:
        // - baseline cost
        // - modest scaling by wave so later shrines feel meaningful
        int waveScale = Math.max(0, (wave - 1) / 3); // +1 every 3 waves
        int base = SHRINE_BASE_UPGRADE_COST + waveScale;

        // Small per-upgrade weight so "bigger" upgrades tend to cost a bit more.
        if (u == null) return base;
        if ("Vitality".equals(u.name)) return base + 1;
        if ("Extra Damage".equals(u.name)) return base + 1;
        return base;
    }

    // -------------------- Combat / collisions --------------------
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


        }
    }

    // -------------------- Dash / roll input --------------------
    private boolean isDashPressed() {
        return Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT);
    }

    /** Dead Cells-like: dash uses WASD direction if held, otherwise aim, otherwise facing. */
    private Vector2 getDashDirection() {
        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;

        Vector2 v;
        if (dx * dx + dy * dy > 0.0001f) {
            v = new Vector2(dx, dy).nor();
        } else {
            // fall back to aim direction (mouse)
            v = getAimDirection();
            if (v.len2() < 0.0001f) {
                // final fall back to facing
                Player.Facing f = player.getFacing();
                if (f == Player.Facing.UP) v.set(0f, 1f);
                else if (f == Player.Facing.DOWN) v.set(0f, -1f);
                else if (f == Player.Facing.LEFT) v.set(-1f, 0f);
                else v.set(1f, 0f);
            } else {
                v.nor();
            }
        }
        return v;
    }

    private void handleDashInput() {
        if (player == null) return;
        if (!isDashPressed()) return;

        Vector2 d = getDashDirection();
        // roll-cancel for snappy combat
        combat.cancelForDash();
        player.startDash(d.x, d.y);
        facePlayerToward(d);
    }

    private Vector2 getAimDirection() {
        Vector2 mouse = new Vector2(aimWorldX, aimWorldY);
        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;
        return mouse.sub(px, py);
    }

    private void facePlayerToward(Vector2 dir) {
        if (player == null) return;
        if (Math.abs(dir.x) > Math.abs(dir.y)) {
            player.setFacing(dir.x > 0 ? Player.Facing.RIGHT : Player.Facing.LEFT);
        } else {
            player.setFacing(dir.y > 0 ? Player.Facing.UP : Player.Facing.DOWN);
        }
    }

    // ✅ Called by PlayerCombat when hitbox should fire
    // Keep old signature for safety (defaults to strength 1)

    // Small helper inside GameWorld
    private static float clampf(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // Universal melee attack: caller supplies a direction (can be aim direction, locked dir, etc)
// strength controls hitstop/knockback feel
    void performMeleeAttackDir(float dirX, float dirY, float strength) {
        if (player == null) return;

        // Normalize direction (safe)
        Vector2 dir = new Vector2(dirX, dirY);
        if (dir.len2() < 0.0001f) dir.set(1f, 0f);
        dir.nor();

        // Player center
        float px = player.getX() + player.getWidth() * 0.5f;
        float py = player.getY() + player.getHeight() * 0.5f;

        // Attack shape/timing (tune these per attack later)
        float reach = 24f;
        float hitW = 64f;
        float hitH = 64f;
        float duration = 0.08f;

        int baseWeaponDamage = (weapon != null) ? weapon.getDamage() : 1;
        DamageType type = items.nextAttackDamageType();

        int damage = items.computeMainDamage(baseWeaponDamage);
        boolean crit = items.rollCrit(rng);
        if (crit) damage = items.applyCritMultiplier(damage);

        AttackHitbox hb = new AttackHitbox(hitW, hitH, dir, reach, duration, damage, px, py);
        hb.damageType = type;
        hb.crit = crit;

        // ✅ Strength controls "impact feel"
        hb.strength = Math.max(0.5f, strength);

        // ✅ Hitstop derived from strength (tune these)
        float base = 0.045f;                          // baseline hitstop
        float extra = 0.045f * (hb.strength - 1f);    // strength adds stop
        hb.hitStop = clampf(base + extra, 0.02f, 0.12f);

        meleeHitboxes.add(hb);
        hitboxHits.put(hb, new HashSet<>());

        hitboxFreezeUsed.put(hb, false);
        hitboxHitSfxUsed.put(hb, false);
        hitboxShakeUsed.put(hb, false);
    }

    
    // -------------------- Enemy death handling --------------------
    private void handleEnemyDeath(int index, Enemy enemy, DamageType causeType) {
        if (enemy == null) {
            if (index >= 0 && index < enemies.size()) enemies.remove(index);
            return;
        }

        float cx = enemy.getX() + enemy.getWidth() * 0.5f;
        float cy = enemy.getY() + enemy.getHeight() * 0.5f;

        // Particles first so we can sample enemy position before it disappears.
        particles.spawnEnemyDeath(cx, cy, causeType);

        // Drop a soul (simple baseline reward).
        drops.add(new Drop(cx - 6f, cy - 6f, 1));

        if (index >= 0 && index < enemies.size()) enemies.remove(index);
        enemiesKilled++;
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
                    // Main hit
                    enemy.takeDamage(hb.damage, hb.damageType);

                    // Particles: hit sparks at enemy center (color varies by DamageType; crit = juicier).
                    float hx = enemy.getX() + enemy.getWidth() * 0.5f;
                    float hy = enemy.getY() + enemy.getHeight() * 0.5f;
                    particles.spawnHit(hx, hy, hb.dir.x, hb.dir.y, hb.crit, hb.damageType);
                    // Tell combat system we connected a hit (for hit-confirm cancels/recovery)
                    combat.notifyHitConfirmed();
                    alreadyHit.add(enemy);

                    spawnDamagePopup(enemy, hb.damage);

                    // On-hit item effects (may apply extra damage/status)
                    int baseWeaponDamage = (weapon != null) ? weapon.getDamage() : 1;
                    items.onHitEnemy(this, enemy, hb.crit, baseWeaponDamage);

                    enemy.takeKnockback(hb.dir.x, hb.dir.y, 400f);
                    float stun = stunFromStrength(hb.strength);
                    enemy.applyHitstun(stun);

                    if (enemy.isDead()) {
                        handleEnemyDeath(e, enemy, hb.damageType);
                    }

if (!sfxUsed) {
                        if (audio != null) audio.playHit();
                        hitboxHitSfxUsed.put(hb, true);
                    }

                    if (!freezeUsed) {
                        // ✅ scale hitstop by attack strength
                        freezeTimer = Math.max(freezeTimer, hb.hitStop);
                        hitboxFreezeUsed.put(hb, true);
                    }


                    if (!shakeUsed) {
                        if (shake != null) {
                            float s = hb.strength;
                            float inten = HIT_SHAKE_INTENSITY * s;
                            float dur = clampf(HIT_SHAKE_DURATION + 0.02f * (s - 1f), 0.06f, 0.18f);
                            shake.addShake(inten, dur);
                        }
                        hitboxShakeUsed.put(hb, true);
                    }
                }
            }
        }
    }

    // -------------------- Upgrades / applying --------------------
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
    }

    private void closeShopMenu() {
        shrineOpen = false;
        choosingUpgrade = false;
        clearOfferedUpgrades();
    }

    private static float stunFromStrength(float strength) {
        float base = 0.08f;                    // light hitstun
        float extra = 0.05f * (strength - 1f); // heavier hits stun more
        return clampf(base + extra, 0.05f, 0.18f);
    }

    // -------------------- Dispose --------------------
    public void dispose() {
        if (particles != null) particles.dispose();
        cardTexture.dispose();

        healthUpgradeSheet.dispose();
        damageUpgradeSheet.dispose();
        fireRateUpgradeSheet.dispose();
        movementUpgradeSheet.dispose();
    }
}
