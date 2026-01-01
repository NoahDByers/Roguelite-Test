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
    private final Room room;

    private SpriteBatch spriteBatch;
    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Bullet> bullets = new ArrayList<>();

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

    // Difficulty (optional; left in for future)
    private float spawnTimer = 0f;
    private float difficultyTimer = 0f;

    private final float startSpawnInterval = 1.0f;
    private final float startMinSpawnInterval = 0.5f;
    private final int startMaxEnemies = 20;

    private float spawnInterval = startSpawnInterval;
    private float minSpawnInterval = startMinSpawnInterval;
    private int maxEnemies = startMaxEnemies;

    // Fire rate
    private float attackCooldown = 0f;
    private float attackCooldownTime = 0.25f;

    // Current Weapon
    private Weapon weapon;

    // Bullet tuning
    private float bulletSpeed = 240f;
    private float bulletSize = 8f;
    private int bulletDamage = 1;

    private final ArrayList<DamagePopup> damagePopups = new ArrayList<>();

    private final Texture cardTexture = new Texture("ui/upgrade_card.png");
    private final Random rng = new Random();

    // Melee hitboxes
    private final ArrayList<AttackHitbox> meleeHitboxes = new ArrayList<>();

    private final IdentityHashMap<AttackHitbox, HashSet<Enemy>> hitboxHits = new IdentityHashMap<>();

    // -------------------- Freeze Frames (Hit Stop) --------------------
    private float freezeTimer = 0f;
    private final float FREEZE_DURATION = 0.08f;

    private final IdentityHashMap<AttackHitbox, Boolean> hitboxFreezeUsed = new IdentityHashMap<>();
    private final IdentityHashMap<AttackHitbox, Boolean> hitboxHitSfxUsed = new IdentityHashMap<>();
    private final IdentityHashMap<AttackHitbox, Boolean> hitboxShakeUsed = new IdentityHashMap<>();

    // Upgrade animations
    private Texture healthUpgradeSheet = new Texture("ui/healthUpgrade.png");
    private Texture damageUpgradeSheet = new Texture("ui/damageUpgrade.png");
    private Texture fireRateUpgradeSheet = new Texture("ui/fireRateUpgrade.png");
    private Texture movementUpgradeSheet = new Texture("ui/movementUpgrade.png");

    ArrayList<TextureRegion> healthUpgrade;
    ArrayList<TextureRegion> damageUpgrade;
    ArrayList<TextureRegion> fireRateUpgrade;
    ArrayList<TextureRegion> movementUpgrade;

    // -------------------- Screen shake callback --------------------
    public interface ScreenShake {
        void addShake(float intensity, float duration);
    }

    private ScreenShake shake;

    public void setScreenShake(ScreenShake shake) {
        this.shake = shake;
    }

    private static final float HIT_SHAKE_INTENSITY = 6f;
    private static final float HIT_SHAKE_DURATION  = 0.12f;

    private AudioManager audio;

    // -------------------- Collision rules --------------------
    private static final int COLLISION_SOLID = 76; // your collision map uses 76 for walls/solids

    public GameWorld(Room room, Player player, SpriteBatch spriteBatch) {
        this.room = room;
        this.player = player;
        this.spriteBatch = spriteBatch;
        restart();

        healthUpgrade = new ArrayList<>();
        damageUpgrade = new ArrayList<>();
        fireRateUpgrade = new ArrayList<>();
        movementUpgrade = new ArrayList<>();

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
    }

    // -------------------- Getters --------------------
    public Room getRoom() { return room; }
    public Player getPlayer() { return player; }
    public ArrayList<Enemy> getEnemies() { return enemies; }
    public ArrayList<Bullet> getBullets() { return bullets; }

    public boolean isGameOver() { return gameOver; }
    public boolean isChoosingUpgrade() { return choosingUpgrade; }

    public int getEnemiesKilled() { return enemiesKilled; }
    public int getWave() { return wave; }

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

    // -------------------- Update loop --------------------
    public void update(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart();
            return;
        }
        if (gameOver) return;

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

        // NOTE: Player.update(...) must be collision-aware too.
        // If Player.update uses room.getTile(...) for collisions, you'll want it to use room.getCollisions().
        player.update(room, room.getTileSize());
        player.updateTimers(delta);
        if (weapon != null) weapon.updateTimers(delta);

        for (Enemy e : enemies) {
            if (e == null) continue;
            e.update(player, room, room.getTileSize());
        }

        handlePlayerEnemyContact();
        updateMeleeHitboxes(delta);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && weapon != null) {
            Vector2 aimDir = getAimDirection();
            facePlayerToward(aimDir);

            if (weapon.isOnCooldown()) {
                player.startDash(aimDir.x, aimDir.y);
                weapon.startAttack(delta);
                if (audio != null) audio.playSwordHit();

                player.startAttackLock(0.25f);
                performMeleeAttack(getAimWorld());
            }
        }

        for (int i = damagePopups.size() - 1; i >= 0; i--) {
            DamagePopup p = damagePopups.get(i);
            p.update(delta);
            if (p.isDead()) damagePopups.remove(i);
        }

        if (!waveActive) startWave();

        if (waveActive && enemies.isEmpty()) {
            waveActive = false;
            wave++;
            beginUpgradeChoice();
        }

        if (player.getHealth() <= 0) gameOver = true;
    }

    // -------------------- Restart --------------------
    public void restart() {
        gameOver = false;

        ensurePlayerFresh();

        enemies.clear();
        bullets.clear();

        meleeHitboxes.clear();
        hitboxHits.clear();
        hitboxFreezeUsed.clear();
        hitboxHitSfxUsed.clear();
        hitboxShakeUsed.clear();

        freezeTimer = 0f;

        attackCooldown = 0f;
        attackCooldownTime = 0.25f;

        if (weapon != null) weapon.setAttackCooldown(0f);

        bulletSpeed = 240f;
        bulletSize = 8f;
        bulletDamage = 1;

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

        int toSpawn = 5 + wave;
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

        // Zombie hitbox
        final float hbW = 28f;
        final float hbH = 58f;

        // Keep away from player
        final float minDist = 120f;
        final float minDist2 = minDist * minDist;

        // How much "breathing room" around walls we require
        // (prevents spawning hugging a wall and instantly colliding)
        final float padding = 2f;

        // Random tries
        for (int tries = 0; tries < 400; tries++) {
            float x = rng.nextFloat() * (roomPixelW - hbW);
            float y = rng.nextFloat() * (roomPixelH - hbH);

            // Distance gate
            float px = player.getX() + player.getWidth() * 0.5f;
            float py = player.getY() + player.getHeight() * 0.5f;
            float ex = x + hbW * 0.5f;
            float ey = y + hbH * 0.5f;

            float dx = ex - px;
            float dy = ey - py;
            if (dx * dx + dy * dy < minDist2) continue;

            // Collision gate (uses collision layer: 76 = solid)
            if (rectHitsCollision(room, x + padding, y + padding, hbW - padding * 2f, hbH - padding * 2f)) {
                continue;
            }

            enemies.add(new Zombie(x, y, speed, hbW, hbH, 3));
            return;
        }

        // Fallback: deterministic search
        float[] open = findFirstOpenSpotRect(hbW, hbH, minDist);
        if (open != null) {
            // Final safety check
            if (!rectHitsCollision(room, open[0], open[1], hbW, hbH)) {
                enemies.add(new Zombie(open[0], open[1], speed, hbW, hbH, 3));
            }
        }
    }

    /**
     * Checks the room COLLISION LAYER only.
     * Treats value 76 as solid (collision).
     *
     * IMPORTANT: This maps world Y -> collision array Y using flip:
     * collisionRow = (roomH - 1) - worldTileY
     * because your rendering uses that same flip to match visuals.
     */
    private boolean rectHitsCollision(Room room, float x, float y, float w, float h) {
        int[][] col;
        try { col = room.getCollisions(); }
        catch (Throwable t) { col = null; }

        if (col == null) return false;

        final int tileSize = room.getTileSize();
        final int roomW = room.getRoomWidth();
        final int roomH = room.getRoomHeight();

        // Compute covered tile range (inclusive)
        int left   = (int)Math.floor(x / tileSize);
        int right  = (int)Math.floor((x + w - 1f) / tileSize);
        int bottom = (int)Math.floor(y / tileSize);
        int top    = (int)Math.floor((y + h - 1f) / tileSize);

        // Clamp to bounds
        left   = clamp(left,   0, roomW - 1);
        right  = clamp(right,  0, roomW - 1);
        bottom = clamp(bottom, 0, roomH - 1);
        top    = clamp(top,    0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            // WORLD tile Y -> collision array Y
            int cy = (roomH - 1) - ty;

            for (int tx = left; tx <= right; tx++) {
                if (col[cy][tx] == 76) return true;
            }
        }

        return false;
    }
    // -------------------- Collision helpers (FIXED) --------------------
    private boolean rectHitsWall(float x, float y, float w, float h) {
        if (room == null) return true;

        int[][] col = null;
        try { col = room.getCollisions(); } catch (Throwable ignored) {}

        // If collisions layer isn't present, fail safe (treat as no collision)
        // Change to `return true;` if you'd rather block everything until the layer exists.
        if (col == null) return false;

        int tileSize = room.getTileSize();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        int left   = clamp((int)Math.floor(x / tileSize), 0, roomW - 1);
        int right  = clamp((int)Math.floor((x + w - 1f) / tileSize), 0, roomW - 1);
        int bottom = clamp((int)Math.floor(y / tileSize), 0, roomH - 1);
        int top    = clamp((int)Math.floor((y + h - 1f) / tileSize), 0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            for (int tx = left; tx <= right; tx++) {
                if (col[ty][tx] == COLLISION_SOLID) return true; // ✅ 76 blocks
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

                if (rectHitsWall(x, y, w, h)) continue;

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

    // -------------------- Combat / upgrades etc (unchanged) --------------------
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

    private void handleUpgradeInput() {}

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
        return new Upgrade("Extra Damage", "Bullet damage +1", damageUpgrade);
    }

    public void setAimWorld(float x, float y) {
        aimWorldX = x;
        aimWorldY = y;
    }

    public Vector2 getAimWorld() {
        return new Vector2(aimWorldX, aimWorldY);
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

    public void chooseUpgrade(int index) {
        if (!choosingUpgrade) return;
        if (index < 0 || index >= offeredUpgrades.length) return;
        applyUpgrade(offeredUpgrades[index]);
    }

    private void applyUpgrade(Upgrade u) {
        if (u == null || player == null) return;

        if (u.name.equals("Rapid Fire")) {
            if (weapon != null) weapon.setAttackCooldownTime(Math.max(0.05f, weapon.getAttackCooldownTime() * 0.8f));
            else attackCooldownTime = Math.max(0.05f, attackCooldownTime * 0.8f);
        } else if (u.name.equals("Runner")) {
            player.setSpeed(player.getSpeed() * 1.15f);
        } else if (u.name.equals("Vitality")) {
            player.increaseMaxHealth(1);
            player.heal(1);
        } else if (u.name.equals("Extra Damage")) {
            bulletDamage += 1;
        } else if (u.name.equals("Projectile Speed")) {
            bulletSpeed *= 1.2f;
        }

        choosingUpgrade = false;
        clearOfferedUpgrades();
    }

    public int getCoins() { return coins; }
    public int getSouls() { return souls; }

    // NOTE: I left melee hitbox code as-is (unchanged from your file)
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

                if (overlaps(hb.rect.x, hb.rect.y, hb.rect.width, hb.rect.height,
                    enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {

                    enemy.takeDamage(hb.damage);
                    alreadyHit.add(enemy);

                    damagePopups.add(new DamagePopup(enemy.getX(), enemy.getY() + enemy.getHeight(), hb.damage));
                    enemy.takeKnockback(hb.dir.x, hb.dir.y, 400f);

                    if (enemy.isDead()) {
                        enemies.remove(e);
                        enemiesKilled++;
                    }

                    // Gate hit SFX once per attack/hitbox
                    if (!sfxUsed) {
                        if (audio != null) audio.playHit();
                        sfxUsed = true;
                        hitboxHitSfxUsed.put(hb, true);
                    }

                    // Gate freeze once per attack/hitbox
                    if (!freezeUsed) {
                        freezeTimer = FREEZE_DURATION;
                        freezeUsed = true;
                        hitboxFreezeUsed.put(hb, true);
                    }

                    // Gate screen shake once per attack/hitbox
                    if (!shakeUsed) {
                        if (shake != null) shake.addShake(HIT_SHAKE_INTENSITY, HIT_SHAKE_DURATION);
                        shakeUsed = true;
                        hitboxShakeUsed.put(hb, true);
                    }
                }
            }
        }
    }

    public void dispose() {
        cardTexture.dispose();
        Zombie.disposeShared();

        // optional (recommended)
        healthUpgradeSheet.dispose();
        damageUpgradeSheet.dispose();
        fireRateUpgradeSheet.dispose();
        movementUpgradeSheet.dispose();
    }
}
