package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
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
    private float attackCooldown = 0f;        // kept for compatibility (not used if weapon != null)
    private float attackCooldownTime = 0.25f; // kept for compatibility (not used if weapon != null)

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

    /**
     * Tracks which enemies have already been hit by a given hitbox.
     * IdentityHashMap ensures we key by the specific hitbox instance.
     */
    private final IdentityHashMap<AttackHitbox, HashSet<Enemy>> hitboxHits = new IdentityHashMap<>();

    // -------------------- Freeze Frames (Hit Stop) --------------------
    private float freezeTimer = 0f;
    private final float FREEZE_DURATION = 0.08f;

    /**
     * Freeze once per attack/hitbox.
     */
    private final IdentityHashMap<AttackHitbox, Boolean> hitboxFreezeUsed = new IdentityHashMap<>();

    /**
     * Gate HIT audio once per attack/hitbox (even if it hits multiple enemies).
     */
    private final IdentityHashMap<AttackHitbox, Boolean> hitboxHitSfxUsed = new IdentityHashMap<>();

    /**
     * Gate SCREEN SHAKE once per attack/hitbox.
     */
    private final IdentityHashMap<AttackHitbox, Boolean> hitboxShakeUsed = new IdentityHashMap<>();

    // -------------------- Screen shake callback --------------------
    public interface ScreenShake {
        void addShake(float intensity, float duration);
    }

    private ScreenShake shake;

    public void setScreenShake(ScreenShake shake) {
        this.shake = shake;
    }

    // Shake tuning
    private static final float HIT_SHAKE_INTENSITY = 6f;
    private static final float HIT_SHAKE_DURATION  = 0.12f;

    private AudioManager audio;

    public GameWorld(Room room, Player player, SpriteBatch spriteBatch) {
        this.room = room;
        this.player = player;
        this.spriteBatch = spriteBatch;
        restart();
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
        // Restart
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart();
            return;
        }

        if (gameOver) return;

        // Freeze frames: pause the world update while timer is active
        if (freezeTimer > 0f) {
            freezeTimer -= delta;
            if (freezeTimer < 0f) freezeTimer = 0f;
            return;
        }

        // Pause world while choosing upgrades
        if (choosingUpgrade) {
            handleUpgradeInput();
            return;
        }

        // Safety
        if (player == null) {
            ensurePlayer();
            if (player == null) return;
        }

        // Update player
        player.update(room, room.getTileSize());
        player.updateTimers(delta);
        if (weapon != null) weapon.updateTimers(delta);

        // Update enemies
        for (Enemy e : enemies) {
            if (e == null) continue;
            e.update(player, room, room.getTileSize());
        }

        // Combat
        handlePlayerEnemyContact();
        updateMeleeHitboxes(delta);

        // Attack input
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && weapon != null) {
            Vector2 aimDir = getAimDirection(); // mouseWorld - playerCenter
            facePlayerToward(aimDir);

            if (weapon.isOnCooldown()) {
                player.startDash(aimDir.x, aimDir.y);
                weapon.startAttack(delta);

                player.startAttackLock(0.25f);
                performMeleeAttack(getAimWorld());
            }
        }

        // Update damage popups
        for (int i = damagePopups.size() - 1; i >= 0; i--) {
            DamagePopup p = damagePopups.get(i);
            p.update(delta);
            if (p.isDead()) damagePopups.remove(i);
        }

        // Wave management
        if (!waveActive) startWave();

        if (waveActive && enemies.isEmpty()) {
            waveActive = false;
            wave++;
            beginUpgradeChoice();
        }

        // Game over
        if (player.getHealth() <= 0) {
            gameOver = true;
        }
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

        // Reset freeze
        freezeTimer = 0f;

        // Reset combat tuning
        attackCooldown = 0f;
        attackCooldownTime = 0.25f;

        if (weapon != null) weapon.setAttackCooldown(0f);

        bulletSpeed = 240f;
        bulletSize = 8f;
        bulletDamage = 1;

        // Difficulty reset
        spawnTimer = 0f;
        difficultyTimer = 0f;
        spawnInterval = startSpawnInterval;
        minSpawnInterval = startMinSpawnInterval;
        maxEnemies = startMaxEnemies;

        // Run stats
        enemiesKilled = 0;
        wave = 1;
        waveActive = false;

        // Upgrades
        choosingUpgrade = false;
        clearOfferedUpgrades();
    }

    private void ensurePlayer() {
        if (player != null) return;
        player = new Player(60, 60, 140f, 24f, 24f);
    }

    private void ensurePlayerFresh() {
        if (player == null) {
            ensurePlayer();
            return;
        }
        float w = player.getWidth();
        float h = player.getHeight();
        float spd = player.getSpeed();
        player = new Player(60, 60, spd, w, h);
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

        int tileSize = room.getTileSize();
        float roomPixelW = room.getRoomWidth() * tileSize;
        float roomPixelH = room.getRoomHeight() * tileSize;

        final float hbW = 28f;
        final float hbH = 58f;

        for (int tries = 0; tries < 200; tries++) {
            float x = rng.nextFloat() * (roomPixelW - hbW);
            float y = rng.nextFloat() * (roomPixelH - hbH);

            float px = player.getX() + player.getWidth() / 2f;
            float py = player.getY() + player.getHeight() / 2f;
            float ex = x + hbW / 2f;
            float ey = y + hbH / 2f;

            float dx = ex - px;
            float dy = ey - py;

            float minDist = 120f;
            if (dx * dx + dy * dy < minDist * minDist) continue;

            if (rectHitsWall(x, y, hbW, hbH)) continue;

            enemies.add(new Zombie(x, y, speed, hbW, hbH, 3));
            return;
        }

        float[] open = findFirstOpenSpotRect(hbW, hbH, 120f);
        if (open != null) {
            enemies.add(new Zombie(open[0], open[1], speed, hbW, hbH, 3));
        }
    }

    // -------------------- Combat (Cursor Aim) --------------------
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
                player.clampToScreen();
            }
        }
    }

    /**
     * Spawns a hitbox that follows the player for its lifetime (Option B).
     * This hitbox can hit multiple enemies, but each enemy only once per swing.
     */
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

    /**
     * Updates hitboxes, keeps them aligned to player, and applies damage to all
     * overlapping enemies (each enemy at most once per hitbox).
     *
     * Freeze frames trigger ONCE per hitbox when the first enemy is hit,
     * even if multiple enemies are hit in the same swing.
     *
     * Hit audio triggers ONCE per hitbox when the first enemy is hit.
     * Screen shake triggers ONCE per hitbox when the first enemy is hit.
     */
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

                    damagePopups.add(new DamagePopup(enemy.getX(), enemy.getY(), hb.damage));
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

    // -------------------- Upgrades --------------------
    private void beginUpgradeChoice() {
        choosingUpgrade = true;
        generateOfferedUpgrades();
    }

    private void handleUpgradeInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) applyUpgrade(offeredUpgrades[0]);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) applyUpgrade(offeredUpgrades[1]);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) applyUpgrade(offeredUpgrades[2]);
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
        int r = rng.nextInt(5);
        if (r == 0) return new Upgrade("Rapid Fire", "Fire rate +20%", cardTexture);
        if (r == 1) return new Upgrade("Runner", "Move speed +15%", cardTexture);
        if (r == 2) return new Upgrade("Vitality", "Max HP +1 and heal 1", cardTexture);
        if (r == 3) return new Upgrade("Extra Damage", "Bullet damage +1", cardTexture);
        return new Upgrade("Projectile Speed", "Bullet speed +20%", cardTexture);
    }

    private void applyUpgrade(Upgrade u) {
        if (u == null || player == null) return;

        if (u.name.equals("Rapid Fire")) {
            if (weapon != null) {
                weapon.setAttackCooldownTime(Math.max(0.05f, weapon.getAttackCooldownTime() * 0.8f));
            } else {
                attackCooldownTime = Math.max(0.05f, attackCooldownTime * 0.8f);
            }
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

    // -------------------- Helpers --------------------
    private boolean overlaps(float ax, float ay, float aw, float ah,
                             float bx, float by, float bw, float bh) {
        return ax < bx + bw &&
            ax + aw > bx &&
            ay < by + bh &&
            ay + ah > by;
    }

    private boolean rectHitsWall(float x, float y, float w, float h) {
        int[][] grid = room.getRoom();
        int tileSize = room.getTileSize();

        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        int left = clamp((int) (x / tileSize), 0, roomW - 1);
        int right = clamp((int) ((x + w - 1) / tileSize), 0, roomW - 1);
        int bottom = clamp((int) (y / tileSize), 0, roomH - 1);
        int top = clamp((int) ((y + h - 1) / tileSize), 0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            for (int tx = left; tx <= right; tx++) {
                int t = grid[ty][tx];
                if (t == 1 || t == 2 || t == 3 || t == 4 || t == 5) return true;
            }
        }
        return false;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public int getCoins() { return coins; }
    public int getSouls() { return souls; }

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

    public void dispose() {
        cardTexture.dispose();
        Zombie.disposeShared();
    }
}
