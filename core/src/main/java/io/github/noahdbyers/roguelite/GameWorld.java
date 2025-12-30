package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.Random;

/**
 * Cleaned-up GameWorld:
 * - Removes the missing Weapon dependency (no Weapon.java needed).
 * - Never relies on uninitialized offeredUpgrades (generates them when a wave clears).
 * - Exposes offered upgrades via getter so UI can display the SAME choices the world will apply.
 * - Safe restart that always gives you a valid Player and resets run state.
 * - Bullet damage/speed are actually tracked (simple and extensible).
 */
public class GameWorld {
    // Room / world
    private final Room room;

    // Entities
    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Bullet> bullets = new ArrayList<>();

    // Run stats / progression
    private boolean gameOver = false;
    private int enemiesKilled = 0;
    private int coins = 0;
    private int souls = 0;
    private int wave = 1;
    private boolean waveActive = false;

    // Upgrades
    private boolean choosingUpgrade = false;
    private final Upgrade[] offeredUpgrades = new Upgrade[3];

    // Difficulty (optional spawn-over-time system)
    private float spawnTimer = 0f;
    private float difficultyTimer = 0f;

    private final float startSpawnInterval = 1.0f;
    private final float startMinSpawnInterval = 0.5f;
    private final int startMaxEnemies = 20;

    private float spawnInterval = startSpawnInterval;
    private float minSpawnInterval = startMinSpawnInterval;
    private int maxEnemies = startMaxEnemies;

    // Auto-fire (replaces Weapon)
    private float attackCooldown = 0f;
    private float attackCooldownTime = 0.25f; // seconds per shot

    // Bullet tuning
    private float bulletSpeed = 240f;
    private float bulletSize = 8f;
    private int bulletDamage = 1;

    Texture cardTexture = new Texture("upgrade_card.png");

    // RNG
    private final Random rng = new Random();

    public GameWorld(Room room, Player player) {
        this.room = room;
        this.player = player; // may be null; restart() will ensure it isn't
        restart();
    }

    // -------------------- Public getters (Main/UI render from these) --------------------
    public Room getRoom() { return room; }
    public Player getPlayer() { return player; }
    public ArrayList<Enemy> getEnemies() { return enemies; }
    public ArrayList<Bullet> getBullets() { return bullets; }

    public boolean isGameOver() { return gameOver; }
    public boolean isChoosingUpgrade() { return choosingUpgrade; }

    public int getEnemiesKilled() { return enemiesKilled; }
    public int getWave() { return wave; }

    /** UI should read these to display the 3 choices currently being offered. */
    public Upgrade[] getOfferedUpgrades() { return offeredUpgrades; }

    // -------------------- Update loop --------------------
    public void update(float delta) {
        // Restart works anytime
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart();
            return;
        }

        if (gameOver) return;

        // If we’re choosing upgrades, pause simulation until selection
        if (choosingUpgrade) {
            handleUpgradeInput();
            return;
        }

        // Safety: player should always exist after restart, but keep this guard
        if (player == null) {
            ensurePlayer();
            if (player == null) return;
        }

        // Normal simulation
        player.update(room, room.getTileSize());
        player.updateTimers(delta);

        for (Enemy e : enemies) {
            if (e == null) continue;
            e.update(player, room, room.getTileSize());
        }

        // Contact damage
        handlePlayerEnemyContact();

        // Auto-fire
        tryShootAutoAim(delta);

        // Bullets update / collisions
        updateBullets();

        // Wave management
        if (!waveActive) {
            startWave();
        }

        // Wave cleared -> offer upgrades
        if (waveActive && enemies.isEmpty()) {
            waveActive = false;
            wave++; // next wave number
            beginUpgradeChoice();
        }

        // Game over condition
        if (player.getHealth() <= 0) {
            gameOver = true;
        }

        // If you want “spawn over time” inside waves, you can call:
        // updateSpawning(delta);
    }

    // -------------------- Restart --------------------
    public void restart() {
        gameOver = false;

        // Ensure player exists and is "fresh"
        // (Player has no setHealth(), so the simplest reliable reset is to recreate it.)
        ensurePlayerFresh();

        // Enemies
        enemies.clear();
        bullets.clear();

        // Reset combat tuning
        attackCooldown = 0f;
        attackCooldownTime = 0.25f;
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
        // Reasonable defaults; adjust if you prefer different starting stats/sizes
        player = new Player(60, 60, 140f, 24f, 24f);
    }

    private void ensurePlayerFresh() {
        // Recreate to reset health to its constructor defaults (5/5 in your Player class).
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

        int toSpawn = 8 + wave;
        float baseSpeed = 80f + wave * 8f;

        for (int i = 0; i < toSpawn; i++) {
            spawnEnemyWithSpeed(baseSpeed);
        }

        waveActive = true;
    }

    private void spawnEnemyWithSpeed(float speed) {
        if (enemies.size() >= maxEnemies) return;
        if (player == null) ensurePlayer();
        if (room == null) return;

        int tileSize = room.getTileSize();

        // Use room pixel bounds NOT Gdx.graphics.getWidth/Height so this matches your virtual room
        float roomPixelW = room.getRoomWidth() * tileSize;
        float roomPixelH = room.getRoomHeight() * tileSize;

        // Try a bunch of random positions
        for (int tries = 0; tries < 200; tries++) {
            float size = 22f + rng.nextFloat() * 10f;

            float x = rng.nextFloat() * (roomPixelW - size);
            float y = rng.nextFloat() * (roomPixelH - size);

            // Don’t spawn too close to player
            float px = player.getX() + player.getWidth() / 2f;
            float py = player.getY() + player.getHeight() / 2f;
            float ex = x + size / 2f;
            float ey = y + size / 2f;

            float dx = ex - px;
            float dy = ey - py;
            float minDist = 120f;
            if (dx * dx + dy * dy < minDist * minDist) continue;

            // NEW: reject if enemy overlaps any wall tiles
            if (rectHitsWall(x, y, size, size)) continue;

            enemies.add(new Enemy(x, y, speed, size, 3));
            return;
        }

        // Fallback: scan for first valid open tile
        float size = 28f;
        float[] open = findFirstOpenSpot(size, 120f);
        if (open != null) {
            enemies.add(new Enemy(open[0], open[1], speed, size, 3));
        }
        // else: no spawn this time (room is too full / blocked)
    }


    @SuppressWarnings("unused")
    private void updateSpawning(float delta) {
        difficultyTimer += delta;
        if (difficultyTimer >= 5f) {
            difficultyTimer = 0f;
            spawnInterval = Math.max(minSpawnInterval, spawnInterval - 0.1f);
        }

        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnEnemyWithSpeed(100f);
        }
    }

    // -------------------- Combat --------------------
    private void tryShootAutoAim(float delta) {
        attackCooldown -= delta;
        if (attackCooldown > 0f) return;

        Enemy target = getNearestEnemy();
        if (target == null) return;

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;
        float ex = target.getX() + target.getWidth() / 2f;
        float ey = target.getY() + target.getHeight() / 2f;

        float dirX = ex - px;
        float dirY = ey - py;

        bullets.add(new Bullet(px, py, dirX, dirY, bulletSpeed, bulletSize));
        attackCooldown = attackCooldownTime;
    }

    private void updateBullets() {
        if (room == null) return;

        int[][] grid = room.getRoom();
        int tileSize = room.getTileSize();

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (b == null) {
                bullets.remove(i);
                continue;
            }

            b.update();

            // Remove if hits wall tile
            if (b.collidesWithRoom(grid, tileSize)) {
                bullets.remove(i);
                continue;
            }

            // Remove if off screen
            if (b.isOffScreen()) {
                bullets.remove(i);
                continue;
            }

            // Bullet-enemy collisions
            boolean hitEnemy = false;
            for (int e = enemies.size() - 1; e >= 0; e--) {
                Enemy enemy = enemies.get(e);
                if (enemy == null) {
                    enemies.remove(e);
                    continue;
                }

                if (overlaps(b.getX(), b.getY(), b.getWidth(), b.getHeight(),
                    enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {

                    enemy.takeDamage(bulletDamage);
                    hitEnemy = true;

                    if (enemy.isDead()) {
                        enemies.remove(e);
                        enemiesKilled++;
                    }
                    break;
                }
            }

            if (hitEnemy) {
                bullets.remove(i);
            }
        }
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

                // Small knockback so you don't "stick"
                float px = player.getX() + player.getWidth() / 2f;
                float py = player.getY() + player.getHeight() / 2f;
                float ex = enemy.getX() + enemy.getWidth() / 2f;
                float ey = enemy.getY() + enemy.getHeight() / 2f;

                float dx = px - ex;
                float dy = py - ey;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len != 0f) { dx /= len; dy /= len; }

                float push = 8f;
                player.setX(player.getX() + dx * push);
                player.setY(player.getY() + dy * push);
                player.clampToScreen();
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
        // Simple: allow duplicates, but avoid nulls.
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
        // Names here must match applyUpgrade() checks
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
            attackCooldownTime = Math.max(0.05f, attackCooldownTime * 0.8f);
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
    private Enemy getNearestEnemy() {
        if (player == null) return null;

        Enemy best = null;
        float bestDist2 = Float.MAX_VALUE;

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        for (Enemy e : enemies) {
            if (e == null) continue;

            float ex = e.getX() + e.getWidth() / 2f;
            float ey = e.getY() + e.getHeight() / 2f;

            float dx = ex - px;
            float dy = ey - py;
            float dist2 = dx * dx + dy * dy;

            if (dist2 < bestDist2) {
                bestDist2 = dist2;
                best = e;
            }
        }
        return best;
    }

    private boolean overlaps(
        float ax, float ay, float aw, float ah,
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

        // Compute tile indices covered by the rect
        int left = clamp((int)(x / tileSize), 0, roomW - 1);
        int right = clamp((int)((x + w - 1) / tileSize), 0, roomW - 1);
        int bottom = clamp((int)(y / tileSize), 0, roomH - 1);
        int top = clamp((int)((y + h - 1) / tileSize), 0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            for (int tx = left; tx <= right; tx++) {
                if (grid[ty][tx] == 1) return true;
            }
        }
        return false;
    }

    private float[] findFirstOpenSpot(float size, float minDistFromPlayer) {
        int tileSize = room.getTileSize();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        // scan interior tiles (skip borders)
        for (int ty = 1; ty < roomH - 1; ty++) {
            for (int tx = 1; tx < roomW - 1; tx++) {
                if (room.getTile(tx, ty) == 1) continue;

                float x = tx * tileSize + (tileSize - size) / 2f;
                float y = ty * tileSize + (tileSize - size) / 2f;

                if (rectHitsWall(x, y, size, size)) continue;

                float ex = x + size / 2f;
                float ey = y + size / 2f;
                float dx = ex - px;
                float dy = ey - py;

                if (dx * dx + dy * dy < minDistFromPlayer * minDistFromPlayer) continue;

                return new float[]{x, y};
            }
        }
        return null;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public int getCoins() { return coins; }
    public int getSouls() { return souls; }
}
